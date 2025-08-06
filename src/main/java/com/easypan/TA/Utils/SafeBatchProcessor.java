package com.easypan.TA.Utils;

import com.easypan.TA.Config.TaBatchConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 安全的批量操作工具类
 */
@Slf4j
@Component
public class SafeBatchProcessor {
    
    @Resource
    private TaBatchConfig batchConfig;
    
    /**
     * 执行安全的批量操作
     * 
     * @param dataList 数据列表
     * @param processor 处理函数，接收批次数据并返回处理结果数量
     * @param operationName 操作名称，用于日志
     * @return 总处理数量
     */
    public <T> int processBatch(List<T> dataList, Function<List<T>, Integer> processor, String operationName) {
        if (dataList == null || dataList.isEmpty()) {
            log.debug("{}：数据列表为空，跳过处理", operationName);
            return 0;
        }
        
        int totalSize = dataList.size();
        int batchSize = calculateOptimalBatchSize(totalSize);
        int totalBatches = (totalSize + batchSize - 1) / batchSize;
        
        log.info("开始执行{}: 总数量={}, 批次大小={}, 预计批次数={}", 
            operationName, totalSize, batchSize, totalBatches);
        
        AtomicInteger totalProcessed = new AtomicInteger(0);
        AtomicInteger successfulBatches = new AtomicInteger(0);
        AtomicInteger failedBatches = new AtomicInteger(0);
        
        try {
            for (int i = 0; i < totalSize; i += batchSize) {
                int endIndex = Math.min(i + batchSize, totalSize);
                List<T> batch = dataList.subList(i, endIndex);
                
                int currentBatch = (i / batchSize) + 1;
                
                try {
                    // 执行批次处理
                    int processed = processSingleBatch(batch, processor, operationName, currentBatch, totalBatches);
                    totalProcessed.addAndGet(processed);
                    successfulBatches.incrementAndGet();
                    
                    // 进度日志
                    if (currentBatch % batchConfig.getTransaction().getProgressLogInterval() == 0) {
                        log.info("{}进度: 已完成 {}/{} 批次，累计处理{}条记录", 
                            operationName, currentBatch, totalBatches, totalProcessed.get());
                    }
                    
                } catch (Exception e) {
                    failedBatches.incrementAndGet();
                    log.error("{}批次 {}/{} 处理失败: 批次大小={}", 
                        operationName, currentBatch, totalBatches, batch.size(), e);
                    
                    // 根据配置决定是否继续处理后续批次
                    if (!batchConfig.getTransaction().isEnableBatchTransaction()) {
                        throw new RuntimeException(String.format("%s批量处理失败，批次 %d/%d 出错", 
                            operationName, currentBatch, totalBatches), e);
                    }
                }
            }
            
            // 处理结果统计
            log.info("{}完成: 总数量={}, 成功批次={}, 失败批次={}, 累计处理={}", 
                operationName, totalSize, successfulBatches.get(), failedBatches.get(), totalProcessed.get());
            
            if (failedBatches.get() > 0) {
                log.warn("{}存在失败批次，请检查错误日志", operationName);
            }
            
            return totalProcessed.get();
            
        } catch (Exception e) {
            log.error("{}执行异常: 总数量={}, 已处理={}", operationName, totalSize, totalProcessed.get(), e);
            throw new RuntimeException(operationName + "批量处理失败", e);
        }
    }
    
    /**
     * 处理单个批次（独立事务）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, 
                   timeout = 300, // 5分钟超时
                   rollbackFor = Exception.class)
    public <T> int processSingleBatch(List<T> batch, Function<List<T>, Integer> processor, 
                                     String operationName, int currentBatch, int totalBatches) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("执行{}批次 {}/{}: 当前批次大小={}", 
                operationName, currentBatch, totalBatches, batch.size());
            
            int processed = processor.apply(batch);
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("{}批次 {}/{} 完成: 处理{}条，耗时{}ms", 
                operationName, currentBatch, totalBatches, processed, duration);
            
            return processed;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("{}批次 {}/{} 失败: 批次大小={}，耗时{}ms", 
                operationName, currentBatch, totalBatches, batch.size(), duration, e);
            throw e;
        }
    }
    
    /**
     * 计算最优批次大小
     */
    private int calculateOptimalBatchSize(int totalSize) {
        if (totalSize <= batchConfig.getMinSize()) {
            return totalSize;
        }
        
        if (totalSize <= batchConfig.getDefaultSize()) {
            return batchConfig.getDefaultSize();
        }
        
        TaBatchConfig.ThresholdConfig threshold = batchConfig.getThreshold();
        
        if (totalSize <= threshold.getSmall()) {
            return batchConfig.getDefaultSize();
        } else if (totalSize <= threshold.getMedium()) {
            return threshold.getMediumBatchSize();
        } else if (totalSize <= threshold.getLarge()) {
            return threshold.getLargeBatchSize();
        } else {
            return batchConfig.getMaxSize();
        }
    }
    
    /**
     * 获取批次统计信息
     */
    public BatchStatistics calculateBatchStatistics(int totalSize) {
        int batchSize = calculateOptimalBatchSize(totalSize);
        int totalBatches = (totalSize + batchSize - 1) / batchSize;
        
        return new BatchStatistics(totalSize, batchSize, totalBatches);
    }
    
    /**
     * 批次统计信息
     */
    public static class BatchStatistics {
        private final int totalSize;
        private final int batchSize;
        private final int totalBatches;
        
        public BatchStatistics(int totalSize, int batchSize, int totalBatches) {
            this.totalSize = totalSize;
            this.batchSize = batchSize;
            this.totalBatches = totalBatches;
        }
        
        public int getTotalSize() { return totalSize; }
        public int getBatchSize() { return batchSize; }
        public int getTotalBatches() { return totalBatches; }
        
        @Override
        public String toString() {
            return String.format("BatchStatistics{totalSize=%d, batchSize=%d, totalBatches=%d}", 
                totalSize, batchSize, totalBatches);
        }
    }
}