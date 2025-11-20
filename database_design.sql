-- ========================================================================
-- 钉钉精益数据统计系统 - MySQL 8.0.18 数据库设计方案
-- ========================================================================
-- 设计原则:
-- 1. 通用性强，支持不同类型的钉钉表单
-- 2. 充分利用JSON字段存储动态数据
-- 3. 优化索引，支持高效查询和统计
-- 4. 支持历史数据追踪和审计
-- ========================================================================

-- 1. 主数据库设置
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 2. 流程实例主表 - 存储每个钉钉流程的核心信息
CREATE TABLE `ding_process_instances` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `business_id` VARCHAR(100) NOT NULL COMMENT '业务ID（钉钉流程唯一标识）',
  `title` VARCHAR(500) NOT NULL COMMENT '流程标题',
  `process_code` VARCHAR(100) COMMENT '流程模板编码',
  `originator_user_id` VARCHAR(50) NOT NULL COMMENT '发起人用户ID',
  `originator_user_name` VARCHAR(100) COMMENT '发起人姓名',
  `originator_dept_id` VARCHAR(50) COMMENT '发起人部门ID',
  `originator_dept_name` VARCHAR(200) COMMENT '发起人部门名称',
  `status` VARCHAR(50) NOT NULL COMMENT '流程状态(RUNNING,COMPLETED,CANCELED)',
  `result` VARCHAR(50) COMMENT '流程结果(agree,refuse等)',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `finish_time` TIMESTAMP NULL COMMENT '完成时间',
  `form_data` JSON COMMENT '表单完整数据JSON',
  `raw_data` JSON COMMENT '钉钉原始返回数据JSON',
  `sync_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '同步时间',
  `version` INT NOT NULL DEFAULT 1 COMMENT '版本号，用于乐观锁',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_business_id` (`business_id`),
  KEY `idx_originator_user` (`originator_user_id`),
  KEY `idx_originator_dept` (`originator_dept_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_finish_time` (`finish_time`),
  KEY `idx_sync_time` (`sync_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程实例主表';

-- 3. 表单组件值表 - 结构化存储表单数据
CREATE TABLE `ding_form_component_values` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `business_id` VARCHAR(100) NOT NULL COMMENT '业务ID',
  `component_id` VARCHAR(100) NOT NULL COMMENT '组件ID',
  `component_name` VARCHAR(200) NOT NULL COMMENT '组件名称',
  `component_type` VARCHAR(50) NOT NULL COMMENT '组件类型(TextField,DDSelectField等)',
  `value_text` TEXT COMMENT '文本值',
  `value_number` DECIMAL(20,4) COMMENT '数值',
  `value_date` DATE COMMENT '日期值',
  `value_json` JSON COMMENT '复杂类型值(JSON格式)',
  `ext_value` JSON COMMENT '扩展值(JSON格式)',
  `biz_alias` VARCHAR(200) COMMENT '业务别名',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_business_component` (`business_id`, `component_id`),
  KEY `idx_component_name` (`component_name`),
  KEY `idx_component_type` (`component_type`),
  KEY `idx_value_text` (`value_text`(50)),
  KEY `idx_value_date` (`value_date`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_form_component_process` FOREIGN KEY (`business_id`) REFERENCES `ding_process_instances` (`business_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表单组件值表';

-- 4. 操作记录表 - 存储流程操作历史
CREATE TABLE `ding_operation_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `business_id` VARCHAR(100) NOT NULL COMMENT '业务ID',
  `operation_id` VARCHAR(100) COMMENT '操作ID',
  `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型(START_PROCESS_INSTANCE,EXECUTE_TASK_NORMAL等)',
  `show_name` VARCHAR(200) COMMENT '操作显示名称',
  `user_id` VARCHAR(50) COMMENT '操作人用户ID',
  `user_name` VARCHAR(100) COMMENT '操作人姓名',
  `result` VARCHAR(50) COMMENT '操作结果(AGREE,REFUSE,NONE等)',
  `remark` TEXT COMMENT '操作备注',
  `activity_id` VARCHAR(100) COMMENT '活动ID',
  `operation_time` TIMESTAMP NOT NULL COMMENT '操作时间',
  `images` JSON COMMENT '相关图片信息',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_business_id` (`business_id`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_operation_time` (`operation_time`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_operation_process` FOREIGN KEY (`business_id`) REFERENCES `ding_process_instances` (`business_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作记录表';

-- 5. 任务表 - 存储流程任务信息
CREATE TABLE `ding_tasks` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `business_id` VARCHAR(100) NOT NULL COMMENT '业务ID',
  `task_id` BIGINT NOT NULL COMMENT '任务ID',
  `activity_id` VARCHAR(100) NOT NULL COMMENT '活动ID',
  `user_id` VARCHAR(50) NOT NULL COMMENT '处理人用户ID',
  `user_name` VARCHAR(100) COMMENT '处理人姓名',
  `status` VARCHAR(50) NOT NULL COMMENT '任务状态(RUNNING,COMPLETED,CANCELED)',
  `result` VARCHAR(50) COMMENT '处理结果(AGREE,REFUSE,REDIRECTED等)',
  `create_time` TIMESTAMP NOT NULL COMMENT '任务创建时间',
  `finish_time` TIMESTAMP NULL COMMENT '任务完成时间',
  `mobile_url` VARCHAR(1000) COMMENT '移动端链接',
  `pc_url` VARCHAR(1000) COMMENT 'PC端链接',
  `sync_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '同步时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_id` (`task_id`),
  KEY `idx_business_id` (`business_id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_finish_time` (`finish_time`),
  CONSTRAINT `fk_task_process` FOREIGN KEY (`business_id`) REFERENCES `ding_process_instances` (`business_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

-- 6. 附件表 - 存储表单中的附件信息
CREATE TABLE `ding_attachments` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `business_id` VARCHAR(100) NOT NULL COMMENT '业务ID',
  `component_id` VARCHAR(100) NOT NULL COMMENT '组件ID',
  `file_id` VARCHAR(100) NOT NULL COMMENT '文件ID',
  `file_name` VARCHAR(500) NOT NULL COMMENT '文件名',
  `file_type` VARCHAR(50) COMMENT '文件类型',
  `file_size` BIGINT COMMENT '文件大小(字节)',
  `space_id` VARCHAR(100) COMMENT '空间ID',
  `thumbnail_url` VARCHAR(1000) COMMENT '缩略图URL',
  `download_url` VARCHAR(1000) COMMENT '下载URL',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_business_id` (`business_id`),
  KEY `idx_component_id` (`component_id`),
  KEY `idx_file_id` (`file_id`),
  KEY `idx_file_type` (`file_type`),
  CONSTRAINT `fk_attachment_process` FOREIGN KEY (`business_id`) REFERENCES `ding_process_instances` (`business_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='附件表';

-- 7. 照片表 - 存储表单中的照片信息
CREATE TABLE `ding_photos` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `business_id` VARCHAR(100) NOT NULL COMMENT '业务ID',
  `component_id` VARCHAR(100) NOT NULL COMMENT '组件ID',
  `photo_url` VARCHAR(1000) NOT NULL COMMENT '照片URL',
  `thumbnail_url` VARCHAR(1000) COMMENT '缩略图URL',
  `width` INT COMMENT '图片宽度',
  `height` INT COMMENT '图片高度',
  `media_id` VARCHAR(200) COMMENT '媒体ID',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_business_id` (`business_id`),
  KEY `idx_component_id` (`component_id`),
  CONSTRAINT `fk_photo_process` FOREIGN KEY (`business_id`) REFERENCES `ding_process_instances` (`business_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='照片表';

-- 8. 联系人表 - 存储表单中的联系人信息
CREATE TABLE `ding_contacts` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `business_id` VARCHAR(100) NOT NULL COMMENT '业务ID',
  `component_id` VARCHAR(100) NOT NULL COMMENT '组件ID',
  `contact_user_id` VARCHAR(50) NOT NULL COMMENT '联系人用户ID',
  `contact_user_name` VARCHAR(100) COMMENT '联系人姓名',
  `contact_dept_id` VARCHAR(50) COMMENT '联系部门ID',
  `contact_dept_name` VARCHAR(200) COMMENT '联系部门名称',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_business_id` (`business_id`),
  KEY `idx_component_id` (`component_id`),
  KEY `idx_contact_user_id` (`contact_user_id`),
  CONSTRAINT `fk_contact_process` FOREIGN KEY (`business_id`) REFERENCES `ding_process_instances` (`business_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='联系人表';

-- 9. 统计维度配置表 - 支持灵活的统计需求
CREATE TABLE `stat_dimension_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dimension_name` VARCHAR(100) NOT NULL COMMENT '维度名称',
  `dimension_code` VARCHAR(50) NOT NULL COMMENT '维度编码',
  `component_name` VARCHAR(200) COMMENT '对应的表单组件名称',
  `component_type` VARCHAR(50) COMMENT '组件类型',
  `extract_rule` VARCHAR(500) COMMENT '提取规则',
  `data_type` VARCHAR(20) DEFAULT 'STRING' COMMENT '数据类型(STRING,NUMBER,DATE)',
  `is_active` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `description` VARCHAR(500) COMMENT '描述',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dimension_code` (`dimension_code`),
  KEY `idx_component_name` (`component_name`),
  KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统计维度配置表';

-- 10. 插入基础统计维度配置
INSERT INTO `stat_dimension_config` (`dimension_name`, `dimension_code`, `component_name`, `component_type`, `data_type`, `description`) VALUES
('姓名', 'proposer_name', '姓名', 'TextField', 'STRING', '提案人姓名'),
('部门', 'department', '分院/部门', 'TextField', 'STRING', '所属部门'),
('提案类别', 'proposal_category', '提案类别', 'DDSelectField', 'STRING', '提案分类'),
('申报日期', 'apply_date', '申报日期', 'DDDateField', 'DATE', '申报日期'),
('是否继续推进', 'is_continue', '是否继续推进', 'DDSelectField', 'STRING', '推进状态'),
('提案分值', 'proposal_score', '提案分值', 'DDSelectField', 'NUMBER', '提案评分'),
('涉及岗位', 'related_position', '本提案属于哪个岗位', 'DDSelectField', 'STRING', '相关岗位'),
('直属主管是否董事长', 'is_direct_supervisor_chairman', '直属主管是否是董事长', 'DDSelectField', 'STRING', '主管层级'),
('实际完成日期', 'actual_finish_date', '实际完成日期', 'DDDateField', 'DATE', '完成时间');

-- 11. 统计结果缓存表 - 提高统计查询性能
CREATE TABLE `stat_result_cache` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `stat_key` VARCHAR(200) NOT NULL COMMENT '统计键值',
  `stat_type` VARCHAR(50) NOT NULL COMMENT '统计类型',
  `dimension1` VARCHAR(100) COMMENT '维度1',
  `dimension2` VARCHAR(100) COMMENT '维度2',
  `dimension3` VARCHAR(100) COMMENT '维度3',
  `stat_value` DECIMAL(20,4) NOT NULL COMMENT '统计值',
  `stat_count` BIGINT DEFAULT 0 COMMENT '统计数量',
  `calc_date` DATE NOT NULL COMMENT '计算日期',
  `data_date` DATE COMMENT '数据日期',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_key` (`stat_key`, `stat_type`, `dimension1`, `dimension2`, `dimension3`, `calc_date`),
  KEY `idx_stat_type` (`stat_type`),
  KEY `idx_calc_date` (`calc_date`),
  KEY `idx_data_date` (`data_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统计结果缓存表';

-- 12. 创建视图 - 简化常用查询
-- 12.1 流程汇总视图
CREATE VIEW `v_process_summary` AS
SELECT
    pi.business_id,
    pi.title,
    pi.originator_user_name,
    pi.originator_dept_name,
    pi.status,
    pi.result,
    pi.create_time,
    pi.finish_time,
    DATEDIFF(IFNULL(pi.finish_time, NOW()), pi.create_time) as process_days,
    (SELECT COUNT(*) FROM ding_tasks t WHERE t.business_id = pi.business_id) as task_count,
    (SELECT COUNT(*) FROM ding_operation_records o WHERE o.business_id = pi.business_id) as operation_count
FROM ding_process_instances pi;

-- 12.2 提案统计视图
CREATE VIEW `v_proposal_stats` AS
SELECT
    pi.business_id,
    pi.title,
    pi.originator_user_name as proposer_name,
    pi.originator_dept_name as department,
    fc1.value_text as proposal_category,
    fc2.value_date as apply_date,
    fc3.value_text as is_continue,
    fc4.value_text as proposal_score,
    fc5.value_text as related_position,
    fc6.value_text as actual_finish_date,
    pi.status,
    pi.result,
    pi.create_time,
    pi.finish_time
FROM ding_process_instances pi
LEFT JOIN ding_form_component_values fc1 ON pi.business_id = fc1.business_id AND fc1.component_name = '提案类别'
LEFT JOIN ding_form_component_values fc2 ON pi.business_id = fc2.business_id AND fc2.component_name = '申报日期'
LEFT JOIN ding_form_component_values fc3 ON pi.business_id = fc3.business_id AND fc3.component_name = '是否继续推进'
LEFT JOIN ding_form_component_values fc4 ON pi.business_id = fc4.business_id AND fc4.component_name = '提案分值'
LEFT JOIN ding_form_component_values fc5 ON pi.business_id = fc5.business_id AND fc5.component_name = '本提案属于哪个岗位'
LEFT JOIN ding_form_component_values fc6 ON pi.business_id = fc6.business_id AND fc6.component_name = '实际完成日期';

-- 13. 创建存储过程 - 支持数据同步和统计计算
-- 13.1 数据同步存储过程
DELIMITER $$
CREATE PROCEDURE `sync_ding_process_data`(
    IN p_json_data JSON,
    OUT p_result INT,
    OUT p_message VARCHAR(500)
)
BEGIN
    DECLARE v_business_id VARCHAR(100);
    DECLARE v_count INT DEFAULT 0;

    -- 获取business_id
    SET v_business_id = JSON_UNQUOTE(JSON_EXTRACT(p_json_data, '$.businessId'));

    -- 检查是否已存在
    SELECT COUNT(*) INTO v_count FROM ding_process_instances WHERE business_id = v_business_id;

    IF v_count > 0 THEN
        -- 更新现有记录
        UPDATE ding_process_instances SET
            title = JSON_UNQUOTE(JSON_EXTRACT(p_json_data, '$.title')),
            status = JSON_UNQUOTE(JSON_EXTRACT(p_json_data, '$.status')),
            result = JSON_UNQUOTE(JSON_EXTRACT(p_json_data, '$.result')),
            finish_time = CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(p_json_data, '$.status')) = 'COMPLETED'
                              THEN NOW() ELSE finish_time END,
            form_data = p_json_data,
            raw_data = p_json_data,
            version = version + 1
        WHERE business_id = v_business_id;

        SET p_result = 1;
        SET p_message = CONCAT('更新成功: ', v_business_id);
    ELSE
        -- 插入新记录
        INSERT INTO ding_process_instances (
            business_id, title, originator_user_id, originator_dept_id,
            originator_dept_name, status, result, create_time,
            form_data, raw_data
        ) VALUES (
            v_business_id,
            JSON_UNQUOTE(JSON_EXTRACT(p_json_data, '$.title')),
            JSON_UNQUOTE(JSON_EXTRACT(p_json_data, '$.originatorUserId')),
            JSON_UNQUOTE(JSON_EXTRACT(p_json_data, '$.originatorDeptId')),
            JSON_UNQUOTE(JSON_EXTRACT(p_json_data, '$.originatorDeptName')),
            JSON_UNQUOTE(JSON_EXTRACT(p_json_data, '$.status')),
            JSON_UNQUOTE(JSON_EXTRACT(p_json_data, '$.result')),
            STR_TO_DATE(JSON_UNQUOTE(JSON_EXTRACT(p_json_data, '$.createTime')), '%Y-%m-%dT%H:%i:%sZ'),
            p_json_data,
            p_json_data
        );

        SET p_result = 2;
        SET p_message = CONCAT('插入成功: ', v_business_id);
    END IF;

    -- 同步表单组件数据
    CALL sync_form_components(v_business_id, p_json_data);

END$$
DELIMITER ;

-- 13.2 同步表单组件数据
DELIMITER $$
CREATE PROCEDURE `sync_form_components`(
    IN p_business_id VARCHAR(100),
    IN p_json_data JSON
)
BEGIN
    DECLARE v_component_count INT DEFAULT 0;
    DECLARE i INT DEFAULT 0;
    DECLARE v_component JSON;
    DECLARE v_component_id VARCHAR(100);
    DECLARE v_component_name VARCHAR(200);
    DECLARE v_component_type VARCHAR(50);
    DECLARE v_value TEXT;
    DECLARE v_ext_value TEXT;

    -- 获取组件数量
    SET v_component_count = JSON_LENGTH(p_json_data, '$.formComponentValues');

    -- 删除现有组件数据
    DELETE FROM ding_form_component_values WHERE business_id = p_business_id;

    -- 循环插入组件数据
    WHILE i < v_component_count DO
        SET v_component = JSON_EXTRACT(p_json_data, CONCAT('$.formComponentValues[', i, ']'));
        SET v_component_id = JSON_UNQUOTE(JSON_EXTRACT(v_component, '$.id'));
        SET v_component_name = JSON_UNQUOTE(JSON_EXTRACT(v_component, '$.name'));
        SET v_component_type = JSON_UNQUOTE(JSON_EXTRACT(v_component, '$.componentType'));
        SET v_value = JSON_UNQUOTE(IFNULL(JSON_EXTRACT(v_component, '$.value'), 'NULL'));
        SET v_ext_value = JSON_UNQUOTE(IFNULL(JSON_EXTRACT(v_component, '$.extValue'), 'NULL'));

        -- 插入组件数据
        INSERT INTO ding_form_component_values (
            business_id, component_id, component_name, component_type,
            value_text, value_json, ext_value, biz_alias
        ) VALUES (
            p_business_id, v_component_id, v_component_name, v_component_type,
            v_value, v_component, v_ext_value, JSON_UNQUOTE(IFNULL(JSON_EXTRACT(v_component, '$.bizAlias'), ''))
        );

        SET i = i + 1;
    END WHILE;

END$$
DELIMITER ;

-- 14. 创建常用统计函数
-- 14.1 计算提案通过率
DELIMITER $$
CREATE FUNCTION `calc_proposal_pass_rate`(p_start_date DATE, p_end_date DATE)
RETURNS DECIMAL(5,2)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE v_total INT DEFAULT 0;
    DECLARE v_passed INT DEFAULT 0;

    SELECT COUNT(*) INTO v_total
    FROM ding_process_instances
    WHERE create_time BETWEEN p_start_date AND p_end_date;

    SELECT COUNT(*) INTO v_passed
    FROM ding_process_instances
    WHERE create_time BETWEEN p_start_date AND p_end_date
    AND result = 'agree';

    RETURN CASE WHEN v_total > 0 THEN (v_passed * 100.0 / v_total) ELSE 0 END;
END$$
DELIMITER ;

-- 15. 数据同步触发器 - 自动更新统计缓存
DELIMITER $$
CREATE TRIGGER `tr_update_stat_cache`
AFTER INSERT ON `ding_process_instances`
FOR EACH ROW
BEGIN
    -- 更新当日统计缓存
    INSERT INTO stat_result_cache (stat_key, stat_type, dimension1, stat_value, stat_count, calc_date, data_date)
    VALUES (
        CONCAT('daily_proposal_count_', DATE(NEW.create_time)),
        'daily_count',
        NEW.originator_dept_name,
        1,
        1,
        CURDATE(),
        DATE(NEW.create_time)
    )
    ON DUPLICATE KEY UPDATE
        stat_value = stat_value + 1,
        stat_count = stat_count + 1,
        update_time = CURRENT_TIMESTAMP;
END$$
DELIMITER ;

SET FOREIGN_KEY_CHECKS = 1;

-- ========================================================================
-- 索引优化建议
-- ========================================================================

-- 针对统计查询的复合索引
ALTER TABLE `ding_process_instances`
ADD INDEX `idx_status_create_time` (`status`, `create_time`),
ADD INDEX `idx_dept_status_time` (`originator_dept_name`, `status`, `create_time`);

ALTER TABLE `ding_form_component_values`
ADD INDEX `idx_name_type_value` (`component_name`, `component_type`, `value_text`(50)),
ADD INDEX `idx_business_name` (`business_id`, `component_name`);

-- ========================================================================
-- 性能监控视图
-- ========================================================================

CREATE VIEW `v_performance_metrics` AS
SELECT
    TABLE_NAME,
    TABLE_ROWS,
    DATA_LENGTH,
    INDEX_LENGTH,
    (DATA_LENGTH + INDEX_LENGTH) as TOTAL_SIZE
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME LIKE 'ding_%' OR TABLE_NAME LIKE 'stat_%';

-- ========================================================================
-- 使用示例SQL
-- ========================================================================

/*
-- 1. 按部门统计提案数量
SELECT
    originator_dept_name,
    COUNT(*) as proposal_count,
    calc_proposal_pass_rate(DATE_SUB(CURDATE(), INTERVAL 30 DAY), CURDATE()) as pass_rate
FROM ding_process_instances
WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
GROUP BY originator_dept_name;

-- 2. 统计各提案类别的分布
SELECT
    fc.value_text as category,
    COUNT(*) as count
FROM ding_process_instances pi
JOIN ding_form_component_values fc ON pi.business_id = fc.business_id
WHERE fc.component_name = '提案类别'
GROUP BY fc.value_text;

-- 3. 计算平均处理时长
SELECT
    AVG(DATEDIFF(finish_time, create_time)) as avg_days,
    MIN(DATEDIFF(finish_time, create_time)) as min_days,
    MAX(DATEDIFF(finish_time, create_time)) as max_days
FROM ding_process_instances
WHERE status = 'COMPLETED'
AND finish_time IS NOT NULL;

-- 4. 查看处理超时的任务
SELECT
    pi.business_id,
    pi.title,
    pi.originator_user_name,
    t.task_id,
    t.status,
    DATEDIFF(NOW(), t.create_time) as pending_days
FROM ding_process_instances pi
JOIN ding_tasks t ON pi.business_id = t.business_id
WHERE t.status = 'RUNNING'
AND DATEDIFF(NOW(), t.create_time) > 30;
*/