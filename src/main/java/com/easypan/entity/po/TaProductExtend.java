package com.easypan.entity.po;

import lombok.Data;

import java.util.Date;

/**
 * TA产品扩展表实体类
 */
@Data
public class TaProductExtend {
    
    private Long id;
    private String productCode;             // 产品代码
    private String extendField;             // 扩展字段名
    private String extendValue;             // 扩展字段值
    private String dataType;                // 数据类型: STRING, NUMBER, DATE, BOOLEAN
    private Date createTime;                // 创建时间
    private Date updateTime;                // 更新时间
    
    // 枚举定义
    public enum DataType {
        STRING, NUMBER, DATE, BOOLEAN
    }
}