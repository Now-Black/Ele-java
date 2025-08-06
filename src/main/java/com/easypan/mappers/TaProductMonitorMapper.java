package com.easypan.mappers;

import com.easypan.entity.po.TaProductMonitor;
import com.easypan.entity.query.TaProductMonitorQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * TA产品监控Mapper接口
 */
public interface TaProductMonitorMapper extends BaseMapper<TaProductMonitor, TaProductMonitorQuery> {
    
    /**
     * 根据批次ID查询产品监控信息
     */
    List<TaProductMonitor> selectByBatchId(@Param("batchId") String batchId);
    
    /**
     * 根据批次ID和产品代码查询
     */
    TaProductMonitor selectByBatchIdAndProductCode(@Param("batchId") String batchId, 
                                                 @Param("productCode") String productCode);
    
    /**
     * 根据母产品代码查询所有子产品监控信息
     */
    List<TaProductMonitor> selectChildrenByParentCode(@Param("batchId") String batchId,
                                                    @Param("parentCode") String parentCode);
    
    /**
     * 更新产品校验状态
     */
    Integer updateValidationStatus(@Param("id") Long id,
                                  @Param("validationStatus") String validationStatus,
                                  @Param("fieldValidation") String fieldValidation,
                                  @Param("businessValidation") String businessValidation,
                                  @Param("crossValidation") String crossValidation,
                                  @Param("errorCount") Integer errorCount,
                                  @Param("warningCount") Integer warningCount,
                                  @Param("skipReason") String skipReason);
    
    /**
     * 批量更新产品校验状态
     */
    Integer batchUpdateValidationStatus(@Param("list") List<TaProductMonitor> list);
    
    /**
     * 标记产品为已处理
     */
    Integer markAsProcessed(@Param("batchId") String batchId, 
                           @Param("productCodes") List<String> productCodes);
    
    /**
     * 批量插入产品监控记录
     */
    Integer insertBatch(@Param("list") List<TaProductMonitor> list);
    
    /**
     * 查询未处理的产品
     */
    List<TaProductMonitor> selectUnprocessed(@Param("batchId") String batchId);
    
    /**
     * 根据ID更新记录
     */
    Integer updateById(@Param("id") Long id, @Param("bean") TaProductMonitor monitor);
}