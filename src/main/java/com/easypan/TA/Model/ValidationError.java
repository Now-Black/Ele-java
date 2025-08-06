package com.easypan.TA.Model;

import lombok.Data;

@Data
public class ValidationError {
    
    private String errorCode;       // 错误代码
    private String errorMessage;    // 错误信息
    private String fieldName;       // 相关字段名
    private String productCode;     // 相关产品代码
    private String severity;        // 严重程度: ERROR, WARNING, INFO
    
    public ValidationError() {
        this.severity = "ERROR";
    }
    
    public ValidationError(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.severity = "ERROR";
    }
    
    public ValidationError(String errorCode, String errorMessage, String fieldName) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.fieldName = fieldName;
        this.severity = "ERROR";
    }
    
    public ValidationError(String errorCode, String errorMessage, String fieldName, String productCode) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.fieldName = fieldName;
        this.productCode = productCode;
        this.severity = "ERROR";
    }
    
    // 创建警告级别的错误
    public static ValidationError warning(String errorCode, String errorMessage, String fieldName) {
        ValidationError error = new ValidationError(errorCode, errorMessage, fieldName);
        error.setSeverity("WARNING");
        return error;
    }
    // 创建警告级别的错误
    public static ValidationError warning(String errorCode, String errorMessage) {
        ValidationError error = new ValidationError(errorCode, errorMessage, null);
        error.setSeverity("WARNING");
        return error;
    }


    // 创建信息级别的错误
    public static ValidationError info(String errorCode, String errorMessage, String fieldName) {
        ValidationError error = new ValidationError(errorCode, errorMessage, fieldName);
        error.setSeverity("INFO");
        return error;
    }
    // 创建信息级别的错误
    public static ValidationError info(String errorCode, String errorMessage) {
        ValidationError error = new ValidationError(errorCode, errorMessage, null);
        error.setSeverity("INFO");
        return error;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(severity).append("] ");
        sb.append(errorCode).append(": ").append(errorMessage);
        if (fieldName != null) {
            sb.append(" (字段: ").append(fieldName).append(")");
        }
        if (productCode != null) {
            sb.append(" (产品: ").append(productCode).append(")");
        }
        return sb.toString();
    }
}