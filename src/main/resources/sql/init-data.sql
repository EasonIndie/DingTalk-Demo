-- 初始化数据脚本

-- 插入测试用户数据（可选）
-- INSERT INTO ding_users (user_id, name, department) VALUES
-- ('2001051333950200', '测试用户', '信息中心-开发部');

-- 插入测试表单模板数据（已在schema.sql中创建）
-- INSERT INTO ding_form_templates (process_code, template_name, description) VALUES
-- ('PROC-FD7C69D9-67AA-4C09-8DB1-3D1A40FC8679', '合理化建议【提给别的部门，提这个，无积分】', '跨部门合理化建议表单'),
-- ('PROC-7C4DF150-6853-49B3-B5E3-742E37CBFCC1', '信息中心专用【精益改善提案】', '信息中心内部精益改善提案表单');

-- 创建测试同步记录
INSERT INTO ding_sync_records (sync_type, sync_start_time, sync_end_time, sync_status, total_count, success_count, failed_count) VALUES
('INIT', NOW(), NOW(), 'SUCCESS', 0, 0, 0);