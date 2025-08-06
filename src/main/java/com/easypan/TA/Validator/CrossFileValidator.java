package com.easypan.TA.Validator;

import com.easypan.TA.Model.ParsedData;
import com.easypan.TA.Model.ProductInfo;
import com.easypan.TA.Model.ValidationError;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class CrossFileValidator {
    
    /**
     * CPDM与JYCS文件数据条数一致性校验
     */
    public ValidationResult validateDataCountConsistency(ParsedData cpdmData, ParsedData jycsData) {
        long startTime = System.currentTimeMillis();
        List<ValidationError> errors = new ArrayList<>();
        
        try {
            // 比较子产品数量（因为JYCS主要包含子产品运营信息）
            int cpdmChildCount = cpdmData.getChildProducts().size();
            int jycsCount = jycsData.getAllProducts().size();
            
            log.info("CPDM文件子产品数量: {}, JYCS文件产品数量: {}", cpdmChildCount, jycsCount);
            
            if (cpdmChildCount != jycsCount) {
                errors.add(new ValidationError("DATA_COUNT_MISMATCH", 
                    String.format("CPDM文件子产品数量[%d]与JYCS文件产品数量[%d]不一致", 
                    cpdmChildCount, jycsCount)));
            }
            
            // 检查总产品数量差异是否合理
            int cpdmTotalCount = cpdmData.getAllProducts().size();
            int cpdmParentCount = cpdmData.getParentProducts().size();
            
            if (Math.abs(cpdmTotalCount - jycsCount) > cpdmParentCount) {
                errors.add(ValidationError.warning("LARGE_COUNT_DIFFERENCE", 
                    String.format("两文件产品数量差异较大，CPDM总数: %d, JYCS: %d", 
                    cpdmTotalCount, jycsCount)));
            }
            
        } catch (Exception e) {
            log.error("数据条数一致性校验异常", e);
            errors.add(new ValidationError("VALIDATION_ERROR", 
                String.format("数据条数一致性校验异常: %s", e.getMessage())));
        }
        
        ValidationResult result = errors.isEmpty() ? 
            ValidationResult.success() : ValidationResult.failure(errors);
        result.setValidationTime(System.currentTimeMillis() - startTime);
        
        return result;
    }
    
    /**
     * 产品代码顺序一致性校验
     */
    public ValidationResult validateProductCodeOrder(ParsedData cpdmData, ParsedData jycsData) {
        long startTime = System.currentTimeMillis();
        List<ValidationError> errors = new ArrayList<>();
        
        try {
            // 获取CPDM子产品代码列表（按顺序）
            List<String> cpdmCodes = cpdmData.getChildProducts().stream()
                .map(ProductInfo::getProductCode)
                .collect(Collectors.toList());
            
            // 获取JYCS产品代码列表（按顺序）
            List<String> jycsCodes = jycsData.getAllProducts().stream()
                .map(ProductInfo::getProductCode)
                .collect(Collectors.toList());
            
            log.debug("CPDM子产品代码顺序: {}", cpdmCodes);
            log.debug("JYCS产品代码顺序: {}", jycsCodes);
            
            // 完全顺序匹配校验
            if (!cpdmCodes.equals(jycsCodes)) {
                errors.add(new ValidationError("PRODUCT_ORDER_MISMATCH", 
                    "CPDM与JYCS文件中产品代码顺序不一致"));
                
                // 详细分析顺序差异
                analyzeOrderDifferences(cpdmCodes, jycsCodes, errors);
            }
            
            // 如果数量相同但顺序不同，提供更详细的信息
            if (cpdmCodes.size() == jycsCodes.size() && !cpdmCodes.equals(jycsCodes)) {
                findOrderMismatches(cpdmCodes, jycsCodes, errors);
            }
            
        } catch (Exception e) {
            log.error("产品代码顺序一致性校验异常", e);
            errors.add(new ValidationError("VALIDATION_ERROR", 
                String.format("产品代码顺序一致性校验异常: %s", e.getMessage())));
        }
        
        ValidationResult result = errors.isEmpty() ? 
            ValidationResult.success() : ValidationResult.failure(errors);
        result.setValidationTime(System.currentTimeMillis() - startTime);
        
        return result;
    }
    
    /**
     * 产品信息完整性校验
     */
    public ValidationResult validateDataCompleteness(ParsedData cpdmData, ParsedData jycsData) {
        long startTime = System.currentTimeMillis();
        List<ValidationError> errors = new ArrayList<>();
        
        try {
            // 获取所有产品代码集合
            Set<String> cpdmCodes = cpdmData.getChildProducts().stream()
                .map(ProductInfo::getProductCode)
                .collect(Collectors.toSet());
            
            Set<String> jycsCodes = jycsData.getAllProducts().stream()
                .map(ProductInfo::getProductCode)
                .collect(Collectors.toSet());
            
            // 找出只在CPDM中存在的产品
            Set<String> onlyInCpdm = new HashSet<>(cpdmCodes);
            onlyInCpdm.removeAll(jycsCodes);
            
            // 找出只在JYCS中存在的产品
            Set<String> onlyInJycs = new HashSet<>(jycsCodes);
            onlyInJycs.removeAll(cpdmCodes);
            
            // 找出两个文件都有的产品
            Set<String> commonCodes = new HashSet<>(cpdmCodes);
            commonCodes.retainAll(jycsCodes);
            
            log.info("共同产品数量: {}, 仅CPDM: {}, 仅JYCS: {}", 
                commonCodes.size(), onlyInCpdm.size(), onlyInJycs.size());
            
            // 记录缺失的产品
            for (String code : onlyInCpdm) {
                errors.add(new ValidationError("PRODUCT_MISSING_IN_JYCS", 
                    String.format("产品[%s]在JYCS文件中缺失", code)));
            }
            
            for (String code : onlyInJycs) {
                errors.add(new ValidationError("PRODUCT_MISSING_IN_CPDM", 
                    String.format("产品[%s]在CPDM文件中缺失", code)));
            }
            
            // 校验共同产品的关键信息一致性
            validateCommonProductsConsistency(cpdmData, jycsData, commonCodes, errors);
            
            // 计算完整性指标
            double completenessRatio = commonCodes.size() / (double) Math.max(cpdmCodes.size(), jycsCodes.size());
            if (completenessRatio < 0.9) { // 完整性低于90%
                errors.add(ValidationError.warning("LOW_DATA_COMPLETENESS", 
                    String.format("文件间数据完整性较低: %.1f%%", completenessRatio * 100)));
            }
            
        } catch (Exception e) {
            log.error("产品信息完整性校验异常", e);
            errors.add(new ValidationError("VALIDATION_ERROR", 
                String.format("产品信息完整性校验异常: %s", e.getMessage())));
        }
        
        ValidationResult result = errors.isEmpty() ? 
            ValidationResult.success() : ValidationResult.failure(errors);
        result.setValidationTime(System.currentTimeMillis() - startTime);
        
        return result;
    }
    
    /**
     * 文件级别的统计校验
     */
    public ValidationResult validateFileStatistics(ParsedData cpdmData, ParsedData jycsData) {
        long startTime = System.currentTimeMillis();
        List<ValidationError> errors = new ArrayList<>();
        
        try {
            // 统计关键指标
            validateAmountStatistics(cpdmData, jycsData, errors);
            validateDateStatistics(cpdmData, jycsData, errors);
            validateCategoryStatistics(cpdmData, jycsData, errors);
            
        } catch (Exception e) {
            log.error("文件统计校验异常", e);
            errors.add(new ValidationError("VALIDATION_ERROR", 
                String.format("文件统计校验异常: %s", e.getMessage())));
        }
        
        ValidationResult result = errors.isEmpty() ? 
            ValidationResult.success() : ValidationResult.failure(errors);
        result.setValidationTime(System.currentTimeMillis() - startTime);
        
        return result;
    }
    
    /**
     * 执行完整的文件交叉校验
     */
    public ValidationResult validateCrossFile(ParsedData cpdmData, ParsedData jycsData) {
        ValidationResult result = new ValidationResult(true);
        
        // 1. 数据条数一致性校验
        ValidationResult countResult = validateDataCountConsistency(cpdmData, jycsData);
        result.merge(countResult);
        
        // 2. 产品代码顺序一致性校验
        ValidationResult orderResult = validateProductCodeOrder(cpdmData, jycsData);
        result.merge(orderResult);
        
        // 3. 产品信息完整性校验
        ValidationResult completenessResult = validateDataCompleteness(cpdmData, jycsData);
        result.merge(completenessResult);
        
        // 4. 文件统计校验
        ValidationResult statisticsResult = validateFileStatistics(cpdmData, jycsData);
        result.merge(statisticsResult);
        
        return result;
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 分析顺序差异的详细情况
     */
    private void analyzeOrderDifferences(List<String> cpdmCodes, List<String> jycsCodes, 
                                       List<ValidationError> errors) {
        int minSize = Math.min(cpdmCodes.size(), jycsCodes.size());
        int mismatchCount = 0;
        
        for (int i = 0; i < minSize; i++) {
            if (!Objects.equals(cpdmCodes.get(i), jycsCodes.get(i))) {
                mismatchCount++;
                if (mismatchCount <= 5) { // 只报告前5个不匹配
                    errors.add(ValidationError.warning("ORDER_MISMATCH_DETAIL", 
                        String.format("位置%d: CPDM[%s] vs JYCS[%s]", 
                        i + 1, cpdmCodes.get(i), jycsCodes.get(i))));
                }
            }
        }
        
        if (mismatchCount > 5) {
            errors.add(ValidationError.info("MORE_MISMATCHES", 
                String.format("还有%d个位置的产品代码不匹配", mismatchCount - 5)));
        }
    }
    
    /**
     * 找出顺序不匹配的具体位置
     */
    private void findOrderMismatches(List<String> cpdmCodes, List<String> jycsCodes, 
                                   List<ValidationError> errors) {
        // 创建JYCS代码位置映射
        Map<String, Integer> jycsPositions = new HashMap<>();
        for (int i = 0; i < jycsCodes.size(); i++) {
            jycsPositions.put(jycsCodes.get(i), i);
        }
        
        // 找出位置发生变化的产品
        List<String> positionChanges = new ArrayList<>();
        for (int i = 0; i < cpdmCodes.size(); i++) {
            String code = cpdmCodes.get(i);
            Integer jycsPosition = jycsPositions.get(code);
            if (jycsPosition != null && jycsPosition != i) {
                positionChanges.add(String.format("%s: CPDM位置%d -> JYCS位置%d", 
                    code, i + 1, jycsPosition + 1));
            }
        }
        
        if (!positionChanges.isEmpty()) {
            int reportCount = Math.min(positionChanges.size(), 5);
            for (int i = 0; i < reportCount; i++) {
                errors.add(ValidationError.warning("POSITION_CHANGE", positionChanges.get(i)));
            }
            
            if (positionChanges.size() > 5) {
                errors.add(ValidationError.info("MORE_POSITION_CHANGES", 
                    String.format("还有%d个产品位置发生变化", positionChanges.size() - 5)));
            }
        }
    }
    
    /**
     * 校验共同产品的一致性
     */
    private void validateCommonProductsConsistency(ParsedData cpdmData, ParsedData jycsData, 
                                                 Set<String> commonCodes, List<ValidationError> errors) {
        for (String code : commonCodes) {
            ProductInfo cpdmProduct = cpdmData.findProductByCode(code);
            ProductInfo jycsProduct = jycsData.findProductByCode(code);
            
            if (cpdmProduct != null && jycsProduct != null) {
                // 产品名称一致性
                if (!Objects.equals(cpdmProduct.getProductName(), jycsProduct.getProductName())) {
                    if (StringUtils.isNotBlank(cpdmProduct.getProductName()) && 
                        StringUtils.isNotBlank(jycsProduct.getProductName())) {
                        errors.add(ValidationError.warning("PRODUCT_NAME_INCONSISTENT", 
                            String.format("产品[%s]在两文件中名称不一致: CPDM[%s] vs JYCS[%s]", 
                            code, cpdmProduct.getProductName(), jycsProduct.getProductName())));
                    }
                }
                
                // 募集金额一致性（如果都有值）
                if (cpdmProduct.getCurrentAmount() != null && jycsProduct.getCurrentAmount() != null) {
                    if (cpdmProduct.getCurrentAmount().compareTo(jycsProduct.getCurrentAmount()) != 0) {
                        errors.add(ValidationError.warning("AMOUNT_INCONSISTENT", 
                            String.format("产品[%s]当前募集金额不一致: CPDM[%s] vs JYCS[%s]", 
                            code, cpdmProduct.getCurrentAmount(), jycsProduct.getCurrentAmount())));
                    }
                }
                
                // 状态一致性
                if (!Objects.equals(cpdmProduct.getStatus(), jycsProduct.getStatus())) {
                    if (StringUtils.isNotBlank(cpdmProduct.getStatus()) && 
                        StringUtils.isNotBlank(jycsProduct.getStatus())) {
                        errors.add(ValidationError.warning("STATUS_INCONSISTENT", 
                            String.format("产品[%s]状态不一致: CPDM[%s] vs JYCS[%s]", 
                            code, cpdmProduct.getStatus(), jycsProduct.getStatus())));
                    }
                }
            }
        }
    }
    
    /**
     * 校验金额统计信息
     */
    private void validateAmountStatistics(ParsedData cpdmData, ParsedData jycsData, 
                                        List<ValidationError> errors) {
        // 计算CPDM文件中有金额的产品总金额
        BigDecimal cpdmTotalAmount = cpdmData.getChildProducts().stream()
            .filter(p -> p.getCurrentAmount() != null)
            .map(ProductInfo::getCurrentAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 计算JYCS文件中有金额的产品总金额
        BigDecimal jycsTotalAmount = jycsData.getAllProducts().stream()
            .filter(p -> p.getCurrentAmount() != null)
            .map(ProductInfo::getCurrentAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 比较总金额差异
        if (cpdmTotalAmount.compareTo(BigDecimal.ZERO) > 0 && 
            jycsTotalAmount.compareTo(BigDecimal.ZERO) > 0) {
            
            BigDecimal diff = cpdmTotalAmount.subtract(jycsTotalAmount).abs();
            BigDecimal diffRatio = diff.divide(cpdmTotalAmount.max(jycsTotalAmount), 4, RoundingMode.HALF_UP);
            
            if (diffRatio.compareTo(new BigDecimal("0.05")) > 0) { // 差异超过5%
                errors.add(ValidationError.warning("AMOUNT_TOTAL_DIFFERENCE", 
                    String.format("两文件总金额差异较大: CPDM[%s] vs JYCS[%s], 差异比例: %.2f%%", 
                    cpdmTotalAmount, jycsTotalAmount, diffRatio.multiply(new BigDecimal("100")))));
            }
        }
    }
    
    /**
     * 校验日期统计信息
     */
    private void validateDateStatistics(ParsedData cpdmData, ParsedData jycsData, 
                                      List<ValidationError> errors) {
        // 统计成立日期范围
        Date cpdmMinDate = cpdmData.getChildProducts().stream()
            .map(ProductInfo::getEstablishDate)
            .filter(Objects::nonNull)
            .min(Date::compareTo)
            .orElse(null);
        
        Date jycsMinDate = jycsData.getAllProducts().stream()
            .map(ProductInfo::getEstablishDate)
            .filter(Objects::nonNull)
            .min(Date::compareTo)
            .orElse(null);
        
        if (cpdmMinDate != null && jycsMinDate != null) {
            long daysDiff = Math.abs(cpdmMinDate.getTime() - jycsMinDate.getTime()) / (24 * 60 * 60 * 1000);
            if (daysDiff > 365) { // 差异超过1年
                errors.add(ValidationError.warning("DATE_RANGE_DIFFERENCE", 
                    String.format("两文件最早成立日期差异较大: %d天", daysDiff)));
            }
        }
    }
    
    /**
     * 校验分类统计信息
     */
    private void validateCategoryStatistics(ParsedData cpdmData, ParsedData jycsData, 
                                          List<ValidationError> errors) {
        // 统计风险等级分布
        Map<String, Long> cpdmRiskDistribution = cpdmData.getChildProducts().stream()
            .filter(p -> StringUtils.isNotBlank(p.getRiskLevel()))
            .collect(Collectors.groupingBy(ProductInfo::getRiskLevel, Collectors.counting()));
        
        Map<String, Long> jycsRiskDistribution = jycsData.getAllProducts().stream()
            .filter(p -> StringUtils.isNotBlank(p.getRiskLevel()))
            .collect(Collectors.groupingBy(ProductInfo::getRiskLevel, Collectors.counting()));
        
        // 比较风险等级分布差异
        for (String riskLevel : cpdmRiskDistribution.keySet()) {
            Long cpdmCount = cpdmRiskDistribution.get(riskLevel);
            Long jycsCount = jycsRiskDistribution.getOrDefault(riskLevel, 0L);
            
            if (Math.abs(cpdmCount - jycsCount) > Math.max(cpdmCount, jycsCount) * 0.1) { // 差异超过10%
                errors.add(ValidationError.info("RISK_DISTRIBUTION_DIFFERENCE", 
                    String.format("风险等级[%s]分布差异: CPDM[%d] vs JYCS[%d]", 
                    riskLevel, cpdmCount, jycsCount)));
            }
        }
    }
}