-- =====================================================================
-- 2026-10-01 模型分组（① 分组）：一个业务模块（如商城/问答）由多张表配合，
--           fly_model_group 提供组织层——分组统一菜单入口与模型管理聚合；
--           表间关系仍由 relate 字段绑定承载（存储层不变：一模型一表）。
--
-- 内容：
--   1) fly_model_group 分组表（name/code/icon/sort/status）
--   2) fly_model.group_id 归属列（NULL=未分组，模型管理页平铺原样）
--
-- 幂等：CREATE TABLE IF NOT EXISTS；ADD COLUMN 重复执行报 Duplicate column 可忽略。
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p flycms < 本文件
-- =====================================================================

CREATE TABLE IF NOT EXISTS `fly_model_group` (
  `id` bigint(20) unsigned NOT NULL COMMENT '雪花ID',
  `name` varchar(64) NOT NULL COMMENT '分组名（如：商城）',
  `code` varchar(32) NOT NULL COMMENT '分组标识（小写字母/数字），唯一',
  `icon` varchar(64) DEFAULT NULL COMMENT '图标（lucide）',
  `description` varchar(255) DEFAULT NULL,
  `sort` int(10) NOT NULL DEFAULT '0',
  `status` tinyint(2) NOT NULL DEFAULT '1' COMMENT '0停用 1启用（停用仅隐藏聚合菜单，不动模型）',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型分组（业务模块组织层）';

ALTER TABLE `fly_model`
  ADD COLUMN `group_id` bigint(20) unsigned DEFAULT NULL COMMENT '所属分组（NULL=未分组）' AFTER `icon`;
