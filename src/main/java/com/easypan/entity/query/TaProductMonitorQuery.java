package com.easypan.entity.query;

import lombok.Data;

/**
 * TA产品监控查询参数
 */
@Data
public class TaProductMonitorQuery extends BaseParam {
    
    private String batchId;
    private String productCode;
    private String productType;
    private String validationStatus;
    private String parentCode;
    private Boolean processed;
}