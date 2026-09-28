-- ============================================================
-- 自定义模型引擎缺口补齐 · 第 1 批（E1 关联引用 / E3 URL 型附件）
-- 依据：《自定义模型能力缺口与内容模块生产方案.md》§3.1 / §3.2
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p flycms < 2026-09-28-model-field-relate.sql
-- 号段：本脚本不新增权限行（无新菜单/新端点），仅字段元数据增列
-- 幂等：可重复执行（先判存在再加列）
-- ============================================================

-- ---------- E1：关联引用字段的目标模型 ----------
-- RELATE / RELATES 字段必须显式指定目标模型 code；关联"本模型"时填本模型自己的 code
-- （用于自关联树，如回答的 parent_id）——显式优于隐式，避免漏配后静默关联错表
SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'fly_model_field'
                 AND column_name = 'relate_model');
SET @sql := IF(@exist = 0,
  'ALTER TABLE `fly_model_field` ADD COLUMN `relate_model` varchar(32) DEFAULT NULL COMMENT ''RELATE/RELATES 字段的目标模型 code（必填）'' AFTER `tips`',
  'SELECT ''relate_model 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 统一列注释（可重复执行；上方 ADD 的注释与这里保持一致）
ALTER TABLE `fly_model_field` MODIFY COLUMN `relate_model` varchar(32) DEFAULT NULL
  COMMENT 'RELATE/RELATES 字段的目标模型 code（必填；关联本模型时填本模型 code，用于自关联树）';

-- ---------- 校验 ----------
SELECT 'fly_model_field.relate_model 就绪' AS item,
       COUNT(*) AS cnt
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'fly_model_field'
  AND column_name = 'relate_model';

-- E3（image_url / file_url）不需要新增表列：
-- 它们只是 FieldTypeEnum 的 varchar(500) 列，由 addField 走既有 ALTER ADD COLUMN 通道生成。
