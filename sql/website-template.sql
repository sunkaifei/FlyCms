-- ============================================================
-- 网站管理 + 前台模板管理（在线编辑）菜单与权限
-- 依据：参考帝国CMS/DedeCMS 的"模板管理 + 站点配置"后台范式
-- 前置：menu-management.sql 已执行（900110 系统管理目录存在）
-- 执行：mysql -u<user> -p --default-character-set=utf8mb4 flycms < website-template.sql
-- ============================================================

-- 网站管理（站点信息/SEO/主题切换）
INSERT INTO fly_admin_permission (id, actionKey, controller, menuType, menuName, path, component, icon, sort, visible, parentId) VALUES
(900150, '/api/system/website/config', 'apiWebsiteController', 'C', '网站管理', '/system/website', '/system/website/index', 'lucide:globe', 6, 1, 900110);
INSERT INTO fly_admin_permission (id, actionKey, controller, menuType, menuName, path, component, icon, sort, visible, parentId) VALUES
(900151, '/api/system/website/save', 'apiWebsiteController', 'F', '保存站点设置', NULL, NULL, NULL, 1, 0, 900150);

-- 前台模板管理（文件树 + 在线编辑）
INSERT INTO fly_admin_permission (id, actionKey, controller, menuType, menuName, path, component, icon, sort, visible, parentId) VALUES
(900160, '/api/system/template/files', 'apiWebsiteController', 'C', '模板管理', '/system/template', '/system/template/index', 'lucide:file-code', 7, 1, 900110);
INSERT INTO fly_admin_permission (id, actionKey, controller, menuType, menuName, path, component, icon, sort, visible, parentId) VALUES
(900161, '/api/system/template/save',   'apiWebsiteController', 'F', '保存模板', NULL, NULL, NULL, 1, 0, 900160),
(900162, '/api/system/template/create', 'apiWebsiteController', 'F', '新建模板', NULL, NULL, NULL, 2, 0, 900160),
(900163, '/api/system/template/delete', 'apiWebsiteController', 'F', '删除模板', NULL, NULL, NULL, 3, 0, 900160);

-- 授权到超级管理员组（按实际组 ID 调整）
INSERT INTO fly_admin_group_permission_merge (group_id, permission_id) VALUES
(272835742965968896, 900150),
(272835742965968896, 900151),
(272835742965968896, 900160),
(272835742965968896, 900161),
(272835742965968896, 900162),
(272835742965968896, 900163);
