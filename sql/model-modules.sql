-- ============================================================
-- 内容模块完善：文章模型 + 图片/下载模型帝国式字段补全 + 一级目录改名"内容"
-- 参照：DedeCMS 文章模型（来源/作者/跳转）+ 帝国CMS 下载模型（授权/语言）+ 帝国图集（作者）
-- 前置：custom-model.sql 已执行
-- 执行：mysql -u<user> -p --default-character-set=utf8mb4 flycms < model-modules.sql
-- ============================================================

-- 1. 一级目录"内容管理"改名"内容"（帝国CMS 式一级菜单）
UPDATE fly_admin_permission SET menuName='内容' WHERE id=900120;

-- 2. 文章模型（code=articles，避免与旧 /article 路由段混淆）
INSERT INTO fly_model (id, name, code, title_label, is_system, icon, description, sort, status, create_time) VALUES
(900000000000000003, '文章模型', 'articles', '文章标题', 1, 'lucide:file-text', '通用文章内容类型', 3, 1, now());

INSERT INTO fly_model_field (id, model_id, field_name, field_label, field_type, column_type, maxlength, options, is_required, is_list, is_search, is_filter, sort, status, create_time) VALUES
(900000000000030101, 900000000000000003, 'source',      '内容来源', 'input', 'varchar(64)',  64,  NULL, 0, 1, 0, 0, 1, 1, now()),
(900000000000030102, 900000000000000003, 'author',      '作者',     'input', 'varchar(64)',  64,  NULL, 0, 1, 0, 0, 2, 1, now()),
(900000000000030103, 900000000000000003, 'tags',        '标签',     'input', 'varchar(255)', 255, NULL, 0, 0, 1, 0, 3, 1, now()),
(900000000000030104, 900000000000000003, 'redirecturl', '跳转地址', 'input', 'varchar(255)', 255, NULL, 0, 0, 0, 0, 4, 1, now());

CREATE TABLE IF NOT EXISTS `fly_cmodel_900000000000000003` (
  `id` bigint(20) unsigned NOT NULL COMMENT '雪花ID',
  `short_url` varchar(10) NOT NULL COMMENT '短链接',
  `user_id` bigint(20) unsigned DEFAULT NULL COMMENT '发布用户',
  `category_id` bigint(20) DEFAULT '0' COMMENT '分类id',
  `title` varchar(250) DEFAULT NULL COMMENT '标题',
  `content` longtext COMMENT '正文(editor字段通道)',
  `keywords` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `thumbnail` bigint(20) unsigned DEFAULT NULL COMMENT '封面图(fly_images.id)',
  `recommend` int(5) DEFAULT '0' COMMENT '推荐权重',
  `count_view` int(11) NOT NULL DEFAULT '0' COMMENT '浏览数',
  `count_comment` int(11) NOT NULL DEFAULT '0' COMMENT '评论数',
  `status` tinyint(2) DEFAULT '0' COMMENT '0未审核 1正常 2未通过 3删除',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `source` varchar(64) DEFAULT NULL,
  `author` varchar(64) DEFAULT NULL,
  `tags` varchar(255) DEFAULT NULL,
  `redirecturl` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`,`short_url`),
  KEY `idx_category` (`category_id`,`status`),
  KEY `idx_short_url` (`short_url`),
  KEY `idx_user` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章模型数据表';

-- 3. 图片模型补字段（帝国图集：作者）
INSERT INTO fly_model_field (id, model_id, field_name, field_label, field_type, column_type, maxlength, options, is_required, is_list, is_search, is_filter, sort, status, create_time) VALUES
(900000000000010105, 900000000000000001, 'author', '作者', 'input', 'varchar(64)', 64, NULL, 0, 1, 0, 0, 5, 1, now());
ALTER TABLE `fly_cmodel_900000000000000001` ADD COLUMN `author` varchar(64) DEFAULT NULL COMMENT '作者';

-- 4. 下载模型补字段（帝国下载：授权方式/程序语言/下载积分）
INSERT INTO fly_model_field (id, model_id, field_name, field_label, field_type, column_type, maxlength, options, is_required, is_list, is_search, is_filter, sort, status, create_time) VALUES
(900000000000020106, 900000000000000002, 'auth',     '授权方式', 'select', 'varchar(64)', NULL, '["免费软件","开源软件","共享软件","商业软件"]', 0, 1, 0, 1, 6, 1, now()),
(900000000000020107, 900000000000000002, 'language', '程序语言', 'select', 'varchar(64)', NULL, '["简体中文","英文","多语言"]', 0, 1, 0, 1, 7, 1, now()),
(900000000000020108, 900000000000000002, 'score',    '下载积分', 'number', 'bigint(20)',  NULL, NULL, 0, 1, 0, 0, 8, 1, now());
ALTER TABLE `fly_cmodel_900000000000000002` ADD COLUMN `auth` varchar(64) DEFAULT NULL COMMENT '授权方式';
ALTER TABLE `fly_cmodel_900000000000000002` ADD COLUMN `language` varchar(64) DEFAULT NULL COMMENT '程序语言';
ALTER TABLE `fly_cmodel_900000000000000002` ADD COLUMN `score` bigint(20) DEFAULT NULL COMMENT '下载积分';
