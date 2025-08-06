package com.easypan.TA.Validator;

import com.easypan.TA.Model.ParsedData;
import com.easypan.TA.Model.ProductInfo;
import com.easypan.TA.Model.ValidationError;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class BusinessValidator {
    
    /**
     * 母子产品关联校验
     */
    public ValidationResult validateParentChildRelation(ParsedData parsedData) {
        long startTime = System.currentTimeMillis();
        List<ValidationError> errors = new ArrayList<>();
        
        try {
            List<ProductInfo> allProducts = parsedData.getAllProducts();
            
            // 1. 提取所有母产品代码
            Set<String> parentCodes = parsedData.getParentProducts().stream()
                .map(ProductInfo::getProductCode)
                .collect(Collectors.toSet());
            
            log.debug("找到母产品数量: {}", parentCodes.size());
            
            // 2. 校验子产品是否都有对应的母产品
            for (ProductInfo product : parsedData.getChildProducts()) {
                if (StringUtils.isBlank(product.getParentCode())) {
                    errors.add(new ValidationError("PARENT_CODE_MISSING", 
                        String.format("子产品[%s]缺少母产品代码", product.getProductCode()), 
                        "parentCode", product.getProductCode()));
                } else if (!parentCodes.contains(product.getParentCode())) {
                    errors.add(new ValidationError("PARENT_NOT_FOUND", 
                        String.format("子产品[%s]对应的母产品[%s]不存在", 
                        product.getProductCode(), product.getParentCode()), 
                        "parentCode", product.getProductCode()));
                }
            }
            
            // 3. 检查是否存在孤立的母产品（没有子产品的母产品）
            Set<String> referencedParentCodes = parsedData.getChildProducts().stream()
                .map(ProductInfo::getParentCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
            
            for (String parentCode : parentCodes) {
                if (!referencedParentCodes.contains(parentCode)) {
                    errors.add(ValidationError.warning("ORPHANED_PARENT", 
                        String.format("母产品[%s]没有关联的子产品", parentCode), 
                        "productCode"));
                }
            }
            
            // 4. 检查循环引用（母产品不能引用自己作为父产品）
            for (ProductInfo product : parsedData.getParentProducts()) {
                if (product.getProductCode().equals(product.getParentCode())) {
                    errors.add(new ValidationError("CIRCULAR_REFERENCE", 
                        String.format("母产品[%s]不能引用自己作为父产品", product.getProductCode()), 
                        "parentCode", product.getProductCode()));
                }
            }
            
        } catch (Exception e) {
            log.error("母子产品关联校验异常", e);
            errors.add(new ValidationError("VALIDATION_ERROR", 
                String.format("母子产品关联校验异常: %s", e.getMessage())));
        }
        
        ValidationResult result = errors.isEmpty() ? 
            ValidationResult.success() : ValidationResult.failure(errors);
        result.setValidationTime(System.currentTimeMillis() - startTime);
        
        return result;
    }
    
    /**
     * 产品参数合理性校验
     */
    public ValidationResult validateProductParameters(ParsedData parsedData) {
        long startTime = System.currentTimeMillis();
        List<ValidationError> errors = new ArrayList<>();
        
        try {
            for (ProductInfo product : parsedData.getAllProducts()) {
                // 募集金额上下限关系校验
                validateAmountRange(product, errors);
                
                // 期限相关参数校验
                validateTermParameters(product, errors);
                
                // 收益率合理性校验
                validateReturnRate(product, errors);
                
                // 产品类型与其他属性的一致性校验
                validateProductTypeConsistency(product, errors);
            }
            
        } catch (Exception e) {
            log.error("产品参数合理性校验异常", e);
            errors.add(new ValidationError("VALIDATION_ERROR", 
                String.format("产品参数合理性校验异常: %s", e.getMessage())));
        }
        
        ValidationResult result = errors.isEmpty() ? 
            ValidationResult.success() : ValidationResult.failure(errors);
        result.setValidationTime(System.currentTimeMillis() - startTime);
        
        return result;
    }
    
    /**
     * 日期逻辑关系校验
     */
    public ValidationResult validateDateLogic(ParsedData parsedData) {
        long startTime = System.currentTimeMillis();
        List<ValidationError> errors = new ArrayList<>();
        
        try {
            for (ProductInfo product : parsedData.getAllProducts()) {
                validateDateRelations(product, errors);
                validateDateReasonableness(product, errors);
            }
            
        } catch (Exception e) {
            log.error("日期逻辑关系校验异常", e);
            errors.add(new ValidationError("VALIDATION_ERROR", 
                String.format("日期逻辑关系校验异常: %s", e.getMessage())));
        }
        
        ValidationResult result = errors.isEmpty() ? 
            ValidationResult.success() : ValidationResult.failure(errors);
        result.setValidationTime(System.currentTimeMillis() - startTime);
        
        return result;
    }
    
    /**
     * 母子产品最大购买人数累加校验
     */
    public ValidationResult validateMaxInvestorsAccumulation(ParsedData parsedData) {
        long startTime = System.currentTimeMillis();
        List<ValidationError> errors = new ArrayList<>();
        
        try {
            Map<String, List<ProductInfo>> parentChildMap = new HashMap<>();
            Map<String, ProductInfo> parentProductMap = new HashMap<>();
            
            // 建立母子产品映射关系
            for (ProductInfo product : parsedData.getAllProducts()) {
                if (product.isParentProduct()) {
                    parentProductMap.put(product.getProductCode(), product);
                    parentChildMap.put(product.getProductCode(), new ArrayList<>());
                } else if (product.isChildProduct() && StringUtils.isNotBlank(product.getParentCode())) {
                    parentChildMap.computeIfAbsent(product.getParentCode(), k -> new ArrayList<>()).add(product);
                }
            }
            
            // 校验每个母产品的子产品最大购买人数累加
            for (Map.Entry<String, List<ProductInfo>> entry : parentChildMap.entrySet()) {
                String parentCode = entry.getKey();
                List<ProductInfo> childProducts = entry.getValue();
                ProductInfo parentProduct = parentProductMap.get(parentCode);
                
                if (parentProduct == null) {
                    continue; // 母产品不存在的情况在其他校验中已经处理
                }
                
                Integer parentMaxInvestors = parentProduct.getMaxInvestors();
                if (parentMaxInvestors == null) {
                    // 母产品未设置最大购买人数，给出警告
                    errors.add(ValidationError.warning("PARENT_MAX_INVESTORS_MISSING", 
                        String.format("母产品[%s]未设置最大购买人数", parentCode), 
                        "maxInvestors"));
                    continue;
                }
                
                // 计算子产品最大购买人数累加
                int childrenTotalMaxInvestors = 0;
                List<String> childrenWithoutMaxInvestors = new ArrayList<>();
                
                for (ProductInfo child : childProducts) {
                    if (child.getMaxInvestors() == null) {
                        childrenWithoutMaxInvestors.add(child.getProductCode());
                    } else {
                        childrenTotalMaxInvestors += child.getMaxInvestors();
                    }
                }
                
                // 如果有子产品缺少最大购买人数设置，给出警告
                if (!childrenWithoutMaxInvestors.isEmpty()) {
                    errors.add(ValidationError.warning("CHILD_MAX_INVESTORS_MISSING", 
                        String.format("母产品[%s]下的子产品[%s]未设置最大购买人数", 
                        parentCode, String.join(", ", childrenWithoutMaxInvestors)), 
                        "maxInvestors"));
                }
                
                // 校验累加结果
                if (childrenWithoutMaxInvestors.isEmpty() && childrenTotalMaxInvestors != parentMaxInvestors) {
                    errors.add(new ValidationError("MAX_INVESTORS_MISMATCH", 
                        String.format("母产品[%s]的最大购买人数[%d]与其子产品累加[%d]不匹配", 
                        parentCode, parentMaxInvestors, childrenTotalMaxInvestors), 
                        "maxInvestors", parentCode));
                }
                
                // 如果有子产品但累加为0，可能是数据问题
                if (!childProducts.isEmpty() && childrenTotalMaxInvestors == 0 && childrenWithoutMaxInvestors.isEmpty()) {
                    errors.add(ValidationError.warning("ZERO_MAX_INVESTORS_SUM", 
                        String.format("母产品[%s]下的子产品最大购买人数累加为0，请确认数据正确性", parentCode), 
                        "maxInvestors"));
                }
            }
            
        } catch (Exception e) {
            log.error("母子产品最大购买人数累加校验异常", e);
            errors.add(new ValidationError("VALIDATION_ERROR", 
                String.format("母子产品最大购买人数累加校验异常: %s", e.getMessage())));
        }
        
        ValidationResult result = errors.isEmpty() ? 
            ValidationResult.success() : ValidationResult.failure(errors);
        result.setValidationTime(System.currentTimeMillis() - startTime);
        
        return result;
    }
    
    /**
     * 产品数据一致性校验
     */
    public ValidationResult validateDataConsistency(ParsedData parsedData) {
        long startTime = System.currentTimeMillis();
        List<ValidationError> errors = new ArrayList<>();
        
        try {
            // 1. 检查产品代码唯一性
            validateProductCodeUniqueness(parsedData, errors);
            
            // 2. 检查同一产品在不同记录中的数据一致性
            validateProductDataConsistency(parsedData, errors);
            
            // 3. 检查关键字段的数据完整性
            validateDataCompleteness(parsedData, errors);
            
        } catch (Exception e) {
            log.error("产品数据一致性校验异常", e);
            errors.add(new ValidationError("VALIDATION_ERROR", 
                String.format("产品数据一致性校验异常: %s", e.getMessage())));
        }
        
        ValidationResult result = errors.isEmpty() ? 
            ValidationResult.success() : ValidationResult.failure(errors);
        result.setValidationTime(System.currentTimeMillis() - startTime);
        
        return result;
    }
    
    /**
     * 执行完整的业务逻辑校验
     */
    public ValidationResult validateBusinessLogic(ParsedData parsedData) {
        ValidationResult result = new ValidationResult(true);
        
        // 1. 母子产品关联校验
        ValidationResult relationResult = validateParentChildRelation(parsedData);
        result.merge(relationResult);
        
        // 2. 母子产品最大购买人数累加校验
        ValidationResult maxInvestorsResult = validateMaxInvestorsAccumulation(parsedData);
        result.merge(maxInvestorsResult);
        
        // 3. 产品参数合理性校验
        ValidationResult paramResult = validateProductParameters(parsedData);
        result.merge(paramResult);
        
        // 4. 日期逻辑关系校验
        ValidationResult dateResult = validateDateLogic(parsedData);
        result.merge(dateResult);
        
        // 5. 数据一致性校验
        ValidationResult consistencyResult = validateDataConsistency(parsedData);
        result.merge(consistencyResult);
        
        return result;
    }
    
    // ========== 私有校验方法 ==========
    
    /**
     * 校验募集金额范围
     */
    private void validateAmountRange(ProductInfo product, List<ValidationError> errors) {
        if (product.getMinAmount() != null && product.getMaxAmount() != null) {
            if (product.getMinAmount().compareTo(product.getMaxAmount()) > 0) {
                errors.add(new ValidationError("AMOUNT_RANGE_INVALID", 
                    String.format("产品[%s]最小募集金额不能大于最大募集金额", product.getProductCode()), 
                    "minAmount", product.getProductCode()));
            }
        }
        
        // 当前募集金额应在范围内
        if (product.getCurrentAmount() != null) {
            if (product.getMinAmount() != null && 
                product.getCurrentAmount().compareTo(product.getMinAmount()) < 0) {
                errors.add(ValidationError.warning("CURRENT_AMOUNT_BELOW_MIN", 
                    String.format("产品[%s]当前募集金额低于最小限额", product.getProductCode()), 
                    "currentAmount"));
            }
            
            if (product.getMaxAmount() != null && 
                product.getCurrentAmount().compareTo(product.getMaxAmount()) > 0) {
                errors.add(new ValidationError("CURRENT_AMOUNT_EXCEED_MAX", 
                    String.format("产品[%s]当前募集金额超过最大限额", product.getProductCode()), 
                    "currentAmount", product.getProductCode()));
            }
        }
    }
    
    /**
     * 校验期限相关参数
     */
    private void validateTermParameters(ProductInfo product, List<ValidationError> errors) {
        if (product.getTermDays() != null) {
            // 期限天数合理性检查
            if (product.getTermDays() <= 0) {
                errors.add(new ValidationError("INVALID_TERM_DAYS", 
                    String.format("产品[%s]期限天数必须大于0", product.getProductCode()), 
                    "termDays", product.getProductCode()));
            }
            
            // 期限天数不应过长（例如超过30年）
            if (product.getTermDays() > 10950) { // 30年
                errors.add(ValidationError.warning("TERM_TOO_LONG", 
                    String.format("产品[%s]期限天数可能过长", product.getProductCode()), 
                    "termDays"));
            }
        }
    }
    
    /**
     * 校验收益率合理性
     */
    private void validateReturnRate(ProductInfo product, List<ValidationError> errors) {
        if (product.getExpectedReturn() != null) {
            BigDecimal returnRate = product.getExpectedReturn();
            
            // 收益率不能为负
            if (returnRate.compareTo(BigDecimal.ZERO) < 0) {
                errors.add(new ValidationError("NEGATIVE_RETURN_RATE", 
                    String.format("产品[%s]预期收益率不能为负", product.getProductCode()), 
                    "expectedReturn", product.getProductCode()));
            }
            
            // 收益率过高警告
            if (returnRate.compareTo(new BigDecimal("50")) > 0) { // 50%
                errors.add(ValidationError.warning("HIGH_RETURN_RATE", 
                    String.format("产品[%s]预期收益率异常高，请确认", product.getProductCode()), 
                    "expectedReturn"));
            }
        }
    }
    
    /**
     * 校验产品类型一致性
     */
    private void validateProductTypeConsistency(ProductInfo product, List<ValidationError> errors) {
        // 母产品不应有父产品代码
        if (product.isParentProduct() && StringUtils.isNotBlank(product.getParentCode())) {
            errors.add(new ValidationError("PARENT_HAS_PARENT_CODE", 
                String.format("母产品[%s]不应该有父产品代码", product.getProductCode()), 
                "parentCode", product.getProductCode()));
        }
        
        // 子产品必须有父产品代码
        if (product.isChildProduct() && StringUtils.isBlank(product.getParentCode())) {
            errors.add(new ValidationError("CHILD_MISSING_PARENT_CODE", 
                String.format("子产品[%s]必须有父产品代码", product.getProductCode()), 
                "parentCode", product.getProductCode()));
        }
    }
    
    /**
     * 校验日期关系
     */
    private void validateDateRelations(ProductInfo product, List<ValidationError> errors) {
        // 成立日期不能晚于到期日期
        if (product.getEstablishDate() != null && product.getMaturityDate() != null) {
            if (product.getEstablishDate().after(product.getMaturityDate())) {
                errors.add(new ValidationError("DATE_LOGIC_ERROR", 
                    String.format("产品[%s]成立日期不能晚于到期日期", product.getProductCode()), 
                    "establishDate", product.getProductCode()));
            }
        }
        
        // 发行日期不能晚于成立日期
        if (product.getIssueDate() != null && product.getEstablishDate() != null) {
            if (product.getIssueDate().after(product.getEstablishDate())) {
                errors.add(new ValidationError("DATE_LOGIC_ERROR", 
                    String.format("产品[%s]发行日期不能晚于成立日期", product.getProductCode()), 
                    "issueDate", product.getProductCode()));
            }
        }
    }
    
    /**
     * 校验日期合理性
     */
    private void validateDateReasonableness(ProductInfo product, List<ValidationError> errors) {
        Date now = new Date();
        
        // 成立日期不应该在很久的将来
        if (product.getEstablishDate() != null) {
            long daysDiff = (product.getEstablishDate().getTime() - now.getTime()) / (24 * 60 * 60 * 1000);
            if (daysDiff > 365 * 2) { // 2年
                errors.add(ValidationError.warning("ESTABLISH_DATE_TOO_FUTURE", 
                    String.format("产品[%s]成立日期距离现在过远", product.getProductCode()), 
                    "establishDate"));
            }
        }
        
        // 到期日期检查
        if (product.getMaturityDate() != null) {
            // 已经到期的产品状态检查
            if (product.getMaturityDate().before(now) && 
                !"终止".equals(product.getStatus()) && !"清算".equals(product.getStatus())) {
                errors.add(ValidationError.warning("MATURED_PRODUCT_STATUS", 
                    String.format("产品[%s]已到期但状态未更新", product.getProductCode()), 
                    "status"));
            }
        }
    }
    
    /**
     * 校验产品代码唯一性
     */
    private void validateProductCodeUniqueness(ParsedData parsedData, List<ValidationError> errors) {
        Map<String, Integer> codeCount = new HashMap<>();
        
        for (ProductInfo product : parsedData.getAllProducts()) {
            String code = product.getProductCode();
            codeCount.put(code, codeCount.getOrDefault(code, 0) + 1);
        }
        
        for (Map.Entry<String, Integer> entry : codeCount.entrySet()) {
            if (entry.getValue() > 1) {
                errors.add(new ValidationError("DUPLICATE_PRODUCT_CODE", 
                    String.format("产品代码[%s]重复出现%d次", entry.getKey(), entry.getValue()), 
                    "productCode", entry.getKey()));
            }
        }
    }
    
    /**
     * 校验同一产品数据一致性
     */
    private void validateProductDataConsistency(ParsedData parsedData, List<ValidationError> errors) {
        // 这个方法主要用于跨文件校验时检查同一产品在不同文件中的数据是否一致
        // 在单文件内部一般不会有重复的产品代码，但可以检查其他一致性
        
        for (ProductInfo product : parsedData.getAllProducts()) {
            // 检查产品名称与代码的一致性（业务规则相关）
            if (StringUtils.isNotBlank(product.getProductCode()) && 
                StringUtils.isNotBlank(product.getProductName())) {
                
                // 例如：产品代码以"FUND"开头的应该是基金类产品
                if (product.getProductCode().startsWith("FUND") && 
                    !product.getProductName().contains("基金")) {
                    errors.add(ValidationError.warning("NAME_CODE_INCONSISTENT", 
                        String.format("产品[%s]代码与名称可能不匹配", product.getProductCode()), 
                        "productName"));
                }
            }
        }
    }
    
    /**
     * 校验数据完整性
     */
    private void validateDataCompleteness(ParsedData parsedData, List<ValidationError> errors) {
        int totalProducts = parsedData.getAllProducts().size();
        
        if (totalProducts == 0) {
            errors.add(new ValidationError("NO_PRODUCT_DATA", 
                "文件中没有找到有效的产品数据", "file"));
            return;
        }
        
        // 统计关键字段的缺失情况
        int missingNameCount = 0;
        int missingTypeCount = 0;
        int missingAmountCount = 0;
        
        for (ProductInfo product : parsedData.getAllProducts()) {
            if (StringUtils.isBlank(product.getProductName())) {
                missingNameCount++;
            }
            if (StringUtils.isBlank(product.getProductType())) {
                missingTypeCount++;
            }
            if (product.getCurrentAmount() == null) {
                missingAmountCount++;
            }
        }
        
        // 如果缺失比例过高，给出警告
        double nameRatio = (double) missingNameCount / totalProducts;
        if (nameRatio > 0.1) { // 超过10%
            errors.add(ValidationError.warning("HIGH_MISSING_NAME_RATIO", 
                String.format("产品名称缺失比例过高: %.1f%%", nameRatio * 100), "productName"));
        }
        
        double amountRatio = (double) missingAmountCount / totalProducts;
        if (amountRatio > 0.2) { // 超过20%
            errors.add(ValidationError.warning("HIGH_MISSING_AMOUNT_RATIO", 
                String.format("募集金额缺失比例过高: %.1f%%", amountRatio * 100), "currentAmount"));
        }
    }
}