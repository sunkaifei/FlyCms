-- ============================================================
-- stage-l：定时任务管理权限（vben 后台「定时任务」页）
-- 说明：900xxx 权限段；菜单挂「系统管理」(900110) 下，sort 28。
-- 幂等：INSERT IGNORE + 已存在则跳过。
-- 导入：mysql --default-character-set=utf8mb4 -uroot -p flycms < 本文件
-- ============================================================

USE flycms;

-- ---------- 0. 修复历史 bug：job_id 为 int 存不下雪花 id，执行日志必然写入失败 ----------
ALTER TABLE `fly_job_log` MODIFY COLUMN `job_id` BIGINT(20) NULL DEFAULT NULL;
-- error_msg 为 varchar(255)，而 ScheduleJob 写入的错误栈截取 2000 字符，同样必然溢出
ALTER TABLE `fly_job_log` MODIFY COLUMN `error_msg` TEXT NULL;

-- ---------- 1. 权限节点：900300 菜单(C) + 900301~900306 功能(F) ----------
INSERT IGNORE INTO `fly_admin_permission`
  (`id`, `actionKey`, `controller`, `remark`, `parentId`, `menuType`, `menuName`, `path`, `component`, `icon`, `sort`, `visible`) VALUES
  (900300, '/api/system/job/list',    'apiJobController', '定时任务：菜单/列表', 900110, 'C', '定时任务', '/system/job', '/system/job/index', 'lucide:timer', 28, 1),
  (900301, '/api/system/job/save',    'apiJobController', '定时任务：新增',       900300, 'F', '新增任务', NULL, NULL, NULL, 1, 1),
  (900302, '/api/system/job/update',  'apiJobController', '定时任务：修改',       900300, 'F', '修改任务', NULL, NULL, NULL, 2, 1),
  (900303, '/api/system/job/status',  'apiJobController', '定时任务：启停',       900300, 'F', '启用暂停', NULL, NULL, NULL, 3, 1),
  (900304, '/api/system/job/run',     'apiJobController', '定时任务：立即执行',   900300, 'F', '立即执行', NULL, NULL, NULL, 4, 1),
  (900305, '/api/system/job/delete',  'apiJobController', '定时任务：删除',       900300, 'F', '删除任务', NULL, NULL, NULL, 5, 1),
  (900306, '/api/system/job/logList', 'apiJobController', '定时任务：执行日志',   900300, 'F', '执行日志', NULL, NULL, NULL, 6, 1);

-- ---------- 2. 授权超管组（id=272835742965968896） ----------
INSERT IGNORE INTO `fly_admin_group_permission_merge` (`group_id`, `permission_id`)
SELECT 272835742965968896, id FROM fly_admin_permission
WHERE id BETWEEN 900300 AND 900306;
