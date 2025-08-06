package com.easypan.TA.Validator;

import com.easypan.TA.Model.ProductInfo;
import com.easypan.TA.Model.ValidationError;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
public class FieldValidator {
    
    // 产品代码格式正则
    private static final Pattern PRODUCT_CODE_PATTERN = Pattern.compile("^[A-Z0-9]{6,20}$");
    
    // 金额范围限制
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999999.99");
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
    
    // 比例范围限制（百分比）
    private static final BigDecimal MAX_PERCENTAGE = new BigDecimal("100");
    private static final BigDecimal MIN_PERCENTAGE = new BigDecimal("0");
    
    // 必填字段定义
    private static final List<String> REQUIRED_FIELDS = Arrays.asList(
        "productCode", "productName", "productType"
    );
    
    // 字段类型枚举
    public enum FieldType {
        STRING, DATE, AMOUNT, PERCENTAGE, INTEGER, PRODUCT_CODE
    }
    
    /**
     * 数据类型校验
     */
    public ValidationResult validateDataType(String fieldName, String value, FieldType expectedType) {
        long startTime = System.currentTimeMillis();
        List<ValidationError> errors = new ArrayList<>();
        
        try {
            if (StringUtils.isBlank(value)) {
                // 空值在必填校验中处理
                return createSuccessResult(startTime);
            }
            
            switch (expectedType) {
                case DATE:
                    validateDateType(fieldName, value, errors);
                    break;
                case AMOUNT:
                    validateAmountType(fieldName, value, errors);
                    break;
                case PERCENTAGE:
                    validatePercentageType(fieldName, value, errors);
                    break;
                case INTEGER:
                    validateIntegerType(fieldName, value, errors);
                    break;
                case PRODUCT_CODE:
                    validateProductCodeType(fieldName, value, errors);
                    break;
                case STRING:
                    validateStringType(fieldName, value, errors);
                    break;
                default:
                    break;
            }
            
        } catch (Exception e) {
            log.error("字段类型校验异常: {} = {}", fieldName, value, e);
            errors.add(new ValidationError("VALIDATION_ERROR", 
                String.format("字段[%s]类型校验异常: %s", fieldName, e.getMessage()), fieldName));
        }
        
        ValidationResult result = errors.isEmpty() ? 
            ValidationResult.success() : ValidationResult.failure(errors);
        result.setValidationTime(System.currentTimeMillis() - startTime);
        
        return result;
    }
    
    /**
     * 必填字段校验
     */
    public ValidationResult validateRequired(ProductInfo product) {
        long startTime = System.currentTimeMillis();
        List<ValidationError> errors = new ArrayList<>();
        
        try {
            for (String fieldName : REQUIRED_FIELDS) {
                if (isFieldEmpty(product, fieldName)) {
                    errors.add(new ValidationError("FIELD_REQUIRED", 
                        String.format("必填字段[%s]不能为空", fieldName), 
                        fieldName, product.getProductCode()));
                }
            }
            
            // 子产品必须有母产品代码
            if (product.isChildProduct() && StringUtils.isBlank(product.getParentCode())) {
                errors.add(new ValidationError("PARENT_CODE_REQUIRED", 
                    "子产品必须指定母产品代码", 
                    "parentCode", product.getProductCode()));
            }
            
        } catch (Exception e) {
            log.error("必填字段校验异常: {}", product.getProductCode(), e);
            errors.add(new ValidationError("VALIDATION_ERROR", 
                String.format("必填字段校验异常: %s", e.getMessage())));
        }
        
        ValidationResult result = errors.isEmpty() ? 
            ValidationResult.success() : ValidationResult.failure(errors);
        result.setValidationTime(System.currentTimeMillis() - startTime);
        
        return result;
    }
    
