package com.easypan.service.impl;

import com.easypan.entity.dto.SummaryResult;
import com.easypan.entity.dto.TableSummaryResult;
import com.easypan.exception.BusinessException;
import com.easypan.mappers.ClientDailyIncomeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

/**
 * 优化的汇总服务实现
 */
@Service("optimizedSummaryService")
public class OptimizedSummaryServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(OptimizedSummaryServiceImpl.class);

    @Resource
    private ClientDailyIncomeMapper clientDailyIncomeMapper;

    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 优化后的主表汇总方法
     */
    public SummaryResult summaryDataToMainTable(Date calculateDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        logger.info("开始并行批量数据汇总到主表，日期: {}", sdf.format(calculateDate));
        
        long startTime = System.currentTimeMillis();
        List<Future<TableSummaryResult>> futures = new ArrayList<>();
        SummaryResult result = new SummaryResult(calculateDate);
        
        // 创建专用线程池
        ThreadPoolExecutor executor = createSummaryExecutor();
        
        try {
            // 并行提交16个分表处理任务
            for (int i = 0; i < 16; i++) {
                Future<TableSummaryResult> future = submitTableSummaryTask(executor, i, calculateDate);
                futures.add(future);
            }
            
            // 收集所有处理结果
            for (int i = 0; i < futures.size(); i++) {
                try {
                    TableSummaryResult tableResult = futures.get(i).get(5, TimeUnit.MINUTES);
                    result.addTableResult(i, tableResult);
                    
                    logger.info("分表 {} 汇总完成: 影响行数={}, 耗时={}ms", 
                        i, tableResult.getAffectedRows(), tableResult.getProcessTime());
                        
                } catch (TimeoutException e) {
                    logger.error("分表 {} 汇总超时", i, e);
                    futures.get(i).cancel(true);
                    result.addFailure(i, "汇总超时");
                    
                } catch (ExecutionException e) {
                    logger.error("分表 {} 汇总异常", i, e.getCause());
                    result.addFailure(i, e.getCause().getMessage());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
            result.setTotalTime(System.currentTimeMillis() - startTime);
            
            // 生成汇总报告
            generateSummaryReport(result);
            
            return result;
            
        } catch (RejectedExecutionException e) {
            logger.error("汇总任务提交被拒绝", e);
            throw new BusinessException("系统繁忙，汇总任务无法执行", e);
        } finally {
            shutdownExecutor(executor);
        }
    }
    
    /**
     * 创建汇总专用线程池
     */
    private ThreadPoolExecutor createSummaryExecutor() {
        return new ThreadPoolExecutor(
            16, 16,  // 固定16个线程处理16个分表
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(20),
            r -> new Thread(r, "summary-worker-" + System.currentTimeMillis()),
            new ThreadPoolExecutor.AbortPolicy()
        );
    }
    
    /**
     * 提交单个分表汇总任务
     */
    private Future<TableSummaryResult> submitTableSummaryTask(ThreadPoolExecutor executor, int tableIndex, Date calculateDate) {
        return executor.submit(() -> processTableSummary(tableIndex, calculateDate));
    }
    
    /**
     * 单表汇总处理（独立事务 + 分批插入）
     */
    private TableSummaryResult processTableSummary(int tableIndex, Date calculateDate) {
        long startTime = System.currentTimeMillis();
        String tableName = "tbclientdlyincome" + tableIndex;
        
        // 每个分表使用独立事务
        return transactionTemplate.execute(status -> {
            try {
                logger.debug("开始汇总分表 {} 到主表", tableName);
                
                // 1. 先统计分表数据总量
                int totalCount = clientDailyIncomeMapper.countTableData(tableIndex, calculateDate);
                
                if (totalCount == 0) {
                    logger.info("分表 {} 无数据需要汇总", tableName);
                    return TableSummaryResult.success(tableIndex, tableName, 0, 
                        System.currentTimeMillis() - startTime);
                }
                
                // 2. 分批插入到主表
                int totalAffectedRows = processBatchInsert(tableIndex, calculateDate, totalCount, tableName);
                
                // 3. 可选：验证数据一致性
                if (totalAffectedRows > 0) {
                    validateDataConsistency(tableIndex, calculateDate, totalAffectedRows);
                }
                
                TableSummaryResult result = TableSummaryResult.success(
                    tableIndex, 
                    tableName, 
                    totalAffectedRows, 
                    System.currentTimeMillis() - startTime
                );
                
                logger.debug("分表 {} 汇总成功: 总数据量={}, 插入行数={}", 
                           tableName, totalCount, totalAffectedRows);
                return result;
                
            } catch (Exception e) {
                logger.error("分表 {} 汇总处理异常", tableName, e);
                status.setRollbackOnly();
                
                return TableSummaryResult.failure(
                    tableIndex, 
                    tableName, 
                    e.getMessage(), 
                    System.currentTimeMillis() - startTime
                );
            }
        });
    }

    /**
     * 分批插入处理
     */
    private int processBatchInsert(int tableIndex, Date calculateDate, int totalCount, String tableName) {
        final int BATCH_SIZE = 5000;  // 每批5000条
        int totalAffectedRows = 0;
        int offset = 0;
        int batchCount = (totalCount + BATCH_SIZE - 1) / BATCH_SIZE;  // 向上取整
        
        logger.debug("分表 {} 开始分批插入，总数据量: {}, 批次数: {}, 每批大小: {}", 
                    tableName, totalCount, batchCount, BATCH_SIZE);
        
        for (int batch = 1; batch <= batchCount; batch++) {
            try {
                // 分批插入
                int currentBatchSize = Math.min(BATCH_SIZE, totalCount - offset);
                int affectedRows = clientDailyIncomeMapper.insertToMainTable(
                    tableIndex, calculateDate, offset, currentBatchSize);
                
                totalAffectedRows += affectedRows;
                offset += currentBatchSize;
                
                logger.debug("分表 {} 第 {}/{} 批插入完成，本批插入: {}, 累计插入: {}", 
                           tableName, batch, batchCount, affectedRows, totalAffectedRows);
                
                // 批次间短暂休息，减少数据库压力
                if (batch < batchCount) {
                    Thread.sleep(10);  // 10ms间隔
                }
                
            } catch (Exception e) {
                logger.error("分表 {} 第 {}/{} 批插入失败", tableName, batch, batchCount, e);
                throw new RuntimeException(String.format("分批插入失败: %s, 批次: %d/%d", 
                                         tableName, batch, batchCount), e);
            }
        }
        
        logger.info("分表 {} 分批插入完成，总插入行数: {}", tableName, totalAffectedRows);
        return totalAffectedRows;
    }
    
    /**
     * 数据一致性验证
     */
    private void validateDataConsistency(int tableIndex, Date calculateDate, int affectedRows) {
        // 这里可以添加数据验证逻辑
        logger.debug("验证分表 {} 数据一致性，影响行数: {}", tableIndex, affectedRows);
    }
    
    /**
     * 生成汇总报告
     */
    private void generateSummaryReport(SummaryResult result) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        logger.info("=============== 汇总报告 ===============");
        logger.info("计算日期: {}", sdf.format(result.getCalculateDate()));
        logger.info("总耗时: {}ms", result.getTotalTime());
        logger.info("成功分表: {}/{}", result.getSuccessCount(), result.getTotalTables());
        logger.info("失败分表: {}", result.getFailureCount());
        logger.info("总影响行数: {}", result.getTotalAffectedRows());
        logger.info("成功率: {:.2f}%", result.getSuccessRate() * 100);
        
        if (!result.getFailures().isEmpty()) {
            logger.warn("失败详情:");
            result.getFailures().forEach(failure -> logger.warn("- {}", failure));
        }
        
        logger.info("=====================================");
    }
    
    /**
     * 优雅关闭线程池
     */
    private void shutdownExecutor(ThreadPoolExecutor executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("汇总线程池未能在30秒内关闭，强制关闭");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("等待线程池关闭被中断", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}