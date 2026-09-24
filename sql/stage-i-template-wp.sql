-- ============================================================
-- 阶段 I：WordPress 主题包转换 + 模板派生（D22）
-- 前置：website-template.sql / stage-d 已执行（900160 模板管理存在）
-- 执行：mysql -u<user> -p --default-character-set=utf8mb4 flycms < stage-i-template-wp.sql
--
-- 新增能力：
--   1. POST /api/system/skin/importWp        上传 WP 主题 zip → 自动转成本站皮肤
--   2. POST /api/system/template/derive      模板派生（list.html → list-news.html）
--   3. GET  /api/system/template/deriveTargets  可派生的层级槽位清单
--   4. GET  /api/system/skin/convertReport   读取最近一次转换报告（TODO 清单）
--
-- 号段：900172~900175（挂在 900160 模板管理下）
-- ============================================================

-- 1. 权限节点
INSERT INTO fly_admin_permission (id, actionKey, controller, menuType, menuName, path, component, icon, sort, visible, parentId) VALUES
(900172, '/api/system/skin/importWp',           'apiTemplateController', 'F', 'WP主题转换', NULL, NULL, NULL, 10, 0, 900160),
(900173, '/api/system/skin/convertReport',      'apiTemplateController', 'F', '转换报告',   NULL, NULL, NULL, 11, 0, 900160),
(900174, '/api/system/template/derive',         'apiTemplateController', 'F', '模板派生',   NULL, NULL, NULL, 12, 0, 900160),
(900175, '/api/system/template/deriveTargets',  'apiTemplateController', 'F', '派生槽位',   NULL, NULL, NULL, 13, 0, 900160);

-- 2. 赋给超管组（272835742965968896）
INSERT INTO fly_admin_group_permission_merge (group_id, permission_id) VALUES
(272835742965968896, 900172),
(272835742965968896, 900173),
(272835742965968896, 900174),
(272835742965968896, 900175);

-- ============================================================
-- 校验：应返回 4
-- SELECT count(*) FROM fly_admin_permission WHERE id BETWEEN 900172 AND 900175;
-- ============================================================
