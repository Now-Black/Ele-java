package com.easypan.TA.Model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 文件扫描通知模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileNotification {
    
    /**
     * 通知ID
     */
    private String notificationId;
    
    /**
     * 配置ID
     */
    private String configId;
    
    /**
     * 批次ID（用于后续处理跟踪）
     */
    private String batchId;
    /**
     * 通知类型：FILE_FOUND, PROCESS_PROGRESS, PROCESS_COMPLETE
     */
    private NotificationType type;
    
    /**
     * 通知状态：PENDING, CONFIRMED, PROCESSED, EXPIRED
     */
    private NotificationStatus status;
    /**
     * 扫描到的新文件信息
     */
    private List<FileInfo> newFiles;
    /**
     * 总文件数
     */
    private Integer totalFiles;
    /**
     * 新文件数
     */
    private Integer newFilesCount;
    /**
     * 通知消息
     */
    private String message;
    /**
     * 进度百分比（用于进度通知）
     */
    private Integer progress;
    /**
     * 扫描时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date scanTime;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    
    /**
     * 过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expireTime;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 扩展信息
     */
    private String extra;
    
    /**
     * 通知类型枚举
     */
    public enum NotificationType {
        FILE_FOUND,      // 发现新文件
        PROCESS_PROGRESS, // 处理进度
        PROCESS_COMPLETE  // 处理完成
    }
    
    /**
     * 通知状态枚举
     */
    public enum NotificationStatus {
        PENDING,    // 等待用户确认
        CONFIRMED,  // 用户已确认
        PROCESSED,  // 已处理完成
        EXPIRED     // 已过期
    }
    
    /**
     * 文件信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo {
        private String fileName;
        private String fileType; // CPDM, JYCS
        private String localPath;
        private Long fileSize;
        private Date downloadTime;
    }
}