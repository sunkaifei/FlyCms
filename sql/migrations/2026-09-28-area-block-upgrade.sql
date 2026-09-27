-- ============================================================
--  2026-09-28 区域编排 V2（P10）DDL 升级
--
--  背景：fly_area_block 建表时 block_ref 定为 varchar(200)，
--        但 §8.4 的 V2 允许往区域里放"列表标签"（如
--        <@fly_page_model model="articles" rows="5">…</@fly_page_model>）
--        与自定义 HTML —— 二者都会轻易超过 200 字符，插入即被截断/报错。
--        另补 block_title：后台布局管理页需要给人看的标题，
--        不能拿 block_ref 原文（可能是一大段 HTML）当列表项。
--
--  执行（已有库升级）：mysql --default-character-set=utf8mb4 -uroot -p flycms < 本文件
--  全新安装：无需执行，全量快照 sql/flycms_*.sql 已包含最终结构。
-- ============================================================

ALTER TABLE `fly_area_block`
  MODIFY COLUMN `block_ref` TEXT NOT NULL COMMENT '碎片key / 标签代码 / HTML 片段',
  ADD COLUMN `block_title` varchar(100) DEFAULT NULL COMMENT '后台展示用标题（仅 UI）' AFTER `block_type`;
