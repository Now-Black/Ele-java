package com.easypan.service;

import java.util.Date;

/**
 * 客户收益汇总服务
 */
public interface ClientIncomeSummaryService {

    /**
     * 汇总数据到主表（独立事务）
     */
    void summaryDataToMainTable(Date calculateDate);

    /**
     * 清理汇总数据
     */
    void cleanupSummaryData(Date calculateDate);

    /**
     * 验证汇总数据的完整性
     */
    boolean validateSummaryData(Date calculateDate);
}