-- =====================================================================
-- 2026-09-28 权限体系复查补丁：/system/admin/group_del 漏授权
--
-- 背景：节点存在（272843576441057280）但从未授权给超级管理员组，
--       导致「删除角色组」接口对所有管理员（含 flycms）一律 403。
--       同时修复 ApiSystemController 三处 id==1L 超管保护失效
--       （实际超管组/账号均为雪花 id），改为「id=1 或组名=超级管理员」。
--
-- 幂等：INSERT IGNORE。
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p flycms < 本文件
-- =====================================================================
INSERT IGNORE INTO `fly_admin_group_permission_merge` (`group_id`, `permission_id`)
SELECT g.id, p.id
FROM `fly_admin_group` g
CROSS JOIN `fly_admin_permission` p
WHERE g.`name` = '超级管理员' AND p.`actionKey` = '/system/admin/group_del';

-- 2) 清理存量脏数据：
--    a) 组已不存在的孤儿授权（如 1272835742965968896，疑似拼接错误）
--    b) 指向已删除权限节点的悬空授权（如 900111/900112，U2 删节点时漏清）
DELETE gp FROM `fly_admin_group_permission_merge` gp
LEFT JOIN `fly_admin_group` g ON g.id = gp.group_id
WHERE g.id IS NULL;
DELETE gp FROM `fly_admin_group_permission_merge` gp
LEFT JOIN `fly_admin_permission` p ON p.id = gp.permission_id
WHERE p.id IS NULL;

