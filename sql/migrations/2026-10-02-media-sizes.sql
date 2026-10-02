-- =====================================================================
-- 2026-10-02 媒体库多尺寸（Q 批次 Q1）：fly_images.sizes
--
-- 背景：对标 WP 上传自动多尺寸裁剪 + 响应式 srcset。原图外按 150/320/768 三档
--       生成缩放副本（只缩不放、保持宽高比与原格式），sizes 存 JSON 数组；
--       读侧生成 {field}Srcset 供模板 <img srcset> 响应式输出。
--       旧记录无 sizes → 读侧回退原图，完全向后兼容；存量不回填。
--
-- 幂等：ADD COLUMN 重复执行报 Duplicate column 可忽略。
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p flycms < 本文件
-- =====================================================================

ALTER TABLE `fly_images`
  ADD COLUMN `sizes` text COMMENT '多尺寸副本 JSON：[{"n":"thumb","u":"…","w":150,"h":112},…]（NULL=无副本）' AFTER `signature`;
