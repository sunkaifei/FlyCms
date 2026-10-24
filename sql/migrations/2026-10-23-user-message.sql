-- ============================================================
-- 1) 用户站内信表 fly_user_message
--
-- 背景：审核页一直弹「已驳回，并已站内信通知作者」，而全库 grep
--      站内信/UserMessage/user_message/MessageService 只有一处注释提及 —— 
--      无表、无实现、无接口。上一轮只做到「不再假承诺」，本脚本把
--      能力补齐，让那句话第一次成为真的。
--
-- 设计要点：
--   * 定向投递：一行 = 一个接收者一条消息（广播由 Service 展开成 N 行），
--     不做「user_id=0 表示全体」的特例，避免已读状态无法按人区分；
--   * 审核通知回填 target_model/target_id，前台可直接跳回被驳回的内容；
--   * 索引 (user_id, is_read, id) 覆盖「我的消息 + 未读数」两条热查询。
--
-- 2) 内容正文服务端清洗开关 fly_content_sanitize（key 缺失 = 开启）
--
-- 3) 新接口的权限节点 + C 菜单（不注册则含超管在内全部 403）
--
-- 执行：
--   E:/mysql57/mysql-5.7.44-winx64/bin/mysql.exe --default-character-set=utf8mb4 -uroot -p flycms < sql/migrations/2026-10-23-user-message.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS `fly_user_message` (
  `id`           BIGINT       NOT NULL COMMENT '雪花ID',
  `user_id`      BIGINT       NOT NULL COMMENT '接收用户 id',
  `msg_type`     TINYINT      NOT NULL DEFAULT 1 COMMENT '1=系统通知 2=审核通知 3=互动提醒',
  `title`        VARCHAR(250) NOT NULL COMMENT '消息标题',
  `content`      VARCHAR(2000)     NULL COMMENT '消息正文（纯文本，前台转义输出）',
  `target_model` VARCHAR(64)      NULL COMMENT '关联模型 code（审核通知回填，可空）',
  `target_id`    BIGINT           NULL COMMENT '关联内容 id（审核通知回填，可空）',
  `is_read`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
  `sender_id`    BIGINT           NULL COMMENT '发送管理员 id；0/NULL=系统',
  `sender_name`  VARCHAR(64)      NULL COMMENT '发送者快照（管理员删号后仍可追溯）',
  `create_time`  DATETIME     NOT NULL COMMENT '发送时间',
  `read_time`    DATETIME         NULL COMMENT '阅读时间',
  PRIMARY KEY (`id`),
  KEY `idx_msg_user` (`user_id`, `is_read`, `id`),
  KEY `idx_msg_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户站内信';

-- 正文清洗开关：1=写入前用 jsoup 白名单清洗富文本（默认），0=关闭
INSERT INTO `fly_config_web` (`typebase`, `keycode`, `keyvalue`, `description`, `sort`)
SELECT 0, 'fly_content_sanitize', '1', '内容正文入库前XSS清洗（1开启 0关闭）', 90
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `fly_config_web` WHERE `keycode` = 'fly_content_sanitize');

-- 站内信：C 菜单 + 两个 F 按钮
INSERT IGNORE INTO `fly_admin_permission`
(`id`, `actionKey`, `controller`, `remark`, `parentId`, `menuType`, `menuName`, `path`, `component`, `icon`, `sort`, `visible`) VALUES
('900310', '/api/system/message/page',  'apiUserMessageController', '站内信：分页列表',     '900110', 'C', '站内信',   '/system/message', '/system/message/index', 'lucide:send',      '26', 'true'),
('900311', '/api/system/message/send',  'apiUserMessageController', '站内信：发送',         '900310', 'F', '发送站内信', NULL, NULL, NULL, '1', 'false'),
-- 注意：900312 已被 /api/system/website/testEmail 占用，删除节点顺延到 900315
('900315', '/api/system/message/delete','apiUserMessageController', '站内信：删除',         '900310', 'F', '删除站内信', NULL, NULL, NULL, '2', 'false'),
('900313', '/api/system/message/read',  'apiUserMessageController', '站内信：标记已读',     '900310', 'F', '标记已读',   NULL, NULL, NULL, '3', 'false'),
('900314', '/api/system/tools/sanitize/backfill', 'apiUserMessageController', '存量正文XSS清洗（按模型分页）', '900110', 'F', '存量正文清洗', NULL, NULL, NULL, '90', 'false');

-- 授权超级管理员组。
-- 注意：超管组是 fly_admin_group 里 id=1 的「超级管理员」（见 fly_admin_group_merge：
-- admin → group_id=1，该组持有 241 个权限节点）。旧笔记里的 272835742965968896 是
-- 一个没有任何管理员的幻影组，授权到它等于没授权 —— 含超管在内全部 403。
INSERT IGNORE INTO `fly_admin_group_permission_merge` (`group_id`, `permission_id`) VALUES
('1', '900310'),
('1', '900311'),
('1', '900313'),
('1', '900315'),
('1', '900314');

-- 核对
SELECT TABLE_NAME, TABLE_COMMENT
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fly_user_message';

SELECT p.id, p.actionKey, p.menuType, p.menuName,
       (SELECT COUNT(*) FROM fly_admin_group_permission_merge gp WHERE gp.permission_id = p.id) AS groups
FROM fly_admin_permission p
WHERE p.actionKey LIKE '/api/system/message%' OR p.actionKey LIKE '%sanitize/backfill'
ORDER BY p.id;

SELECT keycode, keyvalue FROM fly_config_web WHERE keycode = 'fly_content_sanitize';
