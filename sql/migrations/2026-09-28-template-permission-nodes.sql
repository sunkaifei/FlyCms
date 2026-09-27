-- =====================================================================
-- 2026-09-28 模板引擎补齐批次：新增权限节点
-- 适用：P10 区域编排 V2（/api/system/area/*）+ P3-3 区块图案（/api/system/pattern/*）
--
-- 背景：ApiBaseController.requirePermission 用 actionKey 查 fly_admin_permission，
--       新接口不注册权限节点 → 任何角色（含超级管理员组）调用都 403。
--       前端按钮显隐也依赖同一套 actionKey（useAccess().hasAccessByCodes）。
--
-- 幂等：使用 INSERT IGNORE（id 为主键），可重复执行。
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p flycms < 本文件
--       注意中文必须带 --default-character-set=utf8mb4，否则真实乱码。
-- =====================================================================

-- 1) 权限节点：布局管理（C 菜单）+ 保存（F）+ 图案库（F）
INSERT IGNORE INTO `fly_admin_permission`
(`id`, `actionKey`, `controller`, `remark`, `parentId`, `menuType`, `menuName`, `path`, `component`, `icon`, `sort`, `visible`) VALUES
('900195', '/api/system/area/list',    'apiTemplateController', '布局管理：菜单/列表', '900110', 'C', '布局管理', '/system/area', '/system/area/index', 'lucide:layout',    '29', 'true'),
('900196', '/api/system/area/save',    'apiTemplateController', '布局管理：保存区块',   '900195', 'F', '保存区块', NULL, NULL, NULL, '1', 'true'),
('900197', '/api/system/pattern/list', 'apiTemplateController', '区块图案：列表',       '900160', 'F', '图案库',   NULL, NULL, NULL, '14', 'true'),
('900198', '/api/system/pattern/read', 'apiTemplateController', '区块图案：读取',       '900160', 'F', '读取图案', NULL, NULL, NULL, '15', 'true');

-- 2) 授权：与快照既有约定一致——新节点默认只授「超级管理员」组，
--    其他角色组由管理员在「角色组管理」里按需勾选。
INSERT IGNORE INTO `fly_admin_group_permission_merge` (`group_id`, `permission_id`) VALUES
('272835742965968896', '900195'),
('272835742965968896', '900196'),
('272835742965968896', '900197'),
('272835742965968896', '900198');
