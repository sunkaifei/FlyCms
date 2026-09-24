-- ============================================================
-- FlyCms 模板引擎重建设表与权限（规划 §5.3 / §12.1 / P5 / P7）
-- 对应开发文档：backend/doc/前台模板引擎重新设计开发方案.md
-- 执行前请确认数据库为 flycms，建议在低峰期执行。
-- ============================================================

USE flycms;

-- ---------- 1. 主题注册表（扫描结果登记/展示，解析仍以磁盘 theme.json 为事实源） ----------
CREATE TABLE IF NOT EXISTS `fly_theme` (
  `code`         VARCHAR(50)  NOT NULL COMMENT '主题目录名',
  `name`         VARCHAR(100) NOT NULL COMMENT '主题展示名',
  `version`      VARCHAR(20)           DEFAULT NULL COMMENT '版本',
  `author`       VARCHAR(50)           DEFAULT NULL COMMENT '作者',
  `parent_code`  VARCHAR(50)           DEFAULT NULL COMMENT '父主题 code，空=独立主题',
  `thumbnail`    VARCHAR(200)          DEFAULT NULL COMMENT '缩略图',
  `description`  VARCHAR(500)          DEFAULT NULL COMMENT '描述',
  `device`       VARCHAR(10)           DEFAULT 'pc' COMMENT '设备：pc/mobile',
  `status`       TINYINT               DEFAULT 1 COMMENT '状态 1启用 0停用',
  `is_current`   TINYINT               DEFAULT 0 COMMENT '是否当前使用主题',
  `config_json`  TEXT                  DEFAULT NULL COMMENT 'theme.json 全文',
  `create_time`  DATETIME              DEFAULT NULL COMMENT '登记时间',
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主题注册表';

-- ---------- 2. 模板指派（DB 覆盖层，只存"谁用哪个模板"的关系，不存模板正文） ----------
CREATE TABLE IF NOT EXISTS `fly_template_assign` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `target_type` VARCHAR(20) NOT NULL COMMENT 'CONTENT/CATEGORY/CHANNEL/MODEL/SITE',
  `target_id`   VARCHAR(64) NOT NULL COMMENT '内容ID/分类ID/栏目目录/模型code/site',
  `page_type`   VARCHAR(20) NOT NULL COMMENT 'LIST/DETAIL/INDEX',
  `template`    VARCHAR(200) NOT NULL COMMENT '模板相对路径，如 detail-product.html',
  `create_time` DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_target` (`target_type`, `target_id`, `page_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板指派表';

-- ---------- 3. 区域-区块编排（V2 可视化用，§8.4） ----------
CREATE TABLE IF NOT EXISTS `fly_area_block` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `theme_code`  VARCHAR(50)  NOT NULL COMMENT '主题目录名',
  `area_name`   VARCHAR(50)  NOT NULL COMMENT '区域名，对应 theme.json regions',
  `block_type`  VARCHAR(20)  NOT NULL COMMENT 'BLOCK/TAG/HTML',
  `block_ref`   VARCHAR(200) NOT NULL COMMENT '碎片key/标签代码/HTML',
  `sort`        INT          DEFAULT 0 COMMENT '排序',
  `status`      TINYINT      DEFAULT 1 COMMENT '状态 1启用 0停用',
  PRIMARY KEY (`id`),
  KEY `idx_theme_area` (`theme_code`, `area_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域区块编排表';

-- ---------- 4. 权限节点（主题市场 + 模板指派） ----------
-- 沿用模板/皮肤类权限的 900xxx 段，parentId 与其它模板权限一致（900160），controller=apiTemplateController
-- 用 INSERT IGNORE：id 为主键，重复执行自动跳过，安全幂等。
INSERT IGNORE INTO `fly_admin_permission` (`id`, `actionKey`, `controller`, `remark`, `parentId`, `menuType`, `menuName`) VALUES
  (900180, '/api/system/theme/list',        'apiTemplateController', '主题市场：列表',   900160, 'F', '主题列表'),
  (900181, '/api/system/theme/check',       'apiTemplateController', '主题市场：预检',   900160, 'F', '兼容性预检'),
  (900182, '/api/system/theme/enable',      'apiTemplateController', '主题市场：启用',   900160, 'F', '启用主题'),
  (900183, '/api/system/theme/rollback',    'apiTemplateController', '主题市场：回滚',   900160, 'F', '回滚主题'),
  (900184, '/api/system/theme/createChild', 'apiTemplateController', '主题市场：子主题', 900160, 'F', '创建子主题'),
  (900185, '/api/system/theme/preview',     'apiTemplateController', '主题市场：预览',   900160, 'F', '主题预览'),
  (900186, '/api/system/template/assign',   'apiTemplateController', '模板指派：保存',   900160, 'F', '模板指派'),
  (900187, '/api/system/template/unassign', 'apiTemplateController', '模板指派：取消',  900160, 'F', '取消指派');

-- ---------- 5. 授权给超级管理员组（其余角色按需分配） ----------
-- 超级管理员组 id：272835742965968896（若实际部署时组 id 不同，请替换为对应值）
INSERT IGNORE INTO `fly_admin_group_permission_merge` (`group_id`, `permission_id`)
SELECT 272835742965968896, id FROM `fly_admin_permission`
WHERE id BETWEEN 900180 AND 900187;
