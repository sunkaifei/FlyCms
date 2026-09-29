-- =====================================================================
-- 2026-09-28 模型内容菜单/权限节点：按模型粒度授权
--
-- 背景：/modelData（内容，id=900120）下的模型管理入口原先由 /api/menu/all
--       动态追加，无权限锚点——任何能看到「内容」目录的管理员都能看到全部
--       模型菜单，且模型数据 API 只靠通用节点 900006（/api/system/modelData/*）
--       一刀切放行，无法按模型分配给不同角色组。
--
-- 方案：每个启用模型注册 2 个节点（id 从 1 起，与 900xxx 旧段错开）：
--       C  {模型名}管理   path=/modelData/{code}  actionKey=/api/system/modelData/list@{modelId}
--       F  内容发布                              actionKey=/api/system/modelData/save@{modelId}
--       requirePermission 按 /api/system/modelData/{list|save}@{modelId} 校验；
--       通用节点 900006 的尾星号 /api/system/modelData/* 仍能匹配
--       /api/system/modelData/list@6（CheckUrlUtils 尾 * 通配），旧授权不受影响。
--       在线建模新模型由 ApiModelController.modelSave 自动注册同构节点（雪花 id）。
--
-- 授权：新节点默认只授「超级管理员」组（与快照既有约定一致），
--       其他角色组由管理员在「角色组管理」按模型勾选。
--
-- 幂等：INSERT IGNORE（id 为主键），可重复执行。
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p flycms < 本文件
--       中文必须带 --default-character-set=utf8mb4，否则真实乱码。
-- =====================================================================

-- 1) 每模型 C 菜单 + F 按钮（C id = 模型 id，F id = 模型 id + 12）
INSERT IGNORE INTO `fly_admin_permission`
(`id`, `actionKey`, `controller`, `remark`, `parentId`, `menuType`, `menuName`, `path`, `component`, `icon`, `sort`, `visible`) VALUES
(1,  '/api/system/modelData/list@1',  'apiModelController', '图片模型：内容管理菜单', 900120, 'C', '图片模型管理', '/modelData/images',        '/system/modeldata/list', 'lucide:image',         '1',  'true'),
(13, '/api/system/modelData/save@1',  'apiModelController', '图片模型：新增/编辑/删除/状态', 1, 'F', '内容发布', NULL, NULL, NULL, '1', '1'),
(2,  '/api/system/modelData/list@2',  'apiModelController', '下载模型：内容管理菜单', 900120, 'C', '下载模型管理', '/modelData/downloads',     '/system/modeldata/list', 'lucide:download',      '2',  'true'),
(14, '/api/system/modelData/save@2',  'apiModelController', '下载模型：新增/编辑/删除/状态', 2, 'F', '内容发布', NULL, NULL, NULL, '1', '1'),
(3,  '/api/system/modelData/list@3',  'apiModelController', '文章模型：内容管理菜单', 900120, 'C', '文章模型管理', '/modelData/articles',      '/system/modeldata/list', 'lucide:file-text',     '3',  'true'),
(15, '/api/system/modelData/save@3',  'apiModelController', '文章模型：新增/编辑/删除/状态', 3, 'F', '内容发布', NULL, NULL, NULL, '1', '1'),
(4,  '/api/system/modelData/list@4',  'apiModelController', '测试模型：内容管理菜单', 900120, 'C', '测试模型管理', '/modelData/testdemo',      '/system/modeldata/list', 'lucide:file-text',     '4',  'true'),
(16, '/api/system/modelData/save@4',  'apiModelController', '测试模型：新增/编辑/删除/状态', 4, 'F', '内容发布', NULL, NULL, NULL, '1', '1'),
(5,  '/api/system/modelData/list@5',  'apiModelController', '友情链接：内容管理菜单', 900120, 'C', '友情链接管理', '/modelData/links',         '/system/modeldata/list', 'lucide:link',          '5',  'true'),
(17, '/api/system/modelData/save@5',  'apiModelController', '友情链接：新增/编辑/删除/状态', 5, 'F', '内容发布', NULL, NULL, NULL, '1', '1'),
(6,  '/api/system/modelData/list@6',  'apiModelController', '导航：内容管理菜单',     900120, 'C', '导航管理',     '/modelData/guides',        '/system/modeldata/list', 'lucide:compass',       '6',  'true'),
(18, '/api/system/modelData/save@6',  'apiModelController', '导航：新增/编辑/删除/状态',     6, 'F', '内容发布', NULL, NULL, NULL, '1', '1'),
(7,  '/api/system/modelData/list@7',  'apiModelController', '公告：内容管理菜单',     900120, 'C', '公告管理',     '/modelData/announcements', '/system/modeldata/list', 'lucide:megaphone',     '7',  'true'),
(19, '/api/system/modelData/save@7',  'apiModelController', '公告：新增/编辑/删除/状态',     7, 'F', '内容发布', NULL, NULL, NULL, '1', '1'),
(8,  '/api/system/modelData/list@8',  'apiModelController', '留言通知：内容管理菜单', 900120, 'C', '留言通知管理', '/modelData/messages',      '/system/modeldata/list', 'lucide:mail',          '8',  'true'),
(20, '/api/system/modelData/save@8',  'apiModelController', '留言通知：新增/编辑/删除/状态', 8, 'F', '内容发布', NULL, NULL, NULL, '1', '1'),
(9,  '/api/system/modelData/list@9',  'apiModelController', '话题：内容管理菜单',     900120, 'C', '话题管理',     '/modelData/topics',        '/system/modeldata/list', 'lucide:hash',          '9',  'true'),
(21, '/api/system/modelData/save@9',  'apiModelController', '话题：新增/编辑/删除/状态',     9, 'F', '内容发布', NULL, NULL, NULL, '1', '1'),
(10, '/api/system/modelData/list@10', 'apiModelController', '分享：内容管理菜单',     900120, 'C', '分享管理',     '/modelData/shares',        '/system/modeldata/list', 'lucide:share-2',       '10', '1'),
(22, '/api/system/modelData/save@10', 'apiModelController', '分享：新增/编辑/删除/状态',    10, 'F', '内容发布', NULL, NULL, NULL, '1', '1'),
(11, '/api/system/modelData/list@11', 'apiModelController', '问答：内容管理菜单',     900120, 'C', '问答管理',     '/modelData/questions',     '/system/modeldata/list', 'lucide:help-circle',   '11', '1'),
(23, '/api/system/modelData/save@11', 'apiModelController', '问答：新增/编辑/删除/状态',    11, 'F', '内容发布', NULL, NULL, NULL, '1', '1'),
(12, '/api/system/modelData/list@12', 'apiModelController', '回答：内容管理菜单',     900120, 'C', '回答管理',     '/modelData/answers',       '/system/modeldata/list', 'lucide:message-circle','12', '1'),
(24, '/api/system/modelData/save@12', 'apiModelController', '回答：新增/编辑/删除/状态',    12, 'F', '内容发布', NULL, NULL, NULL, '1', '1');

-- 2b) visible 用数字 1（MySQL 把 'true' 转 tinyint 会得到 0，导致菜单被 hideInMenu）
UPDATE `fly_admin_permission` SET `visible` = 1 WHERE `id` BETWEEN 1 AND 24;

-- 2) 授权：授予当前持有通用节点 900006（/api/system/modelData/*）的角色组，
--    保证这些组的既有授权口径平滑过渡到按模型节点。
INSERT IGNORE INTO `fly_admin_group_permission_merge` (`group_id`, `permission_id`)
SELECT m.group_id, p.id
FROM (SELECT DISTINCT group_id FROM `fly_admin_group_permission_merge` WHERE permission_id = 900006) m
CROSS JOIN `fly_admin_permission` p
WHERE p.id BETWEEN 1 AND 24;
