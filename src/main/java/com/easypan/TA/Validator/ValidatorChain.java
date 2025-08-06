package com.easypan.TA.Validator;

import com.easypan.TA.Model.ParsedData;
import com.easypan.TA.Model.ProductInfo;
import com.easypan.TA.Model.ValidationError;
import com.easypan.TA.Parser.CpdmParser;
import com.easypan.TA.Parser.JycsParser;
import com.easypan.TA.Service.TaDataPersistenceService;
import com.easypan.TA.Service.TaCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class ValidatorChain {
    
    private final FieldValidator fieldValidator;
    private final BusinessValidator businessValidator;
    private final CrossFileValidator crossFileValidator;
    private final CpdmParser cpdmParser;
    private final JycsParser jycsParser;
    
    @Resource
    private TaDataPersistenceService taDataPersistenceService;
    
    @Resource(name = "taDataPersistenceServiceV3")
    private TaDataPersistenceService parallelDataPersistenceService;
    
    @Resource
    private TaCacheService taCacheService;
    
    public ValidatorChain() {
        this.fieldValidator = new FieldValidator();
        this.businessValidator = new BusinessValidator();
        this.crossFileValidator = new CrossFileValidator();
        this.cpdmParser = new CpdmParser();
        this.jycsParser = new JycsParser();
    }
    
    /**
     * 执行完整的三层校验并进行数据入库和缓存刷新
     */
    public ValidationReport validateFiles(String cpdmFilePath, String jycsFilePath) {
        long totalStartTime = System.currentTimeMillis();
        String batchId = generateBatchId();
        
        ValidationReport report = new ValidationReport(
            extractFileName(cpdmFilePath), 
            extractFileName(jycsFilePath)
        );
        
        try {
            log.info("开始执行文件校验和数据处理: batchId={}, CPDM[{}], JYCS[{}]", 
                batchId, cpdmFilePath, jycsFilePath);
            
            // 1. 解析文件
            ParsedData cpdmData = null;
            ParsedData jycsData = null;
            
            try {
                cpdmData = cpdmParser.parseCpdmFile(cpdmFilePath);
                log.info("CPDM文件解析完成: {}", cpdmData.getStatistics());
            } catch (Exception e) {
                log.error("CPDM文件解析失败", e);
                report.addError(new ValidationError("CPDM_PARSE_FAILED", 
                    "CPDM文件解析失败: " + e.getMessage()));
                report.setSuccess(false);
                return report;
            }
            
            try {
                jycsData = jycsParser.parseJycsFile(jycsFilePath);
                log.info("JYCS文件解析完成: {}", jycsData.getStatistics());
            } catch (Exception e) {
                log.error("JYCS文件解析失败", e);
                report.addError(new ValidationError("JYCS_PARSE_FAILED", 
                    "JYCS文件解析失败: " + e.getMessage()));
                report.setSuccess(false);
                return report;
            }
            
            // 2. 第一层：字段级校验（仅CPDM文件，智能跳过失败母产品的子产品）
            log.info("开始第一层字段级校验 (仅CPDM文件，启用智能校验策略)");
            ValidationResult fieldResult = performFieldValidation(cpdmData, jycsData);
            report.addLayerResult(1, fieldResult);
            log.info("第一层校验完成: {}", fieldResult.isSuccess() ? "通过" : "失败");
            
            // 3. 第二层：业务逻辑校验（仅CPDM文件，包含母子产品购买人数累加校验和智能校验策略）
            log.info("开始第二层业务逻辑校验 (仅CPDM文件，启用智能校验策略)");
            ValidationResult businessResult = performBusinessValidation(cpdmData, jycsData);
            report.addLayerResult(2, businessResult);
            log.info("第二层校验完成: {}", businessResult.isSuccess() ? "通过" : "失败");
            
            // 4. 第三层：文件交叉校验（JYCS文件主要用于此层校验）
            log.info("开始第三层文件交叉校验 (CPDM与JYCS对比)");
            ValidationResult crossResult = performCrossFileValidation(cpdmData, jycsData);
            report.addLayerResult(3, crossResult);
            log.info("第三层校验完成: {}", crossResult.isSuccess() ? "通过" : "失败");
            
            // 5. 设置整体结果
            boolean overallSuccess = fieldResult.isSuccess() && 
                                   businessResult.isSuccess() && 
                                   crossResult.isSuccess();
            report.setSuccess(overallSuccess);
            
            // 6. 统计总时间
            report.setTotalValidationTime(System.currentTimeMillis() - totalStartTime);
            
            log.info("文件校验完成，整体结果: {}, 总耗时: {}ms", 
                overallSuccess ? "通过" : "失败", report.getTotalValidationTime());
            
            // 7. 执行数据入库和缓存刷新
            performDataPersistenceAndCacheRefresh(batchId, cpdmFilePath, jycsFilePath, 
                cpdmData, jycsData, report);
            
        } catch (Exception e) {
            log.error("校验过程发生系统异常", e);
            report.addError(new ValidationError("SYSTEM_ERROR", 
                "校验过程发生系统异常: " + e.getMessage()));
            report.setSuccess(false);
            report.setTotalValidationTime(System.currentTimeMillis() - totalStartTime);
        }
        
        // 生成校验报告
        report.generateSummary();
        
        return report;
    }
    
    /**
     * 只校验单个CPDM文件
     */
    public ValidationReport validateCpdmFile(String cpdmFilePath) {
        long startTime = System.currentTimeMillis();
        ValidationReport report = new ValidationReport(extractFileName(cpdmFilePath), null);
        
        try {
            log.info("开始校验CPDM文件: {}", cpdmFilePath);
            
            // 解析文件
            ParsedData cpdmData = cpdmParser.parseCpdmFile(cpdmFilePath);
            
            // 执行字段校验和业务校验
            ValidationResult fieldResult = performFieldValidationForSingleFile(cpdmData);
            report.addLayerResult(1, fieldResult);
            
            ValidationResult businessResult = performBusinessValidationForSingleFile(cpdmData);
            report.addLayerResult(2, businessResult);
            
            report.setSuccess(fieldResult.isSuccess() && businessResult.isSuccess());
            report.setTotalValidationTime(System.currentTimeMillis() - startTime);
            
            log.info("CPDM文件校验完成: {}", report.isSuccess() ? "通过" : "失败");
            
        } catch (Exception e) {
            log.error("CPDM文件校验异常", e);
            report.addError(new ValidationError("VALIDATION_ERROR", 
                "CPDM文件校验异常: " + e.getMessage()));
            report.setSuccess(false);
        }
        
        report.generateSummary();
        return report;
    }
    
    /**
     * 只校验单个JYCS文件 - 仅进行文件格式校验，不进行产品校验
     */
    public ValidationReport validateJycsFile(String jycsFilePath) {
        long startTime = System.currentTimeMillis();
        ValidationReport report = new ValidationReport(null, extractFileName(jycsFilePath));
        
        try {
            log.info("开始校验JYCS文件: {}", jycsFilePath);
            
            // 解析文件 - 主要验证文件格式是否正确
            ParsedData jycsData = jycsParser.parseJycsFile(jycsFilePath);
            
            // JYCS文件只需要验证解析成功即可，不需要详细的产品校验
            // 产品校验由CPDM文件负责，JYCS主要用于第三层交叉校验
            ValidationResult parseResult = new ValidationResult(true);
            parseResult.setMessage("JYCS文件解析成功，共解析" + jycsData.getAllProducts().size() + "条产品记录");
            report.addLayerResult(1, parseResult);
            
            report.setSuccess(true);
            report.setTotalValidationTime(System.currentTimeMillis() - startTime);
            
            log.info("JYCS文件校验完成: 通过 (仅验证文件格式)");
            
        } catch (Exception e) {
            log.error("JYCS文件校验异常", e);
            report.addError(new ValidationError("VALIDATION_ERROR", 
                "JYCS文件校验异常: " + e.getMessage()));
            report.setSuccess(false);
        }
        
        report.generateSummary();
        return report;
    }
    
    // ========== 私有校验方法 ==========
    
    /**
     * 执行第一层字段级校验 - 只校验CPDM文件，实现母产品失败时子产品跳过策略
     */
    private ValidationResult performFieldValidation(ParsedData cpdmData, ParsedData jycsData) {
        long startTime = System.currentTimeMillis();
        ValidationResult result = new ValidationResult(true);
        
        // 使用智能校验策略：母产品失败时子产品跳过校验
        ValidationResult smartResult = performSmartValidation(cpdmData, 1);
        result.merge(smartResult);
        
        result.setValidationTime(System.currentTimeMillis() - startTime);
        return result;
    }
    
    /**
     * 执行第二层业务逻辑校验 - 只校验CPDM文件，实现母产品失败时子产品跳过策略
     */
    private ValidationResult performBusinessValidation(ParsedData cpdmData, ParsedData jycsData) {
        long startTime = System.currentTimeMillis();
        ValidationResult result = new ValidationResult(true);
        
        // 使用智能校验策略：母产品失败时子产品跳过校验
        ValidationResult smartResult = performSmartValidation(cpdmData, 2);
        result.merge(smartResult);
        
        result.setValidationTime(System.currentTimeMillis() - startTime);
        return result;
    }
    
    /**
     * 执行第三层文件交叉校验
     */
    private ValidationResult performCrossFileValidation(ParsedData cpdmData, ParsedData jycsData) {
        return crossFileValidator.validateCrossFile(cpdmData, jycsData);
    }
    
    /**
     * 智能校验策略：母产品校验失败时，其子产品跳过校验并标记为失败
     * @param parsedData 解析的数据
     * @param validationLayer 校验层级 (1-字段级, 2-业务级)
     * @return 校验结果
     */
    private ValidationResult performSmartValidation(ParsedData parsedData, int validationLayer) {
        ValidationResult result = new ValidationResult(true);
        
        if (validationLayer == 1) {
            // 第一层：逐个产品进行字段级校验
            Set<String> failedParentCodes = validateParentProducts(parsedData, validationLayer, result);
            validateChildProducts(parsedData, validationLayer, failedParentCodes, result);
            
        } else if (validationLayer == 2) {
            // 第二层：需要完整的业务逻辑校验，包括母子产品关联等
            // 先进行完整的业务逻辑校验
            ValidationResult businessResult = businessValidator.validateBusinessLogic(parsedData);
            result.merge(businessResult);
            
            // 如果有母产品校验失败，需要额外标记其子产品
            Set<String> failedParentCodes = identifyFailedParentProducts(parsedData, businessResult);
            if (!failedParentCodes.isEmpty()) {
                markChildProductsAsFailedDueToParent(parsedData, failedParentCodes, result);
            }
        }
        
        return result;
    }
    
    /**
     * 从业务校验结果中识别校验失败的母产品
     */
    private Set<String> identifyFailedParentProducts(ParsedData parsedData, ValidationResult businessResult) {
        Set<String> failedParentCodes = new HashSet<>();
        
        // 从错误信息中提取失败的母产品代码
        for (ValidationError error : businessResult.getErrors()) {
            String productCode = error.getProductCode();
            if (productCode != null) {
                // 检查是否为母产品
                for (ProductInfo parent : parsedData.getParentProducts()) {
                    if (parent.getProductCode().equals(productCode)) {
                        failedParentCodes.add(productCode);
                        break;
                    }
                }
            }
        }
        
        return failedParentCodes;
    }
    
    /**
     * 标记失败母产品的子产品为失败状态
     */
    private void markChildProductsAsFailedDueToParent(ParsedData parsedData, 
                                                    Set<String> failedParentCodes, 
                                                    ValidationResult result) {
        
        for (ProductInfo child : parsedData.getChildProducts()) {
            String parentCode = child.getParentCode();
            
            if (failedParentCodes.contains(parentCode)) {
                ValidationError skipError = new ValidationError(
                    "PARENT_VALIDATION_FAILED",
                    String.format("子产品[%s]因母产品[%s]校验失败而标记为失败", 
                        child.getProductCode(), parentCode),
                    "parentCode", 
                    child.getProductCode()
                );
                
                ValidationResult skipResult = ValidationResult.failure(Arrays.asList(skipError));
                result.merge(skipResult);
                
                log.info("子产品[{}]因母产品[{}]校验失败而标记为失败", 
                    child.getProductCode(), parentCode);
            }
        }
    }
    
    /**
     * 校验所有母产品
     * @param parsedData 解析的数据
     * @param validationLayer 校验层级
     * @param result 校验结果容器
     * @return 校验失败的母产品代码集合
     */
    private Set<String> validateParentProducts(ParsedData parsedData, int validationLayer, ValidationResult result) {
        Set<String> failedParentCodes = new HashSet<>();
        
        for (ProductInfo product : parsedData.getParentProducts()) {
            ValidationResult productResult;
            
            if (validationLayer == 1) {
                // 第一层：字段级校验
                productResult = fieldValidator.validateProduct(product);
            } else {
                // 第二层：业务逻辑校验（只校验单个产品相关的业务逻辑）
                productResult = businessValidator.validateProductParameters(
                    createSingleProductData(product));
            }
            
            if (!productResult.isSuccess()) {
                failedParentCodes.add(product.getProductCode());
                log.warn("母产品[{}]校验失败，其子产品将跳过校验", product.getProductCode());
            }
            
            result.merge(productResult);
        }
        
        return failedParentCodes;
    }
    
    /**
     * 校验子产品（跳过失败母产品的子产品）
     * @param parsedData 解析的数据
     * @param validationLayer 校验层级
     * @param failedParentCodes 失败的母产品代码集合
     * @param result 校验结果容器
     */
    private void validateChildProducts(ParsedData parsedData, int validationLayer, 
                                     Set<String> failedParentCodes, ValidationResult result) {
        
        for (ProductInfo product : parsedData.getChildProducts()) {
            String parentCode = product.getParentCode();
            
            // 检查该子产品的母产品是否校验失败
            if (failedParentCodes.contains(parentCode)) {
                // 母产品失败，子产品直接标记为失败，不进行具体校验
                ValidationError skipError = new ValidationError(
                    "PARENT_VALIDATION_FAILED",
                    String.format("子产品[%s]因母产品[%s]校验失败而跳过校验", 
                        product.getProductCode(), parentCode),
                    "parentCode", 
                    product.getProductCode()
                );
                
                ValidationResult skipResult = ValidationResult.failure(Arrays.asList(skipError));
                result.merge(skipResult);
                
                log.info("子产品[{}]因母产品[{}]校验失败而跳过校验", 
                    product.getProductCode(), parentCode);
                
            } else {
                // 母产品校验成功，正常校验子产品
                ValidationResult productResult;
                
                if (validationLayer == 1) {
                    // 第一层：字段级校验
                    productResult = fieldValidator.validateProduct(product);
                } else {
                    // 第二层：业务逻辑校验（包含母子产品关联校验等）
                    productResult = businessValidator.validateProductParameters(
                        createSingleProductData(product));
                }
                
                result.merge(productResult);
            }
        }
    }
    
    /**
     * 创建单个产品的ParsedData用于独立校验
     */
    private ParsedData createSingleProductData(ProductInfo product) {
        List<ProductInfo> singleProduct = Arrays.asList(product);
        return new ParsedData(singleProduct.stream().toString());
    }
    
    /**
     * 单文件的字段级校验
     */
    private ValidationResult performFieldValidationForSingleFile(ParsedData parsedData) {
        long startTime = System.currentTimeMillis();
        ValidationResult result = new ValidationResult(true);
        
        for (ProductInfo product : parsedData.getAllProducts()) {
            ValidationResult productResult = fieldValidator.validateProduct(product);
            result.merge(productResult);
        }
        
        result.setValidationTime(System.currentTimeMillis() - startTime);
        return result;
    }
    
    /**
     * 单文件的业务逻辑校验
     */
    private ValidationResult performBusinessValidationForSingleFile(ParsedData parsedData) {
        return businessValidator.validateBusinessLogic(parsedData);
    }
    
    /**
     * 从文件路径提取文件名
     */
    private String extractFileName(String filePath) {
        if (filePath == null) {
            return null;
        }
        
        int lastSeparatorIndex = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return lastSeparatorIndex >= 0 ? filePath.substring(lastSeparatorIndex + 1) : filePath;
    }
    
    /**
     * 生成批次ID
     */
    private String generateBatchId() {
        return "BATCH_" + System.currentTimeMillis();
    }
    
    /**
     * 执行数据入库和缓存刷新
     */
    private void performDataPersistenceAndCacheRefresh(String batchId, String cpdmFilePath, String jycsFilePath,
                                                     ParsedData cpdmData, ParsedData jycsData, 
                                                     ValidationReport validationReport) {
        
        log.info("开始执行数据入库和缓存刷新: batchId={}", batchId);
        
        try {
            // 1. 执行完整的数据入库流程（使用并行优化版本）
            parallelDataPersistenceService.performFullDataPersistence(
                batchId, cpdmFilePath, jycsFilePath, cpdmData, jycsData, validationReport);
            
            log.info("数据入库完成（并行优化）: batchId={}", batchId);
            
            // 2. 增量刷新产品数据到内存和Redis（不重置缓存，只更新变更的数据）
            log.info("开始增量更新产品数据到缓存: batchId={}", batchId);
            
            // 将CPDM数据转换为TaProduct，用于缓存更新
            List<com.easypan.entity.po.TaProduct> updatedProducts = convertToTaProducts(cpdmData.getAllProducts(), batchId);
            
            // 增量更新产品缓存
            taCacheService.incrementalUpdateProductsCache(updatedProducts);
            
            log.info("产品数据增量缓存更新完成: batchId={}, 更新产品数量={}", batchId, updatedProducts.size());
            
            // 3. 刷新文件监控数据到Redis（产品监控数据不刷新）
            log.info("开始刷新文件监控数据到Redis: batchId={}", batchId);
            taCacheService.refreshFileMonitorToRedis(batchId);
            
            log.info("文件监控数据缓存刷新完成: batchId={}", batchId);
            
            log.info("数据入库和缓存刷新全部完成: batchId={}", batchId);
            
        } catch (Exception e) {
            log.error("数据入库和缓存刷新失败: batchId={}", batchId, e);
            // 不抛出异常，避免影响校验结果的返回
            validationReport.addError(new ValidationError("DATA_PERSISTENCE_ERROR", 
                "数据入库和缓存刷新失败: " + e.getMessage()));
        }
    }
    
    /**
     * 将ProductInfo转换为TaProduct
     */
    private List<com.easypan.entity.po.TaProduct> convertToTaProducts(List<ProductInfo> productInfos, String batchId) {
        List<com.easypan.entity.po.TaProduct> products = new ArrayList<>();
        
        for (ProductInfo productInfo : productInfos) {
            com.easypan.entity.po.TaProduct product = new com.easypan.entity.po.TaProduct();
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
}