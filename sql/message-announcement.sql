-- ============================================================
-- 站内短信管理 + 公告管理（参照 DedeCMS mynews / 帝国CMS 站内短信+公告）
-- 前置：menu-management.sql 已执行（900110 系统管理存在）
-- 执行：mysql -u<user> -p --default-character-set=utf8mb4 flycms < message-announcement.sql
-- ============================================================

-- 1. 公告表（帝国公告模式：起止时间窗 + 排序 + 模板标签调用）
CREATE TABLE IF NOT EXISTS `fly_announcement` (
  `id` bigint(20) NOT NULL COMMENT '雪花ID',
  `title` varchar(200) NOT NULL COMMENT '公告标题',
  `content` text COMMENT '公告内容',
  `link_url` varchar(300) DEFAULT '' COMMENT '点击跳转链接，空=仅展示',
  `start_time` datetime DEFAULT NULL COMMENT '上线时间，空=立即',
  `end_time` datetime DEFAULT NULL COMMENT '下线时间，空=长期',
  `status` tinyint(2) DEFAULT '1' COMMENT '0隐藏 1显示',
  `sort` int(10) DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_status_sort` (`status`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网站公告';

-- 2. 菜单/权限（sync 不扫 /api 需手工插行）
INSERT INTO fly_admin_permission (id, actionKey, controller, menuType, menuName, path, component, icon, sort, visible, parentId) VALUES
(900180, '/api/system/message/list',      'apiMessageController', 'C', '站内短信', '/system/message', '/system/message/index', 'lucide:message-square', 9, 1, 900110),
(900181, '/api/system/message/send',      'apiMessageController', 'F', '发送站内信', NULL, NULL, NULL, 1, 0, 900180),
(900182, '/api/system/message/delete',    'apiMessageController', 'F', '删除站内信', NULL, NULL, NULL, 2, 0, 900180),
(900190, '/api/system/announcement/list',   'apiAnnouncementController', 'C', '公告管理', '/system/announcement', '/system/announcement/index', 'lucide:megaphone', 10, 1, 900110),
(900191, '/api/system/announcement/save',   'apiAnnouncementController', 'F', '保存公告', NULL, NULL, NULL, 1, 0, 900190),
(900192, '/api/system/announcement/delete', 'apiAnnouncementController', 'F', '删除公告', NULL, NULL, NULL, 2, 0, 900190);

-- 3. 授权到超级管理员组（按实际组 ID 调整）
INSERT INTO fly_admin_group_permission_merge (group_id, permission_id) VALUES
(272835742965968896, 900180),
(272835742965968896, 900181),
(272835742965968896, 900182),
(272835742965968896, 900190),
(272835742965968896, 900191),
(272835742965968896, 900192);
