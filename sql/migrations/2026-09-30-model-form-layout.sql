-- =====================================================================
-- 2026-09-30 发布表单布局：fly_model 增加 form_tabs / form_default_tab
--
-- 用途：「发布页面布局设计」页（/system/model/layout/:modelId）持久化：
--   form_tabs         页签顺序（JSON 数组，如 ["基础信息","扩展信息"]）；
--                     未登记的字段页签在渲染时按出现顺序追加在后（向后兼容）。
--   form_default_tab  打开发布表单时默认选中的页签名（空=第一个页签）。
--
-- 幂等：仅 ADD COLUMN IF NOT EXISTS 语义用存储过程兜底（MySQL 5.7 无 IF NOT EXISTS），
--       重复执行会报 Duplicate column，可忽略；新库直接用基线脚本。
-- =====================================================================

ALTER TABLE `fly_model`
  ADD COLUMN `form_tabs` text COMMENT '发布表单页签顺序(JSON数组,如 ["基础信息","扩展信息"])',
  ADD COLUMN `form_default_tab` varchar(64) DEFAULT NULL COMMENT '发布表单默认打开的页签名(空=第一个)';

-- 布局设计页路由（后端菜单模式：路由由 C 节点生成；visible=0 不进菜单，仅注册路由）
INSERT IGNORE INTO `fly_admin_permission`
(`id`, `actionKey`, `controller`, `remark`, `parentId`, `menuType`, `menuName`, `path`, `component`, `icon`, `sort`, `visible`) VALUES
(900369, '/api/system/model/layout', 'apiModelController', '发布页面布局设计页路由', 900110, 'C', '布局设计', '/system/model/layout/:modelId', '/system/model/layout', NULL, '6', 0);

INSERT IGNORE INTO `fly_admin_group_permission_merge` (`group_id`, `permission_id`)
SELECT 1, p.id
FROM `fly_admin_permission` p
WHERE p.id = 900369;
