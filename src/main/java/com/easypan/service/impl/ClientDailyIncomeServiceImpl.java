package com.easypan.service.impl;

import com.easypan.entity.dto.BatchProcessResult;
import com.easypan.entity.dto.TableProcessResult;
import com.easypan.entity.po.ClientDailyIncome;
import com.easypan.entity.query.ClientDailyIncomeQuery;
import com.easypan.entity.query.SimplePage;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.entity.enums.PageSize;
import com.easypan.exception.BusinessException;
import com.easypan.mappers.ClientDailyIncomeMapper;
import com.easypan.service.ClientDailyIncomeService;
import com.easypan.service.ClientIncomeCalculationService;
import com.easypan.service.ClientIncomeSummaryService;
import com.easypan.utils.PubRestLessThreadExcutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 客户每日收益Service实现
 */
@Service("clientDailyIncomeService")
public class ClientDailyIncomeServiceImpl implements ClientDailyIncomeService {

    private static final Logger logger = LoggerFactory.getLogger(ClientDailyIncomeServiceImpl.class);

    @Resource
    private ClientDailyIncomeMapper clientDailyIncomeMapper;

    @Resource
    private ClientIncomeCalculationService calculationService;

    @Resource
    private ClientIncomeSummaryService summaryService;

    @Override
    public List<ClientDailyIncome> findListByParam(ClientDailyIncomeQuery param) {
        return this.clientDailyIncomeMapper.selectList(param);
    }

    @Override
    public Integer findCountByParam(ClientDailyIncomeQuery param) {
        return this.clientDailyIncomeMapper.selectCount(param);
    }

    @Override
    public PaginationResultVO<ClientDailyIncome> findListByPage(ClientDailyIncomeQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<ClientDailyIncome> list = this.findListByParam(param);
        PaginationResultVO<ClientDailyIncome> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    @Override
    public Integer add(ClientDailyIncome bean) {
        return this.clientDailyIncomeMapper.insert(bean);
    }

    @Override
    public Integer addBatch(List<ClientDailyIncome> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.clientDailyIncomeMapper.insertBatch(listBean);
    }

    @Override
    public Integer addOrUpdateBatch(List<ClientDailyIncome> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.clientDailyIncomeMapper.insertOrUpdateBatch(listBean);
    }

    @Override
    public void calculateClientDailyIncome(Date calculateDate) {
        logger.info("开始执行客户每日收益计算，计算日期：{}", calculateDate);
        
        long startTime = System.currentTimeMillis();
        
        // 1. 并发处理各分表（每个分表独立事务）
        BatchProcessResult batchResult = processAllTablesInParallel(calculateDate);
        
        // 2. 检查处理结果
        if (!batchResult.isAllSuccess()) {
            handlePartialFailure(batchResult, calculateDate);
            return;
        }
        
        // 3. 汇总数据（独立事务）
        try {
            summaryService.summaryDataToMainTable(calculateDate);
            logger.info("客户每日收益计算完成，总耗时：{}ms", 
                System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            logger.error("汇总数据失败，需要重新处理", e);
            throw new BusinessException("汇总阶段失败", e);
        }
    }

    /**
     * 并发处理所有分表
     */
    private BatchProcessResult processAllTablesInParallel(Date calculateDate) {
        ThreadPoolExecutor executor = createOptimizedExecutor();
        
        try {
            // 使用CompletableFuture处理异步任务
            List<CompletableFuture<TableProcessResult>> futures = new ArrayList<>();
            
            for (int i = 0; i < 16; i++) {
                final int tableIndex = i;
                
                CompletableFuture<TableProcessResult> future = CompletableFuture
                    .supplyAsync(() -> {
                        long startTime = System.currentTimeMillis();
                        try {
                            logger.info("开始处理分表：tbclientdlyincome{}", tableIndex);
                            
                            // 每个分表在独立事务中处理
                            calculationService.processSingleTableWithTransaction(
                                tableIndex, calculateDate);
                            
                            long endTime = System.currentTimeMillis();
                            logger.info("完成处理分表：tbclientdlyincome{}，耗时：{}ms", 
                                      tableIndex, (endTime - startTime));
                            return TableProcessResult.success(tableIndex, endTime - startTime);
                            
                        } catch (Exception e) {
                            long endTime = System.currentTimeMillis();
                            logger.error("处理分表 tbclientdlyincome{} 异常", tableIndex, e);
                            return TableProcessResult.failure(tableIndex, e, endTime - startTime);
                        }
                    }, executor);
                
                futures.add(future);
            }
            
            // 等待所有任务完成并收集结果
            CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
            
            // 设置超时时间
            allTasks.get(30, TimeUnit.MINUTES);
            
            // 收集所有结果
            List<TableProcessResult> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
            
            return new BatchProcessResult(results);
            
        } catch (TimeoutException e) {
            logger.error("批量处理超时", e);
            throw new BusinessException("处理超时", e);
        } catch (Exception e) {
            logger.error("批量处理异常", e);
            throw new BusinessException("批量处理失败", e);
        } finally {
            shutdownExecutor(executor);
        }
    }

    /**
     * 创建优化的线程池
     */
    private ThreadPoolExecutor createOptimizedExecutor() {
        final int TABLE_COUNT = 16;  // 固定16个分表
        
        // 1. 根据任务特性确定线程数
        int coreThreads = Math.min(TABLE_COUNT, 
            Runtime.getRuntime().availableProcessors() * 2);  // IO密集型任务

        int maxThreads = TABLE_COUNT;  // 最大不超过任务数

        // 2. 使用专业的线程工厂
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            private final String namePrefix = "daily-income-worker-";

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, namePrefix + threadNumber.getAndIncrement());
                thread.setDaemon(false);  // 非守护线程，确保任务完成
                thread.setPriority(Thread.NORM_PRIORITY);  // 正常优先级
                thread.setUncaughtExceptionHandler((t, ex) -> {
                    logger.error("线程 {} 发生未捕获异常", t.getName(), ex);
                });
                return thread;
            }
        };
        
