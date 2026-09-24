-- ============================================================
-- 阶段 C：统一栏目体系（规划 §8 阶段 C）
-- 执行：mysql -u<user> -p --default-character-set=utf8mb4 flycms < stage-c.sql
--
-- 设计要点：栏目是「树 + URL 归属层」，model_id 只是默认数据源而非约束。
-- 0=列表（绑模型） 1=单页 2=外链 3=聚合（跨模型混排）
-- ============================================================

CREATE TABLE IF NOT EXISTS `fly_channel` (
  `id` bigint(20) NOT NULL COMMENT '雪花ID',
  `father_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '父栏目，0=根',
  `channel_name` varchar(50) NOT NULL DEFAULT '' COMMENT '栏目名称',
  `channel_dir` varchar(50) NOT NULL DEFAULT '' COMMENT 'URL目录名，全站唯一',
  `model_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '绑定模型，0=不绑定',
  `channel_type` tinyint(2) NOT NULL DEFAULT '0' COMMENT '0列表 1单页 2外链 3聚合',
  `page_content` text COMMENT '单页内容（富文本）',
  `out_url` varchar(300) DEFAULT '' COMMENT '外链地址',
  `list_template` varchar(100) DEFAULT '' COMMENT '自定义列表模板',
  `detail_template` varchar(100) DEFAULT '' COMMENT '自定义详情模板',
  `seo_title` varchar(300) DEFAULT '' COMMENT 'SEO标题，空则取栏目名+站名',
  `seo_keywords` varchar(500) DEFAULT '' COMMENT 'SEO关键词，空则取全站配置',
  `seo_description` varchar(1000) DEFAULT '' COMMENT 'SEO描述，空则取全站配置',
  `page_size` int(11) NOT NULL DEFAULT '20' COMMENT '每页条数',
  `status` tinyint(2) NOT NULL DEFAULT '1' COMMENT '0隐藏 1显示',
  `sort` int(11) NOT NULL DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_father_sort` (`father_id`,`sort`),
  KEY `idx_dir` (`channel_dir`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一栏目树';

-- 菜单/权限（挂在系统管理 900110 下）
-- 说明：栏目删除语义为「只删栏目行」，内容数据不受影响，因此未加任何级联外键。
-- 注意：900180~900182 已被 message-announcement.sql（站内短信）占用，
--       本阶段改用 900210 号段，避免菜单行因主键冲突静默丢失。
INSERT INTO fly_admin_permission (id, actionKey, controller, menuType, menuName, path, component, icon, sort, visible, parentId) VALUES
(900210, '/api/system/channel/tree', 'apiChannelController', 'C', '栏目管理', '/system/channel', '/system/channel/index', 'lucide:folder-tree', 3, 1, 900110),
(900211, '/api/system/channel/save', 'apiChannelController', 'F', '新增/修改栏目', '', '', '', 0, 0, 900210),
(900212, '/api/system/channel/delete', 'apiChannelController', 'F', '删除栏目', '', '', '', 0, 0, 900210),
(900213, '/api/system/channel/status', 'apiChannelController', 'F', '栏目显隐', '', '', '', 0, 0, 900210),
(900214, '/api/system/channel/move', 'apiChannelController', 'F', '栏目排序', '', '', '', 0, 0, 900210),
(900215, '/api/system/channel/moveTo', 'apiChannelController', 'F', '移动栏目', '', '', '', 0, 0, 900210);

INSERT INTO fly_admin_group_permission_merge (group_id, permission_id) VALUES
(272835742965968896, 900210),
(272835742965968896, 900211),
(272835742965968896, 900212),
(272835742965968896, 900213),
(272835742965968896, 900214),
(272835742965968896, 900215);
