-- =====================================================================
-- 2026-09-30 数据字典：fly_dict_type / fly_dict_data（若依式 dict_type 绑定）
--
-- 背景：模型字段 select/radio/checkbox 的候选项原先只能写在 fly_model_field.options
--       （字段定义内联 JSON），选项分散、无法跨字段复用。本次落地平台级数据字典：
--       字典管理页维护类型与数据，字段设置里把 select/radio/checkbox 绑定到
--       dict_type，发布/筛选表单按字典渲染候选项（field.options 变为回退数据源）。
--       fly_model_field.dict_type 列原已预留（建表注释「暂用options，预留字典」），
--       存储层零变更。
--
-- 内容：
--   1) 建表 fly_dict_type / fly_dict_data
--   2) 种子字典：app_os 运行平台 / soft_auth 授权方式 / soft_language 程序语言
--   3) 下载模型三个既有 select 字段改绑字典（os_require/auth/language）
--   4) 菜单：900350「字典管理」C 节点 + 7 个 F 功能点（900351~900357）
--   5) 授权：全部授「超级管理员」组（1）
--
-- 幂等：CREATE TABLE IF NOT EXISTS / INSERT IGNORE，可重复执行。
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p flycms < 本文件
--       中文必须带 --default-character-set=utf8mb4，否则真实乱码。
-- =====================================================================

-- 1) 字典类型表
CREATE TABLE IF NOT EXISTS `fly_dict_type` (
  `id` bigint(20) unsigned NOT NULL COMMENT '主键（雪花）',
  `dict_name` varchar(64) NOT NULL COMMENT '字典名称（如：运行平台）',
  `dict_type` varchar(64) NOT NULL COMMENT '字典类型键（如 app_os），字段绑定用',
  `remark` varchar(255) DEFAULT NULL,
  `sort` int(10) NOT NULL DEFAULT '0',
  `status` tinyint(2) NOT NULL DEFAULT '1' COMMENT '1=启用 0=停用（停用后表单回退 field.options）',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典类型';

-- 2) 字典数据表
CREATE TABLE IF NOT EXISTS `fly_dict_data` (
  `id` bigint(20) unsigned NOT NULL COMMENT '主键（雪花）',
  `dict_type` varchar(64) NOT NULL COMMENT '所属字典类型键',
  `dict_label` varchar(128) NOT NULL COMMENT '标签（表单显示）',
  `dict_value` varchar(128) NOT NULL COMMENT '键值（入库存储）',
  `remark` varchar(255) DEFAULT NULL,
  `sort` int(10) NOT NULL DEFAULT '0',
  `status` tinyint(2) NOT NULL DEFAULT '1' COMMENT '1=启用 0=停用（停用后不出现在候选项）',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典数据';

-- 3) 种子字典（与下载模型既有 options 对齐 + 运行平台补齐移动端）
INSERT IGNORE INTO `fly_dict_type` (`id`, `dict_name`, `dict_type`, `remark`, `sort`, `status`, `create_time`) VALUES
(910000000000000001, '运行平台', 'app_os',        '软件可运行的操作系统/平台', 1, 1, NOW()),
(910000000000000002, '授权方式', 'soft_auth',     '软件授权类型',              2, 1, NOW()),
(910000000000000003, '程序语言', 'soft_language', '软件界面语言',              3, 1, NOW());

INSERT IGNORE INTO `fly_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `sort`, `status`, `create_time`) VALUES
(920000000000000101, 'app_os',        'Windows',   'Windows',   1, 1, NOW()),
(920000000000000102, 'app_os',        'Linux',     'Linux',     2, 1, NOW()),
(920000000000000103, 'app_os',        'macOS',     'macOS',     3, 1, NOW()),
(920000000000000104, 'app_os',        'Android',   'Android',   4, 1, NOW()),
(920000000000000105, 'app_os',        'iOS',       'iOS',       5, 1, NOW()),
(920000000000000201, 'soft_auth',     '免费软件',   '免费软件',   1, 1, NOW()),
(920000000000000202, 'soft_auth',     '开源软件',   '开源软件',   2, 1, NOW()),
(920000000000000203, 'soft_auth',     '共享软件',   '共享软件',   3, 1, NOW()),
(920000000000000204, 'soft_auth',     '商业软件',   '商业软件',   4, 1, NOW()),
(920000000000000301, 'soft_language', '简体中文',   '简体中文',   1, 1, NOW()),
(920000000000000302, 'soft_language', '英文',       '英文',       2, 1, NOW()),
(920000000000000303, 'soft_language', '多语言',     '多语言',     3, 1, NOW());

-- 4) 下载模型既有 select 字段改绑字典（id 见 fly_model_field 种子；绑定后 options 仅作回退）
UPDATE `fly_model_field` SET `dict_type` = 'app_os'        WHERE `id` = 900000000000020105 AND (`dict_type` = '' OR `dict_type` IS NULL);
UPDATE `fly_model_field` SET `dict_type` = 'soft_auth'     WHERE `id` = 900000000000020106 AND (`dict_type` = '' OR `dict_type` IS NULL);
UPDATE `fly_model_field` SET `dict_type` = 'soft_language' WHERE `id` = 900000000000020107 AND (`dict_type` = '' OR `dict_type` IS NULL);

-- 5) 菜单与功能点（C = 字典管理页，F = 页内按钮；/api/** 节点 permission_sync 不登记，必须手工插入）
INSERT IGNORE INTO `fly_admin_permission`
(`id`, `actionKey`, `controller`, `remark`, `parentId`, `menuType`, `menuName`, `path`, `component`, `icon`, `sort`, `visible`) VALUES
(900350, '/api/system/dict/type/list',   'apiDictController', '字典管理：菜单/类型列表', 900110, 'C', '字典管理', '/system/dict', '/system/dict/index', 'lucide:book-text', '29', 1),
(900351, '/api/system/dict/type/save',   'apiDictController', '字典管理：新增类型',     900350, 'F', '新增字典', NULL, NULL, NULL, '1', 1),
(900352, '/api/system/dict/type/update', 'apiDictController', '字典管理：编辑类型',     900350, 'F', '编辑字典', NULL, NULL, NULL, '2', 1),
(900353, '/api/system/dict/type/delete', 'apiDictController', '字典管理：删除类型',     900350, 'F', '删除字典', NULL, NULL, NULL, '3', 1),
(900354, '/api/system/dict/data/list',   'apiDictController', '字典管理：数据列表',     900350, 'F', '字典数据', NULL, NULL, NULL, '4', 1),
(900355, '/api/system/dict/data/save',   'apiDictController', '字典管理：新增数据',     900350, 'F', '新增数据', NULL, NULL, NULL, '5', 1),
(900356, '/api/system/dict/data/update', 'apiDictController', '字典管理：编辑数据',     900350, 'F', '编辑数据', NULL, NULL, NULL, '6', 1),
(900357, '/api/system/dict/data/delete', 'apiDictController', '字典管理：删除数据',     900350, 'F', '删除数据', NULL, NULL, NULL, '7', 1);

-- 6) 授权：全部授「超级管理员」组（1），其他组在角色组管理按需勾选
INSERT IGNORE INTO `fly_admin_group_permission_merge` (`group_id`, `permission_id`)
SELECT 1, p.id
FROM `fly_admin_permission` p
WHERE p.id BETWEEN 900350 AND 900357;
