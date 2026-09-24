-- ============================================================
-- 模型命名改造迁移脚本
-- 依据：《内容体系收敛与模型命名改造开发方案.md》§6（ADR D7/D8）、§9
-- 前置：必须先备份数据库（见文末 §5）
-- 执行：mysql -h127.0.0.1 -uroot -p --default-character-set=utf8mb4 flycms < model-naming-migration.sql
--
-- 本脚本做两件事（互相独立，可分别执行）：
--   A. 物理表名：fly_cmodel_{雪花id} → fly_cmodel_{code}   （ADR D7）
--   B. 元数据主键：fly_model / fly_model_field 改 AUTO_INCREMENT （ADR D8）
--
-- 幂等性说明：本脚本**不是完全幂等**。步骤 1 的 RENAME TABLE 与步骤 3 的
-- ALTER 重复执行会报错。执行前请先用 §5 的备份与校验查询确认当前状态。
-- ============================================================


-- ============================================================
-- 0. 执行前状态确认（先跑这三条，确认表名与主键当前形态）
-- ============================================================
-- 0.1 当前所有 cmodel 表
SELECT table_name, table_rows
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name LIKE 'fly\_cmodel\_%'
ORDER BY table_name;

-- 0.2 模型 code 与当前表名后缀的对照（迁移后 code 必须与表名后缀一致）
SELECT id, code, name FROM fly_model ORDER BY id;

-- 0.3 确认 fly_model / fly_model_field 主键当前形态（预期：非 auto_increment）
SHOW CREATE TABLE fly_model;
SHOW CREATE TABLE fly_model_field;


-- ============================================================
-- A. 物理表名改造：fly_cmodel_{雪花id} → fly_cmodel_{code}
--
-- 依据 code 而非 id 命名（D7）。三个内置模型：
--   fly_cmodel_900000000000000001 → fly_cmodel_images     (code=images)
--   fly_cmodel_900000000000000002 → fly_cmodel_downloads  (code=downloads)
--   fly_cmodel_900000000000000003 → fly_cmodel_articles   (code=articles)
--
-- ⚠️ 执行前核对：步骤 0.2 查出的 code 必须与下面 RENAME 的目标一致。
--    若库里 code 与预期不符（被改过），请先把 RENAME 目标改成实际 code。
-- ⚠️ 若目标表名已存在（计划外残留），RENAME 会失败并报 1050。
-- ============================================================

RENAME TABLE `fly_cmodel_900000000000000001` TO `fly_cmodel_images`;
RENAME TABLE `fly_cmodel_900000000000000002` TO `fly_cmodel_downloads`;
RENAME TABLE `fly_cmodel_900000000000000003` TO `fly_cmodel_articles`;

-- A.2 测试模型 testdemo 建议直接删除（非内置、无业务数据）
--     确认无数据后再执行；若要保留，请改为 RENAME 到 fly_cmodel_testdemo
-- SELECT COUNT(*) FROM `fly_cmodel_1299270553286488064`;
-- DROP TABLE IF EXISTS `fly_cmodel_1299270553286488064`;
-- DELETE FROM fly_model WHERE code = 'testdemo';


-- ============================================================
-- B. 元数据主键改造：改 AUTO_INCREMENT（ADR D8）
--
-- ⚠️⚠️ 关键：MODIFY 与 DROP/ADD PRIMARY KEY **必须写在同一条 ALTER 里**。
--    拆成两条会报 ERROR 1075（Incorrect table definition; there can be
--    only one auto column and it must be defined as a key）——
--    因为 MODIFY 成功后瞬间自增列不在主键上，中间态非法。
--
-- 为什么先 DROP 再 ADD PRIMARY KEY，而不是直接 MODIFY：
--    主键当前定义在 `id`（雪花 bigint），把 id 改成 AUTO_INCREMENT 前，
--    需确保 id 是"单一整数列主键"。本项目飞_model 的主键即为 id 单列，
--    但其上有 uk_code 唯一键，故 DROP PRIMARY KEY 不影响 uk_code。
--
-- 业务数据表（fly_cmodel_*）的 id **保持雪花、完全不动**（D8 边界）。
-- ============================================================

-- B.1 fly_model
ALTER TABLE `fly_model`
  MODIFY COLUMN `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键（自增，D8）',
  DROP PRIMARY KEY,
  ADD PRIMARY KEY (`id`);

-- B.2 fly_model_field
ALTER TABLE `fly_model_field`
  MODIFY COLUMN `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键（自增，D8）',
  DROP PRIMARY KEY,
  ADD PRIMARY KEY (`id`);

-- B.3 自增起始值：**关键坑**（实测踩到）
--
--     仅执行 `ALTER TABLE fly_model AUTO_INCREMENT = 1` 是**无效**的：
--     现存的 4 条模型 id 是雪花值（9e17 量级），MySQL 的 AUTO_INCREMENT 计数器
--     不允许低于当前 MAX(id)，所以下一条新模型的 id 会续在雪花后面
--     （实测得到 1299270553286488065），完全违背"id 可读"的初衷。
--
--     正解：把历史雪花 id 重写为可读小整数（1,2,3,4），并同步三张引用表。
--     模板目录与前台路由都按 code 命名（非 id），故重写 id 不影响模板与 URL。
--
--     引用 fly_model.id 的表（已全量核对）：fly_model_field.model_id、
--     fly_model_category.model_id、fly_channel.model_id（当前 0 行）。
--
--     ⚠️ 若你的库中 fly_model.id 已被外部系统（如统计报表、日志）以明文记录，
--        重写 id 会造成历史数据对不上——请先确认无此依赖，否则跳过 B.3 接受雪花 id。
-- ============================================================

