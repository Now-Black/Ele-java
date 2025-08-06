package com.easypan.TA.Service.impl;

import com.easypan.TA.Service.TaCacheService;
import com.easypan.TA.Service.TaDataPersistenceService;
import com.easypan.entity.po.TaProduct;
import com.easypan.entity.po.TaFileMonitor;
import com.easypan.component.RedisComponent;
import com.easypan.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.ArrayList;

/**
 * TA Redis缓存服务实现类
 */
@Slf4j
@Service
public class TaCacheServiceImpl implements TaCacheService {
    
    @Resource
    private RedisComponent redisComponent;
    
    @Resource
    private TaDataPersistenceService taDataPersistenceService;
    
    // 内存缓存
    private final Map<String, TaProduct> memoryProductCache = new ConcurrentHashMap<>();
    
    // Redis键名常量
    private static final String PRODUCT_CACHE_PREFIX = "ta:product:";
    private static final String PARENT_PRODUCTS_KEY = "ta:parent_products";
    private static final String CHILD_PRODUCTS_PREFIX = "ta:child_products:";
    private static final String FILE_MONITOR_PREFIX = "ta:file_monitor:";
    private static final String CACHE_VERSION_KEY = "ta:cache_version";
    
    // 缓存过期时间（小时）
    private static final int CACHE_EXPIRE_HOURS = 24;
    
    // ========== 产品缓存相关 ==========
    
    @Override
    public void refreshAllProductsToRedis() {
        log.info("开始刷新所有产品数据到Redis缓存");
        
        try {
            // 刷新母产品
            refreshParentProductsToRedis();
            
            // 刷新子产品
            refreshChildProductsToRedis();
            
            // 更新缓存版本
            updateCacheVersion();
            
            log.info("所有产品数据刷新到Redis缓存完成");
            
        } catch (Exception e) {
            log.error("刷新所有产品数据到Redis缓存失败", e);
            throw e;
        }
    }
    
