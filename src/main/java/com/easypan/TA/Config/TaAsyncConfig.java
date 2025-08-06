package com.easypan.TA.Config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置 - 针对性优化版本
 */
@Configuration
@EnableAsync
@Slf4j
public class TaAsyncConfig {

    /**
     * 通知发送任务线程池
     */
    @Bean("notificationExecutor")  
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);  // 核心线程数
        executor.setMaxPoolSize(4);   // 最大线程数  
        executor.setQueueCapacity(100); // 通知量可能较大
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("Notification-");
        
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        
        log.info("通知发送线程池初始化完成: 核心线程={}, 最大线程={}, 队列容量={}", 
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }

    /**
     * 数据库批量操作线程池
     */
    @Bean("databaseBatchExecutor")
    public Executor databaseBatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);  // 核心线程数，支持3种类型数据并行写入
        executor.setMaxPoolSize(5);   // 最大线程数
        executor.setQueueCapacity(50); 
        executor.setKeepAliveSeconds(120);
        executor.setThreadNamePrefix("DBBatch-");
        
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        
        log.info("数据库批量操作线程池初始化完成: 核心线程={}, 最大线程={}, 队列容量={}", 
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }

    /**
     * 缓存更新线程池
     */
    @Bean("cacheUpdateExecutor")
    public Executor cacheUpdateExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);  // 核心线程数，支持Redis和内存并行
        executor.setMaxPoolSize(4);   // 最大线程数
        executor.setQueueCapacity(200); // 缓存更新操作较多
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("CacheUpdate-");
        
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        
        log.info("缓存更新线程池初始化完成: 核心线程={}, 最大线程={}, 队列容量={}", 
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
}