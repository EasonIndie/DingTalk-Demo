-- 钉钉精益数据统计数据库表结构
-- MySQL 8.0.18

-- 创建数据库
CREATE DATABASE IF NOT EXISTS dingding_lean DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE dingding_lean;

-- 1. 流程实例主表
CREATE TABLE ding_process_instances (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    process_instance_id VARCHAR(100) UNIQUE NOT NULL COMMENT '流程实例ID',
    process_code VARCHAR(100) NOT NULL COMMENT '流程模板ID',
    title VARCHAR(500) NOT NULL COMMENT '审批单标题',
    business_id VARCHAR(100) COMMENT '审批单编号',
    originator_userid VARCHAR(50) NOT NULL COMMENT '发起人用户ID',
    originator_dept_id VARCHAR(50) COMMENT '发起人部门ID',
    originator_dept_name VARCHAR(200) COMMENT '发起人部门名称',
    status VARCHAR(20) NOT NULL COMMENT '状态(RUNNING/TERMINATED/COMPLETED/CANCELED)',
    result VARCHAR(20) COMMENT '审批结果(agree/refuse)',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    finish_time DATETIME COMMENT '完成时间',
    attached_process_instance_ids JSON COMMENT '附属单信息',
    cc_userids JSON COMMENT '抄送用户ID列表',
    biz_action VARCHAR(50) COMMENT '业务动作',
    biz_data JSON COMMENT '业务数据',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_process_code (process_code),
    INDEX idx_originator (originator_userid),
    INDEX idx_originator_dept (originator_dept_name),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time),
    INDEX idx_business_id (business_id)
) COMMENT='流程实例主表';

-- 2. 表单组件值表
CREATE TABLE ding_form_component_values (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    process_instance_id VARCHAR(100) NOT NULL COMMENT '流程实例ID',
    component_id VARCHAR(100) NOT NULL COMMENT '组件唯一ID',
    component_name VARCHAR(200) NOT NULL COMMENT '组件显示名称',
    component_type VARCHAR(50) NOT NULL COMMENT '组件类型(TextField/DDSelectField/DDDateField等)',
    value TEXT COMMENT '组件值',
    ext_value JSON COMMENT '扩展信息(JSON格式)',
    biz_alias VARCHAR(100) COMMENT '业务别名',
    is_key_field TINYINT DEFAULT 0 COMMENT '是否关键统计字段',
    field_category VARCHAR(50) COMMENT '字段分类(基础信息/项目详情/完成情况等)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_process_component (process_instance_id, component_id),
    INDEX idx_process_instance (process_instance_id),
    INDEX idx_component_type (component_type),
    INDEX idx_component_name (component_name),
    INDEX idx_field_category (field_category),
    INDEX idx_is_key_field (is_key_field)
) COMMENT='表单组件值表';

-- 3. 操作记录表
CREATE TABLE ding_operation_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    process_instance_id VARCHAR(100) NOT NULL COMMENT '流程实例ID',
    activity_id VARCHAR(100) COMMENT '活动ID',
    operation_date DATETIME NOT NULL COMMENT '操作时间',
    user_id VARCHAR(50) NOT NULL COMMENT '操作人ID',
    show_name VARCHAR(200) COMMENT '操作人显示名称',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    result VARCHAR(20) COMMENT '操作结果(AGREE/REFUSE/NONE/REDIRECTED)',
    remark TEXT COMMENT '评论内容',
    images JSON COMMENT '操作图片',
    task_id BIGINT COMMENT '任务ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_process_instance (process_instance_id),
    INDEX idx_operation_date (operation_date),
    INDEX idx_user_id (user_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_result (result),
    INDEX idx_task_id (task_id)
) COMMENT='操作记录表';

-- 4. 任务表
CREATE TABLE ding_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    process_instance_id VARCHAR(100) NOT NULL COMMENT '流程实例ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    activity_id VARCHAR(100) NOT NULL COMMENT '节点ID',
    user_id VARCHAR(50) NOT NULL COMMENT '任务处理人',
    status VARCHAR(20) NOT NULL COMMENT '任务状态(RUNNING/TERMINATED/COMPLETED/CANCELED)',
    result VARCHAR(20) COMMENT '任务结果(AGREE/REFUSE/REDIRECTED/NONE)',
    create_timestamp BIGINT NOT NULL COMMENT '任务创建时间戳',
    create_time DATETIME COMMENT '任务创建时间',
    finish_timestamp BIGINT COMMENT '任务结束时间戳',
    finish_time DATETIME COMMENT '任务结束时间',
    mobile_url VARCHAR(500) COMMENT '移动端URL',
    pc_url VARCHAR(500) COMMENT 'PC端URL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_process_instance (process_instance_id),
    INDEX idx_task_id (task_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_activity_id (activity_id),
    INDEX idx_create_time (create_time)
) COMMENT='任务表';

