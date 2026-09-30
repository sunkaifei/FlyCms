-- =====================================================================
-- 2026-09-30 角色管理规范化：fly_admin_group 主键重排为从 1 开始 + 菜单更名
--
-- 背景：角色组历史 id 为雪花值（超级管理员=272835742965968896 等），
--       可读性差且后端 PermissionService 的 permission_sync 硬编码了超管组 id。
-- 方案：按原 id 升序重排为 1~4（超级管理员=1、CEO=2、CTO=3、小编=4），
--       两张关联表（权限绑定/管理员归属）同步映射；AUTO_INCREMENT 推进到 5；
--       菜单「角色组管理」更名「角色管理」。
-- 代码：PermissionService 的硬编码同步改为 1L（本次一并修改，需重启后端）。
--
-- 幂等：重排语句按确定值映射，重复执行无副作用（找不到旧 id 时 0 行受影响）。
-- =====================================================================

-- 1) 角色表重排（新 id 1~4 与旧雪花 id 无重叠，升序更新无主键冲突）
UPDATE `fly_admin_group` SET `id` = 1 WHERE `id` = 272835742965968896;
UPDATE `fly_admin_group` SET `id` = 2 WHERE `id` = 272836180612231168;
UPDATE `fly_admin_group` SET `id` = 3 WHERE `id` = 272836256080343040;
UPDATE `fly_admin_group` SET `id` = 4 WHERE `id` = 272836335608541184;
ALTER TABLE `fly_admin_group` AUTO_INCREMENT = 5;

-- 2) 角色权限绑定表同步
UPDATE `fly_admin_group_permission_merge`
SET `group_id` = CASE `group_id`
    WHEN 272835742965968896 THEN 1
    WHEN 272836180612231168 THEN 2
    WHEN 272836256080343040 THEN 3
    WHEN 272836335608541184 THEN 4
    ELSE `group_id` END
WHERE `group_id` IN (272835742965968896, 272836180612231168, 272836256080343040, 272836335608541184);

-- 3) 管理员归属表同步
UPDATE `fly_admin_group_merge`
SET `group_id` = CASE `group_id`
    WHEN 272835742965968896 THEN 1
    WHEN 272836180612231168 THEN 2
    WHEN 272836256080343040 THEN 3
    WHEN 272836335608541184 THEN 4
    ELSE `group_id` END
WHERE `group_id` IN (272835742965968896, 272836180612231168, 272836256080343040, 272836335608541184);

-- 4) 菜单更名：角色组管理 → 角色管理
UPDATE `fly_admin_permission` SET `menuName` = '角色管理' WHERE `id` = 272842095398760448;
