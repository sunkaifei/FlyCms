-- 导入方式：mysql --default-character-set=utf8mb4 -uroot -p flycms < 本文件
-- ============================================================
-- stage-k：数据库备份功能权限（对接 vben 后台「数据库备份」页）
-- 说明：900xxx 权限段；菜单挂「系统管理」(900110) 下，sort 27 排最后。
-- 幂等：INSERT IGNORE + 已存在则跳过。
-- ============================================================

USE flycms;

-- ---------- 1. 权限节点：900290 菜单(C) + 900291~900293 功能(F) ----------
INSERT IGNORE INTO `fly_admin_permission`
  (`id`, `actionKey`, `controller`, `remark`, `parentId`, `menuType`, `menuName`, `path`, `component`, `icon`, `sort`, `visible`) VALUES
  (900290, '/api/system/tools/db/list',     'apiToolsController', '数据库备份：菜单/列表', 900110, 'C', '数据库备份', '/system/dbbackup', '/system/dbbackup/index', 'lucide:database-backup', 27, 1),
  (900291, '/api/system/tools/db/backup',   'apiToolsController', '数据库备份：执行备份',   900290, 'F', '执行备份',   NULL, NULL, NULL, 1, 1),
  (900292, '/api/system/tools/db/download', 'apiToolsController', '数据库备份：下载',       900290, 'F', '下载备份',   NULL, NULL, NULL, 2, 1),
  (900293, '/api/system/tools/db/delete',   'apiToolsController', '数据库备份：删除',       900290, 'F', '删除备份',   NULL, NULL, NULL, 3, 1);

-- ---------- 2. 授权超管组（id=272835742965968896） ----------
INSERT IGNORE INTO `fly_admin_group_permission_merge` (`group_id`, `permission_id`)
SELECT 272835742965968896, id FROM fly_admin_permission
WHERE id BETWEEN 900290 AND 900293;
