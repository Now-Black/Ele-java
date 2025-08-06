package com.easypan.TA.Model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class ParsedData {
    
    private List<ProductInfo> parentProducts;    // 母产品列表
    private List<ProductInfo> childProducts;     // 子产品列表  
    private List<ProductExtend> productExtends;  // 产品扩展信息列表
    private String fileName;                     // 文件名
    private int totalLines;                      // 文件总行数
    private long parseTime;                      // 解析耗时(ms)
    
    public ParsedData() {
        this.parentProducts = new ArrayList<>();
        this.childProducts = new ArrayList<>();
        this.productExtends = new ArrayList<>();
    }
    
    public ParsedData(String fileName) {
        this();
        this.fileName = fileName;
    }
    
    // 获取所有产品（母产品+子产品）
    public List<ProductInfo> getAllProducts() {
        List<ProductInfo> allProducts = new ArrayList<>();
        allProducts.addAll(parentProducts);
        allProducts.addAll(childProducts);
        return allProducts;
    }
    
    // 获取所有产品代码
    public Set<String> getAllProductCodes() {
        return getAllProducts().stream()
                .map(ProductInfo::getProductCode)
                .collect(Collectors.toSet());
    }
    
    // 根据产品代码查找产品
    public ProductInfo findProductByCode(String productCode) {
        return getAllProducts().stream()
                .filter(p -> productCode.equals(p.getProductCode()))
                .findFirst()
                .orElse(null);
    }
    
    // 根据产品代码查找扩展信息
    public ProductExtend findExtendByCode(String productCode) {
        return productExtends.stream()
                .filter(e -> productCode.equals(e.getProductCode()))
                .findFirst()
                .orElse(null);
    }
    
    // 添加产品
    public void addProduct(ProductInfo product) {
        if (product.isParentProduct()) {
            parentProducts.add(product);
        } else if (product.isChildProduct()) {
            childProducts.add(product);
        }
    }
    
    // 添加扩展信息
    public void addExtend(ProductExtend extend) {
        productExtends.add(extend);
    }
    
    // 获取统计信息
    public String getStatistics() {
        return String.format("文件: %s, 母产品: %d个, 子产品: %d个, 扩展信息: %d个, 解析耗时: %dms", 
                fileName, parentProducts.size(), childProducts.size(), productExtends.size(), parseTime);
    }
}