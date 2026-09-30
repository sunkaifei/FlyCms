-- =====================================================================
-- 2026-09-30 权限孤儿节点治理：补名挂靠 + 去重
--
-- 背景：B1 字段绑定（options/users|categories）、自动化规则（automation/*）、
--       AI 助手（ai/*）的接口权限行以 parentId=0 + 无名（menuName NULL）落库，
--       在菜单管理里显示为「- 按钮 /api/...」，像无用数据（实际都在用，删了就 403）。
--       其中 /api/system/automation/list 与有名 C 节点 2760100000000005001 重复。
--
-- 内容：
--   1) 删除重复孤儿（automation/list 孤儿行及其组绑定；正式 C 节点保留）
--   2) 其余 6 个孤儿补 menuName + 挂到「系统管理」（自动化保存/删除挂到自动化规则 C 节点下）
--
-- 幂等：UPDATE 按确定 id；DELETE 带 id 条件，重复执行无副作用。
-- =====================================================================

-- 1) 删除重复孤儿（组绑定一并清理）
DELETE FROM `fly_admin_group_permission_merge` WHERE `permission_id` = 2760100000000004001;
DELETE FROM `fly_admin_permission` WHERE `id` = 2760100000000004001;

-- 2) 补名挂靠
UPDATE `fly_admin_permission` SET `menuName` = '选项接口：用户列表', `parentId` = 900110 WHERE `id` = 2760100000000001001;
UPDATE `fly_admin_permission` SET `menuName` = '选项接口：分类列表', `parentId` = 900110 WHERE `id` = 2760100000000001002;
UPDATE `fly_admin_permission` SET `menuName` = '自动化规则：保存', `parentId` = 2760100000000005001 WHERE `id` = 2760100000000004002;
UPDATE `fly_admin_permission` SET `menuName` = '自动化规则：删除', `parentId` = 2760100000000005001 WHERE `id` = 2760100000000004003;
UPDATE `fly_admin_permission` SET `menuName` = 'AI 助手：生成', `parentId` = 900110 WHERE `id` = 2760100000000006001;
UPDATE `fly_admin_permission` SET `menuName` = 'AI 助手：状态', `parentId` = 900110 WHERE `id` = 2760100000000006002;
