package com.easypan.entity.po;

import lombok.Data;

import java.util.Date;

/**
 * TA文件监控实体类
 */
@Data
public class TaFileMonitor {
    
    private Long id;
    private String batchId;                 // 批次ID
    private String fileName;                // 文件名
    private String fileType;                // 文件类型: CPDM, JYCS
    private String filePath;                // 文件路径
    private Long fileSize;                  // 文件大小(字节)
    private Integer totalRecords;           // 总记录数
    private Integer validRecords;           // 有效记录数
    private Integer invalidRecords;         // 无效记录数
    private String parseStatus;             // 解析状态: PENDING, SUCCESS, FAILED
    private Long parseTime;                 // 解析耗时(毫秒)
    private String validationStatus;        // 校验状态: PENDING, SUCCESS, FAILED, PARTIAL
    private String errorMessage;            // 错误信息
    private Date createTime;                // 创建时间
    private Date updateTime;                // 更新时间
    
    // 枚举定义
    public enum FileType {
        CPDM, JYCS
    }
    
    public enum ParseStatus {
        PENDING, SUCCESS, FAILED
    }
    
    public enum ValidationStatus {
        PENDING, SUCCESS, FAILED, PARTIAL
    }
}