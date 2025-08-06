package com.easypan.TA.Validator;

import com.easypan.TA.Model.ValidationError;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ValidationReport {
    
    private boolean success;                                    // 整体校验是否成功
    private Map<Integer, ValidationResult> layerResults;        // 各层校验结果
    private List<ValidationError> allErrors;                    // 所有错误列表
    private String summary;                                     // 校验总结
    private long totalValidationTime;                           // 总校验时间(ms)
    private String cpdmFileName;                                // CPDM文件名
    private String jycsFileName;                                // JYCS文件名
    
    public ValidationReport() {
        this.layerResults = new HashMap<>();
        this.allErrors = new ArrayList<>();
        this.success = true;
    }
    
    public ValidationReport(String cpdmFileName, String jycsFileName) {
        this();
        this.cpdmFileName = cpdmFileName;
        this.jycsFileName = jycsFileName;
    }
    
    // 添加层级校验结果
    public void addLayerResult(int layer, ValidationResult result) {
        layerResults.put(layer, result);
        
        // 如果该层校验失败，整体就失败
        if (!result.isSuccess()) {
            this.success = false;
        }
        
        // 收集所有错误
        if (result.getErrors() != null) {
            allErrors.addAll(result.getErrors());
        }
        
        // 累加校验时间
        totalValidationTime += result.getValidationTime();
    }
    
    // 添加单个错误
    public void addError(ValidationError error) {
        allErrors.add(error);
        this.success = false;
    }
    
    // 获取指定层的校验结果
    public ValidationResult getLayerResult(int layer) {
        return layerResults.get(layer);
    }
    
    // 获取第一层校验结果
    public ValidationResult getFieldValidationResult() {
        return layerResults.get(1);
    }
    
    // 获取第二层校验结果
    public ValidationResult getBusinessValidationResult() {
        return layerResults.get(2);
    }
    
    // 获取第三层校验结果
    public ValidationResult getCrossFileValidationResult() {
        return layerResults.get(3);
    }
    
    // 生成校验总结
    public String generateSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 文件校验报告 ===\n");
        sb.append("CPDM文件: ").append(cpdmFileName).append("\n");
        sb.append("JYCS文件: ").append(jycsFileName).append("\n");
        sb.append("整体结果: ").append(success ? "通过" : "失败").append("\n");
        sb.append("总校验时间: ").append(totalValidationTime).append("ms\n");
        sb.append("\n");
        
        // 各层校验结果
        sb.append("=== 各层校验结果 ===\n");
        ValidationResult layer1 = layerResults.get(1);
        if (layer1 != null) {
            sb.append("第一层 (字段校验): ").append(layer1.isSuccess() ? "通过" : "失败")
              .append(" - ").append(layer1.getErrorSummary()).append("\n");
        }
        
        ValidationResult layer2 = layerResults.get(2);
        if (layer2 != null) {
            sb.append("第二层 (业务校验): ").append(layer2.isSuccess() ? "通过" : "失败")
              .append(" - ").append(layer2.getErrorSummary()).append("\n");
        }
        
        ValidationResult layer3 = layerResults.get(3);
        if (layer3 != null) {
            sb.append("第三层 (交叉校验): ").append(layer3.isSuccess() ? "通过" : "失败")
              .append(" - ").append(layer3.getErrorSummary()).append("\n");
        }
        
        // 错误详情
        if (!allErrors.isEmpty()) {
            sb.append("\n=== 错误详情 ===\n");
            for (ValidationError error : allErrors) {
                sb.append(error.toString()).append("\n");
            }
        }
        
        this.summary = sb.toString();
        return this.summary;
    }
    
    // 获取错误统计
    public Map<String, Integer> getErrorStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("总错误数", allErrors.size());
        
        long errorCount = allErrors.stream().filter(e -> "ERROR".equals(e.getSeverity())).count();
        long warningCount = allErrors.stream().filter(e -> "WARNING".equals(e.getSeverity())).count();
        long infoCount = allErrors.stream().filter(e -> "INFO".equals(e.getSeverity())).count();
        
        stats.put("错误", (int) errorCount);
        stats.put("警告", (int) warningCount);
        stats.put("信息", (int) infoCount);
        
        return stats;
    }
    
    // 是否有错误级别的问题
    public boolean hasErrors() {
        return allErrors.stream().anyMatch(e -> "ERROR".equals(e.getSeverity()));
    }
    
    // 是否有警告级别的问题
    public boolean hasWarnings() {
        return allErrors.stream().anyMatch(e -> "WARNING".equals(e.getSeverity()));
    }
}