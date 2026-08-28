-- =============================================================================
-- Proofly 开发环境种子数据
-- =============================================================================
-- ⚠️ 警告：本脚本**仅供本地开发**使用！包含默认管理员账号，凭据已公开。
--    生产部署请跳过本脚本，由运营通过后台自助注册或管理后台手动创建账号。
--
-- 使用方法：
--   1. 先执行 docs/mysql-schema.sql 完成建表
--   2. 再执行本脚本插入示例数据
--
-- 默认登录：
--   用户名：admin
--   密码：  admin123 （⚠️ 登录后立即修改）
-- =============================================================================

USE `proofly`;

-- 默认门店
INSERT INTO `store` (
  `id`,
  `name`,
  `contact_name`,
  `contact_phone`,
  `status`,
  `created_at`,
  `updated_at`,
  `deleted`
) VALUES (
  20001,
  '默认门店',
  '管理员',
  '00000000000',
  'active',
  CURRENT_TIMESTAMP(3),
  CURRENT_TIMESTAMP(3),
  0
) ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `status` = VALUES(`status`),
  `updated_at` = CURRENT_TIMESTAMP(3),
  `deleted` = 0;

-- 角色定义（owner / designer / admin）
INSERT INTO `role` (
  `id`,
  `store_id`,
  `code`,
  `name`,
  `description`,
  `created_at`,
  `updated_at`,
  `deleted`
) VALUES
  (21001, 20001, 'owner', '门店老板', '管理门店项目和员工', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0),
  (21002, 20001, 'designer', '设计师', '创建项目、上传版本、处理标注', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0),
  (21003, 20001, 'admin', '管理员', '平台或系统管理员', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `description` = VALUES(`description`),
  `updated_at` = CURRENT_TIMESTAMP(3),
  `deleted` = 0;

-- 默认管理员账号（密码：admin123，登录后请立即修改）
INSERT INTO `user` (
  `id`,
  `store_id`,
  `username`,
  `nickname`,
  `phone`,
  `email`,
  `password_hash`,
  `status`,
  `created_at`,
  `updated_at`,
  `deleted`
) VALUES (
  22001,
  20001,
  'admin',
  '系统管理员',
  '00000000000',
  'admin@proofly.local',
  '$2a$10$w0krIEufDvyKqB5MJinm4.enMJUpTdiP/jfXOKTRTXto0LwZvagJW',
  'active',
  CURRENT_TIMESTAMP(3),
  CURRENT_TIMESTAMP(3),
  0
) ON DUPLICATE KEY UPDATE
  `nickname` = VALUES(`nickname`),
  `phone` = VALUES(`phone`),
  `email` = VALUES(`email`),
  `password_hash` = VALUES(`password_hash`),
  `status` = VALUES(`status`),
  `updated_at` = CURRENT_TIMESTAMP(3),
  `deleted` = 0;

-- 用户角色绑定（admin 既是 owner 也是 admin）
INSERT INTO `user_role` (
  `id`,
  `store_id`,
  `user_id`,
  `role_id`,
  `created_at`
) VALUES
  (23001, 20001, 22001, 21001, CURRENT_TIMESTAMP(3)),
  (23002, 20001, 22001, 21003, CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE
  `store_id` = VALUES(`store_id`);