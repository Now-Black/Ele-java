package com.easypan.TA.Processor;

import com.easypan.TA.Validator.ValidationReport;
import com.easypan.TA.Validator.ValidatorChain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

@Slf4j
@Component
public class FileValidationProcessor {
    
    private final ValidatorChain validatorChain;
    
    public FileValidationProcessor() {
        this.validatorChain = new ValidatorChain();
    }
    
    /**
     * 处理下载完成的文件对：执行三层校验
     */
    public FileProcessResult processDownloadedFiles(String cpdmFilePath, String jycsFilePath) {
        long startTime = System.currentTimeMillis();
        
        log.info("开始处理下载完成的文件: CPDM[{}], JYCS[{}]", cpdmFilePath, jycsFilePath);
        
        FileProcessResult processResult = new FileProcessResult();
        processResult.setCpdmFilePath(cpdmFilePath);
        processResult.setJycsFilePath(jycsFilePath);
        processResult.setProcessTime(startTime);
        
        try {
            // 1. 检查文件是否存在
            if (!validateFileExistence(cpdmFilePath, jycsFilePath, processResult)) {
                return processResult;
            }
            
            // 2. 执行三层校验
            ValidationReport report = validatorChain.validateFiles(cpdmFilePath, jycsFilePath);
            processResult.setValidationReport(report);
            processResult.setSuccess(report.isSuccess());
            
            // 3. 记录处理结果
            if (report.isSuccess()) {
                log.info("文件处理成功: 所有校验通过");
                processResult.setMessage("文件校验通过，数据质量良好");
            } else {
                log.warn("文件处理完成但存在校验错误: {}", report.getErrorStatistics());
                processResult.setMessage("文件校验完成，但发现数据质量问题，详见校验报告");
            }
            
        } catch (Exception e) {
            log.error("文件处理异常", e);
            processResult.setSuccess(false);
            processResult.setMessage("文件处理异常: " + e.getMessage());
            processResult.setException(e);
        } finally {
            processResult.setProcessDuration(System.currentTimeMillis() - startTime);
        }
        
        log.info("文件处理完成: {}, 耗时: {}ms", 
            processResult.isSuccess() ? "成功" : "失败", 
            processResult.getProcessDuration());
        
        return processResult;
    }
    
    /**
     * 处理单个CPDM文件
     */
    public FileProcessResult processCpdmFile(String cpdmFilePath) {
        long startTime = System.currentTimeMillis();
        
        FileProcessResult processResult = new FileProcessResult();
        processResult.setCpdmFilePath(cpdmFilePath);
        processResult.setProcessTime(startTime);
        
        try {
            if (!new File(cpdmFilePath).exists()) {
                processResult.setSuccess(false);
                processResult.setMessage("CPDM文件不存在: " + cpdmFilePath);
                return processResult;
            }
            
            ValidationReport report = validatorChain.validateCpdmFile(cpdmFilePath);
            processResult.setValidationReport(report);
            processResult.setSuccess(report.isSuccess());
            
            if (report.isSuccess()) {
                processResult.setMessage("CPDM文件校验通过");
            } else {
                processResult.setMessage("CPDM文件校验发现问题，详见报告");
            }
            
        } catch (Exception e) {
            log.error("CPDM文件处理异常", e);
            processResult.setSuccess(false);
            processResult.setMessage("CPDM文件处理异常: " + e.getMessage());
            processResult.setException(e);
        } finally {
            processResult.setProcessDuration(System.currentTimeMillis() - startTime);
        }
        
        return processResult;
    }
    
    /**
     * 处理单个JYCS文件
     */
    public FileProcessResult processJycsFile(String jycsFilePath) {
        long startTime = System.currentTimeMillis();
        
        FileProcessResult processResult = new FileProcessResult();
        processResult.setJycsFilePath(jycsFilePath);
        processResult.setProcessTime(startTime);
        
        try {
            if (!new File(jycsFilePath).exists()) {
                processResult.setSuccess(false);
                processResult.setMessage("JYCS文件不存在: " + jycsFilePath);
                return processResult;
            }
            
            ValidationReport report = validatorChain.validateJycsFile(jycsFilePath);
            processResult.setValidationReport(report);
            processResult.setSuccess(report.isSuccess());
            
            if (report.isSuccess()) {
                processResult.setMessage("JYCS文件校验通过");
            } else {
                processResult.setMessage("JYCS文件校验发现问题，详见报告");
            }
            
        } catch (Exception e) {
            log.error("JYCS文件处理异常", e);
            processResult.setSuccess(false);
            processResult.setMessage("JYCS文件处理异常: " + e.getMessage());
            processResult.setException(e);
        } finally {
            processResult.setProcessDuration(System.currentTimeMillis() - startTime);
        }
        
        return processResult;
    }
    
    /**
     * 检查文件是否存在
     */
    private boolean validateFileExistence(String cpdmFilePath, String jycsFilePath, 
                                        FileProcessResult processResult) {
        File cpdmFile = new File(cpdmFilePath);
        File jycsFile = new File(jycsFilePath);
        
        if (!cpdmFile.exists()) {
            processResult.setSuccess(false);
            processResult.setMessage("CPDM文件不存在: " + cpdmFilePath);
            log.error("CPDM文件不存在: {}", cpdmFilePath);
            return false;
        }
        
        if (!jycsFile.exists()) {
            processResult.setSuccess(false);
            processResult.setMessage("JYCS文件不存在: " + jycsFilePath);
            log.error("JYCS文件不存在: {}", jycsFilePath);
            return false;
        }
        
        log.info("文件存在性检查通过: CPDM[{}字节], JYCS[{}字节]", 
            cpdmFile.length(), jycsFile.length());
        
        return true;
    }
}