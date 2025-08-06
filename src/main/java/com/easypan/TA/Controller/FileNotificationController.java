package com.easypan.TA.Controller;

import com.easypan.TA.Model.FileNotification;
import com.easypan.TA.Processor.FileProcessResult;
import com.easypan.TA.Service.FileNotificationService;
import com.easypan.TA.Service.FileTransferScheduler;
import com.easypan.entity.vo.ResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 文件通知控制器
 */
@RestController
@RequestMapping("/api/file-notification")
@Slf4j
public class FileNotificationController {

    @Autowired
    private FileNotificationService notificationService;
    
    @Autowired
    private FileTransferScheduler fileTransferScheduler;

    /**
     * 创建SSE连接用于接收通知
     */
    @GetMapping(value = "/connect/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@PathVariable String userId) {
        log.info("用户 {} 创建SSE连接", userId);
        return notificationService.createConnection(userId);
    }

    /**
     * 获取待处理的通知列表
     */
    @GetMapping("/pending/{userId}")
    public ResponseEntity<ResponseVO<List<FileNotification>>> getPendingNotifications(@PathVariable String userId) {
        try {
            List<FileNotification> notifications = notificationService.getPendingNotifications(userId);
            return ResponseEntity.ok(ResponseVO.success(notifications));
            
        } catch (Exception e) {
            log.error("获取待处理通知失败，用户: {}", userId, e);
            return ResponseEntity.ok(ResponseVO.fail("获取通知失败: " + e.getMessage()));
        }
    }

    /**
     * 用户确认导入文件
     */
    @PostMapping("/confirm-import")
    public ResponseEntity<ResponseVO<Map<String, Object>>> confirmImport(@RequestBody ConfirmImportRequest request) {
        try {
            log.info("用户确认导入文件，通知ID: {}, 批次ID: {}", request.getNotificationId(), request.getBatchId());
            
            // 标记通知为已确认
            notificationService.confirmNotification(request.getNotificationId());
            
            // 触发文件处理
            FileProcessResult result = fileTransferScheduler.processConfirmedFiles(request.getBatchId(), request.getFileInfos());
            
            // 标记通知为已处理
            notificationService.markNotificationAsProcessed(request.getNotificationId());
            
            // 构造响应
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("batchId", request.getBatchId());
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            
            if (result.hasValidationReport()) {
                response.put("validationSummary", result.getValidationReport().generateSummary());
                response.put("hasErrors", !result.getValidationReport().isSuccess());
            }
            
            return ResponseEntity.ok(ResponseVO.success(response));
            
        } catch (Exception e) {
            log.error("确认导入失败", e);
            return ResponseEntity.ok(ResponseVO.fail("导入失败: " + e.getMessage()));
        }
    }

    /**
     * 用户拒绝导入文件
     */
    @PostMapping("/reject-import")
    public ResponseEntity<ResponseVO<String>> rejectImport(@RequestBody RejectImportRequest request) {
        try {
            log.info("用户拒绝导入文件，通知ID: {}, 原因: {}", request.getNotificationId(), request.getReason());
            
            // 标记通知为已处理（拒绝）
            notificationService.markNotificationAsProcessed(request.getNotificationId());
            
            return ResponseEntity.ok(ResponseVO.success("已取消导入"));
            
        } catch (Exception e) {
            log.error("拒绝导入失败", e);
            return ResponseEntity.ok(ResponseVO.fail("操作失败: " + e.getMessage()));
        }
    }

    /**
     * 关闭SSE连接
     */
    @PostMapping("/disconnect/{userId}")
    public ResponseEntity<ResponseVO<String>> disconnect(@PathVariable String userId) {
        try {
            notificationService.closeConnection(userId);
            return ResponseEntity.ok(ResponseVO.success("连接已关闭"));
            
        } catch (Exception e) {
            log.error("关闭连接失败，用户: {}", userId, e);
            return ResponseEntity.ok(ResponseVO.fail("关闭连接失败: " + e.getMessage()));
        }
    }

    /**
     * 确认导入请求
     */
    public static class ConfirmImportRequest {
        private String notificationId;
        private String batchId;
        private List<FileNotification.FileInfo> fileInfos;
        
        // Getters and Setters
        public String getNotificationId() { return notificationId; }
        public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        public List<FileNotification.FileInfo> getFileInfos() { return fileInfos; }
        public void setFileInfos(List<FileNotification.FileInfo> fileInfos) { this.fileInfos = fileInfos; }
    }

    /**
     * 拒绝导入请求
     */
    public static class RejectImportRequest {
        private String notificationId;
        private String reason;
        
        // Getters and Setters
        public String getNotificationId() { return notificationId; }
        public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}