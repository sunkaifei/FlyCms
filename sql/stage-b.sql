-- ============================================================
-- 阶段 B 补齐：评论审核 / 附件库 / 导航管理 / 友情链接（REST 化）
-- 前置：menu-management.sql 已执行（900110 系统管理存在）
-- 执行：mysql -u<user> -p --default-character-set=utf8mb4 flycms < stage-b.sql
--
-- 说明：本阶段不新建业务表——fly_article_comment / fly_images / fly_guide / fly_links
--       均为老库既有表，之前缺的是「后台管理入口与审核能力」，本次补的是索引 + 菜单权限 + 配置键。
--       号段：900220 评论审核 / 900230 附件库 / 900240 导航 / 900250 友链
-- ============================================================

-- 1. 评论审核索引（后台按状态+时间翻页，无索引会全表扫）
ALTER TABLE `fly_article_comment` ADD INDEX `idx_status_time` (`status`, `create_time`);
ALTER TABLE `fly_article_comment` ADD INDEX `idx_article` (`article_id`);

-- 2. 附件库：孤儿图清理依赖 info_count
ALTER TABLE `fly_images` ADD INDEX `idx_info_count` (`info_count`);

-- 3. 菜单/权限（挂载在「系统管理」900110 下）
INSERT INTO fly_admin_permission (id, actionKey, controller, menuType, menuName, path, component, icon, sort, visible, parentId) VALUES
-- 评论审核
(900220, '/api/system/comment/page',   'apiCommentController', 'C', '评论审核', '/system/comment', '/system/comment/index', 'lucide:message-square', 20, 1, 900110),
(900221, '/api/system/comment/audit',  'apiCommentController', 'F', '审核评论', NULL, NULL, NULL, 1, 0, 900220),
(900222, '/api/system/comment/delete', 'apiCommentController', 'F', '删除评论', NULL, NULL, NULL, 2, 0, 900220),
(900223, '/api/system/comment/batch',  'apiCommentController', 'F', '批量操作评论', NULL, NULL, NULL, 3, 0, 900220),
-- 附件库
(900230, '/api/system/images/page',        'apiImagesController', 'C', '附件库', '/system/images', '/system/images/index', 'lucide:image', 21, 1, 900110),
(900231, '/api/system/images/orphanCount', 'apiImagesController', 'F', '查询孤儿附件', NULL, NULL, NULL, 1, 0, 900230),
(900232, '/api/system/images/deleteOrphan','apiImagesController', 'F', '清理孤儿附件', NULL, NULL, NULL, 2, 0, 900230),
-- 导航管理
(900240, '/api/system/guide/page',   'apiGuideController', 'C', '导航管理', '/system/guide', '/system/guide/index', 'lucide:menu', 22, 1, 900110),
(900241, '/api/system/guide/tree',   'apiGuideController', 'F', '导航树', NULL, NULL, NULL, 1, 0, 900240),
(900242, '/api/system/guide/get',    'apiGuideController', 'F', '导航详情', NULL, NULL, NULL, 2, 0, 900240),
(900243, '/api/system/guide/save',   'apiGuideController', 'F', '新增/修改导航', NULL, NULL, NULL, 3, 0, 900240),
(900244, '/api/system/guide/delete', 'apiGuideController', 'F', '删除导航', NULL, NULL, NULL, 4, 0, 900240),
(900245, '/api/system/guide/status', 'apiGuideController', 'F', '导航显隐', NULL, NULL, NULL, 5, 0, 900240),
-- 友情链接
(900250, '/api/system/links/page',   'apiLinksController', 'C', '友情链接', '/system/links', '/system/links/index', 'lucide:link', 23, 1, 900110),
(900251, '/api/system/links/get',    'apiLinksController', 'F', '友链详情', NULL, NULL, NULL, 1, 0, 900250),
(900252, '/api/system/links/save',   'apiLinksController', 'F', '新增/修改友链', NULL, NULL, NULL, 2, 0, 900250),
(900253, '/api/system/links/delete', 'apiLinksController', 'F', '删除友链', NULL, NULL, NULL, 3, 0, 900250),
(900254, '/api/system/links/status', 'apiLinksController', 'F', '友链显隐', NULL, NULL, NULL, 4, 0, 900250);

-- 4. 赋给超管组（group_id 取自初始化数据，若你的库不同请先 SELECT 确认）
INSERT INTO fly_admin_group_permission_merge (group_id, permission_id) VALUES
(272835742965968896, 900220), (272835742965968896, 900221), (272835742965968896, 900222), (272835742965968896, 900223),
(272835742965968896, 900230), (272835742965968896, 900231), (272835742965968896, 900232),
(272835742965968896, 900240), (272835742965968896, 900241), (272835742965968896, 900242), (272835742965968896, 900243), (272835742965968896, 900244), (272835742965968896, 900245),
(272835742965968896, 900250), (272835742965968896, 900251), (272835742965968896, 900252), (272835742965968896, 900253), (272835742965968896, 900254);

-- 5. 评论审核开关（0=免审直接显示 1=先审后显示）；缺失时后端按 1 处理
INSERT INTO `fly_config_web` (`id`, `typebase`, `keycode`, `keyvalue`, `description`, `sort`)
VALUES (900303, 0, 'fly_comment_audit', '1', '评论审核开关：0免审 1先审后显示', 000)
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`);