    /**
     * 数据长度和格式校验
     */
    public ValidationResult validateFormat(ProductInfo product) {
        long startTime = System.currentTimeMillis();
        List<ValidationError> errors = new ArrayList<>();
        
        try {
            // 产品代码长度和格式校验
            if (StringUtils.isNotBlank(product.getProductCode())) {
                if (product.getProductCode().length() > 20) {
                    errors.add(new ValidationError("LENGTH_EXCEEDED", 
                        "产品代码长度不能超过20位", 
                        "productCode", product.getProductCode()));
                }
                if (!PRODUCT_CODE_PATTERN.matcher(product.getProductCode()).matches()) {
                    errors.add(new ValidationError("FORMAT_INVALID", 
                        "产品代码格式不正确，只能包含大写字母和数字", 
                        "productCode", product.getProductCode()));
                }
            }
            
            // 产品名称长度校验
            if (StringUtils.isNotBlank(product.getProductName()) && 
                product.getProductName().length() > 200) {
                errors.add(new ValidationError("LENGTH_EXCEEDED", 
                    "产品名称长度不能超过200个字符", 
                    "productName", product.getProductCode()));
            }
            
            // 母产品代码格式校验
            if (StringUtils.isNotBlank(product.getParentCode()) && 
                !PRODUCT_CODE_PATTERN.matcher(product.getParentCode()).matches()) {
                errors.add(new ValidationError("FORMAT_INVALID", 
                    "母产品代码格式不正确", 
                    "parentCode", product.getProductCode()));
            }
            
            // 风险等级校验
            if (StringUtils.isNotBlank(product.getRiskLevel())) {
                if (!isValidRiskLevel(product.getRiskLevel())) {
                    errors.add(new ValidationError("INVALID_VALUE", 
                        "风险等级值不在有效范围内", 
                        "riskLevel", product.getProductCode()));
                }
            }
            
            // 产品状态校验
            if (StringUtils.isNotBlank(product.getStatus())) {
                if (!isValidProductStatus(product.getStatus())) {
                    errors.add(new ValidationError("INVALID_VALUE", 
                        "产品状态值不在有效范围内", 
                        "status", product.getProductCode()));
                }
            }
            
        } catch (Exception e) {
            log.error("格式校验异常: {}", product.getProductCode(), e);
            errors.add(new ValidationError("VALIDATION_ERROR", 
                String.format("格式校验异常: %s", e.getMessage())));
        }
        
        ValidationResult result = errors.isEmpty() ? 
            ValidationResult.success() : ValidationResult.failure(errors);
        result.setValidationTime(System.currentTimeMillis() - startTime);
        
        return result;
    }
    
    /**
     * 对单个产品执行完整的字段级校验
     */
    public ValidationResult validateProduct(ProductInfo product) {
        ValidationResult result = new ValidationResult(true);
        
        // 1. 必填字段校验
        ValidationResult requiredResult = validateRequired(product);
        result.merge(requiredResult);
        
        // 2. 格式校验
        ValidationResult formatResult = validateFormat(product);
        result.merge(formatResult);
        
        // 3. 具体字段类型校验
        if (StringUtils.isNotBlank(product.getProductCode())) {
            ValidationResult codeResult = validateDataType("productCode", 
                product.getProductCode(), FieldType.PRODUCT_CODE);
            result.merge(codeResult);
        }
        
        return result;
    }
    
    // ========== 私有方法 ==========
    
    private ValidationResult createSuccessResult(long startTime) {
        ValidationResult result = ValidationResult.success();
        result.setValidationTime(System.currentTimeMillis() - startTime);
        return result;
    }
    
    /**
     * 日期类型校验
     */
    private void validateDateType(String fieldName, String value, List<ValidationError> errors) {
        // 日期格式校验
        if (!value.matches("\\d{8}")) {
            errors.add(new ValidationError("DATE_FORMAT_INVALID", 
                String.format("字段[%s]日期格式应为yyyyMMdd", fieldName), fieldName));
        }
        
        // 日期范围校验
        try {
            int dateInt = Integer.parseInt(value);
            if (dateInt < 19000101 || dateInt > 20991231) {
                errors.add(new ValidationError("DATE_RANGE_INVALID", 
                    String.format("字段[%s]日期超出有效范围", fieldName), fieldName));
            }
        } catch (NumberFormatException e) {
            errors.add(new ValidationError("DATE_FORMAT_INVALID", 
                String.format("字段[%s]不是有效的数字日期", fieldName), fieldName));
        }
    }
    
