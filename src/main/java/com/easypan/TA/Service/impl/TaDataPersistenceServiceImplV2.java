package com.easypan.TA.Service.impl;

import com.easypan.TA.Service.TaDataPersistenceService;
import com.easypan.TA.Utils.SafeBatchProcessor;
import com.easypan.entity.po.TaFileMonitor;
import com.easypan.entity.po.TaProductMonitor;
import com.easypan.entity.po.TaProduct;
import com.easypan.TA.Model.ParsedData;
import com.easypan.TA.Model.ProductInfo;
import com.easypan.TA.Validator.ValidationReport;
import com.easypan.TA.Model.ValidationError;
import com.easypan.mappers.TaFileMonitorMapper;
import com.easypan.mappers.TaProductMonitorMapper;
import com.easypan.mappers.TaProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TA数据入库服务实现类 - 增强版（支持独立事务批量处理）
 */
@Slf4j
@Service
public class TaDataPersistenceServiceImplV2 implements TaDataPersistenceService {
    
    @Resource
    private TaFileMonitorMapper taFileMonitorMapper;
    
    @Resource
    private TaProductMonitorMapper taProductMonitorMapper;
    
    @Resource
    private TaProductMapper taProductMapper;
    
    @Resource
    private SafeBatchProcessor safeBatchProcessor;
    
    // ========== 独立事务批量处理方法 ==========
    
    /**
     * 单批次插入产品监控记录 - 独立事务
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, 
                   timeout = 300, 
                   rollbackFor = Exception.class)
    public Integer insertProductMonitorBatch(List<TaProductMonitor> batch) {
        try {
            int result = taProductMonitorMapper.insertBatch(batch);
            log.debug("产品监控记录批次插入成功: 批次大小={}, 实际插入={}", batch.size(), result);
            return result;
        } catch (Exception e) {
            log.error("产品监控记录批次插入失败: 批次大小={}", batch.size(), e);
            throw e;
        }
    }
    
    /**
     * 单批次插入或更新产品数据 - 独立事务（支持新增和更新混合处理）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, 
                   timeout = 300, 
                   rollbackFor = Exception.class)
    public Integer insertOrUpdateProductBatch(List<TaProduct> batch) {
        try {
            List<TaProduct> toInsert = new ArrayList<>();
            List<TaProduct> toUpdate = new ArrayList<>();
            
            // 根据产品代码查询现有产品，分离新增和更新
            List<String> productCodes = batch.stream()
                .map(TaProduct::getProductCode)
                .collect(Collectors.toList());
            
            List<TaProduct> existingProducts = taProductMapper.selectByProductCodes(productCodes);
            Map<String, TaProduct> existingMap = existingProducts.stream()
                .collect(Collectors.toMap(TaProduct::getProductCode, p -> p));
            
            for (TaProduct product : batch) {
                if (existingMap.containsKey(product.getProductCode())) {
                    // 存在则更新，设置ID
                    TaProduct existing = existingMap.get(product.getProductCode());
                    product.setId(existing.getId());
                    toUpdate.add(product);
                } else {
                    // 不存在则新增
                    toInsert.add(product);
                }
            }
            
            int totalProcessed = 0;
            
            // 批量插入新产品
            if (!toInsert.isEmpty()) {
                int insertCount = taProductMapper.insertBatch(toInsert);
                totalProcessed += insertCount;
                log.debug("批次新增产品: {} 条", insertCount);
            }
            
            // 批量更新现有产品
            if (!toUpdate.isEmpty()) {
                int updateCount = taProductMapper.updateBatch(toUpdate);
                totalProcessed += updateCount;
                log.debug("批次更新产品: {} 条", updateCount);
            }
            
            log.debug("产品数据批次处理成功: 批次大小={}, 新增={}, 更新={}, 总处理={}", 
                batch.size(), toInsert.size(), toUpdate.size(), totalProcessed);
            
            return totalProcessed;
            
        } catch (Exception e) {
            log.error("产品数据批次处理失败: 批次大小={}", batch.size(), e);
            throw e;
        }
    }
    
    /**
     * 单批次更新产品监控状态 - 独立事务
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, 
                   timeout = 300, 
                   rollbackFor = Exception.class)
    public Integer updateProductMonitorBatch(List<TaProductMonitor> batch) {
        try {
            int result = taProductMonitorMapper.batchUpdateValidationStatus(batch);
            log.debug("产品监控状态批次更新成功: 批次大小={}, 实际更新={}", batch.size(), result);
            return result;
        } catch (Exception e) {
            log.error("产品监控状态批次更新失败: 批次大小={}", batch.size(), e);
            throw e;
        }
    }
    
    // ========== 安全分批处理封装方法 ==========
    
    /**
     * 安全的分批插入产品监控记录
     */
    private void safeBatchInsertProductMonitors(List<TaProductMonitor> productMonitors) {
        if (productMonitors == null || productMonitors.isEmpty()) {
            return;
        }
        
        int totalProcessed = safeBatchProcessor.processBatch(
            productMonitors,
            this::insertProductMonitorBatch,
            "产品监控记录插入"
        );
        
        log.info("产品监控记录分批插入完成: 总处理数量={}", totalProcessed);
    }
    
