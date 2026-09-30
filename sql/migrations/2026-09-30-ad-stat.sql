-- =====================================================================
-- 2026-09-30 广告统计：fly_ad_stat_daily 按日明细（每个广告的展现/点击都落库）
--
-- 设计：fly_ad 上的 count_view/count_click 是累计总数（快显示）；
--       本表按 (广告, 日期) 粒度 upsert 累加，支撑点击率与按天报表。
--       每次「展现」（标签渲染 / JS 分发）与每次「点击」（/ad/click/{id}）都写。
--
-- 内容：
--   1) 建表 fly_ad_stat_daily（UNIQUE(ad_id, stat_date) 支撑 ON DUPLICATE KEY 累加）
--   2) 菜单：900368「广告统计」F 节点（/api/system/ad/stat/list）+ 超管组授权
--
-- 幂等：CREATE TABLE IF NOT EXISTS / INSERT IGNORE，可重复执行。
-- =====================================================================

CREATE TABLE IF NOT EXISTS `fly_ad_stat_daily` (
  `id` bigint(20) unsigned NOT NULL COMMENT '主键（雪花）',
  `ad_id` bigint(20) unsigned NOT NULL COMMENT '广告 id',
  `position_id` bigint(20) unsigned NOT NULL COMMENT '所属广告位',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `views` bigint(20) NOT NULL DEFAULT '0' COMMENT '当日展现次数',
  `clicks` bigint(20) NOT NULL DEFAULT '0' COMMENT '当日点击次数',
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ad_date` (`ad_id`, `stat_date`),
  KEY `idx_stat_position` (`position_id`, `stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广告按日统计';

INSERT IGNORE INTO `fly_admin_permission`
(`id`, `actionKey`, `controller`, `remark`, `parentId`, `menuType`, `menuName`, `path`, `component`, `icon`, `sort`, `visible`) VALUES
(900368, '/api/system/ad/stat/list', 'apiAdController', '广告管理：统计报表', 900360, 'F', '广告统计', NULL, NULL, NULL, '8', 1);

INSERT IGNORE INTO `fly_admin_group_permission_merge` (`group_id`, `permission_id`)
SELECT 1, p.id
FROM `fly_admin_permission` p
WHERE p.id = 900368;
