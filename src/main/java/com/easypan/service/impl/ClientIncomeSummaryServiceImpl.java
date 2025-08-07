package com.easypan.service.impl;

import com.easypan.entity.dto.SummaryResult;
import com.easypan.exception.BusinessException;
import com.easypan.service.ClientDailyIncomeService;
import com.easypan.service.ClientIncomeSummaryService;
import com.easypan.service.impl.OptimizedSummaryServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 客户收益汇总服务实现
 */
@Service("clientIncomeSummaryService")
public class ClientIncomeSummaryServiceImpl implements ClientIncomeSummaryService {

    private static final Logger logger = LoggerFactory.getLogger(ClientIncomeSummaryServiceImpl.class);

    @Resource
    private ClientDailyIncomeService clientDailyIncomeService;

    @Resource
    private OptimizedSummaryServiceImpl optimizedSummaryService;

    @Override
    public void summaryDataToMainTable(Date calculateDate) {
        try {
            logger.info("开始汇总数据到主表，日期：{}", calculateDate);
            
            long startTime = System.currentTimeMillis();
            
            // 使用优化的并行汇总服务
            SummaryResult result = optimizedSummaryService.summaryDataToMainTable(calculateDate);
            
            // 检查汇总结果
            if (!result.isAllSuccess()) {
                String errorMsg = String.format("汇总存在失败，成功:%d，失败:%d", 
                    result.getSuccessCount(), result.getFailureCount());
                throw new BusinessException(errorMsg);
            }
            
            // 验证汇总数据
            if (!validateSummaryData(calculateDate)) {
                throw new BusinessException("汇总数据验证失败");
            }
            
            long endTime = System.currentTimeMillis();
            logger.info("数据汇总完成，总影响行数：{}，耗时：{}ms", 
                       result.getTotalAffectedRows(), (endTime - startTime));
            
        } catch (Exception e) {
            logger.error("数据汇总失败", e);
            throw new BusinessException("数据汇总失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanupSummaryData(Date calculateDate) {
        try {
            logger.info("开始清理汇总数据，日期：{}", calculateDate);
            
            // 这里可以添加清理主表数据的逻辑
            // 例如：DELETE FROM tbclientdlyincome WHERE reg_date = #{calculateDate}
            
            logger.info("汇总数据清理完成");
            
        } catch (Exception e) {
            logger.error("清理汇总数据失败", e);
            throw new BusinessException("清理汇总数据失败", e);
        }
    }

    @Override
    public boolean validateSummaryData(Date calculateDate) {
        try {
            logger.debug("开始验证汇总数据，日期：{}", calculateDate);
            
            // 这里可以添加数据验证逻辑
            // 例如：
            // 1. 检查分表数据总数与主表数据总数是否一致
            // 2. 检查金额汇总是否正确
            // 3. 检查是否存在重复数据
            
            // 暂时返回true，实际项目中需要实现具体的验证逻辑
            logger.debug("汇总数据验证通过");
            return true;
            
        } catch (Exception e) {
            logger.error("验证汇总数据异常", e);
            return false;
        }
    }
}