    /**
     * 金额类型校验
     */
    private void validateAmountType(String fieldName, String value, List<ValidationError> errors) {
        try {
            BigDecimal amount = new BigDecimal(value);
            
            if (amount.compareTo(MIN_AMOUNT) < 0) {
                errors.add(new ValidationError("AMOUNT_TOO_SMALL", 
                    String.format("字段[%s]金额不能小于%s", fieldName, MIN_AMOUNT), fieldName));
            }
            
            if (amount.compareTo(MAX_AMOUNT) > 0) {
                errors.add(new ValidationError("AMOUNT_TOO_LARGE", 
                    String.format("字段[%s]金额不能大于%s", fieldName, MAX_AMOUNT), fieldName));
            }
            
            // 小数位校验
            if (amount.scale() > 2) {
                errors.add(new ValidationError("DECIMAL_PLACES_EXCEEDED", 
                    String.format("字段[%s]金额小数位不能超过2位", fieldName), fieldName));
            }
            
        } catch (NumberFormatException e) {
            errors.add(new ValidationError("AMOUNT_FORMAT_INVALID", 
                String.format("字段[%s]不是有效的金额格式", fieldName), fieldName));
        }
    }
    
    /**
     * 百分比类型校验
     */
    private void validatePercentageType(String fieldName, String value, List<ValidationError> errors) {
        try {
            BigDecimal percentage = new BigDecimal(value);
            
            if (percentage.compareTo(MIN_PERCENTAGE) < 0) {
                errors.add(new ValidationError("PERCENTAGE_TOO_SMALL", 
                    String.format("字段[%s]百分比不能小于0", fieldName), fieldName));
            }
            
            if (percentage.compareTo(MAX_PERCENTAGE) > 0) {
                errors.add(new ValidationError("PERCENTAGE_TOO_LARGE", 
                    String.format("字段[%s]百分比不能大于100", fieldName), fieldName));
            }
            
        } catch (NumberFormatException e) {
            errors.add(new ValidationError("PERCENTAGE_FORMAT_INVALID", 
                String.format("字段[%s]不是有效的百分比格式", fieldName), fieldName));
        }
    }
    
    /**
     * 整数类型校验
     */
    private void validateIntegerType(String fieldName, String value, List<ValidationError> errors) {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            errors.add(new ValidationError("INTEGER_FORMAT_INVALID", 
                String.format("字段[%s]不是有效的整数格式", fieldName), fieldName));
        }
    }
    
    /**
     * 产品代码类型校验
     */
    private void validateProductCodeType(String fieldName, String value, List<ValidationError> errors) {
        if (!PRODUCT_CODE_PATTERN.matcher(value).matches()) {
            errors.add(new ValidationError("PRODUCT_CODE_FORMAT_INVALID", 
                String.format("字段[%s]产品代码格式不正确", fieldName), fieldName));
        }
    }
    
    /**
     * 字符串类型校验
     */
    private void validateStringType(String fieldName, String value, List<ValidationError> errors) {
        // 检查特殊字符
        if (value.contains("\u0000")) {
            errors.add(new ValidationError("INVALID_CHARACTERS", 
                String.format("字段[%s]包含无效字符", fieldName), fieldName));
        }
    }
    
    /**
     * 判断字段是否为空
     */
    private boolean isFieldEmpty(ProductInfo product, String fieldName) {
        switch (fieldName) {
            case "productCode":
                return StringUtils.isBlank(product.getProductCode());
            case "productName":
                return StringUtils.isBlank(product.getProductName());
            case "productType":
                return StringUtils.isBlank(product.getProductType());
            case "parentCode":
                return StringUtils.isBlank(product.getParentCode());
            default:
                return false;
        }
    }
    
    /**
     * 校验风险等级是否有效
     */
    private boolean isValidRiskLevel(String riskLevel) {
        return Arrays.asList("低", "中低", "中", "中高", "高", "1", "2", "3", "4", "5").contains(riskLevel);
    }
    
    /**
     * 校验产品状态是否有效
     */
    private boolean isValidProductStatus(String status) {
        return Arrays.asList("正常", "暂停", "终止", "清算", "NORMAL", "SUSPENDED", "TERMINATED", "LIQUIDATING").contains(status);
    }
}