    /**
     * 安全的分批插入/更新产品数据（支持混合操作）
     */
    private void safeBatchInsertOrUpdateProducts(List<TaProduct> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        
        int totalProcessed = safeBatchProcessor.processBatch(
            products,
            this::insertOrUpdateProductBatch,
            "产品数据插入/更新"
        );
        
        log.info("产品数据分批插入/更新完成: 总处理数量={}", totalProcessed);
    }
    
    /**
     * 安全的分批更新产品监控状态
     */
    private void safeBatchUpdateProductMonitors(List<TaProductMonitor> productMonitors) {
        if (productMonitors == null || productMonitors.isEmpty()) {
            return;
        }
        
        int totalProcessed = safeBatchProcessor.processBatch(
            productMonitors,
            this::updateProductMonitorBatch,
            "产品监控状态更新"
        );
        
        log.info("产品监控状态分批更新完成: 总处理数量={}", totalProcessed);
    }
    
    // ========== 文件监控相关 ==========
    
    @Override
    @Transactional
    public TaFileMonitor createFileMonitor(String batchId, String fileName, String fileType, String filePath, Long fileSize) {
        TaFileMonitor fileMonitor = new TaFileMonitor();
        fileMonitor.setBatchId(batchId);
        fileMonitor.setFileName(fileName);
        fileMonitor.setFileType(fileType);
        fileMonitor.setFilePath(filePath);
        fileMonitor.setFileSize(fileSize);
        fileMonitor.setTotalRecords(0);
        fileMonitor.setValidRecords(0);
        fileMonitor.setInvalidRecords(0);
        fileMonitor.setParseStatus("PENDING");
        fileMonitor.setParseTime(0L);
        fileMonitor.setValidationStatus("PENDING");
        
        taFileMonitorMapper.insert(fileMonitor);
        log.info("创建文件监控记录: batchId={}, fileName={}, fileType={}", batchId, fileName, fileType);
        
        return fileMonitor;
    }
    
    @Override
    @Transactional
    public void updateFileParseStatus(String batchId, String fileType, String parseStatus,
                                    Long parseTime, Integer totalRecords, Integer validRecords,
                                    Integer invalidRecords, String errorMessage) {
        
        TaFileMonitor fileMonitor = taFileMonitorMapper.selectByBatchIdAndFileType(batchId, fileType);
        if (fileMonitor != null) {
            taFileMonitorMapper.updateParseStatus(
                fileMonitor.getId(), parseStatus, parseTime, 
                totalRecords, validRecords, invalidRecords, errorMessage
            );
            
            log.info("更新文件解析状态: batchId={}, fileType={}, status={}, totalRecords={}", 
                batchId, fileType, parseStatus, totalRecords);
        }
    }
    
