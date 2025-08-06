package com.easypan.entity.po;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * TA产品主表实体类
 */
@Data
public class TaProduct {
    
    private Long id;
    private String productCode;             // 产品代码
    private String productName;             // 产品名称
    private String productType;             // 产品类型: 母产品, 子产品
    private String parentCode;              // 母产品代码
    private String status;                  // 产品状态
    private String riskLevel;               // 风险等级
    private String currency;                // 币种
    private String investmentType;          // 投资类型
    private String description;             // 产品描述
    private BigDecimal minAmount;           // 最小募集金额
    private BigDecimal maxAmount;           // 最大募集金额
    private BigDecimal currentAmount;       // 当前募集金额
    private BigDecimal expectedReturn;      // 预期收益率
    private Integer termDays;               // 期限天数
    private Integer maxInvestors;           // 允许最大购买人数
    private Date establishDate;             // 成立日期
    private Date maturityDate;              // 到期日期
    private Date issueDate;                 // 发行日期
    private String lastUpdateBatch;         // 最后更新批次
    private Date createTime;                // 创建时间
    private Date updateTime;                // 更新时间
}