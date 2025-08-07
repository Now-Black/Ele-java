package com.easypan.service.impl;

import com.easypan.entity.dto.BatchProcessResult;
import com.easypan.entity.dto.TableProcessResult;
import com.easypan.exception.BusinessException;
import com.easypan.service.ClientDailyIncomeService;
import com.easypan.service.ClientIncomeCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 客户收益计算服务实现
 */
@Service("clientIncomeCalculationService")
public class ClientIncomeCalculationServiceImpl implements ClientIncomeCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(ClientIncomeCalculationServiceImpl.class);

    @Resource
    private ClientDailyIncomeService clientDailyIncomeService;

    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 300)
    public void processSingleTableWithTransaction(int tableIndex, Date calculateDate) {
        try {
            logger.debug("开始计算分表 {} 的数据", tableIndex);
            
            long startTime = System.currentTimeMillis();
            
            // 调用原有的单表处理逻辑
            clientDailyIncomeService.processSingleTable(tableIndex, calculateDate);
            
            long endTime = System.currentTimeMillis();
            logger.debug("分表 {} 计算完成，耗时：{}ms", tableIndex, (endTime - startTime));
            
        } catch (Exception e) {
            logger.error("分表 {} 计算异常", tableIndex, e);
            throw new BusinessException(
                String.format("分表%d计算失败", tableIndex), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 300)
    public void retryFailedTable(int tableIndex, Date calculateDate, int retryCount) {
        logger.info("开始重试分表 {}，重试次数：{}", tableIndex, retryCount);
        
        try {
            // 清理之前可能的部分数据
            clientDailyIncomeService.cleanupSingleTable(tableIndex, calculateDate);
            
            // 重新处理
            processSingleTableWithTransaction(tableIndex, calculateDate);
            
            logger.info("分表 {} 重试成功", tableIndex);
            
        } catch (Exception e) {
            logger.error("分表 {} 第{}次重试失败", tableIndex, retryCount, e);
            throw new BusinessException(
                String.format("分表%d重试%d次后仍失败", tableIndex, retryCount), e);
        }
    }

    @Override
    public BatchProcessResult retryFailedTables(List<Integer> failedTableIndexes, Date calculateDate) {
        logger.info("开始重试失败的分表，数量：{}", failedTableIndexes.size());
        
        List<TableProcessResult> results = new ArrayList<>();
        
        for (Integer tableIndex : failedTableIndexes) {
            try {
                retryFailedTable(tableIndex, calculateDate, 1);
                results.add(TableProcessResult.success(tableIndex));
                
            } catch (Exception e) {
                results.add(TableProcessResult.failure(tableIndex, e));
            }
        }
        
        BatchProcessResult batchResult = new BatchProcessResult(results);
        logger.info("重试完成，成功：{}，失败：{}", 
                   batchResult.getSuccessCount(), batchResult.getFailureCount());
        
        return batchResult;
    }
}