    @Override
    @Transactional
    public void updateFileValidationStatus(String batchId, String fileType, String validationStatus, String errorMessage) {
        TaFileMonitor fileMonitor = taFileMonitorMapper.selectByBatchIdAndFileType(batchId, fileType);
        if (fileMonitor != null) {
            taFileMonitorMapper.updateValidationStatus(fileMonitor.getId(), validationStatus, errorMessage);
            
            log.info("更新文件校验状态: batchId={}, fileType={}, status={}", 
                batchId, fileType, validationStatus);
        }
    }
    
    @Override
    public List<TaFileMonitor> getFileMonitorsByBatch(String batchId) {
        return taFileMonitorMapper.selectByBatchId(batchId);
    }
    
    // ========== 产品监控相关 ==========
    
    @Override
    @Transactional
    public void createProductMonitors(String batchId, ParsedData parsedData, String sourceFile) {
        List<TaProductMonitor> productMonitors = new ArrayList<>();
        
        for (ProductInfo product : parsedData.getAllProducts()) {
            TaProductMonitor monitor = new TaProductMonitor();
            monitor.setBatchId(batchId);
            monitor.setProductCode(product.getProductCode());
            monitor.setProductName(product.getProductName());
            monitor.setProductType(product.getProductType());
            monitor.setParentCode(product.getParentCode());
            monitor.setSourceFile(sourceFile);
            monitor.setValidationStatus("PENDING");
            monitor.setFieldValidation("PENDING");
            monitor.setBusinessValidation("PENDING");
            monitor.setCrossValidation("PENDING");
            monitor.setErrorCount(0);
            monitor.setWarningCount(0);
            monitor.setProcessed(false);
            
            productMonitors.add(monitor);
        }
        
        if (!productMonitors.isEmpty()) {
            // 使用独立事务的安全分批插入方法
            safeBatchInsertProductMonitors(productMonitors);
            log.info("创建产品监控记录完成: batchId={}, 产品数量={}, 来源文件={}", 
                batchId, productMonitors.size(), sourceFile);
        }
    }
    
    @Override
    @Transactional
    public void updateProductValidationStatus(String batchId, ValidationReport validationReport) {
        // 统计各产品的错误和警告数量
        Map<String, Long> errorCounts = validationReport.getAllErrors().stream()
            .filter(error -> StringUtils.isNotBlank(error.getProductCode()))
            .collect(Collectors.groupingBy(ValidationError::getProductCode, Collectors.counting()));
        
        Map<String, Long> warningCounts = validationReport.getAllErrors().stream()
            .filter(error -> StringUtils.isNotBlank(error.getProductCode()) && 
                           "WARNING".equals(error.getSeverity()))
            .collect(Collectors.groupingBy(ValidationError::getProductCode, Collectors.counting()));
        
        // 更新每个产品的监控状态
        List<TaProductMonitor> existingMonitors = taProductMonitorMapper.selectByBatchId(batchId);
        List<TaProductMonitor> updatedMonitors = new ArrayList<>();
        
        for (TaProductMonitor monitor : existingMonitors) {
            String productCode = monitor.getProductCode();
            Long errorCount = errorCounts.getOrDefault(productCode, 0L);
            Long warningCount = warningCounts.getOrDefault(productCode, 0L);
            
            // 根据错误数量确定校验状态
            String validationStatus = "SUCCESS";
            if (errorCount > 0) {
                // 检查是否因母产品失败而跳过
                boolean isSkipped = validationReport.getAllErrors().stream()
                    .anyMatch(error -> productCode.equals(error.getProductCode()) && 
                             "PARENT_VALIDATION_FAILED".equals(error.getErrorCode()));
                validationStatus = isSkipped ? "SKIPPED" : "FAILED";
                
                if (isSkipped) {
                    monitor.setSkipReason("PARENT_FAILED");
                }
            }
            
            monitor.setValidationStatus(validationStatus);
            monitor.setErrorCount(errorCount.intValue());
            monitor.setWarningCount(warningCount.intValue());
            
            // 设置各层校验结果
            monitor.setFieldValidation(getLayerValidationStatus(validationReport, productCode, 1));
            monitor.setBusinessValidation(getLayerValidationStatus(validationReport, productCode, 2));
            monitor.setCrossValidation(getLayerValidationStatus(validationReport, productCode, 3));
            
            updatedMonitors.add(monitor);
        }
        
        if (!updatedMonitors.isEmpty()) {
            // 使用独立事务的安全分批更新方法
            safeBatchUpdateProductMonitors(updatedMonitors);
            log.info("更新产品校验状态完成: batchId={}, 产品数量={}", batchId, updatedMonitors.size());
        }
    }
    
