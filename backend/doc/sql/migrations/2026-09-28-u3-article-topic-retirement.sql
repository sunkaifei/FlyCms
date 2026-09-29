-- ============================================================
-- U3 旧模块退役 + E4 平台评论（2026-09-28）
-- 对照《主流CMS对标与全项目优化开发方案.md》§9.4 阶段 U3 / U4(E4)
-- 前置：fly_article 0 行、fly_article_comment 0 行、fly_topic 18 行已迁
--       fly_cmodel_topics（topics 模型 id=9）、fly_share_comment 0 行。
-- 复用既有配置键：fly_comment_audit（评论先审后显）、fly_article_audit（内容先审后发）。
-- ============================================================

-- ---------- E4 平台评论（多态：引用任意模型内容） ----------
CREATE TABLE IF NOT EXISTS `fly_comment` (
  `id` bigint(20) NOT NULL COMMENT '雪花ID',
  `target_model` varchar(32) NOT NULL COMMENT '目标模型code（articles/topics/…）',
  `target_id` bigint(20) NOT NULL COMMENT '目标内容id',
  `user_id` bigint(20) unsigned NOT NULL COMMENT '评论人',
  `parent_id` bigint(20) DEFAULT '0' COMMENT '父评论id（0=顶层）',
  `content` varchar(2000) NOT NULL COMMENT '评论内容',
  `status` tinyint(2) DEFAULT '0' COMMENT '0待审 1通过 2未通过',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_model`,`target_id`,`status`),
  KEY `idx_user` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台评论（多态，按 target_model+target_id 引用任意模型内容）';

-- ---------- 表单布局补齐（模型表单布局：排序/选项卡/隐藏） ----------
-- 字段级：是否在内容表单中显示（0=表单隐藏，仅存储/列表/详情可见）
-- MySQL 5.7 不支持 ADD COLUMN IF NOT EXISTS，重复执行会报 1060 可跳过。
ALTER TABLE `fly_model_field` ADD COLUMN `is_form` tinyint(1) DEFAULT '1' COMMENT '表单显示 0隐藏 1显示';
-- 模型级：基础选项卡开关（链接/导航/留言类模型可关闭富正文与 SEO 页签）
ALTER TABLE `fly_model` ADD COLUMN `use_content` tinyint(1) DEFAULT '1' COMMENT '详细内容选项卡 0关 1开';
ALTER TABLE `fly_model` ADD COLUMN `use_seo` tinyint(1) DEFAULT '1' COMMENT 'SEO设置选项卡 0关 1开';

-- ---------- U3 旧表更名留档（MySQL 5.7 无 IF EXISTS，逐条执行；已更名报 1017 可跳过） ----------
RENAME TABLE `fly_article` TO `fly_retired_article_20260928`;
RENAME TABLE `fly_article_category` TO `fly_retired_article_category_20260928`;
RENAME TABLE `fly_article_category_merge` TO `fly_retired_article_category_merge_20260928`;
RENAME TABLE `fly_article_comment` TO `fly_retired_article_comment_20260928`;
RENAME TABLE `fly_article_count` TO `fly_retired_article_count_20260928`;
RENAME TABLE `fly_article_votes` TO `fly_retired_article_votes_20260928`;
RENAME TABLE `fly_topic` TO `fly_retired_topic_20260928`;
RENAME TABLE `fly_topic_category` TO `fly_retired_topic_category_20260928`;
RENAME TABLE `fly_topic_category_merge` TO `fly_retired_topic_category_merge_20260928`;
RENAME TABLE `fly_topic_edlt` TO `fly_retired_topic_edlt_20260928`;
RENAME TABLE `fly_topic_follow` TO `fly_retired_topic_follow_20260928`;
RENAME TABLE `fly_topic_info_merge` TO `fly_retired_topic_info_merge_20260928`;
RENAME TABLE `fly_share_comment` TO `fly_retired_share_comment_20260928`;

-- ---------- 权限节点收尾 ----------
-- 管理端：老 Freemarker 后台时代的 /system/article|question|share|topic 死节点（URL 已不存在）
DELETE FROM fly_admin_group_permission_merge WHERE permission_id IN (
  SELECT id FROM fly_admin_permission
  WHERE actionKey LIKE '/system/article/%' OR actionKey LIKE '/system/question/%'
     OR actionKey LIKE '/system/share/%' OR actionKey LIKE '/system/topic/%');
DELETE FROM fly_admin_permission
WHERE actionKey LIKE '/system/article/%' OR actionKey LIKE '/system/question/%'
   OR actionKey LIKE '/system/share/%' OR actionKey LIKE '/system/topic/%';

-- 前台：退役投稿/编辑/话题通道的用户权限行（E4 平台评论替代 comment_save）
INSERT INTO fly_user_permission (id, actionKey, controller, remark)
VALUES (2760100000000000001, '/ucenter/comment/save', 'commentController',
        'E4平台评论：发表评论（U3替代/ucenter/article/comment_save）');
INSERT INTO fly_user_group_permission_merge (group_id, permission_id)
SELECT g.id, 2760100000000000001 FROM fly_user_group g;
DELETE FROM fly_user_group_permission_merge WHERE permission_id IN (
  SELECT id FROM fly_user_permission
  WHERE actionKey LIKE '/ucenter/article/%' OR actionKey LIKE '/ucenter/share/%'
     OR actionKey LIKE '/ucenter/topics/%' OR actionKey LIKE '/ucenter/answer/%'
     OR actionKey LIKE '/ucenter/question/%');
DELETE FROM fly_user_permission
WHERE actionKey LIKE '/ucenter/article/%' OR actionKey LIKE '/ucenter/share/%'
   OR actionKey LIKE '/ucenter/topics/%' OR actionKey LIKE '/ucenter/answer/%'
   OR actionKey LIKE '/ucenter/question/%';


-- ============================================================
-- B1 字段绑定数据源（2026-09-29 补充段）
-- user/category 字段类型无 DDL 之外的新列（category 绑定模型复用 relate_model）；
-- 本段只登记选项 API 的管理端权限行并授权全部管理组。
-- ============================================================

INSERT INTO fly_admin_permission (id, actionKey, controller, remark, menuType)
VALUES (2760100000000001001, '/api/system/options/users', 'apiOptionsController', 'B1字段绑定：用户选项数据源', 'F'),
       (2760100000000001002, '/api/system/options/categories', 'apiOptionsController', 'B1字段绑定：分类选项数据源', 'F');
INSERT INTO fly_admin_group_permission_merge (group_id, permission_id)
SELECT g.id, 2760100000000001001 FROM fly_admin_group g;
INSERT INTO fly_admin_group_permission_merge (group_id, permission_id)
SELECT g.id, 2760100000000001002 FROM fly_admin_group g;

-- fly_user_account 为 demo_user 补行（修复记住我自动登录 NPE 的数据面）
INSERT INTO fly_user_account (user_id, balance, score, exp)
SELECT 999999999999999002, 0, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM fly_user_account WHERE user_id = 999999999999999002);

-- demo_user 昵称补全（用户选择器显示昵称语义）
UPDATE fly_user SET nick_name = '演示用户' WHERE user_name = 'demo_user' AND (nick_name IS NULL OR nick_name = '');
