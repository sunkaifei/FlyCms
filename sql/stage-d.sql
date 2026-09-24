-- ============================================================
-- 阶段 D：模板中心（规划 §8 阶段 D）
-- 执行：mysql -u<user> -p --default-character-set=utf8mb4 flycms < stage-d.sql
--
-- 决策：文件系统为事实源，DB 只做「登记 + 版本快照」。
-- 因此手工/FTP 改模板依旧生效（运维后门保留），同时获得：
--   D 保存前语法 parse 校验（坏模板永不上线）
--   D1 版本历史与回滚（升级毁站可从任一历史点回退）
--   D4/D5 皮肤包导入导出（未来模板市场的分发格式）
-- ============================================================

-- 1. 模板登记
CREATE TABLE IF NOT EXISTS `fly_template` (
  `id` bigint(20) NOT NULL COMMENT '雪花ID',
  `skin` varchar(50) NOT NULL DEFAULT '' COMMENT '所属皮肤目录名',
  `file_path` varchar(200) NOT NULL DEFAULT '' COMMENT '皮肤内相对路径，如 index.html',
  `update_time` datetime DEFAULT NULL,
  `editor_id` bigint(20) DEFAULT NULL COMMENT '最后编辑管理员',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_skin_file` (`skin`,`file_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板登记（文件为事实源）';

-- 2. 版本快照：历史不清理，回滚 = 把历史内容作为新版本写入
CREATE TABLE IF NOT EXISTS `fly_template_version` (
  `id` bigint(20) NOT NULL COMMENT '雪花ID',
  `template_id` bigint(20) NOT NULL COMMENT 'fly_template.id',
  `version` int(11) NOT NULL DEFAULT '1',
  `content` longtext NOT NULL COMMENT '模板全文快照',
  `remark` varchar(200) DEFAULT '' COMMENT '备注（含回滚标记）',
  `editor_id` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tpl_ver` (`template_id`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板版本快照';

-- 3. 权限（挂在模板管理 900160 下）
INSERT INTO fly_admin_permission (id, actionKey, controller, menuType, menuName, path, component, icon, sort, visible, parentId) VALUES
(900164, '/api/system/template/restore', 'apiTemplateController', 'F', '回滚版本', '', '', '', 0, 0, 900160),
(900165, '/api/system/template/preview', 'apiTemplateController', 'F', '模板试渲染', '', '', '', 0, 0, 900160),
(900166, '/api/system/tags/manual', 'apiTemplateController', 'F', '在线标签手册', '', '', '', 0, 0, 900160),
(900167, '/api/system/skin/save', 'apiTemplateController', 'F', '新建皮肤', '', '', '', 0, 0, 900160),
(900168, '/api/system/skin/delete', 'apiTemplateController', 'F', '删除皮肤', '', '', '', 0, 0, 900160),
(900169, '/api/system/skin/export', 'apiTemplateController', 'F', '导出皮肤包', '', '', '', 0, 0, 900160),
(900171, '/api/system/skin/import', 'apiTemplateController', 'F', '导入皮肤包', '', '', '', 0, 0, 900160);

INSERT INTO fly_admin_group_permission_merge (group_id, permission_id) VALUES
(272835742965968896, 900164),
(272835742965968896, 900165),
(272835742965968896, 900166),
(272835742965968896, 900167),
(272835742965968896, 900168),
(272835742965968896, 900169),
(272835742965968896, 900171);

-- 4. 阶段 E 遗留补充：碎片渲染缓存默认改为 300 秒（原表默认 0=不缓存，
--    而 §6.4 要求首页碎片位默认走缓存，两次 save 之间不必重复打库）
ALTER TABLE `fly_block` MODIFY COLUMN `cache_seconds` int(11) NOT NULL DEFAULT '300' COMMENT '渲染缓存秒数，0=不缓存';
