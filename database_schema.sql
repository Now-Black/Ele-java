-- 客户每日收益计算相关表结构

-- 1. 主表：客户每日收益表
CREATE TABLE IF NOT EXISTS `tbclientdlyincome` (
  `client_no` varchar(32) NOT NULL COMMENT '客户编号',
  `asset_acc` varchar(32) DEFAULT NULL COMMENT '资产账户',
  `ta_client` varchar(32) DEFAULT NULL COMMENT 'TA客户号',
  `prd_code` varchar(32) NOT NULL COMMENT '产品代码',
  `real_prd_code` varchar(32) DEFAULT NULL COMMENT '实际产品代码',
  `reg_date` date NOT NULL COMMENT '登记日期',
  `allot_amt` decimal(20,4) DEFAULT 0.0000 COMMENT '申购/认购金额',
  `redeem_amt` decimal(20,4) DEFAULT 0.0000 COMMENT '赎回金额',
  `div_income` decimal(20,4) DEFAULT 0.0000 COMMENT '分红收益',
  `redeem_income` decimal(20,4) DEFAULT 0.0000 COMMENT '赎回收益',
  `force_add_amt` decimal(20,4) DEFAULT 0.0000 COMMENT '强增强减金额',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`client_no`, `prd_code`, `reg_date`),
  KEY `idx_reg_date` (`reg_date`),
  KEY `idx_prd_code` (`prd_code`),
  KEY `idx_client_no` (`client_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户每日收益表';

-- 2. 分表：客户每日收益分表（16张表）
-- 根据客户号进行hash分区，tbclientdlyincome0 ~ tbclientdlyincome15

DROP PROCEDURE IF EXISTS create_client_daily_income_tables;

DELIMITER $$

CREATE PROCEDURE create_client_daily_income_tables()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE table_name VARCHAR(50);
    DECLARE temp_table_name VARCHAR(50);
    
    WHILE i < 16 DO
        SET table_name = CONCAT('tbclientdlyincome', i);
        SET temp_table_name = CONCAT('tbclientdlyincometmp', i);
        
        -- 创建分表
        SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS `', table_name, '` (
          `client_no` varchar(32) NOT NULL COMMENT ''客户编号'',
          `asset_acc` varchar(32) DEFAULT NULL COMMENT ''资产账户'',
          `ta_client` varchar(32) DEFAULT NULL COMMENT ''TA客户号'',
          `prd_code` varchar(32) NOT NULL COMMENT ''产品代码'',
          `real_prd_code` varchar(32) DEFAULT NULL COMMENT ''实际产品代码'',
          `reg_date` date NOT NULL COMMENT ''登记日期'',
          `allot_amt` decimal(20,4) DEFAULT 0.0000 COMMENT ''申购/认购金额'',
          `redeem_amt` decimal(20,4) DEFAULT 0.0000 COMMENT ''赎回金额'',
          `div_income` decimal(20,4) DEFAULT 0.0000 COMMENT ''分红收益'',
          `redeem_income` decimal(20,4) DEFAULT 0.0000 COMMENT ''赎回收益'',
          `force_add_amt` decimal(20,4) DEFAULT 0.0000 COMMENT ''强增强减金额'',
          `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
          `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
          PRIMARY KEY (`client_no`, `prd_code`, `reg_date`),
          KEY `idx_reg_date` (`reg_date`),
          KEY `idx_prd_code` (`prd_code`),
          KEY `idx_client_no` (`client_no`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''客户每日收益分表', i, ''';');
        
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        
        -- 创建对应的临时表
        SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS `', temp_table_name, '` (
          `client_no` varchar(32) NOT NULL COMMENT ''客户编号'',
          `asset_acc` varchar(32) DEFAULT NULL COMMENT ''资产账户'',
          `ta_client` varchar(32) DEFAULT NULL COMMENT ''TA客户号'',
          `prd_code` varchar(32) NOT NULL COMMENT ''产品代码'',
          `real_prd_code` varchar(32) DEFAULT NULL COMMENT ''实际产品代码'',
          `reg_date` date NOT NULL COMMENT ''登记日期'',
          `allot_amt` decimal(20,4) DEFAULT 0.0000 COMMENT ''申购/认购金额'',
          `redeem_amt` decimal(20,4) DEFAULT 0.0000 COMMENT ''赎回金额'',
          `div_income` decimal(20,4) DEFAULT 0.0000 COMMENT ''分红收益'',
          `redeem_income` decimal(20,4) DEFAULT 0.0000 COMMENT ''赎回收益'',
          `force_add_amt` decimal(20,4) DEFAULT 0.0000 COMMENT ''强增强减金额'',
          `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
          `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',
          KEY `idx_reg_date` (`reg_date`),
          KEY `idx_prd_code` (`prd_code`),
          KEY `idx_client_no` (`client_no`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''客户每日收益临时表', i, ''';');
        
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        
        SET i = i + 1;
    END WHILE;
    
END$$

DELIMITER ;

-- 执行存储过程创建分表和临时表
CALL create_client_daily_income_tables();

-- 删除存储过程
DROP PROCEDURE create_client_daily_income_tables;

-- 3. 基础数据表（假设存在，用于关联查询）

-- 份额表示例（分16个表：tbshare0 ~ tbshare15）
-- CREATE TABLE IF NOT EXISTS `tbshare0` (
--   `client_no` varchar(32) NOT NULL COMMENT '客户编号',
--   `asset_acc` varchar(32) DEFAULT NULL COMMENT '资产账户',
--   `ta_client` varchar(32) DEFAULT NULL COMMENT 'TA客户号',
--   `prd_code` varchar(32) NOT NULL COMMENT '产品代码',
--   `tot_vol` decimal(20,4) DEFAULT 0.0000 COMMENT '持有总份额',
--   `last_date` date DEFAULT NULL COMMENT '最后交易日期',
--   PRIMARY KEY (`client_no`, `prd_code`)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户份额表0';

-- 交易确认表
-- CREATE TABLE IF NOT EXISTS `tbtatranscfm00` (
--   `client_no` varchar(32) NOT NULL COMMENT '客户编号',
--   `prd_code` varchar(32) NOT NULL COMMENT '产品代码',
--   `busin_code` varchar(10) NOT NULL COMMENT '业务代码',
--   `cfm_amt` decimal(20,4) DEFAULT 0.0000 COMMENT '确认金额',
--   `cfm_date` date NOT NULL COMMENT '确认日期',
--   KEY `idx_cfm_date` (`cfm_date`),
--   KEY `idx_client_prd` (`client_no`, `prd_code`),
--   KEY `idx_busin_code` (`busin_code`)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易确认表';

-- 分红明细表
-- CREATE TABLE IF NOT EXISTS `tbtadivdetail00` (
--   `client_no` varchar(32) NOT NULL COMMENT '客户编号',
--   `prd_code` varchar(32) NOT NULL COMMENT '产品代码',
--   `div_mode` char(1) DEFAULT NULL COMMENT '分红方式（1:现金分红，0:红利再投资）',
--   `real_amt` decimal(20,4) DEFAULT 0.0000 COMMENT '实际分红金额',
--   `reinvest_amt` decimal(20,4) DEFAULT 0.0000 COMMENT '再投资金额',
--   `div_date` date NOT NULL COMMENT '分红日期',
--   KEY `idx_div_date` (`div_date`),
--   KEY `idx_client_prd` (`client_no`, `prd_code`)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分红明细表';

-- 产品信息表
-- CREATE TABLE IF NOT EXISTS `tbproduct_asy` (
--   `prd_code` varchar(32) NOT NULL COMMENT '产品代码',
--   `real_prd_code` varchar(32) DEFAULT NULL COMMENT '实际产品代码',
--   `interest_way` varchar(10) DEFAULT NULL COMMENT '计息方式（JZL:净值类）',
--   `nav_value` decimal(10,6) DEFAULT NULL COMMENT '产品净值',
--   PRIMARY KEY (`prd_code`),
--   KEY `idx_interest_way` (`interest_way`)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品信息表';

-- 业务代码说明：
-- 50: 认购
-- 02: 申购  
-- 03: 赎回
-- 51: 清仓赎回
-- 70: 强增
-- 71: 强减
-- 210: 特殊调整

SELECT '客户每日收益计算表结构创建完成' as message;