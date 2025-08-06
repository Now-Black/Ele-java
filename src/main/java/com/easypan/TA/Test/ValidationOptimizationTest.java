//package com.easypan.TA.Test;
//
//import com.easypan.TA.Model.ParsedData;
//import com.easypan.TA.Model.ProductInfo;
//import com.easypan.TA.Validator.BusinessValidator;
//import com.easypan.TA.Validator.ValidationResult;
//import lombok.extern.slf4j.Slf4j;
//
//import java.math.BigDecimal;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
///**
// * 校验优化测试类 - 测试新增的母子产品最大购买人数累加校验
// */
//@Slf4j
//public class ValidationOptimizationTest {
//
//    public static void main(String[] args) {
//        ValidationOptimizationTest test = new ValidationOptimizationTest();
//        test.testMaxInvestorsAccumulation();
//    }
//
//    /**
//     * 测试母子产品最大购买人数累加校验
//     */
//    public void testMaxInvestorsAccumulation() {
//        log.info("开始测试母子产品最大购买人数累加校验");
//
//        BusinessValidator validator = new BusinessValidator();
//
//        // 测试用例1: 母子产品购买人数匹配
//        testCase1_MatchingInvestors(validator);
//
//        // 测试用例2: 母子产品购买人数不匹配
//        testCase2_MismatchingInvestors(validator);
//
//        // 测试用例3: 子产品缺少购买人数设置
//        testCase3_MissingChildInvestors(validator);
//
//        log.info("母子产品最大购买人数累加校验测试完成");
//    }
//
//    /**
//     * 测试用例1: 母子产品购买人数匹配的情况
//     */
//    private void testCase1_MatchingInvestors(BusinessValidator validator) {
//        log.info("测试用例1: 母子产品购买人数匹配");
//
//        ParsedData testData = createTestData_Matching();
//        ValidationResult result = validator.validateMaxInvestorsAccumulation(testData);
//
//        log.info("校验结果: {}", result.isSuccess() ? "通过" : "失败");
//        log.info("错误信息: {}", result.getErrors());
//
//        assert result.isSuccess() : "测试用例1应该通过校验";
//    }
//
//    /**
//     * 测试用例2: 母子产品购买人数不匹配的情况
//     */
//    private void testCase2_MismatchingInvestors(BusinessValidator validator) {
//        log.info("测试用例2: 母子产品购买人数不匹配");
//
//        ParsedData testData = createTestData_Mismatching();
//        ValidationResult result = validator.validateMaxInvestorsAccumulation(testData);
//
//        log.info("校验结果: {}", result.isSuccess() ? "通过" : "失败");
//        log.info("错误信息: {}", result.getErrors());
//
//        assert !result.isSuccess() : "测试用例2应该校验失败";
//    }
//
//    /**
//     * 测试用例3: 子产品缺少购买人数设置的情况
//     */
//    private void testCase3_MissingChildInvestors(BusinessValidator validator) {
//        log.info("测试用例3: 子产品缺少购买人数设置");
//
//        ParsedData testData = createTestData_MissingChild();
//        ValidationResult result = validator.validateMaxInvestorsAccumulation(testData);
//
//        log.info("校验结果: {}", result.isSuccess() ? "通过" : "失败");
//        log.info("错误信息: {}", result.getErrors());
//
//        // 这种情况会产生警告，但不算错误
//        log.info("警告数量: {}", result.getErrors().size());
//    }
//
//    /**
//     * 创建测试数据 - 母子产品购买人数匹配
//     */
//    private ParsedData createTestData_Matching() {
//        List<ProductInfo> products = new ArrayList<>();
//
//        // 母产品: 最大购买人数100
//        ProductInfo parent = new ProductInfo("PARENT001");
//        parent.setProductName("测试母产品");
//        parent.setProductType("母产品");
//        parent.setMaxInvestors(100);
//        parent.setCurrentAmount(new BigDecimal("1000000"));
//        products.add(parent);
//
//        // 子产品1: 最大购买人数60
//        ProductInfo child1 = new ProductInfo("CHILD001");
//        child1.setProductName("测试子产品1");
//        child1.setProductType("子产品");
//        child1.setParentCode("PARENT001");
//        child1.setMaxInvestors(60);
//        child1.setCurrentAmount(new BigDecimal("600000"));
//        products.add(child1);
//
//        // 子产品2: 最大购买人数40
//        ProductInfo child2 = new ProductInfo("CHILD002");
//        child2.setProductName("测试子产品2");
//        child2.setProductType("子产品");
//        child2.setParentCode("PARENT001");
//        child2.setMaxInvestors(40);
//        child2.setCurrentAmount(new BigDecimal("400000"));
//        products.add(child2);
//
//        // 60 + 40 = 100，应该匹配
//        return new ParsedData(products);
//    }
//
//    /**
//     * 创建测试数据 - 母子产品购买人数不匹配
//     */
//    private ParsedData createTestData_Mismatching() {
//        List<ProductInfo> products = new ArrayList<>();
//
//        // 母产品: 最大购买人数100
//        ProductInfo parent = new ProductInfo("PARENT002");
//        parent.setProductName("测试母产品2");
//        parent.setProductType("母产品");
//        parent.setMaxInvestors(100);
//        parent.setCurrentAmount(new BigDecimal("1000000"));
//        products.add(parent);
//
//        // 子产品1: 最大购买人数70
//        ProductInfo child1 = new ProductInfo("CHILD003");
//        child1.setProductName("测试子产品3");
//        child1.setProductType("子产品");
//        child1.setParentCode("PARENT002");
//        child1.setMaxInvestors(70);
//        child1.setCurrentAmount(new BigDecimal("700000"));
//        products.add(child1);
//
//        // 子产品2: 最大购买人数40
//        ProductInfo child2 = new ProductInfo("CHILD004");
//        child2.setProductName("测试子产品4");
//        child2.setProductType("子产品");
//        child2.setParentCode("PARENT002");
//        child2.setMaxInvestors(40);
//        child2.setCurrentAmount(new BigDecimal("300000"));
//        products.add(child2);
//
//        // 70 + 40 = 110，不等于母产品的100，应该校验失败
//        return new ParsedData(products);
//    }
//
//    /**
//     * 创建测试数据 - 子产品缺少购买人数设置
//     */
//    private ParsedData createTestData_MissingChild() {
//        List<ProductInfo> products = new ArrayList<>();
//
//        // 母产品: 最大购买人数100
//        ProductInfo parent = new ProductInfo("PARENT003");
//        parent.setProductName("测试母产品3");
//        parent.setProductType("母产品");
//        parent.setMaxInvestors(100);
//        parent.setCurrentAmount(new BigDecimal("1000000"));
//        products.add(parent);
//
//        // 子产品1: 最大购买人数50
//        ProductInfo child1 = new ProductInfo("CHILD005");
//        child1.setProductName("测试子产品5");
//        child1.setProductType("子产品");
//        child1.setParentCode("PARENT003");
//        child1.setMaxInvestors(50);
//        child1.setCurrentAmount(new BigDecimal("500000"));
//        products.add(child1);
//
//        // 子产品2: 未设置最大购买人数
//        ProductInfo child2 = new ProductInfo("CHILD006");
//        child2.setProductName("测试子产品6");
//        child2.setProductType("子产品");
//        child2.setParentCode("PARENT003");
//        // 故意不设置maxInvestors
//        child2.setCurrentAmount(new BigDecimal("500000"));
//        products.add(child2);
//
//        // 应该产生警告但不算错误
//        return new ParsedData(products);
//    }
//}