-- ============================================================
-- M 阶段（G12 内容版本 / G14 字段级权限）（2026-09-29）
-- 对照《主流CMS对标与全项目优化开发方案.md》§7.8 / §7.10
-- ============================================================

-- ---------- G12 内容版本快照（DB 为事实源，快照即版本；恢复=写入新版本不丢历史） ----------
CREATE TABLE IF NOT EXISTS `fly_content_version` (
  `id` bigint(20) NOT NULL COMMENT '雪花ID',
  `target_model` varchar(32) NOT NULL COMMENT '目标模型code（fly_cmodel_{code}）',
  `target_id` bigint(20) NOT NULL COMMENT '目标内容id',
  `version` int(11) NOT NULL COMMENT '版本号，同一内容递增',
  `content_json` mediumtext COMMENT '整行快照（含自定义字段）',
  `status` tinyint(2) DEFAULT '0' COMMENT '快照时主行状态（0待审 1发布 2未通过…）',
  `editor_id` bigint(20) DEFAULT NULL COMMENT '操作管理员',
  `remark` varchar(200) DEFAULT '' COMMENT '备注（恢复时记录来源版本）',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_target_ver` (`target_model`,`target_id`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容版本快照（M/G12）';

-- ---------- G14 字段级权限说明 ----------
-- 约定：字段锁 = fly_admin_permission 中存在精确 actionKey 行
--   `/api/system/modelData/field/{modelId}/{fieldName}`
-- 存在该行 → 未被授权（角色组合并表无匹配授权）的管理员：表单/列表/详情剔除该字段，写入直接拒绝；
-- 不存在该行 → 字段对所有有模型访问权的管理员开放（向后兼容，默认开放）。
-- 授权方式与普通节点一致：角色组勾选（支持尾星号通配，如 /api/system/modelData/field/3/*）。

-- ---------- 前台投稿通道（U3 收口）：用户权限行（全组授权） ----------
INSERT INTO fly_user_permission (id, actionKey, controller, remark)
VALUES (2760100000000003001, '/ucenter/submit/*', 'submitController', '前台投稿（万能模型，U3 重建）');
INSERT INTO fly_user_group_permission_merge (group_id, permission_id)
SELECT g.id, 2760100000000003001 FROM fly_user_group g;

-- ---------- 授权校验方式 ----------
-- G14 字段锁：INSERT 一行 actionKey=/api/system/modelData/field/{modelId}/{fieldName} 即生效（行存在=锁定），
-- 再到「角色组→权限」勾选授权给可见角色（支持尾星号通配）。
-- 改动 fly_admin_permission 后调 POST /api/system/permission/sync 清权限缓存。

-- ---------- G15 字段组库（Component，复制式复用：字段组定义存 JSON，可应用到任意模型） ----------
CREATE TABLE IF NOT EXISTS `fly_component` (
  `id` bigint(20) NOT NULL COMMENT '雪花ID',
  `code` varchar(32) NOT NULL COMMENT '字段组标识（应用时作为 group 字段名）',
  `name` varchar(64) NOT NULL COMMENT '字段组名称',
  `remark` varchar(255) DEFAULT '' COMMENT '用途说明',
  `fields_json` text COMMENT '子字段定义数组（与模型导出 fields 同构）',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字段组库（G15，复制式 Component）';

-- ---------- G17 自动化规则（Directus Flows 简化版：规则表+内置动作，非可视化编排） ----------
CREATE TABLE IF NOT EXISTS `fly_automation_rule` (
  `id` bigint(20) NOT NULL COMMENT '雪花ID',
  `rule_name` varchar(60) NOT NULL,
  `event` varchar(40) NOT NULL COMMENT '匹配 ContentChangedEvent.action：insert/update/delete/status/comment_add/comment_audit/comment_delete',
  `model_code` varchar(32) DEFAULT NULL COMMENT '限定模型 code，留空=全部模型',
  `conditions` varchar(1000) DEFAULT NULL COMMENT 'JSON [{field,op,value}]，op白名单 eq/neq/gt/lt/contains；对首行内容求值，留空=无条件',
  `actions` varchar(1000) NOT NULL COMMENT 'JSON [{type:"webhook",url:...},{type:"log"}]',
  `status` tinyint(2) DEFAULT '1' COMMENT '1启用 0停用',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_event` (`event`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动化规则（G17）';

-- 自动化管理端权限行（授权超管组）
INSERT INTO fly_admin_permission (id, actionKey, controller, remark, menuType)
VALUES (2760100000000004001, '/api/system/automation/list', 'apiAutomationController', 'G17自动化：规则列表', 'F'),
       (2760100000000004002, '/api/system/automation/save', 'apiAutomationController', 'G17自动化：规则保存', 'F'),
       (2760100000000004003, '/api/system/automation/del', 'apiAutomationController', 'G17自动化：规则删除', 'F');
INSERT INTO fly_admin_group_permission_merge (group_id, permission_id)
SELECT g.id, p.permission_id FROM (SELECT 2760100000000004001 permission_id UNION SELECT 2760100000000004002 UNION SELECT 2760100000000004003) p
CROSS JOIN (SELECT DISTINCT group_id FROM fly_admin_group_permission_merge) g;

-- E9 模型级评论开关 + V2 前台投稿开关
ALTER TABLE `fly_model` ADD COLUMN `enable_comment` tinyint(1) DEFAULT '1' COMMENT 'E9 模型级评论开关 0关 1开';
ALTER TABLE `fly_model` ADD COLUMN `enable_submit` tinyint(1) DEFAULT '1' COMMENT 'V2 前台投稿开关 0关 1开';
-- E6 行内公式
ALTER TABLE `fly_model_field` ADD COLUMN `formula` varchar(255) DEFAULT NULL COMMENT 'E6 行内公式（FORMULA 类型）';

-- ---------- G21 语义搜索（P 阶段可自研部分）：向量存 MySQL（小站规模免向量库） ----------
CREATE TABLE IF NOT EXISTS `fly_embedding` (
  `id` bigint(20) NOT NULL COMMENT '雪花ID',
  `target_model` varchar(32) NOT NULL COMMENT '目标模型code',
  `target_id` bigint(20) NOT NULL COMMENT '目标内容id',
  `chunk` int(11) DEFAULT '0' COMMENT '分块序号（长文按段落切分）',
  `vector_json` mediumtext COMMENT '向量（JSON 浮点数组）',
  `embed_model` varchar(64) DEFAULT NULL COMMENT '生成向量所用模型（换模型后需重建）',
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_target_chunk` (`target_model`,`target_id`,`chunk`),
  KEY `idx_model` (`target_model`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容向量（G21 语义搜索）';

-- G22 AI 助手配置键（值由部署方填写；未配置时 AI 功能整体停用且不影响其他功能）
INSERT INTO fly_config_web (typebase, keycode, keyvalue, description)
VALUES (0, 'fly_ai_base_url', '', 'G22 AI：OpenAI 兼容服务地址（如 https://api.deepseek.com/v1）'),
       (0, 'fly_ai_api_key', '', 'G22 AI：API Key（留空=AI 功能停用）'),
       (0, 'fly_ai_model', '', 'G22 AI：对话模型名'),
       (0, 'fly_ai_embed_model', '', 'G21 语义搜索：向量模型名');

-- ---------- G23 MCP server 接入令牌（留空 = MCP 端点 404 关闭；填值后用 Authorization: Bearer <token> 访问 /mcp） ----------
INSERT INTO fly_config_web (typebase, keycode, keyvalue, description)
VALUES (0, 'fly_mcp_token', '', 'G23 MCP：接入令牌（留空=MCP 端点关闭）');

-- ---------- E7 收藏平台化：fly_favorite 支持任意模型内容（model_code 空=遗留文章收藏） ----------
ALTER TABLE `fly_favorite` ADD COLUMN `model_code` varchar(32) DEFAULT NULL COMMENT 'E7 目标模型code（NULL=遗留文章收藏）';
