-- ============================================================
-- 阶段 F：表单系统（表单生成器 + 统一数据表）
-- 前置：menu-management.sql 已执行（900110 系统管理存在）
-- 执行：mysql -u<user> -p --default-character-set=utf8mb4 flycms < stage-f.sql
--
-- 设计决策（与规划一致）：表单数据不走动态建表，统一落 fly_form_data.data_json。
-- 原因：表单数据只用于「后台查看 + 导出 CSV」，不需要按字段做索引查询；
--       动态 DDL 会带来权限放大、字段漂移、备份复杂等问题，收益不抵成本。
-- 号段：900260 表单管理 / 900265 表单数据
-- ============================================================

-- 1. 表单定义
CREATE TABLE IF NOT EXISTS `fly_form` (
  `id` bigint(20) NOT NULL COMMENT '雪花ID',
  `form_code` varchar(50) NOT NULL COMMENT '调用码，模板 <@fly_form code="feedback"/>',
  `form_name` varchar(100) NOT NULL COMMENT '表单名称',
  `audit` tinyint(2) NOT NULL DEFAULT '0' COMMENT '0不审核 1提交后需审核',
  `submit_limit` int(11) NOT NULL DEFAULT '0' COMMENT '同一IP/用户每日提交上限，0不限',
  `need_captcha` tinyint(2) NOT NULL DEFAULT '0' COMMENT '是否需要验证码 0否 1是',
  `success_tip` varchar(255) DEFAULT '' COMMENT '提交成功提示语',
  `notify_email` varchar(255) DEFAULT '' COMMENT '新提交通知邮箱，空=不通知',
  `status` tinyint(2) NOT NULL DEFAULT '1' COMMENT '0停用 1启用',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`form_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单定义';

-- 2. 表单字段
CREATE TABLE IF NOT EXISTS `fly_form_field` (
  `id` bigint(20) NOT NULL COMMENT '雪花ID',
  `form_id` bigint(20) NOT NULL COMMENT '所属表单',
  `field_code` varchar(50) NOT NULL COMMENT '字段key，英文',
  `field_name` varchar(100) NOT NULL COMMENT '字段中文名',
  `field_type` varchar(20) NOT NULL DEFAULT 'text' COMMENT 'text/textarea/number/radio/checkbox/select/date/email/mobile/file',
  `required` tinyint(2) NOT NULL DEFAULT '0' COMMENT '0选填 1必填',
  `default_value` varchar(500) DEFAULT '' COMMENT '默认值',
  `placeholder` varchar(255) DEFAULT '' COMMENT '输入提示',
  `options` text COMMENT '选项集合，JSON 数组：["男","女"]',
  `sort` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_form` (`form_id`, `sort`),
  UNIQUE KEY `uk_form_field` (`form_id`, `field_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单字段定义';

-- 3. 表单数据（统一表 + JSON）
CREATE TABLE IF NOT EXISTS `fly_form_data` (
  `id` bigint(20) NOT NULL COMMENT '雪花ID',
  `form_id` bigint(20) NOT NULL COMMENT '所属表单',
  `user_id` bigint(20) DEFAULT '0' COMMENT '提交用户，0=游客',
  `ip` varchar(64) DEFAULT '' COMMENT '提交IP',
  `data_json` mediumtext COMMENT '提交内容 JSON：{fieldCode: value}',
  `status` tinyint(2) NOT NULL DEFAULT '1' COMMENT '0待审 1正常 2已处理',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_form_time` (`form_id`, `create_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单提交数据';

-- 4. 菜单/权限
INSERT INTO fly_admin_permission (id, actionKey, controller, menuType, menuName, path, component, icon, sort, visible, parentId) VALUES
(900260, '/api/system/form/page',        'apiFormController', 'C', '表单管理', '/system/form', '/system/form/index', 'lucide:clipboard-list', 24, 1, 900110),
(900261, '/api/system/form/get',         'apiFormController', 'F', '表单详情', NULL, NULL, NULL, 1, 0, 900260),
(900262, '/api/system/form/save',        'apiFormController', 'F', '新增/修改表单', NULL, NULL, NULL, 2, 0, 900260),
(900263, '/api/system/form/delete',      'apiFormController', 'F', '删除表单', NULL, NULL, NULL, 3, 0, 900260),
(900264, '/api/system/formField/list',   'apiFormController', 'F', '字段列表', NULL, NULL, NULL, 4, 0, 900260),
(900265, '/api/system/formField/save',   'apiFormController', 'F', '新增/修改字段', NULL, NULL, NULL, 5, 0, 900260),
(900266, '/api/system/formField/delete', 'apiFormController', 'F', '删除字段', NULL, NULL, NULL, 6, 0, 900260),
-- 表单数据
(900270, '/api/system/formData/page',    'apiFormController', 'C', '表单数据', '/system/formdata', '/system/formdata/index', 'lucide:database', 25, 1, 900110),
(900271, '/api/system/formData/detail',  'apiFormController', 'F', '数据详情', NULL, NULL, NULL, 1, 0, 900270),
(900272, '/api/system/formData/audit',   'apiFormController', 'F', '标记处理状态', NULL, NULL, NULL, 2, 0, 900270),
(900273, '/api/system/formData/delete',  'apiFormController', 'F', '删除数据', NULL, NULL, NULL, 3, 0, 900270),
(900274, '/api/system/formData/export',  'apiFormController', 'F', '导出CSV', NULL, NULL, NULL, 4, 0, 900270);

-- 5. 赋给超管组
INSERT INTO fly_admin_group_permission_merge (group_id, permission_id) VALUES
(272835742965968896, 900260), (272835742965968896, 900261), (272835742965968896, 900262), (272835742965968896, 900263),
(272835742965968896, 900264), (272835742965968896, 900265), (272835742965968896, 900266),
(272835742965968896, 900270), (272835742965968896, 900271), (272835742965968896, 900272), (272835742965968896, 900273), (272835742965968896, 900274);
