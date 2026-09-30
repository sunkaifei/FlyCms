-- =====================================================================
-- 2026-10-01 字段表单控件增强（W 批次 / W1）：fly_model_field.widget_conf
--
-- 背景：字段类型层已富（32+2 类型）但录入控件"能用不好用"——附件控件裸 ID
--       输入框、单图非头像式、时间无格式/范围、多行文本行数写死。本轮补
--       「控件层」：字段类型定存储、widget_conf 定录入（对标 Directus
--       interface options / 迅睿控件类），一列 JSON + 每类型白名单 schema
--       （FieldWidgetConfUtil 校验），NULL = 全默认行为，存量字段零影响。
--       同批新增 date_range / datetime_range 两类型（json 列，无需本迁移）。
--
-- 内容：
--   1) fly_model_field 加 widget_conf text 列（控件配置 JSON）
--
-- 幂等：ADD COLUMN 不可重复执行（MySQL 5.7 无 IF NOT EXISTS），重复执行会报
--       Duplicate column，可忽略。全新安装无需执行（基线已含）。
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p flycms < 本文件
-- =====================================================================

ALTER TABLE `fly_model_field`
  ADD COLUMN `widget_conf` text COMMENT '控件配置 JSON（白名单 schema，NULL=全默认）' AFTER `options`;
