-- P1 结构层 + P2 关系聚合层（万能建模批次 2）
-- 1) fly_model_field：结构/显隐/聚合三列（幂等）
-- 2) fly_relation：M2A 多对任意中间表（幂等）
-- MySQL 5.7：information_schema 判列存在性；雪花 ID 非自增。

-- ---------- fly_model_field 加列 ----------
-- P2 虚拟字段（rollup/m2a）没有物理列定义，column_type 必须允许 NULL
SET @col_nullable := (SELECT IS_NULLABLE FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'fly_model_field' AND column_name = 'column_type');
SET @ddl := IF(@col_nullable = 'NO',
  'ALTER TABLE fly_model_field MODIFY column_type varchar(64) DEFAULT NULL COMMENT ''列定义(由FieldTypeEnum推导,虚拟字段为NULL)''',
  'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'fly_model_field' AND column_name = 'parent_id');
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE fly_model_field ADD COLUMN parent_id bigint(20) NOT NULL DEFAULT 0 COMMENT ''父字段id(GROUP/REPEATER子字段)'' AFTER tab_name',
  'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'fly_model_field' AND column_name = 'visible_when');
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE fly_model_field ADD COLUMN visible_when varchar(500) DEFAULT NULL COMMENT ''条件显隐JSON(field/op/value)'' AFTER parent_id',
  'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'fly_model_field' AND column_name = 'rollup_expr');
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE fly_model_field ADD COLUMN rollup_expr varchar(255) DEFAULT NULL COMMENT ''Rollup聚合JSON(source/func/column)'' AFTER visible_when',
  'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'fly_model_field' AND column_name = 'lookup_fields');
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE fly_model_field ADD COLUMN lookup_fields varchar(255) DEFAULT NULL COMMENT ''Lookup展示列JSON(relate/relates展开目标行时的额外列)'' AFTER rollup_expr',
  'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------- fly_relation（M2A 中间表） ----------
CREATE TABLE IF NOT EXISTS fly_relation (
  `id` bigint(20) unsigned NOT NULL COMMENT '雪花ID',
  `from_model` varchar(32) NOT NULL COMMENT '来源模型code',
  `from_id` bigint(20) unsigned NOT NULL COMMENT '来源内容id',
  `field_name` varchar(64) NOT NULL COMMENT '来源m2a字段名',
  `to_model` varchar(32) NOT NULL COMMENT '目标模型code',
  `to_id` bigint(20) unsigned NOT NULL COMMENT '目标内容id',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  PRIMARY KEY (`id`),
  KEY `idx_from` (`from_model`,`from_id`,`field_name`),
  KEY `idx_to` (`to_model`,`to_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='M2A多对任意关系表(P2)';
