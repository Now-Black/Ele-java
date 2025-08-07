package com.easypan.service;

import com.easypan.entity.po.ClientDailyIncome;
import com.easypan.entity.query.ClientDailyIncomeQuery;
import com.easypan.entity.vo.PaginationResultVO;

import java.util.Date;
import java.util.List;

/**
 * 客户每日收益Service
 */
public interface ClientDailyIncomeService {

    /**
     * 根据条件查询列表
     */
    List<ClientDailyIncome> findListByParam(ClientDailyIncomeQuery param);

    /**
     * 根据条件查询列表
     */
    Integer findCountByParam(ClientDailyIncomeQuery param);

    /**
     * 分页查询
     */
    PaginationResultVO<ClientDailyIncome> findListByPage(ClientDailyIncomeQuery param);

    /**
     * 新增
     */
    Integer add(ClientDailyIncome bean);

    /**
     * 批量新增
     */
    Integer addBatch(List<ClientDailyIncome> listBean);

    /**
     * 批量新增/修改
     */
    Integer addOrUpdateBatch(List<ClientDailyIncome> listBean);

    /**
     * 客户每日收益计算 - 主方法
     * @param calculateDate 计算日期
     */
    void calculateClientDailyIncome(Date calculateDate);

    /**
     * 处理单个分表的收益计算
     * @param tableIndex 分表索引 (0-15)
     * @param calculateDate 计算日期
     */
    void processSingleTable(int tableIndex, Date calculateDate);

    /**
     * 第一步：临时表数据准备（包含正常业务和强增强减）
     * @param tableIndex 分表索引
     * @param cfmDate 确认日期
     */
    void regCUSDLYIncomePre(int tableIndex, Date cfmDate);

    /**
     * 第二步：正式表数据处理
     * @param tableIndex 分表索引
     * @param regDate 登记日期
     */
    void regCUSDLYIncomeCal(int tableIndex, Date regDate);

    /**
     * 批量数据汇总到主表
     * @param calculateDate 计算日期
     */
    void summaryDataToMainTable(Date calculateDate);

    /**
     * 清理单个分表的数据
     * @param tableIndex 分表索引
     * @param calculateDate 计算日期
     */
    void cleanupSingleTable(int tableIndex, Date calculateDate);
}