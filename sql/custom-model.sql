-- ============================================================
-- FlyCms 自定义模型系统：元数据表 + 权限 + 内置种子模型
-- 依据：backend/doc/自定义模型系统开发手册.md v1.1（§4 数据库设计 / §12.4 种子数据）
-- 执行方式：mysql -u<user> -p flycms < custom-model.sql
-- 注意：超管组授权语句里的 group_id 按实际环境的超级管理员组 ID 调整
--       （本地开发库为 272835742965968896）。
-- ============================================================

-- ----------------------------
-- 1. 模型定义表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `fly_model` (
  `id`           bigint(20) unsigned NOT NULL COMMENT '雪花ID',
  `name`         varchar(64)  NOT NULL COMMENT '模型名称，如：下载模型',
  `code`         varchar(32)  NOT NULL COMMENT '模型标识（路由/模板目录用），如 downloads',
  `title_label`  varchar(32)  NOT NULL DEFAULT '标题' COMMENT '标题字段显示名',
  `is_system`    tinyint(1)   NOT NULL DEFAULT 0 COMMENT '1内置模型禁删禁改code',
  `list_template` varchar(100) DEFAULT NULL COMMENT '列表页模板（pc_theme下相对路径，空=/{code}/list.html）',
  `detail_template` varchar(100) DEFAULT NULL COMMENT '详情页模板（空=/{code}/detail.html）',
  `icon`         varchar(64)  DEFAULT NULL COMMENT '后台图标',
  `description`  varchar(255) DEFAULT NULL,
  `sort`         int(10)      NOT NULL DEFAULT 0,
  `status`       tinyint(2)   NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
  `create_time`  datetime DEFAULT NULL,
  `update_time`  datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容模型定义';

-- ----------------------------
-- 2. 模型字段定义表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `fly_model_field` (
  `id`            bigint(20) unsigned NOT NULL,
  `model_id`      bigint(20) unsigned NOT NULL,
  `field_name`    varchar(64)  NOT NULL COMMENT '字段名=列名，须匹配 ^[a-z][a-z0-9_]{0,63}$',
  `field_label`   varchar(64)  NOT NULL COMMENT '显示名',
  `field_type`    varchar(20)  NOT NULL COMMENT '见 FieldTypeEnum',
  `column_type`   varchar(64)  NOT NULL COMMENT '列定义，后端由field_type推导后冗余存储，前端不可传',
  `default_value` varchar(255) DEFAULT NULL,
  `maxlength`     int(11)      DEFAULT NULL,
  `dict_type`     varchar(64)  DEFAULT NULL COMMENT 'select/radio数据来源（暂用options，预留字典）',
  `options`       varchar(2000) DEFAULT NULL COMMENT '选项，JSON数组 ["A","B"] 或 [{"label":"","value":""}]',
  `is_required`   tinyint(1)   NOT NULL DEFAULT 0,
  `is_list`       tinyint(1)   NOT NULL DEFAULT 1 COMMENT '后台列表显示',
  `is_search`     tinyint(1)   NOT NULL DEFAULT 0 COMMENT '是否参与全文搜索（预留，待接 Elasticsearch）',
  `is_filter`     tinyint(1)   NOT NULL DEFAULT 0 COMMENT '后台列表筛选条件',
  `regex`         varchar(255) DEFAULT NULL,
  `placeholder`   varchar(255) DEFAULT NULL,
  `tips`          varchar(255) DEFAULT NULL,
  `sort`          int(10)      NOT NULL DEFAULT 0,
  `status`        tinyint(2)   NOT NULL DEFAULT 1,
  `create_time`   datetime DEFAULT NULL,
  `update_time`   datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_field` (`model_id`,`field_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型字段定义';

-- ----------------------------
-- 3. 模型通用分类表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `fly_model_category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `model_id` bigint(20) unsigned NOT NULL,
  `father_id` bigint(20) DEFAULT '0' COMMENT '上级类目，顶级为0',
  `name` varchar(50) DEFAULT NULL,
  `keywords` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `sort` int(10) DEFAULT '0',
  `status` tinyint(2) DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_model_father` (`model_id`,`father_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型通用分类';

-- ----------------------------
-- 4. 权限节点（/api/** 不被 permission_sync 扫描，必须手工插入）
--    授权：把下面权限行关联到目标管理员组（本地超管组 272835742965968896）
-- ----------------------------
INSERT INTO fly_admin_permission (id, actionKey, controller, remark) VALUES
(900001, '/api/system/model/list',      'apiModelController', '模型列表'),
(900002, '/api/system/model/save',      'apiModelController', '新增模型'),
(900003, '/api/system/model/update',    'apiModelController', '编辑模型'),
(900004, '/api/system/model/del',       'apiModelController', '删除模型'),
(900005, '/api/system/modelField/*',    'apiModelController', '模型字段管理'),
(900006, '/api/system/modelData/*',     'apiModelController', '模型内容管理'),
(900007, '/api/system/modelCategory/*', 'apiModelController', '模型分类管理');

INSERT INTO fly_admin_group_permission_merge (group_id, permission_id) VALUES
(272835742965968896, 900001),
(272835742965968896, 900002),
(272835742965968896, 900003),
(272835742965968896, 900004),
(272835742965968896, 900005),
(272835742965968896, 900006),
(272835742965968896, 900007);

-- ----------------------------
-- 5. 内置种子模型：图片模型 / 下载模型（is_system=1）
--    动态数据表同步建好（固定列见手册 §4.3）
-- ----------------------------
INSERT INTO fly_model (id, name, code, title_label, is_system, icon, description, sort, status, create_time) VALUES
(900000000000000001, '图片模型', 'images', '图片标题', 1, 'lucide:image', '图组内容类型', 1, 1, now()),
(900000000000000002, '下载模型', 'downloads', '资源标题', 1, 'lucide:download', '软件/资源下载内容类型', 2, 1, now());

INSERT INTO fly_model_field (id, model_id, field_name, field_label, field_type, column_type, maxlength, options, is_required, is_list, is_search, is_filter, sort, status, create_time) VALUES
(900000000000010101, 900000000000000001, 'images',     '图组',     'images',   'json',                 NULL, NULL, 1, 0, 1, 0, 1, 1, now()),
(900000000000010102, 900000000000000001, 'shoot_time', '拍摄时间', 'datetime', 'datetime',             NULL, NULL, 0, 1, 0, 0, 2, 1, now()),
(900000000000010103, 900000000000000001, 'location',   '拍摄地点', 'region',   'varchar(64)',          NULL, NULL, 0, 1, 0, 1, 3, 1, now()),
(900000000000010104, 900000000000000001, 'camera',     '相机',     'input',    'varchar(255)',         255,  NULL, 0, 1, 0, 0, 4, 1, now()),
(900000000000020101, 900000000000000002, 'file',       '附件',     'file',     'bigint(20) unsigned',  NULL, NULL, 1, 0, 1, 0, 1, 1, now()),
(900000000000020102, 900000000000000002, 'file_size',  '文件大小', 'number',   'bigint(20)',           NULL, NULL, 0, 1, 0, 0, 2, 1, now()),
(900000000000020103, 900000000000000002, 'run_env',    '运行环境', 'textarea', 'text',                 NULL, NULL, 0, 0, 0, 0, 3, 1, now()),
(900000000000020104, 900000000000000002, 'demo_url',   '演示地址', 'input',    'varchar(255)',         255,  NULL, 0, 1, 0, 0, 4, 1, now()),
(900000000000020105, 900000000000000002, 'os_require', '系统要求', 'select',   'varchar(64)',          NULL, '["Windows","Linux","macOS"]', 0, 1, 0, 1, 5, 1, now());

-- 内置模型动态数据表（固定列模板见手册 §4.3）
CREATE TABLE IF NOT EXISTS `fly_cmodel_900000000000000001` (
  `id` bigint(20) unsigned NOT NULL COMMENT '雪花ID',
  `short_url` varchar(10) NOT NULL COMMENT '短链接',
  `user_id` bigint(20) unsigned DEFAULT NULL COMMENT '发布用户',
  `category_id` bigint(20) DEFAULT '0' COMMENT '分类id',
  `title` varchar(250) DEFAULT NULL COMMENT '标题',
  `content` longtext COMMENT '正文',
  `keywords` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `thumbnail` bigint(20) unsigned DEFAULT NULL COMMENT '封面图(fly_images.id)',
  `recommend` int(5) DEFAULT '0' COMMENT '推荐权重',
  `count_view` int(11) NOT NULL DEFAULT '0' COMMENT '浏览数',
  `count_comment` int(11) NOT NULL DEFAULT '0' COMMENT '评论数',
  `status` tinyint(2) DEFAULT '0' COMMENT '0未审核 1正常 2未通过 3删除',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `images` json DEFAULT NULL,
  `shoot_time` datetime DEFAULT NULL,
  `location` varchar(64) DEFAULT NULL,
  `camera` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`,`short_url`),
  KEY `idx_category` (`category_id`,`status`),
  KEY `idx_short_url` (`short_url`),
  KEY `idx_user` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片模型数据表';

CREATE TABLE IF NOT EXISTS `fly_cmodel_900000000000000002` (
  `id` bigint(20) unsigned NOT NULL COMMENT '雪花ID',
  `short_url` varchar(10) NOT NULL COMMENT '短链接',
  `user_id` bigint(20) unsigned DEFAULT NULL COMMENT '发布用户',
  `category_id` bigint(20) DEFAULT '0' COMMENT '分类id',
  `title` varchar(250) DEFAULT NULL COMMENT '标题',
  `content` longtext COMMENT '正文',
  `keywords` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `thumbnail` bigint(20) unsigned DEFAULT NULL COMMENT '封面图(fly_images.id)',
  `recommend` int(5) DEFAULT '0' COMMENT '推荐权重',
  `count_view` int(11) NOT NULL DEFAULT '0' COMMENT '浏览数',
  `count_comment` int(11) NOT NULL DEFAULT '0' COMMENT '评论数',
  `status` tinyint(2) DEFAULT '0' COMMENT '0未审核 1正常 2未通过 3删除',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `file` bigint(20) unsigned DEFAULT NULL,
  `file_size` bigint(20) DEFAULT NULL,
  `run_env` text DEFAULT NULL,
  `demo_url` varchar(255) DEFAULT NULL,
  `os_require` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`,`short_url`),
  KEY `idx_category` (`category_id`,`status`),
  KEY `idx_short_url` (`short_url`),
  KEY `idx_user` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='下载模型数据表';
