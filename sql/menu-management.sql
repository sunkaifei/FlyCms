-- ============================================================
-- 菜单管理改造（若依式）：fly_admin_permission 单表演进为菜单表
-- 依据：ADR D3 翻案（后台导航由菜单表驱动）+ D2（权限码=actionKey 不变）
-- 执行：mysql -u<user> -p --default-character-set=utf8mb4 flycms < menu-management.sql
-- 前置：custom-model.sql 已执行（900001-900007 存在）
-- ============================================================

-- 1. 菜单结构列（表内列名为驼峰风格，与 actionKey 一致）
ALTER TABLE `fly_admin_permission`
  ADD COLUMN `parentId` bigint(20) DEFAULT 0 COMMENT '上级菜单id，顶级为0',
  ADD COLUMN `menuType` varchar(1) DEFAULT 'F' COMMENT 'M目录 C菜单 F按钮/接口',
  ADD COLUMN `menuName` varchar(50) DEFAULT NULL COMMENT '菜单显示名',
  ADD COLUMN `path` varchar(100) DEFAULT NULL COMMENT '前端路由路径（M/C）',
  ADD COLUMN `component` varchar(100) DEFAULT NULL COMMENT '组件路径（C，相对views）',
  ADD COLUMN `icon` varchar(64) DEFAULT NULL COMMENT '菜单图标',
  ADD COLUMN `sort` int(10) DEFAULT 0 COMMENT '排序',
  ADD COLUMN `visible` tinyint(1) DEFAULT 1 COMMENT '1显示 0隐藏（隐藏仅注册路由）';

-- 2. 目录节点（actionKey 空串 = 登录即可见；M 目录可见性由子节点推导）
INSERT INTO fly_admin_permission (id, actionKey, controller, menuType, menuName, path, component, icon, sort, visible, parentId) VALUES
(900100, '', '', 'M', '概览', '/dashboard', NULL, 'lucide:home', 0, 1, 0);
INSERT INTO fly_admin_permission (id, actionKey, controller, menuType, menuName, path, component, icon, sort, visible, parentId) VALUES
(900110, '', '', 'M', '系统管理', '/system', NULL, 'lucide:settings', 1, 1, 0);
INSERT INTO fly_admin_permission (id, actionKey, controller, menuType, menuName, path, component, icon, sort, visible, parentId) VALUES
(900120, '', '', 'M', '内容管理', '/modelData', NULL, 'lucide:folder-open', 2, 1, 0);
INSERT INTO fly_admin_permission (id, actionKey, controller, menuType, menuName, path, component, icon, sort, visible, parentId) VALUES
(900130, '', '', 'M', '旧后台接口', '/legacy', NULL, NULL, 99, 0, 0);
INSERT INTO fly_admin_permission (id, actionKey, controller, menuType, menuName, path, component, icon, sort, visible, parentId) VALUES
(900101, '', '', 'C', '分析页', '/dashboard/analytics', '/dashboard/analytics/index', 'lucide:area-chart', 1, 1, 900100);

-- 3. 存量同步行编排
-- 3.1 提升两个旧后台页面行为 C 菜单（复用其 actionKey，权限绑定关系随之生效）
UPDATE fly_admin_permission SET menuType='C', menuName='管理员管理', path='/system/admin',
  component='/system/admin/list', icon='lucide:user-cog', sort=1, visible=1, parentId=900110
WHERE actionKey='/system/admin/admin_list' LIMIT 1;
UPDATE fly_admin_permission SET menuType='C', menuName='角色组管理', path='/system/group',
  component='/system/group/list', icon='lucide:users', sort=2, visible=1, parentId=900110
WHERE actionKey='/system/admin/group_list' LIMIT 1;

-- 3.2 模型系统行转菜单节点
UPDATE fly_admin_permission SET menuType='C', menuName='模型管理', path='/system/model',
  component='/system/model/list', icon='lucide:box-select', sort=4, visible=1, parentId=900110
WHERE id=900001;
UPDATE fly_admin_permission SET menuType='C', menuName='字段管理', path='/system/model/field/:modelId',
  component='/system/model/field', sort=5, visible=0, parentId=900110
WHERE id=900005;
UPDATE fly_admin_permission SET menuType='F', visible=0, parentId=900001 WHERE id IN (900002,900003,900004,900006,900007);

-- 3.3 其余旧后台 /system/** 行：归入"旧后台接口"隐藏目录
-- （ALTER 后存量行 menuType 已填默认值 'F'，用排除法：排除上面已提升的 C 行）
UPDATE fly_admin_permission SET menuType='F', visible=0, parentId=900130
WHERE actionKey LIKE '/system/%' AND menuType='F' AND parentId=0;

-- 4. 菜单管理自身端点的权限行 + C 节点
INSERT INTO fly_admin_permission (id, actionKey, controller, menuType, menuName, path, component, icon, sort, visible, parentId) VALUES
(900140, '/api/system/menu/list',   'apiMenuController', 'C', '菜单管理', '/system/menu', '/system/menu/index', 'lucide:menu', 3, 1, 900110),
(900141, '/api/system/menu/save',   'apiMenuController', 'F', '新增菜单', NULL, NULL, NULL, 1, 0, 900140),
(900142, '/api/system/menu/update', 'apiMenuController', 'F', '更新菜单', NULL, NULL, NULL, 2, 0, 900140),
(900143, '/api/system/menu/del',    'apiMenuController', 'F', '删除菜单', NULL, NULL, NULL, 3, 0, 900140);

-- 5. 授权到超级管理员组（按实际组 ID 调整；M 目录不需绑定，可见性由子节点推导）
INSERT INTO fly_admin_group_permission_merge (group_id, permission_id) VALUES
(272835742965968896, 900111),
(272835742965968896, 900112),
(272835742965968896, 900140),
(272835742965968896, 900141),
(272835742965968896, 900142),
(272835742965968896, 900143);

-- 6. 存量 remark 补为 menuName（老行 remark 已有中文语义的保留）
UPDATE fly_admin_permission SET menuName = remark WHERE menuName IS NULL AND menuType='F' AND remark IS NOT NULL;
