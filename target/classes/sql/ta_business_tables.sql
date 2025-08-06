-- 创建TA系统相关表结构

-- 1. 文件监控表
CREATE TABLE `ta_file_monitor` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_id` varchar(50) NOT NULL COMMENT '批次ID',
  `file_name` varchar(255) NOT NULL COMMENT '文件名',
  `file_type` varchar(20) NOT NULL COMMENT '文件类型: CPDM, JYCS',
  `file_path` varchar(500) DEFAULT NULL COMMENT '文件路径',
  `file_size` bigint(20) DEFAULT 0 COMMENT '文件大小(字节)',
  `total_records` int(11) DEFAULT 0 COMMENT '总记录数',
  `valid_records` int(11) DEFAULT 0 COMMENT '有效记录数',
  `invalid_records` int(11) DEFAULT 0 COMMENT '无效记录数',
  `parse_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '解析状态: PENDING, SUCCESS, FAILED',
  `parse_time` bigint(20) DEFAULT 0 COMMENT '解析耗时(毫秒)',
  `validation_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '校验状态: PENDING, SUCCESS, FAILED, PARTIAL',
  `error_message` text COMMENT '错误信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_file_type` (`file_type`),
  KEY `idx_parse_status` (`parse_status`),
  KEY `idx_validation_status` (`validation_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TA文件监控表';

-- 2. 产品监控表
CREATE TABLE `ta_product_monitor` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_id` varchar(50) NOT NULL COMMENT '批次ID',
  `product_code` varchar(50) NOT NULL COMMENT '产品代码',
  `product_name` varchar(255) DEFAULT NULL COMMENT '产品名称',
  `product_type` varchar(50) DEFAULT NULL COMMENT '产品类型: 母产品, 子产品',
  `parent_code` varchar(50) DEFAULT NULL COMMENT '母产品代码',
  `source_file` varchar(20) DEFAULT NULL COMMENT '数据来源文件: CPDM, JYCS, BOTH',
  `validation_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '校验状态: SUCCESS, FAILED, SKIPPED',
  `skip_reason` varchar(100) DEFAULT NULL COMMENT '跳过原因: PARENT_FAILED等',
  `field_validation` varchar(20) DEFAULT 'PENDING' COMMENT '字段校验结果: SUCCESS, FAILED, SKIPPED',
  `business_validation` varchar(20) DEFAULT 'PENDING' COMMENT '业务校验结果: SUCCESS, FAILED, SKIPPED',
  `cross_validation` varchar(20) DEFAULT 'PENDING' COMMENT '交叉校验结果: SUCCESS, FAILED, SKIPPED',
  `error_count` int(11) DEFAULT 0 COMMENT '错误数量',
  `warning_count` int(11) DEFAULT 0 COMMENT '警告数量',
  `processed` tinyint(1) DEFAULT 0 COMMENT '是否已处理入库',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_product` (`batch_id`, `product_code`),
  KEY `idx_product_code` (`product_code`),
  KEY `idx_product_type` (`product_type`),
  KEY `idx_validation_status` (`validation_status`),
  KEY `idx_processed` (`processed`),
  KEY `idx_parent_code` (`parent_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TA产品监控表';

-- 3. TA产品主表
CREATE TABLE `ta_product` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_code` varchar(50) NOT NULL COMMENT '产品代码',
  `product_name` varchar(255) NOT NULL COMMENT '产品名称',
  `product_type` varchar(50) NOT NULL COMMENT '产品类型: 母产品, 子产品',
  `parent_code` varchar(50) DEFAULT NULL COMMENT '母产品代码',
  `status` varchar(50) DEFAULT NULL COMMENT '产品状态',
  `risk_level` varchar(50) DEFAULT NULL COMMENT '风险等级',
  `currency` varchar(10) DEFAULT 'CNY' COMMENT '币种',
  `investment_type` varchar(100) DEFAULT NULL COMMENT '投资类型',
  `description` text COMMENT '产品描述',
  `min_amount` decimal(20,2) DEFAULT NULL COMMENT '最小募集金额',
  `max_amount` decimal(20,2) DEFAULT NULL COMMENT '最大募集金额',
  `current_amount` decimal(20,2) DEFAULT NULL COMMENT '当前募集金额',
  `expected_return` decimal(8,4) DEFAULT NULL COMMENT '预期收益率',
  `term_days` int(11) DEFAULT NULL COMMENT '期限天数',
  `max_investors` int(11) DEFAULT NULL COMMENT '允许最大购买人数',
  `establish_date` datetime DEFAULT NULL COMMENT '成立日期',
  `maturity_date` datetime DEFAULT NULL COMMENT '到期日期',
  `issue_date` datetime DEFAULT NULL COMMENT '发行日期',
  `last_update_batch` varchar(50) DEFAULT NULL COMMENT '最后更新批次',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_code` (`product_code`),
  KEY `idx_product_type` (`product_type`),
  KEY `idx_parent_code` (`parent_code`),
  KEY `idx_status` (`status`),
  KEY `idx_last_update_batch` (`last_update_batch`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TA产品主表';

-- 4. TA产品扩展表
CREATE TABLE `ta_product_extend` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_code` varchar(50) NOT NULL COMMENT '产品代码',
  `extend_field` varchar(100) NOT NULL COMMENT '扩展字段名',
  `extend_value` text COMMENT '扩展字段值',
  `data_type` varchar(20) DEFAULT 'STRING' COMMENT '数据类型: STRING, NUMBER, DATE, BOOLEAN',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_field` (`product_code`, `extend_field`),
  KEY `idx_product_code` (`product_code`),
  CONSTRAINT `fk_product_extend_main` FOREIGN KEY (`product_code`) REFERENCES `ta_product` (`product_code`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TA产品扩展表';

-- 添加索引
CREATE INDEX `idx_file_monitor_batch_type` ON `ta_file_monitor` (`batch_id`, `file_type`);
CREATE INDEX `idx_product_monitor_batch_status` ON `ta_product_monitor` (`batch_id`, `validation_status`);
CREATE INDEX `idx_product_monitor_parent_status` ON `ta_product_monitor` (`parent_code`, `validation_status`);