package com.easypan.TA.Validator;

import com.easypan.TA.Model.ValidationError;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ValidationResult {
    
    private boolean success;                    // 校验是否成功
    private List<ValidationError> errors;       // 错误列表
    private String message;                     // 校验结果信息
    private long validationTime;                // 校验耗时(ms)
    
    public ValidationResult() {
        this.errors = new ArrayList<>();
    }
    
    public ValidationResult(boolean success) {
        this();
        this.success = success;
    }
    
    // 创建成功结果
    public static ValidationResult success() {
        return new ValidationResult(true);
    }
    
    public static ValidationResult success(String message) {
        ValidationResult result = new ValidationResult(true);
        result.setMessage(message);
        return result;
    }
    
    // 创建失败结果
    public static ValidationResult failure(String message) {
        ValidationResult result = new ValidationResult(false);
        result.setMessage(message);
        return result;
    }
    
    public static ValidationResult failure(List<ValidationError> errors) {
        ValidationResult result = new ValidationResult(false);
        result.setErrors(errors);
        return result;
    }
    
    public static ValidationResult failure(ValidationError error) {
        ValidationResult result = new ValidationResult(false);
        result.addError(error);
        return result;
    }
    
    // 添加错误
    public void addError(ValidationError error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
        this.success = false;
    }
    
    // 添加多个错误
    public void addErrors(List<ValidationError> errors) {
        if (errors != null && !errors.isEmpty()) {
            if (this.errors == null) {
                this.errors = new ArrayList<>();
            }
            this.errors.addAll(errors);
            this.success = false;
        }
    }
    
    // 合并其他校验结果
    public void merge(ValidationResult other) {
        if (other != null) {
            if (!other.isSuccess()) {
                this.success = false;
            }
            if (other.getErrors() != null && !other.getErrors().isEmpty()) {
                addErrors(other.getErrors());
            }
        }
    }
    
    // 获取错误数量
    public int getErrorCount() {
        return errors != null ? errors.size() : 0;
    }
    
    // 获取错误统计
    public String getErrorSummary() {
        if (errors == null || errors.isEmpty()) {
            return "无错误";
        }
        
        long errorCount = errors.stream().filter(e -> "ERROR".equals(e.getSeverity())).count();
        long warningCount = errors.stream().filter(e -> "WARNING".equals(e.getSeverity())).count();
        long infoCount = errors.stream().filter(e -> "INFO".equals(e.getSeverity())).count();
        
        return String.format("错误: %d, 警告: %d, 信息: %d", errorCount, warningCount, infoCount);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ValidationResult{success=").append(success);
        if (message != null) {
            sb.append(", message='").append(message).append("'");
        }
        sb.append(", errorCount=").append(getErrorCount());
        sb.append(", validationTime=").append(validationTime).append("ms}");
        return sb.toString();
    }
}