START TRANSACTION;

-- 映射：images=1, downloads=2, articles=3, testdemo=4
UPDATE `fly_model` SET `id` = 1 WHERE `id` = 900000000000000001;
UPDATE `fly_model` SET `id` = 2 WHERE `id` = 900000000000000002;
UPDATE `fly_model` SET `id` = 3 WHERE `id` = 900000000000000003;
UPDATE `fly_model` SET `id` = 4 WHERE `id` = 1299270553286488064;

-- 同步引用表（缺一不可，否则字段/分类挂到不存在的模型上）
UPDATE `fly_model_field`    SET `model_id` = 1 WHERE `model_id` = 900000000000000001;
UPDATE `fly_model_field`    SET `model_id` = 2 WHERE `model_id` = 900000000000000002;
UPDATE `fly_model_field`    SET `model_id` = 3 WHERE `model_id` = 900000000000000003;
UPDATE `fly_model_field`    SET `model_id` = 4 WHERE `model_id` = 1299270553286488064;
UPDATE `fly_model_category` SET `model_id` = 2 WHERE `model_id` = 900000000000000002;
UPDATE `fly_model_category` SET `model_id` = 4 WHERE `model_id` = 1299270553286488064;
-- fly_channel 当前 0 行，如有数据需同样处理
UPDATE `fly_channel`        SET `model_id` = 1 WHERE `model_id` = 900000000000000001;
UPDATE `fly_channel`        SET `model_id` = 2 WHERE `model_id` = 900000000000000002;
UPDATE `fly_channel`        SET `model_id` = 3 WHERE `model_id` = 900000000000000003;
UPDATE `fly_channel`        SET `model_id` = 4 WHERE `model_id` = 1299270553286488064;

-- 重置自增序列，下一个模型 id = 5，下一个字段 id = 20
ALTER TABLE `fly_model` AUTO_INCREMENT = 5;
ALTER TABLE `fly_model_field` AUTO_INCREMENT = 20;

COMMIT;

-- B.4 重写后立即校验：不得有孤儿引用（预期两个 0）
SELECT 'field 孤儿' AS chk, COUNT(*) AS n FROM fly_model_field f
  LEFT JOIN fly_model m ON m.id = f.model_id WHERE m.id IS NULL
UNION ALL
SELECT 'category 孤儿', COUNT(*) FROM fly_model_category c
  LEFT JOIN fly_model m ON m.id = c.model_id WHERE m.id IS NULL;


-- ============================================================
-- 5. 执行后校验（每条都要过）
-- ============================================================

-- 5.1 表名已按 code 生成
SELECT table_name FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name LIKE 'fly\_cmodel\_%'
ORDER BY table_name;
-- 预期：fly_cmodel_articles / fly_cmodel_downloads / fly_cmodel_images

-- 5.2 主键已是 auto_increment
SELECT table_name, column_name, extra
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN ('fly_model','fly_model_field') AND column_name = 'id';
-- 预期：extra = 'auto_increment'

-- 5.3 数据未丢失（迁移前后行数一致）
SELECT 'fly_model' AS t, COUNT(*) AS n FROM fly_model
UNION ALL SELECT 'fly_model_field', COUNT(*) FROM fly_model_field
UNION ALL SELECT 'fly_cmodel_images', COUNT(*) FROM fly_cmodel_images
UNION ALL SELECT 'fly_cmodel_downloads', COUNT(*) FROM fly_cmodel_downloads
UNION ALL SELECT 'fly_cmodel_articles', COUNT(*) FROM fly_cmodel_articles;
-- 预期：fly_model 4（或减去 testdemo 后 3）、fly_model_field 19、三张 cmodel 表行数不变

-- 5.4 无硬编码雪花表名残留（应返回 0 行）
SELECT table_name FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name REGEXP 'fly_cmodel_[0-9]{15,}';


-- ============================================================
-- 6. 回滚方案（仅在迁移失败、需紧急恢复时执行）
--
-- ⚠️ 回滚前必须先停应用，否则新代码会往旧表名写入造成数据分叉。
-- ============================================================
-- RENAME TABLE `fly_cmodel_images`    TO `fly_cmodel_900000000000000001`;
-- RENAME TABLE `fly_cmodel_downloads` TO `fly_cmodel_900000000000000002`;
-- RENAME TABLE `fly_cmodel_articles`  TO `fly_cmodel_900000000000000003`;
-- ALTER TABLE `fly_model`
--   MODIFY COLUMN `id` bigint(20) NOT NULL COMMENT '雪花ID',
--   DROP PRIMARY KEY, ADD PRIMARY KEY (`id`);
-- ALTER TABLE `fly_model_field`
--   MODIFY COLUMN `id` bigint(20) NOT NULL COMMENT '雪花ID',
--   DROP PRIMARY KEY, ADD PRIMARY KEY (`id`);
-- 然后回退代码版本并重启。


-- ============================================================
-- 7. 备份命令（执行本脚本前必须已完成）
-- ============================================================
-- mysqldump -h127.0.0.1 -uroot -p --default-character-set=utf8mb4 \
--   --single-transaction --routines --triggers flycms \
--   fly_model fly_model_field fly_model_category \
--   fly_cmodel_900000000000000001 fly_cmodel_900000000000000002 fly_cmodel_900000000000000003 \
--   > backup_before_rename_$(date +%Y%m%d_%H%M%S).sql
-- 校验：文件非空，且 grep 到 CREATE TABLE 与若干 INSERT INTO