    /**
     * 获取指定层级的校验状态
     */
    private String getLayerValidationStatus(ValidationReport report, String productCode, int layer) {
        Map<Integer, com.easypan.TA.Validator.ValidationResult> layerResults = report.getLayerResults();
        com.easypan.TA.Validator.ValidationResult layerResult = layerResults.get(layer);
        
        if (layerResult == null) {
            return "PENDING";
        }
        
        // 检查该层是否有该产品的错误
        boolean hasError = layerResult.getErrors().stream()
            .anyMatch(error -> productCode.equals(error.getProductCode()));
        
        return hasError ? "FAILED" : "SUCCESS";
    }
    
    @Override
    @Transactional
    public void markProductsAsProcessed(String batchId, List<String> productCodes) {
        if (!productCodes.isEmpty()) {
            taProductMonitorMapper.markAsProcessed(batchId, productCodes);
            log.info("标记产品为已处理: batchId={}, 产品数量={}", batchId, productCodes.size());
        }
    }
    
    @Override
    public List<TaProductMonitor> getUnprocessedProducts(String batchId) {
        return taProductMonitorMapper.selectUnprocessed(batchId);
    }
    
    // ========== 产品数据相关 ==========
    
    @Override
    @Transactional
    public void saveAllProducts(String batchId, ParsedData parsedData) {
        List<TaProduct> products = convertToTaProducts(parsedData.getAllProducts(), batchId);
        
        if (!products.isEmpty()) {
            // 使用独立事务的安全分批插入/更新方法
            safeBatchInsertOrUpdateProducts(products);
            log.info("保存所有产品数据完成: batchId={}, 产品数量={}", batchId, products.size());
        }
    }
    
    @Override
    @Transactional
    public void saveParentProducts(String batchId, List<ProductInfo> parentProducts) {
        List<TaProduct> products = convertToTaProducts(parentProducts, batchId);
        
        if (!products.isEmpty()) {
            // 使用独立事务的安全分批插入/更新方法
            safeBatchInsertOrUpdateProducts(products);
            log.info("保存母产品数据完成: batchId={}, 产品数量={}", batchId, products.size());
        }
    }
    
    @Override
    @Transactional
    public void saveChildProducts(String batchId, List<ProductInfo> childProducts) {
        List<TaProduct> products = convertToTaProducts(childProducts, batchId);
        
        if (!products.isEmpty()) {
            // 使用独立事务的安全分批插入/更新方法
            safeBatchInsertOrUpdateProducts(products);
            log.info("保存子产品数据完成: batchId={}, 产品数量={}", batchId, products.size());
        }
    }
    
    /**
     * 将ProductInfo转换为TaProduct
     */
    private List<TaProduct> convertToTaProducts(List<ProductInfo> productInfos, String batchId) {
        List<TaProduct> products = new ArrayList<>();
        
        for (ProductInfo productInfo : productInfos) {
            TaProduct product = new TaProduct();
            product.setProductCode(productInfo.getProductCode());
            product.setProductName(productInfo.getProductName());
            product.setProductType(productInfo.getProductType());
            product.setParentCode(productInfo.getParentCode());
            product.setStatus(productInfo.getStatus());
            product.setRiskLevel(productInfo.getRiskLevel());
            product.setCurrency(productInfo.getCurrency());
            product.setInvestmentType(productInfo.getInvestmentType());
            product.setDescription(productInfo.getDescription());
            product.setMinAmount(productInfo.getMinAmount());
            product.setMaxAmount(productInfo.getMaxAmount());
            product.setCurrentAmount(productInfo.getCurrentAmount());
            product.setExpectedReturn(productInfo.getExpectedReturn());
            product.setTermDays(productInfo.getTermDays());
            product.setMaxInvestors(productInfo.getMaxInvestors());
            product.setEstablishDate(productInfo.getEstablishDate());
            product.setMaturityDate(productInfo.getMaturityDate());
            product.setIssueDate(productInfo.getIssueDate());
            product.setLastUpdateBatch(batchId);
            
            products.add(product);
        }
        
        return products;
    }
    
