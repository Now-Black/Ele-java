package com.easypan.service;

import com.easypan.entity.dto.BatchProcessResult;

import java.util.Date;
import java.util.List;

/**
 * 客户收益计算服务
 */
public interface ClientIncomeCalculationService {

    /**
     * 在事务中处理单个分表
     */
    void processSingleTableWithTransaction(int tableIndex, Date calculateDate);

    /**
     * 重试失败的分表处理
     */
    void retryFailedTable(int tableIndex, Date calculateDate, int retryCount);

    /**
     * 批量重试失败的分表
     */
    BatchProcessResult retryFailedTables(List<Integer> failedTableIndexes, Date calculateDate);
}