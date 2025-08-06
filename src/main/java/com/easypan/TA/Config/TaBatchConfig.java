package com.easypan.TA.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * TA系统批量操作配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "ta.batch")
public class TaBatchConfig {
    
    /**
     * 默认批次大小
     */
    private int defaultSize = 1000;
    
    /**
     * 最大批次大小
     */
    private int maxSize = 5000;
    
    /**
     * 最小批次大小
     */
    private int minSize = 100;
    
    /**
     * 大数据量阈值配置
     */
    private ThresholdConfig threshold = new ThresholdConfig();
    
    /**
     * 事务配置
     */
    private TransactionConfig transaction = new TransactionConfig();
    
    @Data
    public static class ThresholdConfig {
        /**
         * 小数据量阈值（使用默认批次大小）
         */
        private int small = 10000;
        
        /**
         * 中等数据量阈值
         */
        private int medium = 50000;
        
        /**
         * 大数据量阈值
         */
        private int large = 100000;
        
        /**
         * 中等数据量批次大小
         */
        private int mediumBatchSize = 2000;
        
        /**
         * 大数据量批次大小
         */
        private int largeBatchSize = 3000;
    }
    
    @Data
    public static class TransactionConfig {
        /**
         * 事务超时时间（秒）
         */
        private int timeout = 300;
        
        /**
         * 是否启用分批事务（每批独立事务）
         */
        private boolean enableBatchTransaction = true;
        
        /**
         * 批次进度日志间隔
         */
        private int progressLogInterval = 10;
    }
}