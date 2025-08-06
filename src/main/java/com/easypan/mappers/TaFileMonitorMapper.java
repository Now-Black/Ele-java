package com.easypan.mappers;

import com.easypan.entity.po.TaFileMonitor;
import com.easypan.entity.query.TaFileMonitorQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * TA文件监控Mapper接口
 */
public interface TaFileMonitorMapper extends BaseMapper<TaFileMonitor, TaFileMonitorQuery> {
    
    /**
     * 根据批次ID查询文件监控信息
     */
    List<TaFileMonitor> selectByBatchId(@Param("batchId") String batchId);
    
    /**
     * 根据批次ID和文件类型查询
     */
    TaFileMonitor selectByBatchIdAndFileType(@Param("batchId") String batchId, 
                                           @Param("fileType") String fileType);
    
    /**
     * 更新文件解析状态
     */
    Integer updateParseStatus(@Param("id") Long id, 
                             @Param("parseStatus") String parseStatus,
                             @Param("parseTime") Long parseTime,
                             @Param("totalRecords") Integer totalRecords,
                             @Param("validRecords") Integer validRecords,
                             @Param("invalidRecords") Integer invalidRecords,
                             @Param("errorMessage") String errorMessage);
    
    /**
     * 更新文件校验状态
     */
    Integer updateValidationStatus(@Param("id") Long id,
                                  @Param("validationStatus") String validationStatus,
                                  @Param("errorMessage") String errorMessage);
    
    /**
     * 批量插入文件监控记录
     */
    Integer insertBatch(@Param("list") List<TaFileMonitor> list);
}