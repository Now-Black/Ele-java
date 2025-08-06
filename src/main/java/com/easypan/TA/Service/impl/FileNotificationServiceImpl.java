package com.easypan.TA.Service.impl;

import com.easypan.TA.Model.FileNotification;
import com.easypan.TA.Service.FileNotificationService;
import com.easypan.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 文件扫描通知服务实现（异步优化版本）
 */
@Slf4j
@Service
public class FileNotificationServiceImpl implements FileNotificationService {
    
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    
    // SSE连接管理
    private final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();
    
    // Redis键前缀
    private static final String NOTIFICATION_KEY_PREFIX = "ta:notification:";
    private static final String USER_NOTIFICATIONS_KEY_PREFIX = "ta:user_notifications:";
    private static final long SSE_TIMEOUT = 30 * 60 * 1000; // 30分钟超时
    private static final long NOTIFICATION_EXPIRE_HOURS = 2; // 通知2小时过期
    
    @Override
    public SseEmitter createConnection(String userId) {
        // 关闭已存在的连接
        closeConnection(userId);
        
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        sseEmitters.put(userId, emitter);
        
        // 设置连接事件处理
        emitter.onCompletion(() -> {
            sseEmitters.remove(userId);
            log.info("SSE连接完成，用户: {}", userId);
        });
        
        emitter.onTimeout(() -> {
            sseEmitters.remove(userId);
            log.info("SSE连接超时，用户: {}", userId);
        });
        
        emitter.onError((ex) -> {
            sseEmitters.remove(userId);
            log.error("SSE连接异常，用户: {}", userId, ex);
        });
        
        try {
            // 发送连接成功消息
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("{\"message\":\"连接成功\",\"timestamp\":\"" + new Date() + "\"}")
            );
            
            log.info("SSE连接创建成功，用户: {}", userId);
            
            // 异步发送待处理的通知
            asyncSendPendingNotifications(userId);
            
        } catch (IOException e) {
            log.error("发送SSE连接消息失败，用户: {}", userId, e);
            sseEmitters.remove(userId);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }
    
    @Override
    public void sendFileFoundNotification(FileNotification notification) {
        // 同步保存通知到Redis
        saveNotification(notification);
        addNotificationToUser(notification.getUserId(), notification.getNotificationId());
        
        // 异步发送SSE通知
        asyncSendNotificationToUser(notification.getUserId(), notification);
        
        log.info("发送文件发现通知（异步）: 用户={}, 批次={}, 新文件数={}",
            notification.getUserId(), notification.getBatchId(), notification.getNewFilesCount());
    }
    
    @Override
    public void sendProcessProgressNotification(String batchId, int progress, String message) {
        FileNotification notification = FileNotification.builder()
            .notificationId(UUID.randomUUID().toString())
            .batchId(batchId)
            .type(FileNotification.NotificationType.PROCESS_PROGRESS)
            .status(FileNotification.NotificationStatus.PROCESSED) // 进度通知直接标记为已处理
            .progress(progress)
            .message(message)
            .createTime(new Date())
            .build();
        
        // 获取该批次对应的用户ID（从原始通知中获取）
        String userId = getUserByBatchId(batchId);
        if (userId != null) {
            notification.setUserId(userId);
            
            // 异步发送通知
            asyncSendNotificationToUser(userId, notification);
            
            log.debug("发送处理进度通知（异步）: 批次={}, 进度={}%, 消息={}", batchId, progress, message);
        }
    }
    
    @Override
    public void sendProcessCompleteNotification(String batchId, boolean success, String message) {
        FileNotification notification = FileNotification.builder()
            .notificationId(UUID.randomUUID().toString())
            .batchId(batchId)
            .type(FileNotification.NotificationType.PROCESS_COMPLETE)
            .status(FileNotification.NotificationStatus.PROCESSED) // 完成通知直接标记为已处理
            .message(message)
            .createTime(new Date())
            .build();
        
        // 获取该批次对应的用户ID
        String userId = getUserByBatchId(batchId);
        if (userId != null) {
            notification.setUserId(userId);
            
            // 异步发送通知
            asyncSendNotificationToUser(userId, notification);
            
            log.info("发送处理完成通知（异步）: 批次={}, 成功={}, 消息={}", batchId, success, message);
        }
    }
    
    /**
     * 异步发送待处理通知
     */
    @Async("notificationExecutor")
    protected void asyncSendPendingNotifications(String userId) {
        try {
            List<FileNotification> pendingNotifications = getPendingNotifications(userId);
            for (FileNotification notification : pendingNotifications) {
                sendNotificationToUser(userId, notification);
                
                // 避免过于频繁的发送
                Thread.sleep(100);
            }
            
            log.debug("异步发送待处理通知完成: 用户={}, 数量={}", userId, pendingNotifications.size());
            
        } catch (Exception e) {
            log.error("异步发送待处理通知失败: 用户={}", userId, e);
        }
    }
    
    /**
     * 异步发送通知给指定用户
     */
    @Async("notificationExecutor")
    protected void asyncSendNotificationToUser(String userId, FileNotification notification) {
        try {
            sendNotificationToUser(userId, notification);
            
        } catch (Exception e) {
            log.error("异步发送通知失败: 用户={}, 通知ID={}", userId, notification.getNotificationId(), e);
        }
    }
    
