-- =====================================================================
-- 2026-10-01 站点导航管理（fly_guide 升级 + 菜单授权补齐）
--
-- 背景：fly_guide 是遗留极简表（name/link/sort/status，MyISAM，无树形无绑定），
--       且旧后台 guideAdminController 已随旧后台下线、前台 fly_guidepage 标签
--       从未在现行代码中存在——该表长期是死数据。本次落地完整导航管理：
--       vben 端 /system/guide 页面 + ApiGuideController + 前台 fly_guide 标签。
--       导航项三种来源：0=自定义链接（link 直存）1=栏目（ref_id=栏目id，
--       渲染 /{channelDir}/）2=模型分类（ref_id=分类id，渲染 /{modelCode}/c{id}）。
--
-- 内容：
--   1) fly_guide 重建：father_id/type/ref_id/target 树形+绑定结构（InnoDB+utf8mb4）；
--      旧演示数据（产品官网/后台管理/商家管理，从未被任何页面渲染过）替换为「首页」
--   2) 菜单节点 900240~900245 兜底补插（幂等；正常建库已含）
--   3) 授权：900240~900245 全部授「超级管理员」组（1），其他组按需勾选
--
-- 幂等：DROP+CREATE / INSERT IGNORE，全部可重复执行。
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p flycms < 本文件
--       中文必须带 --default-character-set=utf8mb4，否则真实乱码。
-- 全新安装：无需执行，sql/flycms_*.sql 基线已含最终结构。
-- =====================================================================

-- 1) fly_guide 重建（旧表仅含从未被现行代码渲染过的演示数据，直接换种子；
--    线上库已无此表时等价新建）。DROP+CREATE 幂等，可重复执行。
DROP TABLE IF EXISTS `fly_guide`;
CREATE TABLE `fly_guide` (
  `id` bigint(20) unsigned NOT NULL COMMENT '雪花ID',
  `father_id` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '上级导航项，0=顶级',
  `name` varchar(255) NOT NULL COMMENT '导航名字',
  `type` tinyint(2) NOT NULL DEFAULT '0' COMMENT '来源类型：0自定义链接 1栏目 2模型分类',
  `ref_id` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '绑定对象id：栏目id/模型分类id',
  `link` varchar(300) NOT NULL DEFAULT '' COMMENT '自定义链接（type=0 时生效）',
  `target` varchar(10) NOT NULL DEFAULT '' COMMENT '打开方式：_blank 新窗口，空为当前窗口',
  `sort` int(5) NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint(2) DEFAULT '1' COMMENT '显示状态：1导航显示，0不显示',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_father_sort` (`father_id`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站点导航（导航管理页 /system/guide 维护，前台 fly_guide 标签渲染）';

INSERT IGNORE INTO `fly_guide` (`id`, `father_id`, `name`, `type`, `ref_id`, `link`, `target`, `sort`, `status`) VALUES
('920000000000000001', '0', '首页', '0', '0', '/', '', '1', '1');

-- 2) 菜单节点兜底（与基线一致；已存在则跳过）
INSERT IGNORE INTO `fly_admin_permission`
(`id`, `actionKey`, `controller`, `remark`, `parentId`, `menuType`, `menuName`, `path`, `component`, `icon`, `sort`, `visible`) VALUES
(900240, '/api/system/guide/page',   'apiGuideController', NULL, '900110', 'C', '导航管理', '/system/guide', '/system/guide/index', 'lucide:menu', '22', 'true'),
(900241, '/api/system/guide/tree',   'apiGuideController', NULL, '900240', 'F', '导航树', NULL, NULL, NULL, '1', 'false'),
(900242, '/api/system/guide/get',    'apiGuideController', NULL, '900240', 'F', '导航详情', NULL, NULL, NULL, '2', 'false'),
(900243, '/api/system/guide/save',   'apiGuideController', NULL, '900240', 'F', '新增/修改导航', NULL, NULL, NULL, '3', 'false'),
(900244, '/api/system/guide/delete', 'apiGuideController', NULL, '900240', 'F', '删除导航', NULL, NULL, NULL, '4', 'false'),
(900245, '/api/system/guide/status', 'apiGuideController', NULL, '900240', 'F', '导航显隐', NULL, NULL, NULL, '5', 'false');

-- 3) 授权：全部授「超级管理员」组（1），其他组在角色组管理按需勾选
INSERT IGNORE INTO `fly_admin_group_permission_merge` (`group_id`, `permission_id`)
SELECT 1, p.id
FROM `fly_admin_permission` p
WHERE p.id BETWEEN 900240 AND 900245;