        // 3. 队列大小：只需要能容纳剩余任务即可
        int queueCapacity = Math.max(10, TABLE_COUNT - coreThreads);
        
        // 4. 选择合适的拒绝策略
//        RejectedExecutionHandler rejectionHandler = new RejectedExecutionHandler() {
//            @Override
//            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
//                logger.error("任务被拒绝执行，当前线程池状态：活跃线程={}, 队列大小={}",
//                    executor.getActiveCount(), executor.getQueue().size());
//                throw new RejectedExecutionException("任务执行被拒绝");
//            }
//        };
        RejectedExecutionHandler rejectionHandler = new ThreadPoolExecutor.AbortPolicy();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            coreThreads,                              // 核心线程数
            maxThreads,                               // 最大线程数
            60L, TimeUnit.SECONDS,                    // 空闲存活时间
            new LinkedBlockingQueue<>(queueCapacity), // 工作队列
            threadFactory,                            // 线程工厂
            rejectionHandler                          // 拒绝策略
        );
        
        // 5. 允许核心线程超时（可选）
        executor.allowCoreThreadTimeOut(true);
        
        logger.info("创建优化线程池 - 核心线程数：{}，最大线程数：{}，队列容量：{}", 
                   coreThreads, maxThreads, queueCapacity);
        
        return executor;
    }

    /**
     * 处理部分失败的情况
     */
    private void handlePartialFailure(BatchProcessResult result, Date calculateDate) {
        logger.error("部分分表处理失败，成功：{}，失败：{}", 
            result.getSuccessCount(), result.getFailureCount());
        
        // 记录失败详情
        result.getFailures().forEach(failure -> 
            logger.error("分表 {} 处理失败：{}", failure.getTableIndex(), failure.getError())
        );
        
        // 根据业务需求决定后续处理
        if (result.getFailureCount() > 3) {  // 失败超过3个分表
            throw new BusinessException("处理失败分表过多，需要人工介入");
        } else {
            // 重试失败的分表
            retryFailedTables(result.getFailures(), calculateDate);
        }
    }

    /**
     * 重试失败的分表
     */
    private void retryFailedTables(List<TableProcessResult> failures, Date calculateDate) {
        logger.info("开始重试失败的分表，数量：{}", failures.size());
        
        List<Integer> failedTableIndexes = failures.stream()
            .map(TableProcessResult::getTableIndex)
            .collect(Collectors.toList());
        
        BatchProcessResult retryResult = calculationService.retryFailedTables(
            failedTableIndexes, calculateDate);
        
        if (!retryResult.isAllSuccess()) {
            throw new BusinessException(
                String.format("重试后仍有%d个分表处理失败", retryResult.getFailureCount()));
        }
    }

    /**
     * 优雅关闭线程池
     */
    private void shutdownExecutor(ThreadPoolExecutor executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.warn("线程池未能在60秒内关闭，强制关闭");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("等待线程池关闭被中断", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processSingleTable(int tableIndex, Date calculateDate) {
        try {
            logger.debug("处理分表 {} - 第一步：临时表数据准备（含强增强减）", tableIndex);
            regCUSDLYIncomePre(tableIndex, calculateDate);
            
            logger.debug("处理分表 {} - 第二步：正式表数据处理", tableIndex);
            regCUSDLYIncomeCal(tableIndex, calculateDate);
            
        } catch (Exception e) {
            logger.error("处理分表 {} 异常", tableIndex, e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void regCUSDLYIncomePre(int tableIndex, Date cfmDate) {
        try {
            // 清空临时表
            clientDailyIncomeMapper.truncateTempTable(tableIndex);
            logger.debug("清空临时表 tbclientdlyincometmp{}", tableIndex);
            
            // 联合查询插入临时表（包含正常业务和强增强减）
            clientDailyIncomeMapper.regCUSDLYIncomePre(tableIndex, cfmDate);
            logger.debug("完成临时表数据准备 tbclientdlyincometmp{}（含强增强减）", tableIndex);
            
        } catch (Exception e) {
            logger.error("临时表数据准备异常，分表索引：{}", tableIndex, e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void regCUSDLYIncomeCal(int tableIndex, Date regDate) {
        try {
            // 删除当天的数据（避免重复）
            clientDailyIncomeMapper.deleteByDateRange(tableIndex, regDate);
            logger.debug("删除分表 {} 的当天数据", tableIndex);
            
            // 将临时表数据插入正式表
            clientDailyIncomeMapper.regCUSDLYIncomeCal(tableIndex, regDate);
            logger.debug("完成分表 {} 的当天数据插入", tableIndex);
            
        } catch (Exception e) {
            logger.error("正式表数据处理异常，分表索引：{}", tableIndex, e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void summaryDataToMainTable(Date calculateDate) {
        logger.info("开始批量数据汇总到主表");
        
        // ⚠️ 注意：这是旧版本的串行汇总方法，性能较低
        // 建议使用 OptimizedSummaryServiceImpl.summaryDataToMainTable() 并行版本
        
        try {
            // 分批处理，每批处理5000条数据
            int totalCount = 0;
            
            for (int i = 0; i < 16; i++) {
                logger.debug("开始汇总分表 {} 的数据到主表", i);
                clientDailyIncomeMapper.insertToMainTableAll(i, calculateDate);
                
                // 统计处理的数据量
                List<ClientDailyIncome> dataList = clientDailyIncomeMapper.selectFromSubTable(i, calculateDate);
                totalCount += dataList.size();
                logger.debug("分表 {} 汇总完成，数据量：{}", i, dataList.size());
            }
            
            logger.info("批量数据汇总完成，总数据量：{}", totalCount);
            
        } catch (Exception e) {
            logger.error("批量数据汇总异常", e);
            throw e;
        }
    }

    /**
     * 在独立事务中处理单个分表
     */
    @Transactional(rollbackFor = Exception.class)
    public void processSingleTableWithTransaction(int tableIndex, Date calculateDate) {
        processSingleTable(tableIndex, calculateDate);
    }

    /**
     * 在独立事务中汇总数据到主表
     */
    @Transactional(rollbackFor = Exception.class)
    public void summaryDataToMainTableWithTransaction(Date calculateDate) {
        summaryDataToMainTable(calculateDate);
    }

    /**
     * 清理部分处理的数据（补偿机制）
     */
    @Transactional(rollbackFor = Exception.class)
    public void cleanupPartialData(Date calculateDate) {
        logger.warn("开始清理当日部分处理的数据，日期：{}", calculateDate);
        
        try {
            // 清理所有分表中当日的数据
            for (int i = 0; i < 16; i++) {
                try {
                    clientDailyIncomeMapper.deleteByDateRange(i, calculateDate);
                    clientDailyIncomeMapper.truncateTempTable(i);
                    logger.debug("清理分表 {} 的数据完成", i);
                } catch (Exception e) {
                    logger.warn("清理分表 {} 数据时异常，继续清理其他分表", i, e);
                }
            }
            
            // 清理主表中当日的数据
            ClientDailyIncomeQuery deleteQuery = new ClientDailyIncomeQuery();
            deleteQuery.setRegDate(calculateDate);
            // 这里可以添加删除主表数据的逻辑
            
            logger.info("数据清理完成，日期：{}", calculateDate);
            
        } catch (Exception e) {
            logger.error("数据清理过程异常", e);
            // 清理失败不抛异常，避免影响主流程的异常处理
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanupSingleTable(int tableIndex, Date calculateDate) {
        try {
            logger.debug("开始清理分表 {} 的数据", tableIndex);
            
            // 删除分表中当日的数据
            clientDailyIncomeMapper.deleteByDateRange(tableIndex, calculateDate);
            
            // 清空临时表
            clientDailyIncomeMapper.truncateTempTable(tableIndex);
            
            logger.debug("清理分表 {} 的数据完成", tableIndex);
            
        } catch (Exception e) {
            logger.error("清理分表 {} 数据异常", tableIndex, e);
            throw e;
        }
    }
}