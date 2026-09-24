-- 导入方式：mysql --default-character-set=utf8mb4 -uroot -p flycms < 本文件
-- ============================================================
-- FlyCms 主题市场菜单与权限补录（规划 P5）
-- 说明：stage-j-theme-engine.sql 原打算占用 900180~900187，
-- 但 900180~900182 与「站内短信」权限 id 冲突（INSERT IGNORE 跳过），
-- 导致 /api/system/theme/list、/api/system/theme/check、/api/system/theme/enable
-- 三个节点未落库；copyParent 复用 createChild(900184) 已存在。
-- 本文件补齐这三个节点，并把主题市场做成"菜单项"（menuType=C），
-- 挂在「模板管理」(900160) 之下，组件 views/system/theme/index.vue。
-- 幂等：INSERT IGNORE + 已存在则跳过。
-- ============================================================

USE flycms;

-- ---------- 1. 主题市场：菜单项 + 两个功能点 ----------
INSERT IGNORE INTO `fly_admin_permission`
  (`id`, `actionKey`, `controller`, `remark`, `parentId`, `menuType`, `menuName`, `path`, `component`, `icon`, `sort`, `visible`) VALUES
  (900188, '/api/system/theme/list',     'apiTemplateController', '主题市场：菜单/列表', 900160, 'C', '主题市场', 'system/theme', '/system/theme/index', 'lucide:palette', 14, 1),
  (900189, '/api/system/theme/check',    'apiTemplateController', '主题市场：兼容性预检', 900188, 'F', '兼容性预检', NULL, NULL, NULL, 1, 1),
  (900193, '/api/system/theme/enable',   'apiTemplateController', '主题市场：启用主题',   900188, 'F', '启用主题',   NULL, NULL, NULL, 2, 1);

-- ---------- 2. 授权给超级管理员组（其余角色按需分配） ----------
-- 超级管理员组 id：272835742965968896（若实际部署时组 id 不同，请替换为对应值）
INSERT IGNORE INTO `fly_admin_group_permission_merge` (`group_id`, `permission_id`)
SELECT 272835742965968896, id FROM `fly_admin_permission`
WHERE id IN (900188, 900189, 900193);
