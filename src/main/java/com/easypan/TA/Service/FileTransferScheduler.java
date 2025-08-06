package com.easypan.TA.Service;

import com.easypan.TA.Config.TransferConfig;
import com.easypan.TA.Config.TransferConfigService;
import com.easypan.TA.Entity.ScanResult;
import com.easypan.TA.Entity.TransferResult;
import com.easypan.TA.Model.FileNotification;
import com.easypan.TA.Processor.FileProcessResult;
import com.easypan.TA.Processor.FileValidationProcessor;
import com.easypan.TA.Validator.ValidatorChain;
import com.easypan.TA.Validator.ValidationReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class FileTransferScheduler {

    @Autowired
    private TransferConfigService configService;

    @Autowired
    private FileTransferService transferService;
    
    @Autowired
    private FileValidationProcessor fileValidationProcessor;
    
    @Autowired
    private FileNotificationService notificationService;
    
    @Autowired
    private ValidatorChain validatorChain;

    /**
     * 每5分钟执行一次自动扫描
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void scanAndNotifyFiles() {
        log.info("开始自动文件扫描任务");

        try {
            List<TransferConfig> configs = configService.getAllEnabledConfigs();

            if (configs.isEmpty()) {
                log.info("没有启用的传输配置，跳过扫描");
                return;
            }

            for (TransferConfig config : configs) {
                try {
                    log.info("扫描配置: {}", config.getConfigId());
                    ScanResult result = transferService.scanAndDownloadFiles(config);

                    if (result.isSuccess()) {
                        log.info("配置 {} 扫描完成，新文件: {}/{}",
                                config.getConfigId(), result.getNewFiles(), result.getTotalFiles());

                        // 记录成功传输的文件
                        result.getTransferResults().stream()
                                .filter(TransferResult::isSuccess)
                                .forEach(tr -> log.info("文件传输成功: {}", tr.getFileName()));

                        // 如果有新文件，发送通知而不是直接处理
                        if (result.getNewFiles() > 0) {
                            sendFileFoundNotification(result);
                        }
                    } else {
                        log.error("配置 {} 扫描失败: {}", config.getConfigId(), result.getMessage());
                    }

                } catch (Exception e) {
                    log.error("处理配置 {} 异常", config.getConfigId(), e);
                }
            }

        } catch (Exception e) {
            log.error("自动文件扫描任务异常", e);
        }

        log.info("自动文件扫描任务完成");
    }

    /**
     * 发送文件发现通知
     */
    private void sendFileFoundNotification(ScanResult scanResult) {
        try {
            // 构造文件信息列表
            List<FileNotification.FileInfo> fileInfos = new ArrayList<>();
            
            List<TransferResult> successfulTransfers = scanResult.getTransferResults().stream()
                .filter(TransferResult::isSuccess)
                .collect(Collectors.toList());
            
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
            
            // 创建通知
            FileNotification notification = FileNotification.builder()
                .notificationId(UUID.randomUUID().toString())
                .configId(scanResult.getConfigId())
                .batchId(UUID.randomUUID().toString()) // 生成批次ID用于后续处理跟踪
                .type(FileNotification.NotificationType.FILE_FOUND)
                .status(FileNotification.NotificationStatus.PENDING)
                .newFiles(fileInfos)
                .totalFiles(scanResult.getTotalFiles())
                .newFilesCount(scanResult.getNewFiles())
                .message(String.format("发现 %d 个新文件需要处理", scanResult.getNewFiles()))
                .scanTime(new Date(System.currentTimeMillis() - scanResult.getScanTime()))
                .createTime(new Date())
                .expireTime(new Date(System.currentTimeMillis() + 2 * 60 * 60 * 1000)) // 2小时后过期
                .userId("admin") // 暂时硬编码，实际应用中根据配置或业务逻辑获取
                .build();
            
            // 发送通知
            notificationService.sendFileFoundNotification(notification);
            
            log.info("文件发现通知已发送: 配置={}, 批次={}, 新文件数={}",
                scanResult.getConfigId(), notification.getBatchId(), scanResult.getNewFiles());
                
        } catch (Exception e) {
            log.error("发送文件发现通知失败", e);
        }
    }

    /**
     * 用户确认后触发文件处理和校验流程
     * 此方法将通过REST API调用，不再是定时任务自动执行
     */
    public FileProcessResult processConfirmedFiles(String batchId, List<FileNotification.FileInfo> fileInfos) {
        log.info("开始处理用户确认的文件，批次: {}, 文件数: {}", batchId, fileInfos.size());

        try {
            // 发送开始处理通知
            notificationService.sendProcessProgressNotification(batchId, 10, "开始处理文件...");

            // 按文件类型分组处理
            String cpdmFilePath = null;
            String jycsFilePath = null;

            for (FileNotification.FileInfo fileInfo : fileInfos) {
                if ("CPDM".equals(fileInfo.getFileType())) {
                    cpdmFilePath = fileInfo.getLocalPath();
                } else if ("JYCS".equals(fileInfo.getFileType())) {
                    jycsFilePath = fileInfo.getLocalPath();
                }
            }

            notificationService.sendProcessProgressNotification(batchId, 30, "文件分类完成，开始校验...");

            FileProcessResult result = null;

            // 执行文件校验处理
            if (cpdmFilePath != null && jycsFilePath != null) {
                // 有CPDM和JYCS文件对，执行完整的三层校验
                result = processFilePair(batchId, cpdmFilePath, jycsFilePath);
            } else if (cpdmFilePath != null) {
                // 只有CPDM文件，执行单文件校验
                result = processSingleFile(batchId, cpdmFilePath, "CPDM");
            } else if (jycsFilePath != null) {
                // 只有JYCS文件，执行单文件校验
                result = processSingleFile(batchId, jycsFilePath, "JYCS");
            } else {
                result = FileProcessResult.failure("未找到CPDM或JYCS文件");
                notificationService.sendProcessCompleteNotification(batchId, false, "未找到有效的处理文件");
            }

            if (result != null && result.isSuccess()) {
                notificationService.sendProcessCompleteNotification(batchId, true, "文件处理完成");
            } else {
                String errorMsg = result != null ? result.getMessage() : "处理失败";
                notificationService.sendProcessCompleteNotification(batchId, false, "文件处理失败: " + errorMsg);
            }

            return result;

        } catch (Exception e) {
            log.error("处理确认文件异常", e);
            notificationService.sendProcessCompleteNotification(batchId, false, "处理异常: " + e.getMessage());
            return FileProcessResult.failure("处理异常: " + e.getMessage());
        }
    }

    /**
     * 处理文件对（CPDM + JYCS）
     */
    private FileProcessResult processFilePair(String batchId, String cpdmFilePath, String jycsFilePath) {
        log.info("处理文件对: CPDM[{}], JYCS[{}]", 
            new File(cpdmFilePath).getName(), new File(jycsFilePath).getName());

        notificationService.sendProcessProgressNotification(batchId, 50, "开始校验文件对...");

        FileProcessResult result = fileValidationProcessor.processDownloadedFiles(cpdmFilePath, jycsFilePath);

        notificationService.sendProcessProgressNotification(batchId, 80, "文件校验完成，开始后续处理...");

        if (result.isSuccess()) {
            log.info("文件对校验通过: {}", result.getMessage());
            
            // 这里可以触发后续的业务处理
            triggerPostProcessing(batchId, result);
            
        } else {
            log.error("文件对校验失败: {}", result.getMessage());
            
            // 记录详细的校验错误
            if (result.hasValidationReport()) {
                String reportSummary = result.getValidationReport().generateSummary();
                log.error("详细校验报告:\n{}", reportSummary);
            }
        }

        notificationService.sendProcessProgressNotification(batchId, 100, "处理完成");
        return result;
    }

    /**
     * 处理单个文件
     */
    private FileProcessResult processSingleFile(String batchId, String filePath, String fileType) {
        log.info("处理单个{}文件: {}", fileType, new File(filePath).getName());

        notificationService.sendProcessProgressNotification(batchId, 50, "开始校验" + fileType + "文件...");

        FileProcessResult result = null;
        if ("CPDM".equals(fileType)) {
            result = fileValidationProcessor.processCpdmFile(filePath);
        } else if ("JYCS".equals(fileType)) {
            result = fileValidationProcessor.processJycsFile(filePath);
        }

        notificationService.sendProcessProgressNotification(batchId, 100, fileType + "文件处理完成");

        if (result != null) {
            if (result.isSuccess()) {
                log.info("{}文件校验通过: {}", fileType, result.getMessage());
            } else {
                log.warn("{}文件校验发现问题: {}", fileType, result.getMessage());
                
                if (result.hasValidationReport()) {
                    log.warn("校验报告摘要: {}", result.getValidationReport().generateSummary());
                }
            }
        }

        return result;
    }

    /**
     * 触发后续处理流程
     */
    private void triggerPostProcessing(String batchId, FileProcessResult processResult) {
        log.info("触发后续处理流程 - 批次: {}, 文件校验通过，开始完整校验和数据入库流程", batchId);
        
        try {
            // 发送进度通知
            notificationService.sendProcessProgressNotification(batchId, 85, "开始执行完整校验和数据处理...");
            
            // 从ProcessResult中获取文件路径
            String cpdmFilePath = null;
            String jycsFilePath = null;
            
            // 这里需要根据实际的FileProcessResult结构获取文件路径
            // 假设processResult包含处理的文件信息
            if (processResult.getProcessedFiles() != null) {
                for (String filePath : processResult.getProcessedFiles()) {
                    String fileName = new File(filePath).getName().toUpperCase();
                    if (fileName.startsWith("CPDM-")) {
                        cpdmFilePath = filePath;
                    } else if (fileName.startsWith("JYCS-")) {
                        jycsFilePath = filePath;
                    }
                }
            }
            
            // 使用ValidatorChain进行完整的三层校验和数据处理
            if (cpdmFilePath != null) {
                notificationService.sendProcessProgressNotification(batchId, 90, "执行三层校验和数据入库...");
                
                ValidationReport validationReport = validatorChain.validateFiles(cpdmFilePath, jycsFilePath);
                
                if (validationReport.isSuccess()) {
                    log.info("完整校验和数据处理成功 - 批次: {}", batchId);
                    notificationService.sendProcessProgressNotification(batchId, 100, "数据处理完成");
                } else {
                    log.warn("完整校验发现问题 - 批次: {}, 错误数: {}", 
                        batchId, validationReport.getAllErrors().size());
                    
                    // 即使有警告也继续处理，只有严重错误才停止
                    long errorCount = validationReport.getAllErrors().stream()
                        .filter(error -> "ERROR".equals(error.getSeverity()))
                        .count();
                        
                    if (errorCount == 0) {
                        notificationService.sendProcessProgressNotification(batchId, 100, 
                            "数据处理完成（存在" + validationReport.getAllErrors().size() + "个警告）");
                    } else {
                        notificationService.sendProcessProgressNotification(batchId, 95, 
                            "校验发现" + errorCount + "个错误，数据处理终止");
                    }
                }
                
                // 记录详细的校验报告
                String reportSummary = validationReport.generateSummary();
                log.info("完整校验报告摘要 - 批次: {}\n{}", batchId, reportSummary);
                
            } else {
                log.warn("未找到CPDM文件路径，跳过完整校验流程 - 批次: {}", batchId);
                notificationService.sendProcessProgressNotification(batchId, 100, "基础校验完成");
            }
            
        } catch (Exception e) {
            log.error("后续处理流程异常 - 批次: {}", batchId, e);
            notificationService.sendProcessProgressNotification(batchId, 100, 
                "处理异常: " + e.getMessage());
        }
    }
}

