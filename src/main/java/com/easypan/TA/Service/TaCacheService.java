package com.easypan.TA.Service;

import com.easypan.entity.po.TaProduct;
import com.easypan.entity.po.TaFileMonitor;

import java.util.List;
import java.util.Map;

/**
 * TA Redis缓存服务接口
 */
public interface TaCacheService {
    
    // ========== 产品缓存相关 ==========
    
    /**
     * 刷新所有产品到Redis缓存
     */
    void refreshAllProductsToRedis();
    
    /**
     * 刷新母产品到Redis缓存
     */
    void refreshParentProductsToRedis();
    
    /**
     * 刷新子产品到Redis缓存
     */
    void refreshChildProductsToRedis();
    
    /**
     * 根据产品代码从缓存获取产品信息
     */
    TaProduct getProductFromCache(String productCode);
    
    /**
     * 从缓存获取所有母产品
     */
    List<TaProduct> getAllParentProductsFromCache();
    
    /**
     * 从缓存获取指定母产品的所有子产品
     */
    List<TaProduct> getChildProductsFromCache(String parentCode);
    
    /**
     * 更新单个产品缓存
     */
    void updateProductCache(TaProduct product);
    
    /**
     * 批量增量更新产品缓存（新增或更新，不清空原有缓存）
     */
    void incrementalUpdateProductsCache(List<TaProduct> products);
    
    /**
     * 删除产品缓存
     */
    void removeProductCache(String productCode);
    
    // ========== 文件监控缓存相关 ==========
    
    /**
     * 刷新文件监控数据到Redis
     */
    void refreshFileMonitorToRedis(String batchId);
    
    /**
     * 从缓存获取文件监控信息
     */
    List<TaFileMonitor> getFileMonitorFromCache(String batchId);
    
    /**
     * 更新文件监控缓存
     */
    void updateFileMonitorCache(String batchId, List<TaFileMonitor> fileMonitors);
    
    /**
     * 删除文件监控缓存
     */
    void removeFileMonitorCache(String batchId);
    
    // ========== 内存缓存相关 ==========
    
    /**
     * 刷新产品数据到内存缓存
     */
    void refreshProductsToMemory();
    
    /**
     * 从内存缓存获取产品
     */
    TaProduct getProductFromMemory(String productCode);
    
    /**
     * 从内存缓存获取所有产品
     */
    Map<String, TaProduct> getAllProductsFromMemory();
    
    /**
     * 清除内存缓存
     */
    void clearMemoryCache();
    
    // ========== 缓存统计 ==========
    
    /**
     * 获取缓存统计信息
     */
    Map<String, Object> getCacheStatistics();
    
    /**
     * 检查缓存是否需要刷新
     */
    boolean needRefreshCache(String batchId);
}