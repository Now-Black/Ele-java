package com.easypan.TA.Service;

import com.easypan.entity.po.TaFileMonitor;
import com.easypan.entity.po.TaProductMonitor;
import com.easypan.entity.po.TaProduct;
import com.easypan.TA.Model.ParsedData;
import com.easypan.TA.Model.ProductInfo;
import com.easypan.TA.Validator.ValidationReport;
import com.easypan.TA.Validator.ValidationResult;
import com.easypan.TA.Model.ValidationError;

import java.util.List;

/**
 * TA数据入库服务接口
 */
public interface TaDataPersistenceService {
    
    // ========== 文件监控相关 ==========
    
    /**
     * 创建文件监控记录
     */
    TaFileMonitor createFileMonitor(String batchId, String fileName, String fileType, String filePath, Long fileSize);
    
    /**
     * 更新文件解析状态
     */
    void updateFileParseStatus(String batchId, String fileType, String parseStatus, 
                              Long parseTime, Integer totalRecords, Integer validRecords, 
                              Integer invalidRecords, String errorMessage);
    
    /**
     * 更新文件校验状态
     */
    void updateFileValidationStatus(String batchId, String fileType, String validationStatus, String errorMessage);
    
    /**
     * 根据批次ID查询文件监控信息
     */
    List<TaFileMonitor> getFileMonitorsByBatch(String batchId);
    
    // ========== 产品监控相关 ==========
    
    /**
     * 创建产品监控记录
     */
    void createProductMonitors(String batchId, ParsedData parsedData, String sourceFile);
    
    /**
     * 更新产品校验状态
     */
    void updateProductValidationStatus(String batchId, ValidationReport validationReport);
    
    /**
     * 标记产品为已处理
     */
    void markProductsAsProcessed(String batchId, List<String> productCodes);
    
    /**
     * 查询未处理的产品
     */
    List<TaProductMonitor> getUnprocessedProducts(String batchId);
    
    // ========== 产品数据相关 ==========
    
    /**
     * 保存所有产品数据
     */
    void saveAllProducts(String batchId, ParsedData parsedData);
    
    /**
     * 保存母产品数据
     */
    void saveParentProducts(String batchId, List<ProductInfo> parentProducts);
    
    /**
     * 保存子产品数据
     */
    void saveChildProducts(String batchId, List<ProductInfo> childProducts);
    
    /**
     * 根据产品代码查询产品
     */
    TaProduct getProductByCode(String productCode);
    
    /**
     * 查询所有母产品
     */
    List<TaProduct> getAllParentProducts();
    
    /**
     * 根据母产品代码查询子产品
     */
    List<TaProduct> getChildProductsByParent(String parentCode);
    
    // ========== 批量处理 ==========
    
    /**
     * 完整的数据入库流程
     */
    void performFullDataPersistence(String batchId, String cpdmFilePath, String jycsFilePath, 
                                   ParsedData cpdmData, ParsedData jycsData, 
                                   ValidationReport validationReport);
}