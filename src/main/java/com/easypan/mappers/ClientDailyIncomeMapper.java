package com.easypan.mappers;

import com.easypan.entity.po.ClientDailyIncome;
import com.easypan.entity.query.ClientDailyIncomeQuery;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 客户每日收益Mapper
 */
public interface ClientDailyIncomeMapper extends BaseMapper<ClientDailyIncome, ClientDailyIncomeQuery> {

    /**
     * 清空临时表
     */
    void truncateTempTable(@Param("tableIndex") int tableIndex);

    /**
     * 第一步：临时表数据准备
     */
    void regCUSDLYIncomePre(@Param("tableIndex") int tableIndex, @Param("cfmDate") Date cfmDate);

    /**
     * 删除当天和前一天的数据
     */
    void deleteByDateRange(@Param("tableIndex") int tableIndex, @Param("regDate") Date regDate);

    /**
     * 第二步：正式表数据处理
     */
    void regCUSDLYIncomeCal(@Param("tableIndex") int tableIndex, @Param("regDate") Date regDate);

    /**
     * 批量插入数据到主表（分批）
     * @param tableIndex 分表索引
     * @param regDate 登记日期
     * @param offset 偏移量
     * @param batchSize 批次大小
     * @return 影响行数
     */
    int insertToMainTable(@Param("tableIndex") int tableIndex, @Param("regDate") Date regDate, 
                         @Param("offset") int offset, @Param("batchSize") int batchSize);

    /**
     * 批量插入数据到主表（一次性，兼容旧版本）
     * @param tableIndex 分表索引
     * @param regDate 登记日期
     * @return 影响行数
     */
    int insertToMainTableAll(@Param("tableIndex") int tableIndex, @Param("regDate") Date regDate);

    /**
     * 统计分表数据总量
     * @param tableIndex 分表索引
     * @param regDate 登记日期
     * @return 数据总量
     */
    int countTableData(@Param("tableIndex") int tableIndex, @Param("regDate") Date regDate);

    /**
     * 获取分表数据
     */
    List<ClientDailyIncome> selectFromSubTable(@Param("tableIndex") int tableIndex, @Param("regDate") Date regDate);

    /**
     * 计算客户编号的分表索引
     */
    default int getTableIndex(String clientNo) {
        return Math.abs(clientNo.hashCode()) % 16;
    }
}