-- 5. 表单模板配置表
CREATE TABLE ding_form_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    process_code VARCHAR(100) UNIQUE NOT NULL COMMENT '流程模板ID',
    template_name VARCHAR(200) NOT NULL COMMENT '模板名称',
    description TEXT COMMENT '模板描述',
    form_config JSON COMMENT '表单配置(JSON格式)',
    is_active TINYINT DEFAULT 1 COMMENT '是否启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_process_code (process_code),
    INDEX idx_is_active (is_active)
) COMMENT='表单模板配置表';

-- 6. 数据同步记录表
CREATE TABLE ding_sync_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    sync_type VARCHAR(50) NOT NULL COMMENT '同步类型(INSTANCE_IDS/PROCESS_DETAILS)',
    sync_start_time DATETIME NOT NULL COMMENT '同步开始时间',
    sync_end_time DATETIME COMMENT '同步结束时间',
    sync_status VARCHAR(20) NOT NULL COMMENT '同步状态(SUCCESS/FAILED/PARTIAL)',
    total_count INT DEFAULT 0 COMMENT '处理总数',
    success_count INT DEFAULT 0 COMMENT '成功数量',
    failed_count INT DEFAULT 0 COMMENT '失败数量',
    error_message TEXT COMMENT '错误信息',
    extra_data JSON COMMENT '额外数据',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_sync_type (sync_type),
    INDEX idx_sync_status (sync_status),
    INDEX idx_sync_start_time (sync_start_time)
) COMMENT='数据同步记录表';

-- 添加外键约束
ALTER TABLE ding_form_component_values
ADD CONSTRAINT fk_fcv_process_instance
FOREIGN KEY (process_instance_id) REFERENCES ding_process_instances(process_instance_id) ON DELETE CASCADE;

ALTER TABLE ding_operation_records
ADD CONSTRAINT fk_or_process_instance
FOREIGN KEY (process_instance_id) REFERENCES ding_process_instances(process_instance_id) ON DELETE CASCADE;

ALTER TABLE ding_tasks
ADD CONSTRAINT fk_tasks_process_instance
FOREIGN KEY (process_instance_id) REFERENCES ding_process_instances(process_instance_id) ON DELETE CASCADE;

-- 初始化表单模板数据
INSERT INTO ding_form_templates (process_code, template_name, description) VALUES
('PROC-FD7C69D9-67AA-4C09-8DB1-3D1A40FC8679', '合理化建议【提给别的部门，提这个，无积分】', '跨部门合理化建议表单'),
('PROC-7C4DF150-6853-49B3-B5E3-742E37CBFCC1', '信息中心专用【精益改善提案】', '信息中心内部精益改善提案表单');

-- 创建统计视图
CREATE VIEW v_process_summary AS
SELECT
    dpi.process_instance_id,
    dpi.title,
    dpi.originator_dept_name,
    dpi.status,
    dpi.result,
    DATE_FORMAT(dpi.create_time, '%Y-%m') as month,
    TIMESTAMPDIFF(DAY, dpi.create_time, COALESCE(dpi.finish_time, NOW())) as process_days,
    fcv_economic.value as economic_benefit,
    fcv_level.value as improvement_level,
    fcv_category.value as proposal_category
FROM ding_process_instances dpi
LEFT JOIN ding_form_component_values fcv_economic ON
    dpi.process_instance_id = fcv_economic.process_instance_id
    AND fcv_economic.component_name = '预计产生经济效益(元)'
LEFT JOIN ding_form_component_values fcv_level ON
    dpi.process_instance_id = fcv_level.process_instance_id
    AND fcv_level.component_name = '改善等级'
LEFT JOIN ding_form_component_values fcv_category ON
    dpi.process_instance_id = fcv_category.process_instance_id
    AND fcv_category.component_name = '提案类别';