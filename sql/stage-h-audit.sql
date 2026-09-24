-- ============================================================
-- 阶段 H 补齐：投稿审核开关 + 后台待审列表
-- 前置：menu-management.sql 已执行（900110 系统管理存在）
-- 执行：mysql -u<user> -p --default-character-set=utf8mb4 flycms < stage-h-audit.sql
--
-- 开关语义：fly_article_audit = 0 直接发布 / 1 先审后发（投稿落 status=0）
-- 兼容：老键 user_article_verify 语义相反（1=直接发布），后端读取时做了反向映射，
--       未配置新键时行为与老版本完全一致，不会出现「升级后投稿全部转待审」。
-- 号段：900280 投稿审核
-- ============================================================

-- 1. 投稿审核开关
INSERT INTO `fly_config_web` (`id`, `typebase`, `keycode`, `keyvalue`, `description`, `sort`)
VALUES (900304, 0, 'fly_article_audit', '0', '投稿审核开关：0直接发布 1先审后发', 000)
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`);

-- 2. 待审列表索引（后台按 status + create_time 翻页）
ALTER TABLE `fly_article` ADD INDEX `idx_status_time` (`status`, `create_time`);

-- 3. 菜单/权限
INSERT INTO fly_admin_permission (id, actionKey, controller, menuType, menuName, path, component, icon, sort, visible, parentId) VALUES
(900280, '/api/system/audit/page',         'apiAuditController', 'C', '投稿审核', '/system/audit', '/system/audit/index', 'lucide:file-check', 26, 1, 900110),
(900281, '/api/system/audit/pendingCount', 'apiAuditController', 'F', '待审数量', NULL, NULL, NULL, 1, 0, 900280),
(900282, '/api/system/audit/audit',        'apiAuditController', 'F', '审核单条', NULL, NULL, NULL, 2, 0, 900280),
(900283, '/api/system/audit/batch',        'apiAuditController', 'F', '批量审核', NULL, NULL, NULL, 3, 0, 900280),
(900284, '/api/system/audit/switch',       'apiAuditController', 'F', '审核开关设置', NULL, NULL, NULL, 4, 0, 900280);

-- 4. 赋给超管组
INSERT INTO fly_admin_group_permission_merge (group_id, permission_id) VALUES
(272835742965968896, 900280), (272835742965968896, 900281), (272835742965968896, 900282),
(272835742965968896, 900283), (272835742965968896, 900284);
