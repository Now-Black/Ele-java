package com.easypan.entity.query;

import lombok.Data;

/**
 * TA产品查询参数
 */
@Data
public class TaProductQuery extends BaseParam {
    
    private String productCode;
    private String productType;
    private String parentCode;
    private String status;
    private String lastUpdateBatch;
}