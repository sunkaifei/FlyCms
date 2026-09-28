-- ============================================================
-- 万能建模 P0 批次（对标调研 2026-09-28）：
--   字段高级属性：唯一约束 + 数值区间（min/max）
--   配合 FieldTypeEnum 新增 SWITCH/EMAIL/URL/PHONE/COLOR/RATING/SLUG
-- 幂等：information_schema 探测后条件执行
-- ============================================================

SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'fly_model_field'
                 AND column_name = 'is_unique');
SET @sql := IF(@exist = 0,
  'ALTER TABLE `fly_model_field` ADD COLUMN `is_unique` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''值是否全模型唯一（slug/编号等），1=是'' AFTER `relate_model`',
  'SELECT ''is_unique 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'fly_model_field'
                 AND column_name = 'min_value');
SET @sql := IF(@exist = 0,
  'ALTER TABLE `fly_model_field` ADD COLUMN `min_value` decimal(18,4) DEFAULT NULL COMMENT ''数值区间下限（number/decimal/rating）'' AFTER `is_unique`',
  'SELECT ''min_value 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'fly_model_field'
                 AND column_name = 'max_value');
SET @sql := IF(@exist = 0,
  'ALTER TABLE `fly_model_field` ADD COLUMN `max_value` decimal(18,4) DEFAULT NULL COMMENT ''数值区间上限（number/decimal/rating）'' AFTER `min_value`',
  'SELECT ''max_value 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