    @Override
    public TaProduct getProductByCode(String productCode) {
        return taProductMapper.selectByProductCode(productCode);
    }
    
    @Override
    public List<TaProduct> getAllParentProducts() {
        return taProductMapper.selectAllParentProducts();
    }
    
    @Override
    public List<TaProduct> getChildProductsByParent(String parentCode) {
        return taProductMapper.selectChildrenByParentCode(parentCode);
    }
    
    // ========== 批量处理 ==========
    
    @Override
    @Transactional
    public void performFullDataPersistence(String batchId, String cpdmFilePath, String jycsFilePath,
                                         ParsedData cpdmData, ParsedData jycsData,
                                         ValidationReport validationReport) {
        
        log.info("开始完整数据入库流程（独立事务版本）: batchId={}", batchId);
        
        try {
            // 1. 创建文件监控记录
            if (StringUtils.isNotBlank(cpdmFilePath)) {
                createFileMonitor(batchId, extractFileName(cpdmFilePath), "CPDM", cpdmFilePath, 0L);
                updateFileParseStatus(batchId, "CPDM", "SUCCESS", 0L, 
                    cpdmData.getAllProducts().size(), cpdmData.getAllProducts().size(), 0, null);
            }
            
            if (StringUtils.isNotBlank(jycsFilePath)) {
                createFileMonitor(batchId, extractFileName(jycsFilePath), "JYCS", jycsFilePath, 0L);
                updateFileParseStatus(batchId, "JYCS", "SUCCESS", 0L, 
                    jycsData.getAllProducts().size(), jycsData.getAllProducts().size(), 0, null);
            }
            
            // 2. 创建产品监控记录（使用独立事务批量处理）
            createProductMonitors(batchId, cpdmData, "CPDM");
            if (jycsData != null && !jycsData.getAllProducts().isEmpty()) {
                updateProductMonitorsForJycs(batchId, jycsData);
            }
            
            // 3. 更新校验状态（使用独立事务批量处理）
            updateProductValidationStatus(batchId, validationReport);
            
            // 4. 保存产品数据（使用独立事务批量处理）
            saveAllProducts(batchId, cpdmData);
            
            // 5. 更新文件校验状态
            String overallStatus = validationReport.isSuccess() ? "SUCCESS" : "FAILED";
            updateFileValidationStatus(batchId, "CPDM", overallStatus, null);
            if (StringUtils.isNotBlank(jycsFilePath)) {
                updateFileValidationStatus(batchId, "JYCS", overallStatus, null);
            }
            
            log.info("完整数据入库流程完成（独立事务版本）: batchId={}", batchId);
            
        } catch (Exception e) {
            log.error("完整数据入库流程失败（独立事务版本）: batchId={}", batchId, e);
            throw e;
        }
    }
    
    /**
     * 更新JYCS相关的产品监控记录
     */
    private void updateProductMonitorsForJycs(String batchId, ParsedData jycsData) {
        for (ProductInfo product : jycsData.getAllProducts()) {
            TaProductMonitor existing = taProductMonitorMapper.selectByBatchIdAndProductCode(
                batchId, product.getProductCode());
            if (existing != null) {
                existing.setSourceFile("BOTH");
                // 注意：这里应该使用独立事务，但为了简化，保持原有逻辑
                taProductMonitorMapper.updateById(existing.getId(), existing);
            }
        }
    }
    
    /**
     * 从文件路径提取文件名
     */
    private String extractFileName(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return "";
        }
        int lastSeparatorIndex = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return lastSeparatorIndex >= 0 ? filePath.substring(lastSeparatorIndex + 1) : filePath;
    }
}