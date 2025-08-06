package com.easypan.TA.Processor;

import com.easypan.TA.Validator.ValidationReport;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

@Data
public class FileProcessResult {
    
    private boolean success;                // 处理是否成功
    private String message;                 // 处理结果消息
    private String cpdmFilePath;            // CPDM文件路径
    private String jycsFilePath;            // JYCS文件路径
    private ValidationReport validationReport; // 校验报告
    private long processTime;               // 处理开始时间
    private long processDuration;           // 处理耗时(ms)
    private Exception exception;            // 异常信息
    private List<String> processedFiles;    // 已处理的文件列表
    
    public FileProcessResult() {
        this.success = false;
        this.processedFiles = new ArrayList<>();
    }
    
    public FileProcessResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.processedFiles = new ArrayList<>();
    }
    
    /**
     * 创建成功结果
     */
    public static FileProcessResult success(String message) {
        return new FileProcessResult(true, message);
    }
    
    /**
     * 创建失败结果
     */
    public static FileProcessResult failure(String message) {
        return new FileProcessResult(false, message);
    }
    
    /**
     * 创建异常结果
     */
    public static FileProcessResult error(String message, Exception exception) {
        FileProcessResult result = new FileProcessResult(false, message);
        result.setException(exception);
        return result;
    }
    
    /**
     * 是否有校验报告
     */
    public boolean hasValidationReport() {
        return validationReport != null;
    }
    
    /**
     * 获取校验错误数量
     */
    public int getValidationErrorCount() {
        return hasValidationReport() ? validationReport.getAllErrors().size() : 0;
    }
    
    /**
     * 是否有校验错误
     */
    public boolean hasValidationErrors() {
        return getValidationErrorCount() > 0;
    }
    
    /**
     * 获取处理结果摘要
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("FileProcessResult{");
        sb.append("success=").append(success);
        sb.append(", message='").append(message).append("'");
        sb.append(", duration=").append(processDuration).append("ms");
        
        if (hasValidationReport()) {
            sb.append(", validationErrors=").append(getValidationErrorCount());
        }
        
        if (exception != null) {
            sb.append(", exception=").append(exception.getClass().getSimpleName());
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * 添加已处理的文件
     */
    public void addProcessedFile(String filePath) {
        if (processedFiles == null) {
            processedFiles = new ArrayList<>();
        }
        if (filePath != null && !processedFiles.contains(filePath)) {
            processedFiles.add(filePath);
        }
    }
    
    /**
     * 添加多个已处理的文件
     */
    public void addProcessedFiles(List<String> filePaths) {
        if (filePaths != null) {
            for (String filePath : filePaths) {
                addProcessedFile(filePath);
            }
        }
    }
    
    /**
     * 设置已处理的文件列表
     */
    public void setProcessedFiles(String cpdmPath, String jycsPath) {
        if (processedFiles == null) {
            processedFiles = new ArrayList<>();
        }
        if (cpdmPath != null) {
            addProcessedFile(cpdmPath);
        }
        if (jycsPath != null) {
            addProcessedFile(jycsPath);
        }
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
}