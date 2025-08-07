package com.easypan.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 多线程执行器，专门用于处理客户收益计算等批量任务
 */
public class PubRestLessThreadExcutor {
    
    private static final Logger logger = LoggerFactory.getLogger(PubRestLessThreadExcutor.class);
    
    private ThreadPoolExecutor executor;
    private String threadName;
    
    public PubRestLessThreadExcutor(int corePoolSize, int maximumPoolSize, String threadName) {
        this.threadName = threadName;
        this.executor = new ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            r -> new Thread(r, threadName + "-" + System.currentTimeMillis())
        );
        
        logger.info("创建线程池成功，核心线程数：{}，最大线程数：{}，线程名前缀：{}", 
                   corePoolSize, maximumPoolSize, threadName);
    }
    
    public void submit(Runnable task) {
        executor.submit(task);
    }
    
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            logger.info("线程池 {} 已关闭", threadName);
        }
    }
    
    public boolean isShutdown() {
        return executor.isShutdown();
    }
    
    public int getActiveCount() {
        return executor.getActiveCount();
    }
    
    public long getCompletedTaskCount() {
        return executor.getCompletedTaskCount();
    }
}