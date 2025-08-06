//package com.easypan.TA.Test;
//
//import com.easypan.TA.Model.ParsedData;
//import com.easypan.TA.Model.ProductInfo;
//import com.easypan.TA.Validator.ValidatorChain;
//import com.easypan.TA.Validator.ValidationReport;
//import com.easypan.TA.Model.ValidationError;
//import lombok.extern.slf4j.Slf4j;
//
//import java.math.BigDecimal;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * 智能校验策略测试类 - 测试母产品失败时子产品跳过校验的功能
// */
//@Slf4j
//public class SmartValidationTest {
//
//    public static void main(String[] args) {
//        SmartValidationTest test = new SmartValidationTest();
//        test.testSmartValidationStrategy();
//    }
//
//    /**
//     * 测试智能校验策略
//     */
//    public void testSmartValidationStrategy() {
//        log.info("开始测试智能校验策略");
//
//        ValidatorChain validator = new ValidatorChain();
//
//        // 测试用例1: 母产品校验失败的情况
//        testCase1_ParentProductFailed(validator);
//
//        log.info("智能校验策略测试完成");
//    }
//
//    /**
//     * 测试用例1: 母产品校验失败，子产品应被跳过校验
//     */
//    private void testCase1_ParentProductFailed(ValidatorChain validator) {
//        log.info("测试用例1: 母产品校验失败，子产品跳过校验");
//
//        ParsedData testData = createTestDataWithFailedParent();
//
//        // 使用CPDM文件校验方法进行测试
//        ValidationReport report = validator.validateCpdmFile("test_failed_parent.cpdm");
//
//        log.info("整体校验结果: {}", report.isSuccess() ? "通过" : "失败");
//        log.info("错误总数: {}", report.getAllErrors().size());
//
//        // 分析错误信息
//        for (ValidationError error : report.getAllErrors()) {
//            log.info("错误: [{}] {} - {}", error.getErrorCode(),
//                error.getProductCode(), error.getErrorMessage());
//        }
//
//        // 检查是否包含预期的跳过错误
//        boolean hasSkipError = report.getAllErrors().stream()
//            .anyMatch(error -> "PARENT_VALIDATION_FAILED".equals(error.getErrorCode()));
//
//        log.info("是否包含子产品跳过校验的错误: {}", hasSkipError);
//    }
//
//    /**
//     * 创建包含失败母产品的测试数据
//     */
//    private ParsedData createTestDataWithFailedParent() {
//        List<ProductInfo> products = new ArrayList<>();
//
//        // 母产品: 故意设置错误的字段导致校验失败
//        ProductInfo parent = new ProductInfo("INVALID_PARENT");
//        parent.setProductName(""); // 空产品名称，会导致字段校验失败
//        parent.setProductType("母产品");
//        parent.setMaxInvestors(100);
//        parent.setMinAmount(new BigDecimal("100000")); // 最小金额
//        parent.setMaxAmount(new BigDecimal("50000"));  // 最大金额小于最小金额，业务校验失败
//        products.add(parent);
//
//        // 子产品1: 正常的子产品，但因母产品失败应被跳过
//        ProductInfo child1 = new ProductInfo("CHILD001");
//        child1.setProductName("正常子产品1");
//        child1.setProductType("子产品");
//        child1.setParentCode("INVALID_PARENT");
//        child1.setMaxInvestors(60);
//        child1.setCurrentAmount(new BigDecimal("600000"));
//        products.add(child1);
//
//        // 子产品2: 正常的子产品，但因母产品失败应被跳过
//        ProductInfo child2 = new ProductInfo("CHILD002");
//        child2.setProductName("正常子产品2");
//        child2.setProductType("子产品");
//        child2.setParentCode("INVALID_PARENT");
//        child2.setMaxInvestors(40);
//        child2.setCurrentAmount(new BigDecimal("400000"));
//        products.add(child2);
//
//        // 另一个正常的母产品用于对比
//        ProductInfo normalParent = new ProductInfo("NORMAL_PARENT");
//        normalParent.setProductName("正常母产品");
//        normalParent.setProductType("母产品");
//        normalParent.setMaxInvestors(50);
//        normalParent.setCurrentAmount(new BigDecimal("500000"));
//        products.add(normalParent);
//
//        // 正常母产品的子产品
//        ProductInfo normalChild = new ProductInfo("NORMAL_CHILD");
//        normalChild.setProductName("正常子产品");
//        normalChild.setProductType("子产品");
//        normalChild.setParentCode("NORMAL_PARENT");
//        normalChild.setMaxInvestors(50);
//        normalChild.setCurrentAmount(new BigDecimal("300000"));
//        products.add(normalChild);
//
//        return new ParsedData(products);
//    }
//}