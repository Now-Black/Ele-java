package com.easypan.TA.Controller;

import com.easypan.TA.Config.TransferConfig;
import com.easypan.TA.Config.TransferConfigService;
import com.easypan.TA.Entity.TransferResult;
import com.easypan.TA.Model.FileNotification;
import com.easypan.TA.Processor.FileProcessResult;
import com.easypan.TA.Service.FileTransferService;
import com.easypan.TA.Service.FileTransferScheduler;
import com.easypan.entity.vo.ResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/file-transfer")
@Slf4j
public class FileTransferController {

    @Autowired
    private TransferConfigService configService;

    @Autowired
    private FileTransferService transferService;
    
    @Autowired
    private FileTransferScheduler fileTransferScheduler;

    @Value("${project.folder:/tmp/easypan/}")
    private String uploadPath;

    /**
     * 保存传输配置
     */
    @PostMapping("/config")
    public ResponseEntity<String> saveConfig(@RequestBody TransferConfig config) {
        try {
            if (StringUtils.isBlank(config.getConfigId())) {
                config.setConfigId(UUID.randomUUID().toString());
            }

            configService.saveConfig(config);
            return ResponseEntity.ok("配置保存成功");
        } catch (Exception e) {
            log.error("保存配置异常", e);
            return ResponseEntity.status(500).body("保存配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有配置
     */
    @GetMapping("/config")
    public ResponseEntity<List<TransferConfig>> getAllConfigs() {
        try {
            List<TransferConfig> configs = configService.getAllEnabledConfigs();
            return ResponseEntity.ok(configs);
        } catch (Exception e) {
            log.error("获取配置异常", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * 测试连接
     */
    @PostMapping("/test-connection")
    public ResponseEntity<String> testConnection(@RequestBody TransferConfig config) {
        try {
            boolean success = configService.testConnection(config);
            if (success) {
                return ResponseEntity.ok("连接测试成功");
            } else {
                return ResponseEntity.status(400).body("连接测试失败");
            }
        } catch (Exception e) {
            log.error("连接测试异常", e);
            return ResponseEntity.status(500).body("连接测试异常: " + e.getMessage());
        }
    }

    /**
     * 手动触发自动拉取（直接处理，无通知）
     */
    @PostMapping("/manual-transfer/{configId}")
    public ResponseEntity<ResponseVO<Map<String, Object>>> manualTransfer(@PathVariable String configId) {
        try {
            log.info("手动触发自动拉取，配置ID: {}", configId);
            
            List<TransferResult> transferResults = transferService.manualTransfer(configId);
            
            // 直接处理下载的文件，不发送通知
            List<TransferResult> successfulTransfers = transferResults.stream()
                .filter(TransferResult::isSuccess)
                .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
            
            if (!successfulTransfers.isEmpty()) {
                // 构造文件信息进行直接处理
                List<FileNotification.FileInfo> fileInfos = new ArrayList<>();
                for (TransferResult transfer : successfulTransfers) {
                    String fileName = transfer.getFileName();
                    String fileType = fileName.toUpperCase().startsWith("CPDM") ? "CPDM" : 
                                    fileName.toUpperCase().startsWith("JYCS") ? "JYCS" : "UNKNOWN";
                    
                    FileNotification.FileInfo fileInfo = FileNotification.FileInfo.builder()
                        .fileName(fileName)
                        .fileType(fileType)
                        .localPath(transfer.getLocalPath())
                        .fileSize(transfer.getFileSize())
                        .downloadTime(new Date())
                        .build();
                        
                    fileInfos.add(fileInfo);
                }
                
                // 直接处理文件
                String batchId = UUID.randomUUID().toString();
                FileProcessResult processResult = fileTransferScheduler.processConfirmedFiles(batchId, fileInfos);
                
                // 构造响应
                Map<String, Object> response = new HashMap<>();
                response.put("transferResults", transferResults);
                response.put("batchId", batchId);
                response.put("processSuccess", processResult.isSuccess());
                response.put("processMessage", processResult.getMessage());
                
                if (processResult.hasValidationReport()) {
                    response.put("validationSummary", processResult.getValidationReport().generateSummary());
                }
                
                return ResponseEntity.ok(ResponseVO.success(response));
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("transferResults", transferResults);
                response.put("message", "没有成功下载的文件需要处理");
                return ResponseEntity.ok(ResponseVO.success(response));
            }
            
        } catch (Exception e) {
            log.error("手动传输异常", e);
            return ResponseEntity.ok(ResponseVO.fail("手动传输失败: " + e.getMessage()));
        }
    }

    /**
     * 手动上传本地文件（直接处理，无通知）
     */
    @PostMapping("/upload-files")
    public ResponseEntity<ResponseVO<Map<String, Object>>> uploadFiles(
            @RequestParam("cpdmFile") MultipartFile cpdmFile,
            @RequestParam(value = "jycsFile", required = false) MultipartFile jycsFile) {
        
        try {
            log.info("手动上传文件，CPDM: {}, JYCS: {}", 
                cpdmFile.getOriginalFilename(), 
                jycsFile != null ? jycsFile.getOriginalFilename() : "无");
            
            // 验证文件类型
            if (cpdmFile.isEmpty()) {
                return ResponseEntity.ok(ResponseVO.fail("CPDM文件不能为空"));
            }
            
            String cpdmFileName = cpdmFile.getOriginalFilename();
            if (!isValidFileName(cpdmFileName, "CPDM")) {
                return ResponseEntity.ok(ResponseVO.fail("CPDM文件名格式不正确"));
            }
            
            if (jycsFile != null && !jycsFile.isEmpty()) {
                String jycsFileName = jycsFile.getOriginalFilename();
                if (!isValidFileName(jycsFileName, "JYCS")) {
                    return ResponseEntity.ok(ResponseVO.fail("JYCS文件名格式不正确"));
                }
            }
            
            // 保存上传的文件
            String uploadDir = uploadPath + "upload" + File.separator + System.currentTimeMillis() + File.separator;
            File uploadDirFile = new File(uploadDir);
            if (!uploadDirFile.exists()) {
                uploadDirFile.mkdirs();
            }
            
            String cpdmPath = saveUploadedFile(cpdmFile, uploadDir);
            String jycsPath = null;
            if (jycsFile != null && !jycsFile.isEmpty()) {
                jycsPath = saveUploadedFile(jycsFile, uploadDir);
            }
            
            // 构造文件信息
            List<FileNotification.FileInfo> fileInfos = new ArrayList<>();
            
            fileInfos.add(FileNotification.FileInfo.builder()
                .fileName(cpdmFile.getOriginalFilename())
                .fileType("CPDM")
                .localPath(cpdmPath)
                .fileSize(cpdmFile.getSize())
                .downloadTime(new Date())
                .build());
            
            if (jycsPath != null) {
                fileInfos.add(FileNotification.FileInfo.builder()
                    .fileName(jycsFile.getOriginalFilename())
                    .fileType("JYCS")
                    .localPath(jycsPath)
                    .fileSize(jycsFile.getSize())
                    .downloadTime(new Date())
                    .build());
            }
            
            // 直接处理文件
            String batchId = UUID.randomUUID().toString();
            FileProcessResult processResult = fileTransferScheduler.processConfirmedFiles(batchId, fileInfos);
            
            // 构造响应
            Map<String, Object> response = new HashMap<>();
            response.put("uploadedFiles", fileInfos);
            response.put("batchId", batchId);
            response.put("processSuccess", processResult.isSuccess());
            response.put("processMessage", processResult.getMessage());
            
            if (processResult.hasValidationReport()) {
                response.put("validationSummary", processResult.getValidationReport().generateSummary());
                response.put("hasErrors", !processResult.getValidationReport().isSuccess());
            }
            
            return ResponseEntity.ok(ResponseVO.success(response));
            
        } catch (Exception e) {
            log.error("文件上传异常", e);
            return ResponseEntity.ok(ResponseVO.fail("文件上传失败: " + e.getMessage()));
        }
    }

    /**
     * 获取传输状态
     */
    @GetMapping("/status/{configId}")
    public ResponseEntity<Map<String, Object>> getTransferStatus(@PathVariable String configId) {
        try {
            TransferConfig config = configService.getConfig(configId);
            Long lastSyncTime = transferService.getLastSyncTime(configId);

            Map<String, Object> status = new HashMap<>();
            status.put("configId", configId);
            status.put("enabled", config != null ? config.isEnabled() : false);
            status.put("lastSyncTime", lastSyncTime);
            status.put("lastSyncDate", lastSyncTime != null ? new Date(lastSyncTime) : null);

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("获取传输状态异常", e);
            return ResponseEntity.status(500).body(null);
        }
    }
    
    /**
     * 验证文件名是否符合格式
     */
    private boolean isValidFileName(String fileName, String expectedType) {
        if (StringUtils.isBlank(fileName)) {
            return false;
        }
        
        String upperFileName = fileName.toUpperCase();
        return upperFileName.startsWith(expectedType + "-") && upperFileName.endsWith(".TXT");
    }
    
    /**
     * 保存上传的文件
     */
    private String saveUploadedFile(MultipartFile file, String uploadDir) throws IOException {
        String fileName = file.getOriginalFilename();
        String filePath = uploadDir + fileName;
        File destinationFile = new File(filePath);
        
        file.transferTo(destinationFile);
        
        log.info("文件上传成功: {}", filePath);
        return filePath;
    }
}

