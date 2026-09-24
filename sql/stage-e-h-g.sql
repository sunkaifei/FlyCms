-- ============================================================
-- 阶段 E：碎片/推荐位系统 + 阶段 H：定时发布字段 + 阶段 G：SEO 配置键
-- 前置：menu-management.sql 已执行（900110 系统管理存在）
-- 执行：mysql -u<user> -p --default-character-set=utf8mb4 flycms < stage-e-h-g.sql
-- 注意：若对已有动态表重复执行 ALTER 会报 Duplicate column，忽略即可
-- ============================================================

-- 1. 碎片位
CREATE TABLE IF NOT EXISTS `fly_block` (
  `id` bigint(20) NOT NULL COMMENT '雪花ID',
  `block_key` varchar(50) NOT NULL COMMENT '调用键，模板 <@fly_block key="home_focus"/>',
  `block_name` varchar(60) NOT NULL,
  `block_type` tinyint(2) NOT NULL DEFAULT '0' COMMENT '0富文本 1图片 2推荐位列表 3模板碎片',
  `content` mediumtext COMMENT '富文本/模板内容(block_type=0/3)',
  `item_count` int(11) DEFAULT '10' COMMENT '推荐位展示条数',
  `cache_seconds` int(11) DEFAULT '0' COMMENT '渲染缓存秒数，0=不缓存',
  `status` tinyint(2) DEFAULT '1',
  `sort` int(11) DEFAULT '0',
  `create_time` datetime DEFAULT NULL, `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`block_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='碎片位';

CREATE TABLE IF NOT EXISTS `fly_block_item` (
  `id` bigint(20) NOT NULL,
  `block_id` bigint(20) NOT NULL,
  `title` varchar(200) DEFAULT '',
  `image` varchar(500) DEFAULT '',
  `url` varchar(500) DEFAULT '',
  `summary` varchar(500) DEFAULT '',
  `start_time` datetime DEFAULT NULL COMMENT '定时上线',
  `end_time` datetime DEFAULT NULL COMMENT '定时下线',
  `status` tinyint(2) DEFAULT '1',
  `sort` int(11) DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_block` (`block_id`,`status`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='碎片条目（推荐位/广告位条目）';

-- 2. 碎片管理菜单/权限
INSERT INTO fly_admin_permission (id, actionKey, controller, menuType, menuName, path, component, icon, sort, visible, parentId) VALUES
(900200, '/api/system/block/list',   'apiBlockController', 'C', '碎片管理', '/system/block', '/system/block/index', 'lucide:layout-dashboard', 11, 1, 900110),
(900201, '/api/system/block/save',   'apiBlockController', 'F', '保存碎片位', NULL, NULL, NULL, 1, 0, 900200),
(900202, '/api/system/block/delete', 'apiBlockController', 'F', '删除碎片位', NULL, NULL, NULL, 2, 0, 900200),
(900203, '/api/system/blockItem/save',   'apiBlockController', 'F', '保存碎片条目', NULL, NULL, NULL, 3, 0, 900200),
(900204, '/api/system/blockItem/delete', 'apiBlockController', 'F', '删除碎片条目', NULL, NULL, NULL, 4, 0, 900200);

INSERT INTO fly_admin_group_permission_merge (group_id, permission_id) VALUES
(272835742965968896, 900200),
(272835742965968896, 900201),
(272835742965968896, 900202),
(272835742965968896, 900203),
(272835742965968896, 900204);

-- 3. 阶段 H：动态模型表 + 旧文章表补定时发布字段（status=4 待定时发布）
ALTER TABLE `fly_cmodel_900000000000000001` ADD COLUMN `publish_time` datetime DEFAULT NULL COMMENT '定时发布时间(status=4)';
ALTER TABLE `fly_cmodel_900000000000000002` ADD COLUMN `publish_time` datetime DEFAULT NULL COMMENT '定时发布时间(status=4)';
ALTER TABLE `fly_cmodel_900000000000000003` ADD COLUMN `publish_time` datetime DEFAULT NULL COMMENT '定时发布时间(status=4)';
ALTER TABLE `fly_cmodel_1299270553286488064` ADD COLUMN `publish_time` datetime DEFAULT NULL COMMENT '定时发布时间(status=4)';
ALTER TABLE `fly_article` ADD COLUMN `publish_time` datetime DEFAULT NULL COMMENT '定时发布时间(status=4)';

-- 4. 阶段 G：sitemap/robots 配置键
INSERT INTO fly_config_web (id, typebase, keycode, keyvalue, description, sort) VALUES
(900300, 0, 'fly_sitemap_status', '1', 'sitemap开关：1开启', 0),
(900301, 0, 'fly_sitemap_limit', '100', 'sitemap每个模型收录条数', 0),
(900302, 0, 'fly_robots', 'User-agent: *\nAllow: /\nDisallow: /api/\nDisallow: /system/', 'robots.txt 内容', 0);