    @Override
    public void closeConnection(String userId) {
        SseEmitter emitter = sseEmitters.remove(userId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.warn("关闭SSE连接异常，用户: {}", userId, e);
            }
        }
    }
    
    @Override
    public List<FileNotification> getPendingNotifications(String userId) {
        try {
            String userNotificationsKey = USER_NOTIFICATIONS_KEY_PREFIX + userId;
            Set<Object> notificationIds = redisTemplate.opsForSet().members(userNotificationsKey);
            
            if (notificationIds == null || notificationIds.isEmpty()) {
                return Collections.emptyList();
            }
            
            List<FileNotification> notifications = new ArrayList<>();
            for (Object notificationId : notificationIds) {
                String key = NOTIFICATION_KEY_PREFIX + notificationId.toString();
                Object notificationData = redisTemplate.opsForValue().get(key);
                
                if (notificationData != null) {
                    FileNotification notification = JsonUtils.convertJson2Obj(
                        notificationData.toString(), FileNotification.class);
                    
                    // 只返回待处理的通知
                    if (notification.getStatus() == FileNotification.NotificationStatus.PENDING) {
                        notifications.add(notification);
                    }
                }
            }
            
            // 按创建时间排序
            return notifications.stream()
                .sorted(Comparator.comparing(FileNotification::getCreateTime))
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("获取待处理通知失败，用户: {}", userId, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public void confirmNotification(String notificationId) {
        try {
            String key = NOTIFICATION_KEY_PREFIX + notificationId;
            Object notificationData = redisTemplate.opsForValue().get(key);
            
            if (notificationData != null) {
                FileNotification notification = JsonUtils.convertJson2Obj(
                    notificationData.toString(), FileNotification.class);
                
                notification.setStatus(FileNotification.NotificationStatus.CONFIRMED);
                
                String updatedData = JsonUtils.convertObj2Json(notification);
                redisTemplate.opsForValue().set(key, updatedData, NOTIFICATION_EXPIRE_HOURS, TimeUnit.HOURS);
                
                log.info("通知已确认: {}", notificationId);
            }
            
        } catch (Exception e) {
            log.error("确认通知失败: {}", notificationId, e);
        }
    }
    
    @Override
    public void markNotificationAsProcessed(String notificationId) {
        try {
            String key = NOTIFICATION_KEY_PREFIX + notificationId;
            Object notificationData = redisTemplate.opsForValue().get(key);
            
            if (notificationData != null) {
                FileNotification notification = JsonUtils.convertJson2Obj(
                    notificationData.toString(), FileNotification.class);
                
                notification.setStatus(FileNotification.NotificationStatus.PROCESSED);
                
                String updatedData = JsonUtils.convertObj2Json(notification);
                redisTemplate.opsForValue().set(key, updatedData, NOTIFICATION_EXPIRE_HOURS, TimeUnit.HOURS);
                
                log.info("通知已标记为处理完成: {}", notificationId);
            }
            
        } catch (Exception e) {
            log.error("标记通知处理状态失败: {}", notificationId, e);
        }
    }
    
    @Override
    public void cleanupExpiredData() {
        // 清理过期的SSE连接
        sseEmitters.entrySet().removeIf(entry -> {
            try {
                // 发送心跳检测连接是否还有效
                entry.getValue().send(SseEmitter.event().name("ping").data(""));
                return false;
            } catch (Exception e) {
                log.debug("清理无效SSE连接: {}", entry.getKey());
                return true;
            }
        });
        
        log.info("清理过期数据完成，当前活跃SSE连接数: {}", sseEmitters.size());
    }
    
    /**
     * 发送通知给指定用户（同步方法，由异步方法调用）
     */
    private void sendNotificationToUser(String userId, FileNotification notification) {
        SseEmitter emitter = sseEmitters.get(userId);
        if (emitter != null) {
            try {
                String eventName = notification.getType().name().toLowerCase();
                String data = JsonUtils.convertObj2Json(notification);
                
                emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data)
                );
                
                log.debug("SSE通知发送成功: 用户={}, 类型={}", userId, notification.getType());
                
            } catch (IOException e) {
                log.error("SSE通知发送失败: 用户={}", userId, e);
                sseEmitters.remove(userId);
            }
        } else {
            log.debug("用户{}未建立SSE连接，通知将保存待推送", userId);
        }
    }
    
    /**
     * 保存通知到Redis
     */
    private void saveNotification(FileNotification notification) {
        try {
            String key = NOTIFICATION_KEY_PREFIX + notification.getNotificationId();
            String data = JsonUtils.convertObj2Json(notification);
            redisTemplate.opsForValue().set(key, data, NOTIFICATION_EXPIRE_HOURS, TimeUnit.HOURS);
            
        } catch (Exception e) {
            log.error("保存通知失败: {}", notification.getNotificationId(), e);
        }
    }
    
    /**
     * 添加通知到用户通知列表
     */
    private void addNotificationToUser(String userId, String notificationId) {
        try {
            String userNotificationsKey = USER_NOTIFICATIONS_KEY_PREFIX + userId;
            redisTemplate.opsForSet().add(userNotificationsKey, notificationId);
            redisTemplate.expire(userNotificationsKey, NOTIFICATION_EXPIRE_HOURS, TimeUnit.HOURS);
            
        } catch (Exception e) {
            log.error("添加用户通知失败: 用户={}, 通知={}", userId, notificationId, e);
        }
    }
    
    /**
     * 根据批次ID获取用户ID
     */
    private String getUserByBatchId(String batchId) {
        // 这里简化实现，实际应用中可以从数据库或缓存中获取
        // 目前假设用户ID为"admin"，实际应用中需要根据业务逻辑获取
        return "admin";
    }
}