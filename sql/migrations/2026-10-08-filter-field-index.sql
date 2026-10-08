-- ============================================================
-- 2026-10-08 为存量「筛选字段」补建索引（P3）
-- ------------------------------------------------------------
-- 背景：动态模型表 fly_cmodel_* 建表时只有 idx_category(category_id,status)、
--       idx_short_url(short_url)、idx_user(user_id,status) 三条索引；
--       而 is_filter=1 的自定义字段参与列表筛选，且日期范围筛选走
--       CAST(JSON_UNQUOTE(JSON_EXTRACT(col,'$[0]'))) 这类函数包裹列 ——
--       两者都无法走索引，数据量上来后列表页是全表扫描 + filesort。
--
-- 范围：本脚本只处理**存量**字段。新建模型 / 新增字段 / 把已有字段改成
--       参与筛选，均已由 ModelTableService.addFilterIndexIfNeeded 自动建索引。
--
-- 幂等：按 information_schema.statistics 判存在，可重复执行。
--
-- 用法：
--   mysql --default-character-set=utf8mb4 -uroot -p flycms < 2026-10-08-filter-field-index.sql
-- ============================================================

DROP PROCEDURE IF EXISTS flycms_backfill_filter_index;

DELIMITER $$
CREATE PROCEDURE flycms_backfill_filter_index()
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE v_table VARCHAR(64);
  DECLARE v_col   VARCHAR(64);
  DECLARE v_idx   VARCHAR(80);
  DECLARE v_exists INT;

  -- 只挑真正有物理列、且可建索引的字段：
  --   排除结构层（group/repeater 子字段存父列 JSON 内）、虚拟字段（rollup/m2a/formula）、
  --   富文本（editor 走主表 content 通道 / textarea 为 text 列）、以及 JSON 列（需生成列）
  DECLARE cur CURSOR FOR
    SELECT CONCAT('fly_cmodel_', m.code),
           f.field_name,
           CONCAT('idx_c_', f.field_name)
    FROM fly_model_field f
    JOIN fly_model m ON m.id = f.model_id
    WHERE f.is_filter = 1
      AND f.status = 1
      AND f.column_type IS NOT NULL
      AND f.column_type NOT LIKE '%text%'
      AND f.column_type NOT LIKE '%json%'
      AND f.field_type NOT IN ('group', 'repeater', 'rollup', 'm2a', 'formula', 'editor', 'textarea');

  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO v_table, v_col, v_idx;
    IF done = 1 THEN
      LEAVE read_loop;
    END IF;

    SELECT COUNT(*) INTO v_exists
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = v_table
       AND index_name = v_idx;

    IF v_exists = 0 THEN
      SET @ddl = CONCAT('ALTER TABLE `', v_table, '` ADD INDEX `', v_idx, '` (`', v_col, '`)');
      PREPARE stmt FROM @ddl;
      EXECUTE stmt;
      DEALLOCATE PREPARE stmt;
    END IF;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

CALL flycms_backfill_filter_index();
DROP PROCEDURE flycms_backfill_filter_index;
