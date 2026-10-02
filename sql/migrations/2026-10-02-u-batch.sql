-- =====================================================================
-- 2026-10-02 U 批次 DDL：
--   1) fly_cmodel_orders.pay_state —— 支付状态（待付款/已付款/已退款）
--   2) 支付流走内容模型 + 动作原语（orders.state 由模板按钮流转，pay_state 记录收款）
-- 幂等：ADD COLUMN 重复执行报 Duplicate column 可忽略。
-- =====================================================================

SET @orders_id = (SELECT id FROM fly_model WHERE code = 'orders');

ALTER TABLE fly_cmodel_orders ADD COLUMN pay_state varchar(16) DEFAULT '待付款' COMMENT '支付状态：待付款/已付款/已退款';


INSERT IGNORE INTO fly_model_field
(id, model_id, field_name, field_label, field_type, column_type, options, is_required, is_list, is_filter, is_form, sort, status, create_time, update_time, tab_name)
VALUES
(900000000000130102, @orders_id, 'pay_state', '支付状态', 'radio', 'varchar(16)', '["待付款","已付款","已退款"]', 0, 1, 1, 1, 7, 1, NOW(), NOW(), '基础信息');
