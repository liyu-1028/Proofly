-- Proofly local authentication seed data
-- Default login:
--   account: admin
--   password: Proofly@123
--
-- Change this password after first login in any non-local environment.

USE `proofly`;

INSERT INTO `store` (
  `id`,
  `name`,
  `contact_name`,
  `contact_phone`,
  `status`,
  `deployment_mode`,
  `created_at`,
  `updated_at`,
  `deleted`
) VALUES (
  20001,
  '默认门店',
  '管理员',
  '13800000000',
  'active',
  'single-store',
  CURRENT_TIMESTAMP(3),
  CURRENT_TIMESTAMP(3),
  0
) ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `status` = VALUES(`status`),
  `deployment_mode` = VALUES(`deployment_mode`),
  `updated_at` = CURRENT_TIMESTAMP(3),
  `deleted` = 0;

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
  '13800000000',
  'admin@proofly.local',
  '$2y$10$7BwVGcyIudUBpNDL/k.wkO8n5OFl0F/X1/JxNQyXnY8.uSvpw0PaW',
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
