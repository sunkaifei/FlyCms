-- =====================================================================
-- 2026-10-02 内容图片本地化（Q 批次 Q3）：fly_model.localize_images + fly_img_domain
--
-- 语义：模型开启 localize_images=1 后，内容保存时自动把编辑器（content）里的
--       外站图片抓取到本地（/upload/content/），登记 fly_images（进附件库、引用计数），
--       img src 替换为本地地址；替换域名取站点参数 fly_img_domain（空=相对路径/当前域名）。
--
-- 内容：
--   1) fly_model.localize_images 开关列（默认 0）
--   2) 站点参数 fly_img_domain（图片本地化替换域名，空=默认当前域名）
--
-- 幂等：ADD COLUMN / INSERT IGNORE，可重复执行。
-- =====================================================================

ALTER TABLE `fly_model`
  ADD COLUMN `localize_images` tinyint(1) NOT NULL DEFAULT '0' COMMENT '内容图片本地化：1=保存时自动抓取编辑器外站图片到本地并替换地址' AFTER `enable_submit`;

INSERT IGNORE INTO `fly_config_web` (`id`, `typebase`, `keycode`, `keyvalue`, `description`, `sort`) VALUES
(900390001, 0, 'fly_img_domain', '', '图片本地化替换域名（空=默认当前域名/相对路径）', 0);
