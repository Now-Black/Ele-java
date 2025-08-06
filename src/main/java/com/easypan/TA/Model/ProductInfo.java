package com.easypan.TA.Model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ProductInfo {
    
    // 基础信息
    private String productCode;        // 产品代码
    private String productName;        // 产品名称
    private String productType;        // 产品类型（母产品/子产品）
    private String parentCode;         // 母产品代码（子产品用）
    
    // 金额相关
    private BigDecimal minAmount;      // 最小募集金额
    private BigDecimal maxAmount;      // 最大募集金额
    private BigDecimal currentAmount;  // 当前募集金额
    
    // 日期相关
    private Date establishDate;        // 成立日期
    private Date maturityDate;         // 到期日期
    private Date issueDate;           // 发行日期
    
    // 其他属性
    private String riskLevel;         // 风险等级
    private String status;            // 产品状态
    private String description;       // 产品描述
    
    // 扩展属性
    private String currency;          // 币种
    private BigDecimal expectedReturn; // 预期收益率
    private String investmentType;    // 投资类型
    private Integer termDays;         // 期限天数
    private Integer maxInvestors;     // 允许最大购买人数
    
    // 构造方法
    public ProductInfo() {}
    
    public ProductInfo(String productCode) {
        this.productCode = productCode;
    }
    
    // 判断是否为母产品
    public boolean isParentProduct() {
        return "母产品".equals(this.productType) || "PARENT".equalsIgnoreCase(this.productType);
    }
    
    // 判断是否为子产品
    public boolean isChildProduct() {
        return "子产品".equals(this.productType) || "CHILD".equalsIgnoreCase(this.productType);
    }
}