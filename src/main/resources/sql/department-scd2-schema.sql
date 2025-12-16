-- 部门维度表SCD2结构
-- 用于实现部门全量同步的SCD2（Slowly Changing Dimension Type 2）模式

USE dingding_lean;

-- 创建部门维度表（SCD2，保存部门历史版本）
CREATE TABLE `dim_department_jy` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'SCD2 替代键（主键）',

    `dept_id` BIGINT NOT NULL COMMENT '钉钉部门ID（业务键）',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父部门ID',
    `name` VARCHAR(255) NOT NULL COMMENT '部门名称',

    -- SCD2 字段
    `valid_from` DATE NOT NULL COMMENT '版本生效日期',
    `valid_to` DATE NOT NULL COMMENT '版本失效日期',
    `is_current` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否当前版本（1=是，0=否）',

    -- 审计字段
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),

    -- 同一部门只能有一个当前版本
    UNIQUE KEY `uk_dept_current` (`dept_id`, `is_current`),

    -- 索引优化
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_valid_range` (`valid_from`, `valid_to`),
    KEY `idx_is_current` (`is_current`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_general_ci
  COMMENT='部门维度表（SCD2，保存部门历史版本）';

-- 初始化9999-12-31的特殊日期处理（可选，用于默认值）
-- 实际应用中会在插入时动态设置