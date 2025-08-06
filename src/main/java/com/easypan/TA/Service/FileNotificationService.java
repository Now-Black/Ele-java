package com.easypan.TA.Service;

import com.easypan.TA.Model.FileNotification;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 文件扫描通知服务接口
 */
public interface FileNotificationService {
    
    /**
     * 创建SSE连接
     * @param userId 用户ID
     * @return SSE发射器
     */
    SseEmitter createConnection(String userId);
    
    /**
     * 发送文件发现通知
     * @param notification 通知信息
     */
    void sendFileFoundNotification(FileNotification notification);
    
    /**
     * 发送处理进度通知
     * @param batchId 批次ID
     * @param progress 进度百分比
     * @param message 进度消息
     */
    void sendProcessProgressNotification(String batchId, int progress, String message);
    
    /**
     * 发送处理完成通知
     * @param batchId 批次ID
     * @param success 是否成功
     * @param message 结果消息
     */
    void sendProcessCompleteNotification(String batchId, boolean success, String message);
    
    /**
     * 关闭指定用户的连接
     * @param userId 用户ID
     */
    void closeConnection(String userId);
    
    /**
     * 获取待处理的文件通知列表
     * @param userId 用户ID
     * @return 通知列表
     */
    List<FileNotification> getPendingNotifications(String userId);
    
    /**
     * 标记通知为已确认
     * @param notificationId 通知ID
     */
    void confirmNotification(String notificationId);
    
    /**
     * 标记通知为已处理
     * @param notificationId 通知ID
     */
    void markNotificationAsProcessed(String notificationId);
    
    /**
     * 清理过期的通知和连接
     */
    void cleanupExpiredData();
}