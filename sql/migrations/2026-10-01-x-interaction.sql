-- =====================================================================
-- 2026-10-01 前台互动闭环（X 批次）运行时配置：问答频道
--
-- 背景：questions/answers 模型为 U2 批次运行时建模（不在基线种子，同 testdemo
--       等在线建模产物）。本迁移补齐问答频道的三项运行时配置：
--   1) answers.question（relate→questions）勾「列表筛选」——问题详情页
--      fly_list_model model="answers" question="{id}" 出完整回答列表
--   2) answers 新增 accepted 单选字段（待采纳/已采纳，可筛选）——提问者采纳语义
--   3) 前台用户权限行 /ucenter/content/update（X2 所有者操作通道）并授权全部用户组
--
-- 幂等：UPDATE 天然幂等；INSERT 均带固定主键可重复执行（重复报主键冲突可忽略）。
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p flycms < 本文件
-- =====================================================================

-- 1) answers.question 勾列表筛选
UPDATE `fly_model_field` SET `is_filter` = 1
WHERE `model_id` = (SELECT id FROM fly_model WHERE code = 'answers')
  AND `field_name` = 'question';

-- 2) accepted 单选字段（元数据；物理列 fly_cmodel_answers.accepted 已随首轮 ALTER 落地——
--    MySQL 5.7 无 ADD COLUMN IF NOT EXISTS，重复执行本文件时该列已存在，勿再执行 ALTER）
--    需手工补列时执行：ALTER TABLE `fly_cmodel_answers`
--      ADD COLUMN `accepted` varchar(64) DEFAULT NULL COMMENT '采纳状态（待采纳/已采纳）';
SET @answers_id = (SELECT id FROM fly_model WHERE code = 'answers');
INSERT INTO `fly_model_field`
(`id`, `model_id`, `field_name`, `field_label`, `field_type`, `column_type`, `default_value`,
 `maxlength`, `dict_type`, `options`, `is_required`, `is_list`, `is_search`, `is_filter`,
 `regex`, `placeholder`, `tips`, `sort`, `status`, `create_time`, `update_time`, `tab_name`)
VALUES
(900000000000120101, @answers_id, 'accepted', '采纳状态', 'radio', 'varchar(64)', NULL,
 NULL, NULL, '["待采纳","已采纳"]', 0, 1, 0, 1,
 NULL, NULL, NULL, 5, 1, NOW(), NOW(), '基础信息');

-- 3) X2 通道权限行 + 全用户组授权（对齐 /ucenter/submit/* 2760100000000003001 的授权面）
INSERT INTO `fly_user_permission` (`id`, `actionKey`, `controller`, `remark`) VALUES
('2760100000000003002', '/ucenter/content/update', 'submitController', 'X2 所有者更新自己的内容');

INSERT INTO `fly_user_group_permission_merge` (`group_id`, `permission_id`)
SELECT g.id, 2760100000000003002 FROM fly_user_group g;
