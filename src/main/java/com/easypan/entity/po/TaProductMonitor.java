package com.easypan.entity.po;

import lombok.Data;

import java.util.Date;

/**
 * TA产品监控实体类
 */
@Data
public class TaProductMonitor {
    
    private Long id;
    private String batchId;                 // 批次ID
    private String productCode;             // 产品代码
    private String productName;             // 产品名称
    private String productType;             // 产品类型: 母产品, 子产品
    private String parentCode;              // 母产品代码
    private String sourceFile;              // 数据来源文件: CPDM, JYCS, BOTH
    private String validationStatus;        // 校验状态: SUCCESS, FAILED, SKIPPED
    private String skipReason;              // 跳过原因: PARENT_FAILED等
    private String fieldValidation;         // 字段校验结果: SUCCESS, FAILED, SKIPPED
    private String businessValidation;      // 业务校验结果: SUCCESS, FAILED, SKIPPED
    private String crossValidation;         // 交叉校验结果: SUCCESS, FAILED, SKIPPED
    private Integer errorCount;             // 错误数量
    private Integer warningCount;           // 警告数量
    private Boolean processed;              // 是否已处理入库
    private Date createTime;                // 创建时间
    private Date updateTime;                // 更新时间
    
    // 枚举定义
    public enum ProductType {
        PARENT("母产品"), CHILD("子产品");
        
        private final String value;
        ProductType(String value) { this.value = value; }
        public String getValue() { return value; }
    }
    
    public enum SourceFile {
        CPDM, JYCS, BOTH
    }
    
    public enum ValidationStatus {
        SUCCESS, FAILED, SKIPPED, PENDING
    }
    
    public enum SkipReason {
        PARENT_FAILED("母产品校验失败"), PARSE_FAILED("解析失败"), SYSTEM_ERROR("系统错误");
        
        private final String description;
        SkipReason(String description) { this.description = description; }
        public String getDescription() { return description; }
    }
}