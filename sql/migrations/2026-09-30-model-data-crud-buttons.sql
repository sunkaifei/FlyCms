-- ============================================================
-- 模型内容权限：F 按钮从单一「内容发布」拆分为 增删改查 四粒度
-- 2026-09-30
--
-- 背景：在线建模此前只自动注册 C 菜单(list@{mid}) + F「内容发布」(save@{mid})，
-- 写操作（新增/编辑/删除/上下架）共用一个锚点，无法只授「录入」不授「删除」。
-- 拆分后锚点口径（ApiModelController 同步注册同构节点）：
--   内容查询 list@{mid}  —— 列表/详情/表单元元数据/版本查看
--   内容新增 add@{mid}   —— 数据 save
--   内容编辑 edit@{mid}  —— update / 上下架 / 版本恢复 / 预览签发
--   内容删除 del@{mid}   —— del
-- 兼容：通用节点 900006（/api/system/modelData/*）尾星号匹配全部新锚点，
-- 超管组零影响；存量持「内容发布」的组自动延续 新增+编辑+删除 授权。
--
-- 节点 id 约定（存量 12 个模型，避免与 1~24 / 900xxx / 雪花 id 冲突）：
--   查询 = 100+模型id，编辑 = 120+模型id，删除 = 140+模型id
--   （旧「内容发布」节点原 id 不变，改名换锚点为「内容新增」）
-- ============================================================

-- 1) 旧 F「内容发布」→「内容新增」，锚点 save@{mid} → add@{mid}（保留原授权行）
UPDATE `fly_admin_permission` p
JOIN `fly_model` m
  ON p.`actionKey` = CONCAT('/api/system/modelData/save@', m.`id`)
SET p.`menuName`  = '内容新增',
    p.`actionKey` = CONCAT('/api/system/modelData/add@', m.`id`),
    p.`sort`      = 2
WHERE p.`menuType` = 'F';

-- 2) 补建 内容查询 / 内容编辑 / 内容删除 三类 F 按钮（父节点 = 各模型的 C 菜单）
INSERT INTO `fly_admin_permission`
  (`id`, `parentId`, `menuType`, `menuName`, `actionKey`, `sort`, `visible`)
SELECT 100 + m.`id`, c.`id`, 'F', '内容查询',
       CONCAT('/api/system/modelData/list@', m.`id`), 1, 1
FROM `fly_model` m
JOIN `fly_admin_permission` c
  ON c.`actionKey` = CONCAT('/api/system/modelData/list@', m.`id`)
 AND c.`menuType` = 'C'
WHERE NOT EXISTS (
  SELECT 1 FROM `fly_admin_permission` x
  WHERE x.`actionKey` = CONCAT('/api/system/modelData/list@', m.`id`)
    AND x.`menuType` = 'F');

INSERT INTO `fly_admin_permission`
  (`id`, `parentId`, `menuType`, `menuName`, `actionKey`, `sort`, `visible`)
SELECT 120 + m.`id`, c.`id`, 'F', '内容编辑',
       CONCAT('/api/system/modelData/edit@', m.`id`), 3, 1
FROM `fly_model` m
JOIN `fly_admin_permission` c
  ON c.`actionKey` = CONCAT('/api/system/modelData/list@', m.`id`)
 AND c.`menuType` = 'C'
WHERE NOT EXISTS (
  SELECT 1 FROM `fly_admin_permission` x
  WHERE x.`actionKey` = CONCAT('/api/system/modelData/edit@', m.`id`));

INSERT INTO `fly_admin_permission`
  (`id`, `parentId`, `menuType`, `menuName`, `actionKey`, `sort`, `visible`)
SELECT 140 + m.`id`, c.`id`, 'F', '内容删除',
       CONCAT('/api/system/modelData/del@', m.`id`), 4, 1
FROM `fly_model` m
JOIN `fly_admin_permission` c
  ON c.`actionKey` = CONCAT('/api/system/modelData/list@', m.`id`)
 AND c.`menuType` = 'C'
WHERE NOT EXISTS (
  SELECT 1 FROM `fly_admin_permission` x
  WHERE x.`actionKey` = CONCAT('/api/system/modelData/del@', m.`id`));

-- 3) 授权延续：凡持「内容新增」（原内容发布）的组，同步授予 查询/编辑/删除
INSERT IGNORE INTO `fly_admin_group_permission_merge` (`group_id`, `permission_id`)
SELECT gp.`group_id`, q.`id`
FROM `fly_admin_group_permission_merge` gp
JOIN `fly_admin_permission` a ON a.`id` = gp.`permission_id`
JOIN `fly_model` m
  ON a.`actionKey` = CONCAT('/api/system/modelData/add@', m.`id`)
JOIN `fly_admin_permission` q
  ON q.`actionKey` IN (
      CONCAT('/api/system/modelData/list@', m.`id`),
      CONCAT('/api/system/modelData/edit@', m.`id`),
      CONCAT('/api/system/modelData/del@', m.`id`))
WHERE NOT EXISTS (
  SELECT 1 FROM `fly_admin_group_permission_merge` g2
  WHERE g2.`group_id` = gp.`group_id` AND g2.`permission_id` = q.`id`);