    @Override
    public void refreshParentProductsToRedis() {
        log.info("开始刷新母产品到Redis缓存");
        
        try {
            List<TaProduct> parentProducts = taDataPersistenceService.getAllParentProducts();
            
            // 缓存每个母产品
            for (TaProduct product : parentProducts) {
                String key = PRODUCT_CACHE_PREFIX + product.getProductCode();
                redisComponent.set(key, JsonUtils.convertObj2Json(product), CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            }
            
            // 缓存母产品列表
            String parentListJson = JsonUtils.convertObj2Json(parentProducts);
            redisComponent.set(PARENT_PRODUCTS_KEY, parentListJson, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            
            log.info("母产品刷新到Redis缓存完成，数量: {}", parentProducts.size());
            
        } catch (Exception e) {
            log.error("刷新母产品到Redis缓存失败", e);
            throw e;
        }
    }
    
    @Override
    public void refreshChildProductsToRedis() {
        log.info("开始刷新子产品到Redis缓存");
        
        try {
            // 先获取所有母产品
            List<TaProduct> parentProducts = taDataPersistenceService.getAllParentProducts();
            int totalChildProducts = 0;
            
            for (TaProduct parentProduct : parentProducts) {
                List<TaProduct> childProducts = taDataPersistenceService.getChildProductsByParent(
                    parentProduct.getProductCode());
                
                // 缓存每个子产品
                for (TaProduct childProduct : childProducts) {
                    String key = PRODUCT_CACHE_PREFIX + childProduct.getProductCode();
                    redisComponent.set(key, JsonUtils.convertObj2Json(childProduct), 
                        CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                }
                
                // 缓存该母产品的子产品列表
                if (!childProducts.isEmpty()) {
                    String childListKey = CHILD_PRODUCTS_PREFIX + parentProduct.getProductCode();
                    String childListJson = JsonUtils.convertObj2Json(childProducts);
                    redisComponent.set(childListKey, childListJson, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                    
                    totalChildProducts += childProducts.size();
                }
            }
            
            log.info("子产品刷新到Redis缓存完成，数量: {}", totalChildProducts);
            
        } catch (Exception e) {
            log.error("刷新子产品到Redis缓存失败", e);
            throw e;
        }
    }
    
    @Override
    public TaProduct getProductFromCache(String productCode) {
        try {
            String key = PRODUCT_CACHE_PREFIX + productCode;
            String productJson = redisComponent.get(key);
            
            if (productJson != null) {
                return JsonUtils.convertJson2Obj(productJson, TaProduct.class);
            }
            
            // 缓存未命中，从数据库加载并缓存
            TaProduct product = taDataPersistenceService.getProductByCode(productCode);
            if (product != null) {
                updateProductCache(product);
            }
            
            return product;
            
        } catch (Exception e) {
            log.error("从缓存获取产品失败: productCode={}", productCode, e);
            return null;
        }
    }
    
    @Override
    public List<TaProduct> getAllParentProductsFromCache() {
        try {
            String parentListJson = redisComponent.get(PARENT_PRODUCTS_KEY);
            if (parentListJson != null) {
                return JsonUtils.convertJsonArray2List(parentListJson, TaProduct.class);
            }
            
            // 缓存未命中，从数据库加载
            List<TaProduct> parentProducts = taDataPersistenceService.getAllParentProducts();
            if (!parentProducts.isEmpty()) {
                redisComponent.set(PARENT_PRODUCTS_KEY, JsonUtils.convertObj2Json(parentProducts),
                    CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            }
            
            return parentProducts;
            
        } catch (Exception e) {
            log.error("从缓存获取母产品列表失败", e);
            return taDataPersistenceService.getAllParentProducts();
        }
    }
    
    @Override
    public List<TaProduct> getChildProductsFromCache(String parentCode) {
        try {
            String childListKey = CHILD_PRODUCTS_PREFIX + parentCode;
            String childListJson = redisComponent.get(childListKey);
            
            if (childListJson != null) {
                return JsonUtils.convertJsonArray2List(childListJson, TaProduct.class);
            }
            
            // 缓存未命中，从数据库加载
            List<TaProduct> childProducts = taDataPersistenceService.getChildProductsByParent(parentCode);
            if (!childProducts.isEmpty()) {
                redisComponent.set(childListKey, JsonUtils.convertObj2Json(childProducts),
                    CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            }
            
            return childProducts;
            
        } catch (Exception e) {
            log.error("从缓存获取子产品列表失败: parentCode={}", parentCode, e);
            return taDataPersistenceService.getChildProductsByParent(parentCode);
        }
    }
    
    @Override
    public void updateProductCache(TaProduct product) {
        try {
            String key = PRODUCT_CACHE_PREFIX + product.getProductCode();
            redisComponent.set(key, JsonUtils.convertObj2Json(product), 
                CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            
            // 同时更新内存缓存
            memoryProductCache.put(product.getProductCode(), product);
            
            log.debug("更新产品缓存: productCode={}", product.getProductCode());
            
        } catch (Exception e) {
            log.error("更新产品缓存失败: productCode={}", product.getProductCode(), e);
        }
    }
    
    @Override
    public void incrementalUpdateProductsCache(List<TaProduct> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        
        log.info("开始增量更新产品缓存（并行优化）: 产品数量={}", products.size());
        
        try {
            // 分离母产品和子产品进行处理
            List<TaProduct> parentProducts = products.stream()
                .filter(p -> p.getParentCode() == null || p.getParentCode().trim().isEmpty())
                .collect(Collectors.toList());
            
            List<TaProduct> childProducts = products.stream()
                .filter(p -> p.getParentCode() != null && !p.getParentCode().trim().isEmpty())
                .collect(Collectors.toList());
            
            // 并行执行三个更新任务
            CompletableFuture<Integer> individualCacheFuture = asyncUpdateIndividualProductsCache(products);
            CompletableFuture<Void> parentListFuture = asyncUpdateParentProductsList(parentProducts);
            CompletableFuture<Void> childListFuture = asyncUpdateChildProductsList(childProducts);
            
            // 等待所有并行任务完成并获取结果
            CompletableFuture.allOf(individualCacheFuture, parentListFuture, childListFuture).join();
            
            int updatedCount = individualCacheFuture.join();
            
            log.info("增量更新产品缓存完成（并行优化）: 总数={}, 母产品={}, 子产品={}", 
                updatedCount, parentProducts.size(), childProducts.size());
            
        } catch (Exception e) {
            log.error("增量更新产品缓存失败", e);
            throw e;
        }
    }
    
    /**
     * 异步更新每个产品的个体缓存（Redis + 内存）
     */
    @Async("cacheUpdateExecutor")
    public CompletableFuture<Integer> asyncUpdateIndividualProductsCache(List<TaProduct> products) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("开始并行更新个体产品缓存: 产品数量={}", products.size());
                
                int updatedCount = 0;
                
                // 分批并行更新，避免单批过大
                int batchSize = 50;
                List<CompletableFuture<Integer>> batchFutures = new ArrayList<>();
                
                for (int i = 0; i < products.size(); i += batchSize) {
                    int endIndex = Math.min(i + batchSize, products.size());
                    List<TaProduct> batch = products.subList(i, endIndex);
                    
                    CompletableFuture<Integer> batchFuture = CompletableFuture.supplyAsync(() -> {
                        int batchCount = 0;
                        for (TaProduct product : batch) {
                            // 并行更新Redis和内存
                            CompletableFuture<Void> redisFuture = CompletableFuture.runAsync(() -> {
                                String key = PRODUCT_CACHE_PREFIX + product.getProductCode();
                                redisComponent.set(key, JsonUtils.convertObj2Json(product), 
                                    CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                            });
                            
                            CompletableFuture<Void> memoryFuture = CompletableFuture.runAsync(() -> {
                                memoryProductCache.put(product.getProductCode(), product);
                            });
                            
                            // 等待Redis和内存更新完成
                            CompletableFuture.allOf(redisFuture, memoryFuture).join();
                            batchCount++;
                        }
                        return batchCount;
                    });
                    
                    batchFutures.add(batchFuture);
                }
                
                // 等待所有批次完成并汇总结果
                for (CompletableFuture<Integer> future : batchFutures) {
                    updatedCount += future.join();
                }
                
                log.debug("个体产品缓存更新完成: 更新数量={}", updatedCount);
                return updatedCount;
                
            } catch (Exception e) {
                log.error("并行更新个体产品缓存失败", e);
                throw new RuntimeException("个体产品缓存更新失败", e);
            }
        });
    }
    
    /**
     * 异步增量更新母产品列表缓存
     */
    @Async("cacheUpdateExecutor")
    public CompletableFuture<Void> asyncUpdateParentProductsList(List<TaProduct> newParentProducts) {
        return CompletableFuture.runAsync(() -> {
            if (newParentProducts.isEmpty()) {
                return;
            }
            
            try {
                log.debug("开始并行更新母产品列表: 产品数量={}", newParentProducts.size());
                
                incrementalUpdateParentProductsList(newParentProducts);
                
                log.debug("母产品列表更新完成: 更新数量={}", newParentProducts.size());
                
            } catch (Exception e) {
                log.error("并行更新母产品列表失败", e);
                // 出错时重新刷新整个母产品列表
                try {
                    refreshParentProductsToRedis();
                } catch (Exception ex) {
                    log.error("刷新母产品列表也失败", ex);
                }
            }
        });
    }
    
    /**
     * 异步增量更新子产品列表缓存
     */
    @Async("cacheUpdateExecutor")
    public CompletableFuture<Void> asyncUpdateChildProductsList(List<TaProduct> newChildProducts) {
        return CompletableFuture.runAsync(() -> {
            if (newChildProducts.isEmpty()) {
                return;
            }
            
            try {
                log.debug("开始并行更新子产品列表: 产品数量={}", newChildProducts.size());
                
                incrementalUpdateChildProductsList(newChildProducts);
                
                log.debug("子产品列表更新完成: 更新数量={}", newChildProducts.size());
                
            } catch (Exception e) {
                log.error("并行更新子产品列表失败", e);
                // 出错时重新刷新所有子产品列表
                try {
                    refreshChildProductsToRedis();
                } catch (Exception ex) {
                    log.error("刷新子产品列表也失败", ex);
                }
            }
        });
    }
    
    @Override
    public void removeProductCache(String productCode) {
        try {
            String key = PRODUCT_CACHE_PREFIX + productCode;
            redisComponent.delete(key);
            
            log.debug("删除产品缓存: productCode={}", productCode);
            
        } catch (Exception e) {
            log.error("删除产品缓存失败: productCode={}", productCode, e);
        }
    }
    
    // ========== 文件监控缓存相关 ==========
    
    @Override
    public void refreshFileMonitorToRedis(String batchId) {
        log.info("开始刷新文件监控数据到Redis: batchId={}", batchId);
        
        try {
            List<TaFileMonitor> fileMonitors = taDataPersistenceService.getFileMonitorsByBatch(batchId);
            updateFileMonitorCache(batchId, fileMonitors);
            
            log.info("文件监控数据刷新到Redis完成: batchId={}, 文件数量={}", batchId, fileMonitors.size());
            
        } catch (Exception e) {
            log.error("刷新文件监控数据到Redis失败: batchId={}", batchId, e);
            throw e;
        }
    }
    
    @Override
    public List<TaFileMonitor> getFileMonitorFromCache(String batchId) {
        try {
            String key = FILE_MONITOR_PREFIX + batchId;
            String fileMonitorJson = redisComponent.get(key);
            
            if (fileMonitorJson != null) {
                return JsonUtils.convertJsonArray2List(fileMonitorJson, TaFileMonitor.class);
            }
            
            // 缓存未命中，从数据库加载
            List<TaFileMonitor> fileMonitors = taDataPersistenceService.getFileMonitorsByBatch(batchId);
            if (!fileMonitors.isEmpty()) {
                updateFileMonitorCache(batchId, fileMonitors);
            }
            
            return fileMonitors;
            
        } catch (Exception e) {
            log.error("从缓存获取文件监控数据失败: batchId={}", batchId, e);
            return taDataPersistenceService.getFileMonitorsByBatch(batchId);
        }
    }
    
    @Override
    public void updateFileMonitorCache(String batchId, List<TaFileMonitor> fileMonitors) {
        try {
            String key = FILE_MONITOR_PREFIX + batchId;
            String fileMonitorJson = JsonUtils.convertObj2Json(fileMonitors);
            redisComponent.set(key, fileMonitorJson, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            
            log.debug("更新文件监控缓存: batchId={}", batchId);
            
        } catch (Exception e) {
            log.error("更新文件监控缓存失败: batchId={}", batchId, e);
        }
    }
    
    @Override
    public void removeFileMonitorCache(String batchId) {
        try {
            String key = FILE_MONITOR_PREFIX + batchId;
            redisComponent.delete(key);
            
            log.debug("删除文件监控缓存: batchId={}", batchId);
            
        } catch (Exception e) {
            log.error("删除文件监控缓存失败: batchId={}", batchId, e);
        }
    }
    
    // ========== 内存缓存相关 ==========
    
    @Override
    public void refreshProductsToMemory() {
        log.info("开始刷新产品数据到内存缓存");
        
        try {
            // 清空现有内存缓存
            memoryProductCache.clear();
            
            // 获取所有产品
            List<TaProduct> allProducts = taDataPersistenceService.getAllParentProducts();
            for (TaProduct parentProduct : allProducts) {
                memoryProductCache.put(parentProduct.getProductCode(), parentProduct);
                
                // 加载子产品
                List<TaProduct> childProducts = taDataPersistenceService.getChildProductsByParent(
                    parentProduct.getProductCode());
                for (TaProduct childProduct : childProducts) {
                    memoryProductCache.put(childProduct.getProductCode(), childProduct);
                }
            }
            
            log.info("产品数据刷新到内存缓存完成，总数量: {}", memoryProductCache.size());
            
        } catch (Exception e) {
            log.error("刷新产品数据到内存缓存失败", e);
            throw e;
        }
    }
    
    @Override
    public TaProduct getProductFromMemory(String productCode) {
        return memoryProductCache.get(productCode);
    }
    
    @Override
    public Map<String, TaProduct> getAllProductsFromMemory() {
        return new HashMap<>(memoryProductCache);
    }
    
    @Override
    public void clearMemoryCache() {
        memoryProductCache.clear();
        log.info("内存缓存已清空");
    }
    
    // ========== 缓存统计 ==========
    
    @Override
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 内存缓存统计
            stats.put("memoryProductCount", memoryProductCache.size());
            
            // Redis缓存统计
            boolean redisAvailable = redisComponent.exists(PARENT_PRODUCTS_KEY);
            stats.put("redisAvailable", redisAvailable);
            
            if (redisAvailable) {
                String parentListJson = redisComponent.get(PARENT_PRODUCTS_KEY);
                if (parentListJson != null) {
                    List<TaProduct> parentProducts = JsonUtils.convertJsonArray2List(parentListJson, TaProduct.class);
                    stats.put("redisParentProductCount", parentProducts.size());
                }
            }
            
            // 缓存版本
            String cacheVersion = redisComponent.get(CACHE_VERSION_KEY);
            stats.put("cacheVersion", cacheVersion);
            
        } catch (Exception e) {
            log.error("获取缓存统计信息失败", e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    @Override
    public boolean needRefreshCache(String batchId) {
        try {
            // 检查缓存版本是否匹配当前批次
            String cacheVersion = redisComponent.get(CACHE_VERSION_KEY);
            return !batchId.equals(cacheVersion);
            
        } catch (Exception e) {
            log.error("检查缓存刷新状态失败: batchId={}", batchId, e);
            return true; // 出错时默认需要刷新
        }
    }
    
    /**
     * 更新缓存版本
     */
    private void updateCacheVersion() {
        try {
            String version = String.valueOf(System.currentTimeMillis());
            redisComponent.set(CACHE_VERSION_KEY, version, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            
            log.debug("更新缓存版本: {}", version);
            
        } catch (Exception e) {
            log.error("更新缓存版本失败", e);
        }
    }
    
    /**
     * 增量更新母产品列表缓存
     */
    private void incrementalUpdateParentProductsList(List<TaProduct> newParentProducts) {
        try {
            // 获取现有的母产品列表
            List<TaProduct> existingParents = getAllParentProductsFromCache();
            
            // 创建代码到产品的映射，便于更新
            Map<String, TaProduct> parentMap = existingParents.stream()
                .collect(Collectors.toMap(TaProduct::getProductCode, p -> p));
            
            // 更新或添加新的母产品
            for (TaProduct newParent : newParentProducts) {
                parentMap.put(newParent.getProductCode(), newParent);
            }
            
            // 重新缓存更新后的母产品列表
            List<TaProduct> updatedParents = parentMap.values().stream()
                .collect(Collectors.toList());
            
            String parentListJson = JsonUtils.convertObj2Json(updatedParents);
            redisComponent.set(PARENT_PRODUCTS_KEY, parentListJson, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            
            log.debug("增量更新母产品列表完成: 新增/更新{}个", newParentProducts.size());
            
        } catch (Exception e) {
            log.error("增量更新母产品列表失败", e);
            // 出错时重新刷新整个母产品列表
            refreshParentProductsToRedis();
        }
    }
    
    /**
     * 增量更新子产品列表缓存（按母产品分组）
     */
    private void incrementalUpdateChildProductsList(List<TaProduct> newChildProducts) {
        try {
            // 按母产品代码分组
            Map<String, List<TaProduct>> childProductsByParent = newChildProducts.stream()
                .collect(Collectors.groupingBy(TaProduct::getParentCode));
            
            // 为每个母产品更新其子产品列表缓存
            for (Map.Entry<String, List<TaProduct>> entry : childProductsByParent.entrySet()) {
                String parentCode = entry.getKey();
                List<TaProduct> newChildren = entry.getValue();
                
                // 获取该母产品现有的子产品列表
                List<TaProduct> existingChildren = getChildProductsFromCache(parentCode);
                
                // 创建代码到产品的映射，便于更新
                Map<String, TaProduct> childMap = existingChildren.stream()
                    .collect(Collectors.toMap(TaProduct::getProductCode, p -> p));
                
                // 更新或添加新的子产品
                for (TaProduct newChild : newChildren) {
                    childMap.put(newChild.getProductCode(), newChild);
                }
                
                // 重新缓存更新后的子产品列表
                List<TaProduct> updatedChildren = childMap.values().stream()
                    .collect(Collectors.toList());
                
                String childListKey = CHILD_PRODUCTS_PREFIX + parentCode;
                String childListJson = JsonUtils.convertObj2Json(updatedChildren);
                redisComponent.set(childListKey, childListJson, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                
                log.debug("增量更新母产品{}的子产品列表完成: 新增/更新{}个", parentCode, newChildren.size());
            }
            
        } catch (Exception e) {
            log.error("增量更新子产品列表失败", e);
            // 出错时重新刷新所有子产品列表
            refreshChildProductsToRedis();
        }
    }
}