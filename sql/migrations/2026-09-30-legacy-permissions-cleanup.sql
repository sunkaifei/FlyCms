-- =====================================================================
-- 2026-09-30 旧后台接口权限节点治理：900130 容器整体退役
--
-- 背景：900130「旧后台接口」下 101 个 F 节点是老 FreeMarker 后台的接口登记，
--       旧控制器目录已随 vben 迁移删除，这些 actionKey 指向的端点全部 404。
--       但其中 12 个键被现行 /api/system/** 接口（管理员/角色/菜单权限体系）
--       用作 requirePermission 鉴权码——必须保留。
--
-- 内容：
--   1) 12 个活键：改挂「系统管理」(900110) 并补语义名（鉴权码 actionKey 不变，
--      组绑定不变，相关页面权限不受影响）；
--   2) 其余死键（含空 actionKey 行）连同组绑定整体删除；
--   3) 删除 900130 容器节点本身。
--
-- 附注：PermissionService.permission_sync 扫描 /system/** 老路由注册——老控制器
--       已删除，同步不会再产生 900130 子节点，不会复活。
-- 幂等：UPDATE 按确定 actionKey；DELETE 带 parentId/id 条件，重复执行无副作用。
-- =====================================================================

-- 1) 12 个活键改挂系统管理并补名
UPDATE `fly_admin_permission` SET `parentId` = 900110, `menuName` = '管理员：列表' WHERE `parentId` = 900130 AND `actionKey` = '/system/admin/admin_list';
UPDATE `fly_admin_permission` SET `parentId` = 900110, `menuName` = '管理员：保存' WHERE `parentId` = 900130 AND `actionKey` = '/system/admin/admin_save';
UPDATE `fly_admin_permission` SET `parentId` = 900110, `menuName` = '管理员：编辑' WHERE `parentId` = 900130 AND `actionKey` = '/system/admin/admin_act';
UPDATE `fly_admin_permission` SET `parentId` = 900110, `menuName` = '管理员：删除' WHERE `parentId` = 900130 AND `actionKey` = '/system/admin/delAdmin';
UPDATE `fly_admin_permission` SET `parentId` = 900110, `menuName` = '角色：列表' WHERE `parentId` = 900130 AND `actionKey` = '/system/admin/group_list';
UPDATE `fly_admin_permission` SET `parentId` = 900110, `menuName` = '角色：删除' WHERE `parentId` = 900130 AND `actionKey` = '/system/admin/group_del';
UPDATE `fly_admin_permission` SET `parentId` = 900110, `menuName` = '角色：已勾权限' WHERE `parentId` = 900130 AND `actionKey` = '/system/admin/group_assignPermissions/*';
UPDATE `fly_admin_permission` SET `parentId` = 900110, `menuName` = '角色：保存勾选' WHERE `parentId` = 900130 AND `actionKey` = '/system/admin/group_markpermissions';
UPDATE `fly_admin_permission` SET `parentId` = 900110, `menuName` = '权限：列表' WHERE `parentId` = 900130 AND `actionKey` = '/system/admin/permission_list';
UPDATE `fly_admin_permission` SET `parentId` = 900110, `menuName` = '权限：同步' WHERE `parentId` = 900130 AND `actionKey` = '/system/admin/permission_sync';
UPDATE `fly_admin_permission` SET `parentId` = 900110, `menuName` = '权限：删除' WHERE `parentId` = 900130 AND `actionKey` = '/system/admin/permission_del';
UPDATE `fly_admin_permission` SET `parentId` = 900110, `menuName` = '权限：更新' WHERE `parentId` = 900130 AND `actionKey` = '/system/admin/permission_update_save';

-- 2) 删除其余死键（此时 900130 子节点只剩死键与空键）
DELETE FROM `fly_admin_group_permission_merge`
WHERE `permission_id` IN (SELECT `id` FROM `fly_admin_permission` WHERE `parentId` = 900130);
DELETE FROM `fly_admin_permission` WHERE `parentId` = 900130;

-- 3) 删除 900130 容器
DELETE FROM `fly_admin_group_permission_merge` WHERE `permission_id` = 900130;
DELETE FROM `fly_admin_permission` WHERE `id` = 900130;
