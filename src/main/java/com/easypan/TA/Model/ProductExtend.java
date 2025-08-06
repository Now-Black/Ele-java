package com.easypan.TA.Model;

import lombok.Data;

@Data
public class ProductExtend {
    
    private String productCode;       // 产品代码
    private String extendField1;      // 扩展字段1
    private String extendField2;      // 扩展字段2
    private String extendField3;      // 扩展字段3
    private String extendField4;      // 扩展字段4
    private String extendField5;      // 扩展字段5
    
    // 运营相关扩展信息
    private String operationStatus;   // 运营状态
    private String marketChannel;     // 销售渠道
    private String targetCustomer;    // 目标客户类型
    private String riskWarning;       // 风险提示
    private String performanceInfo;   // 业绩信息
    
    public ProductExtend() {}
    
    public ProductExtend(String productCode) {
        this.productCode = productCode;
    }
}