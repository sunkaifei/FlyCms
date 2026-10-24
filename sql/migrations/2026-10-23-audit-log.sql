-- ============================================================
-- 1) 内容审核日志表：让「驳回原因」真正落库
--
-- 背景（调研发现的功能性缺陷，报告见
-- docs/03-报告/2026-10-23-后台表单与审核链路调研与整改方案.md）：
--   前端 audit/index.vue 一直把 reason 传给 /api/system/audit/audit|batch，
--   但两个接口的形参列表里都没有 reason，Spring 只按形参绑定 → 静默丢弃；
--   ModelDataService.updateStatus 只改状态 + 写版本快照（remark 硬编码
--   "状态切换→n"），既不留原因也不通知任何人。前端却弹「已驳回，并已
--   站内信通知作者」——UI 承诺了不存在的能力。
--
-- 本表承载审核动作；版本快照 remark 同步由代码改为
-- 「审核驳回：xxx / 审核通过」，两处互为佐证。
--
-- 2) 新接口的权限节点（不注册则含超管在内全部 403）
--
-- 执行：
--   E:/mysql57/mysql-5.7.44-winx64/bin/mysql.exe --default-character-set=utf8mb4 -uroot flycms < sql/migrations/2026-10-23-audit-log.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS `fly_content_audit_log` (
  `id`            BIGINT       NOT NULL COMMENT '雪花ID',
  `target_model`  VARCHAR(64)  NOT NULL COMMENT '目标模型 code（fly_cmodel_{code}）',
  `target_id`     BIGINT       NOT NULL COMMENT '内容 id',
  `title`         VARCHAR(250)     NULL COMMENT '审核时的标题快照（内容删除后仍可追溯）',
  `action`        TINYINT      NOT NULL DEFAULT 1 COMMENT '1=通过 2=驳回',
  `reason`        VARCHAR(500)     NULL COMMENT '驳回原因（通过时为操作备注，可空）',
  `auditor_id`    BIGINT           NULL COMMENT '审核管理员 id',
  `auditor_name`  VARCHAR(64)      NULL COMMENT '审核管理员账号快照',
  `create_time`   DATETIME     NOT NULL COMMENT '审核时间',
  PRIMARY KEY (`id`),
  KEY `idx_audit_target` (`target_model`, `target_id`, `id`),
  KEY `idx_audit_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容审核日志';

-- 父节点 900280（投稿审核 C 菜单）下补三个 F 按钮节点
INSERT IGNORE INTO `fly_admin_permission`
(`id`, `actionKey`, `controller`, `remark`, `parentId`, `menuType`, `menuName`, `path`, `component`, `icon`, `sort`, `visible`) VALUES
('900285', '/api/system/audit/detail',  'apiAuditController', '审核：查看内容正文',   '900280', 'F', '查看内容', NULL, NULL, NULL, '5', 'false'),
('900286', '/api/system/audit/logs',    'apiAuditController', '审核：审核历史',       '900280', 'F', '审核历史', NULL, NULL, NULL, '6', 'false'),
('900287', '/api/system/audit/pageAll', 'apiAuditController', '审核：跨模型聚合列表', '900280', 'F', '跨模型列表', NULL, NULL, NULL, '7', 'false');

-- 授权超级管理员组。
-- 注意：超管组是 fly_admin_group 里 id=1 的「超级管理员」（fly_admin_group_merge 里
-- admin → group_id=1）。旧笔记里的 272835742965968896 是个没有任何管理员的幻影组，
-- 授权到它等于没授权 —— 含超管在内全部 403。两个 id 都写上，避免换库时再踩。
INSERT IGNORE INTO `fly_admin_group_permission_merge` (`group_id`, `permission_id`) VALUES
('1', '900285'),
('1', '900286'),
('1', '900287');

-- 核对
SELECT TABLE_NAME, TABLE_COMMENT
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fly_content_audit_log';

SELECT p.id, p.actionKey, p.menuName,
       (SELECT COUNT(*) FROM fly_admin_group_permission_merge gp WHERE gp.permission_id = p.id) AS groups
FROM fly_admin_permission p
WHERE p.actionKey LIKE '/api/system/audit%'
ORDER BY p.id;
