package com.easypan.TA.Service;

import com.easypan.TA.Service.FileNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时清理任务
 */
@Component
@Slf4j
public class ScheduledCleanupService {

    @Autowired
    private FileNotificationService notificationService;

    /**
     * 每小时清理过期的通知和连接
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupExpiredData() {
        log.info("开始清理过期数据");
        
        try {
            notificationService.cleanupExpiredData();
            log.info("过期数据清理完成");
            
        } catch (Exception e) {
            log.error("清理过期数据异常", e);
        }
    }
}