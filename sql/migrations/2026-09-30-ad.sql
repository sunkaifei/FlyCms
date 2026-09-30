-- =====================================================================
-- 2026-09-30 广告系统：fly_ad_position / fly_ad（对标帝国/Dede/PHPCMS 广告模块）
--
-- 设计（行业共识的两层结构）：
--   广告位 fly_ad_position：名称 + 标识(ad_key，模板 <@fly_ad key="..."> 调用用)
--   广告   fly_ad：所属广告位 + 类型(image图片/text文字/code代码) + 投放素材
--                  + 权重/排序 + 起止时间窗 + 启停 + 展示/点击计数
--   前台：<@fly_ad key="banner_top" rows="5">（无 body 输出即用 HTML，有 body 可
--         自定义循环，变量 adList/adPosition）；点击走 /ad/click/{id} 302 跳转计数。
--
-- 内容：
--   1) 建表 fly_ad_position / fly_ad
--   2) 菜单：900360「广告管理」C 节点 + 7 个 F 功能点（900361~900367）
--   3) 授权：全部授「超级管理员」组（1）
--
-- 幂等：CREATE TABLE IF NOT EXISTS / INSERT IGNORE，可重复执行。
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p flycms < 本文件
-- =====================================================================

-- 1) 广告位
CREATE TABLE IF NOT EXISTS `fly_ad_position` (
  `id` bigint(20) unsigned NOT NULL COMMENT '主键（雪花）',
  `name` varchar(64) NOT NULL COMMENT '广告位名称（如：首页轮播）',
  `ad_key` varchar(64) NOT NULL COMMENT '调用标识（模板 <@fly_ad key="...">）',
  `description` varchar(255) DEFAULT NULL COMMENT '描述/投放位置说明',
  `width` int(11) DEFAULT NULL COMMENT '建议宽度(px)',
  `height` int(11) DEFAULT NULL COMMENT '建议高度(px)',
  `sort` int(10) NOT NULL DEFAULT '0',
  `status` tinyint(2) NOT NULL DEFAULT '1' COMMENT '1=启用 0=停用（停用后前台不输出）',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ad_key` (`ad_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广告位';

-- 2) 广告
CREATE TABLE IF NOT EXISTS `fly_ad` (
  `id` bigint(20) unsigned NOT NULL COMMENT '主键（雪花）',
  `position_id` bigint(20) unsigned NOT NULL COMMENT '所属广告位',
  `name` varchar(128) NOT NULL COMMENT '广告名称',
  `ad_type` varchar(20) NOT NULL DEFAULT 'image' COMMENT '类型：image图片/text文字/code代码',
  `image_url` varchar(500) DEFAULT NULL COMMENT '图片地址（image 类型，直存 URL）',
  `url` varchar(500) DEFAULT NULL COMMENT '跳转链接（点击经 /ad/click/{id} 计数后 302）',
  `text_content` varchar(500) DEFAULT NULL COMMENT '文字内容（text 类型）',
  `html_code` text COMMENT '代码（code 类型，原样输出）',
  `weight` int(11) NOT NULL DEFAULT '1' COMMENT '权重（越大越靠前）',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间（空=立即）',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间（空=永久）',
  `status` tinyint(2) NOT NULL DEFAULT '1' COMMENT '1=启用 0=停用',
  `count_view` bigint(20) NOT NULL DEFAULT '0' COMMENT '展示次数',
  `count_click` bigint(20) NOT NULL DEFAULT '0' COMMENT '点击次数',
  `remark` varchar(255) DEFAULT NULL,
  `sort` int(10) NOT NULL DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ad_position` (`position_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广告';

-- 3) 菜单与功能点（/api/** 节点 permission_sync 不登记，必须手工插入）
INSERT IGNORE INTO `fly_admin_permission`
(`id`, `actionKey`, `controller`, `remark`, `parentId`, `menuType`, `menuName`, `path`, `component`, `icon`, `sort`, `visible`) VALUES
(900360, '/api/system/ad/position/list', 'apiAdController', '广告管理：菜单/广告位列表', 900110, 'C', '广告管理', '/system/ad', '/system/ad/index', 'lucide:megaphone', '30', 1),
(900361, '/api/system/ad/position/save',   'apiAdController', '广告管理：新增广告位', 900360, 'F', '新增广告位', NULL, NULL, NULL, '1', 1),
(900362, '/api/system/ad/position/update', 'apiAdController', '广告管理：编辑广告位', 900360, 'F', '编辑广告位', NULL, NULL, NULL, '2', 1),
(900363, '/api/system/ad/position/delete', 'apiAdController', '广告管理：删除广告位', 900360, 'F', '删除广告位', NULL, NULL, NULL, '3', 1),
(900364, '/api/system/ad/ad/list',         'apiAdController', '广告管理：广告列表',   900360, 'F', '广告列表',   NULL, NULL, NULL, '4', 1),
(900365, '/api/system/ad/ad/save',         'apiAdController', '广告管理：新增广告',   900360, 'F', '新增广告',   NULL, NULL, NULL, '5', 1),
(900366, '/api/system/ad/ad/update',       'apiAdController', '广告管理：编辑广告',   900360, 'F', '编辑广告',   NULL, NULL, NULL, '6', 1),
(900367, '/api/system/ad/ad/delete',       'apiAdController', '广告管理：删除广告',   900360, 'F', '删除广告',   NULL, NULL, NULL, '7', 1);

-- 4) 授权：全部授「超级管理员」组
INSERT IGNORE INTO `fly_admin_group_permission_merge` (`group_id`, `permission_id`)
SELECT 1, p.id
FROM `fly_admin_permission` p
WHERE p.id BETWEEN 900360 AND 900367;

-- 5) 种子：两个常用广告位（前台快速上手的示例位）
INSERT IGNORE INTO `fly_ad_position` (`id`, `name`, `ad_key`, `description`, `width`, `height`, `sort`, `status`, `create_time`) VALUES
(930000000000000001, '首页轮播', 'home_carousel', '首页顶部轮播图位（建议多张同尺寸图）', 1200, 300, 1, 1, NOW()),
(930000000000000002, '侧栏广告', 'sidebar',       '内容页/列表页侧栏广告位',             300, 250,  2, 1, NOW());
