package com.easypan.entity.query;

import lombok.Data;

/**
 * TA文件监控查询参数
 */
@Data
public class TaFileMonitorQuery extends BaseParam {
    
    private String batchId;
    private String fileType;
    private String parseStatus;
    private String validationStatus;
}