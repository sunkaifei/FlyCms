-- =====================================================================
-- 2026-10-02 S/T 批次 DDL：
--   1) fly_area_block.wrapper_class —— S1-a 区块属性面板（per-block CSS 类）
-- 幂等：ADD COLUMN 重复执行报 Duplicate column 可忽略。
-- =====================================================================

ALTER TABLE `fly_area_block`
  ADD COLUMN `wrapper_class` varchar(200) DEFAULT NULL COMMENT '包装器 CSS 类（渲染时附加到区块外层 div）' AFTER `block_ref`;
