-- ============================================================
-- 修复：fly_model_field.status 被静默写成 0 造成的字段误禁用
--
-- 根因（两处叠加，已同步修代码）：
--   1) ModelField.status 是原始类型 int（未赋值即 0），而
--      ModelFieldDao.updateField 的 XML 无条件写 status = #{status}；
--   2) ModelTransferService.exportModel 不导出 status、fieldFromJson 也不读 status。
--
-- 触发链：预设首次导入用 addField（内部 setStatus(1)）→ 一切正常；
-- 之后任何一次「预设重新导入」或「后台编辑字段」都会走 updateField，
-- 把该模型下**所有已存在字段**写成 status=0（禁用）。表现为：
--   · 字段从列表投影中消失（列表接口/模板拿不到值，前端显示 0/空）
--   · is_filter 字段不再参与筛选
--   · relate 字段被 findRelateFieldsByTargetModel 的 `status = 1` 过滤掉，
--     反向引用 {field}Backs 整条链路断掉（问答「回答数」恒为 0）
--
-- 影响面：全库 91 个字段中 10 个被误禁用（新增字段走 addField 所以正常，
-- 这解释了同一模型内 status 有 0 有 1 的分布）。
--
-- 执行：
--   mysql --default-character-set=utf8mb4 -uroot -p flycms < sql/migrations/2026-10-08-model-field-status.sql
-- ============================================================

-- 修复前核对（应列出下面 10 行）
SELECT m.code AS model_code, f.field_name, f.field_type, f.status
FROM fly_model_field f
JOIN fly_model m ON m.id = f.model_id
WHERE f.status = 0
ORDER BY m.code, f.sort;

START TRANSACTION;

-- 逐 (模型, 字段) 精确点名，不用 `WHERE status = 0` 全量放开，
-- 避免把将来可能新增的「有意停用」字段一起打开。
UPDATE fly_model_field f
JOIN fly_model m ON m.id = f.model_id
SET f.status = 1
WHERE f.status = 0
  AND (
        (m.code = 'questions' AND f.field_name = 'weight')                      -- 悬赏：列表投影/高悬赏侧栏
     OR (m.code = 'answers'   AND f.field_name IN ('question', 'parent'))       -- 所属问题：questionBacks 反向引用
     OR (m.code = 'answers'   AND f.field_name = 'accepted')                    -- 采纳状态：已采纳过滤/采纳按钮
     OR (m.code = 'info'      AND f.field_name IN ('price', 'region', 'contact', 'expire_time'))  -- classifieds 预设
     OR (m.code = 'brands'    AND f.field_name IN ('logo', 'website'))          -- mall 预设
  );

COMMIT;

-- 修复后核对：应为 0 行；且 status=1 的字段数为 91
SELECT COUNT(*) AS still_disabled FROM fly_model_field WHERE status = 0;
SELECT COUNT(*) AS total, SUM(status = 1) AS enabled FROM fly_model_field;

-- answers.question 是 is_filter 字段，反向引用（questionBacks）按 `WHERE question IN (...)` 查。
-- 字段「启用」不经过 addColumn/updateField，索引不会自动补，故在此显式建（幂等）。
SET @has_idx = (SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'fly_cmodel_answers'
                  AND INDEX_NAME = 'idx_filter_question');
SET @ddl = IF(@has_idx = 0,
              'ALTER TABLE fly_cmodel_answers ADD INDEX idx_filter_question (question)',
              'SELECT ''idx_filter_question 已存在，跳过'' AS msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
