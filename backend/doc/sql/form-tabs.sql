-- ============================================================
-- 动态表单选项卡：字段可配置归属选项卡（帝国CMS 式分组建模）
-- 表单按 tab_name 分组渲染 a-tabs，组内按 sort 排序；
-- 全部字段同组时不显示选项卡。未配置默认"基础信息"。
-- 执行：mysql -u<user> -p --default-character-set=utf8mb4 flycms < form-tabs.sql
-- ============================================================

ALTER TABLE `fly_model_field`
  ADD COLUMN `tab_name` varchar(64) DEFAULT '基础信息' COMMENT '表单选项卡名，表单按此分组渲染';
