package com.easypan.mappers;

import com.easypan.entity.po.TaProduct;
import com.easypan.entity.query.TaProductQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * TA产品主表Mapper接口
 */
public interface TaProductMapper extends BaseMapper<TaProduct, TaProductQuery> {
    
    /**
     * 根据产品代码查询产品
     */
    TaProduct selectByProductCode(@Param("productCode") String productCode);
    
    /**
     * 根据母产品代码查询所有子产品
     */
    List<TaProduct> selectChildrenByParentCode(@Param("parentCode") String parentCode);
    
    /**
     * 查询所有母产品
     */
    List<TaProduct> selectAllParentProducts();
    
    /**
     * 根据产品类型查询
     */
    List<TaProduct> selectByProductType(@Param("productType") String productType);
    
    /**
     * 批量插入或更新产品
     */
    Integer insertOrUpdateBatch(@Param("list") List<TaProduct> list);
    
    /**
     * 批量插入产品
     */
    Integer insertBatch(@Param("list") List<TaProduct> list);
    
    /**
     * 批量更新产品
     */
    Integer updateBatch(@Param("list") List<TaProduct> list);
    
    /**
     * 删除指定批次之外的产品数据
     */
    Integer deleteExceptBatch(@Param("batchId") String batchId);
    
    /**
     * 根据产品代码列表查询产品
     */
    List<TaProduct> selectByProductCodes(@Param("productCodes") List<String> productCodes);
}