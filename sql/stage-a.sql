-- ============================================================
-- 阶段 A：安全加固与操作审计（规划 §8 阶段 A）
-- 执行：mysql -u<user> -p --default-character-set=utf8mb4 flycms < stage-a.sql
-- ============================================================

-- 1. 管理操作审计表
CREATE TABLE IF NOT EXISTS `fly_admin_log` (
  `id` bigint(20) NOT NULL COMMENT '雪花ID',
  `admin_id` bigint(20) NOT NULL,
  `admin_name` varchar(50) DEFAULT '',
  `method` varchar(10) DEFAULT '',
  `path` varchar(200) DEFAULT '',
  `query` varchar(500) DEFAULT '' COMMENT '参数摘要，截断500',
  `ip` varchar(50) DEFAULT '',
  `status` int(11) DEFAULT NULL COMMENT '响应码',
  `cost_ms` int(11) DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_admin_time` (`admin_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理操作审计';

-- 2. 菜单/权限（挂在系统管理 900110 下；sync 不扫 /api 需手工插行）
INSERT INTO fly_admin_permission (id, actionKey, controller, menuType, menuName, path, component, icon, sort, visible, parentId) VALUES
(900170, '/api/system/log/list', 'apiLogController', 'C', '审计日志', '/system/log', '/system/log/index', 'lucide:scroll-text', 8, 1, 900110);

INSERT INTO fly_admin_group_permission_merge (group_id, permission_id) VALUES
(272835742965968896, 900170);
