-- =====================================================================
-- 2026-10-02 模型级「后台可新增」开关（admin_create）：
--   内容全由前台生成的模型（问答/动作层模型）——后台隐藏新增入口与内容菜单，
--   保留列表/审核/编辑/删除能力。
--
-- 语义矩阵（与既有开关组合）：
--   admin_create=1 + enable_submit=0  常规内容：只有后台发（默认，现状不变）
--   admin_create=0 + enable_submit=1  纯前台生成：后台只审不发（问答问题）
--   admin_create=0 + enable_submit=0  动作模型：后台完全不出现（购物车/订单行）
--   双开                              前后台都可发
--
-- admin_create=0 的行为（三处收口，均模型级）：
--   ① fly_admin_permission 内容管理菜单/新增按钮 visible=0（路由仍注册，直连可用）
--   ② vben modeldata「添加内容」按钮隐藏（前端 hasAccessByCodes + 模型标志）
--   ③ /api/system/modelData/save 后端兜底拒绝（model.adminCreate==0 → 拒绝后台直发）
-- 注意：审核/编辑/删除不受影响；前台投稿通道 enable_submit 独立判断。
--
-- 幂等：ADD COLUMN 重复执行报 Duplicate column 可忽略。
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p flycms < 本文件
-- =====================================================================

ALTER TABLE `fly_model`
  ADD COLUMN `admin_create` tinyint(1) NOT NULL DEFAULT '1' COMMENT '后台可新增：0=内容仅前台生成（隐藏新增入口与内容菜单，审核/编辑/删除保留）' AFTER `enable_submit`;

-- 动作层模型立即收口：购物车/订单/明细后台不出现（本次迁移同时把历史脏开关归位）
UPDATE `fly_model` SET `admin_create` = 0
WHERE `code` IN ('carts', 'orders', 'order_items');
