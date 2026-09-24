# FlyCms 规划文档对标 · 漏开发补齐报告

- 日期：2026-09-24
- 对标文档：`backend/doc/全智能CMS对标分析与开发规划.md`（阶段 A ~ J）
- 校验方式：静态走查 + 接口/路由逐条核对 + `mvn -o compile` 通过 + `vue-tsc --noEmit` 通过

## 一、结论速览

| 阶段 | 规划内容 | 补齐前状态 | 本次动作 |
|------|----------|-----------|----------|
| A | Freemarker 沙箱 / 审计 / 上传安全 | 已实现 | 无需改动 |
| B | 评论审核、附件库、导航、友链 | **缺后台接口** | 新增 4 个 REST 控制器 + DAO/Service 方法 |
| C | 统一栏目体系 | 已实现 | 无需改动 |
| D | 模板中心 | 已实现 | 无需改动 |
| E | 碎片 / 推荐位 | 已实现 | 无需改动 |
| F | 表单系统 | **整个模块缺失** | 新建 `module/form/*` + 2 个控制器 + 模板标签 |
| G | sitemap / robots | 已实现（`SeoController`） | 无需改动 |
| H | 定时发布 | 已实现（`TimingPublishJob`） | 无需改动 |
| H | 投稿审核开关 | **未实现** | 新增 `fly_article_audit` 开关 + 待审列表 + 批量通过/驳回 |
| I/J | — | 已实现 | 无需改动 |

## 二、致命 SQL 语法错误（系统性，18 处）

7 个 Mapper 中时间筛选条件写成：

```sql
and a.create_time BETWEEN STR_TO_DATE(#{createTime}, '%Y-%m-%d %H')
```

`BETWEEN` 只有左操作数，**所有带时间筛选的后台列表查询都会抛 SQL 语法异常**，
等于后台「按时间查」功能全线不可用。已全部改为 `like concat(#{createTime}, '%')`。

影响文件：`ArticleDao.xml`、`MessageDao.xml`、`OrderDao.xml`、`AnswerDao.xml`、
`QuestionDao.xml`、`ScoreRuleDao.xml`、`ShareDao.xml`（共 18 处）。

## 三、潜伏 Bug（本次一并修复）

| 问题 | 位置 | 后果 |
|------|------|------|
| `ConfigService.getIntKey` 恒返回 1 | `ConfigService.java:147` | 原实现 `return Integer.parseInt("1")`，配置值被丢弃，任何已存在的整型配置都读成 1 |
| `Integer.parseInt(getStringByKey(...))` 空串崩溃 | `ArticleService` 投稿/编辑 | `getStringByKey` 缺失时返回 `""`，`parseInt("")` 抛异常 → 投稿直接 500 |
| `fly_guide.id` 用 `Integer` 接 bigint | `Guide.java` | 雪花 ID 超 int 范围，插入/查询溢出 |
| `addGuide` 未写 id | `GuideDao.xml` | `id` 列 NOT NULL 且非自增，导航新增必失败 |
| 评论删除按 `article_id` 删 | `deleteArticleCommentById` | 删一条评论会清空整篇文章的评论 |
| `updateLinksById` 用 `isShow != ''` | `LinksDao.xml` | OGNL 下 Integer 0 被判为 false，友链「隐藏」永远存不进去 |
| `fly_links.id` 用 `Integer` | `Links.java` | 同上溢出 |
| `getCommentAuditList` 调用缺参 | `ArticleService` | 编译期就报错，评论审核列表实际不可用 |

## 四、新增后端能力

### 阶段 B
- `web/api/ApiCommentController.java` — `/api/system/comment/page|audit|delete|batch`
- `web/api/ApiImagesController.java` — `/api/system/images/page|orphanCount|deleteOrphan`（孤儿附件只清 `info_count<=0`）
- `web/api/ApiGuideController.java` — `/api/system/guide/tree|page|get|save|delete|status`
- `web/api/ApiLinksController.java` — `/api/system/links/page|get|save|delete|status`
- 评论审核开关 `fly_comment_audit`（0 免审 / 1 先审后显示），投稿时按开关落 `status`

### 阶段 F（全新模块）
- `module/form/model/{Form,FormField,FormData}.java`
- `module/form/dao/FormDao.java` + `FormDao.xml`
- `module/form/service/FormService.java` — CRUD + 提交（验证码、IP/用户每日频次、jsoup `Safelist.none()` 清洗、敏感词过滤、邮件通知）+ CSV 导出（带 BOM）
- `web/api/ApiFormController.java`（后台）/ `ApiFormSubmitController.java`（前台 `POST /api/form/submit/{code}`，刻意不继承 `ApiBaseController`）
- `web/tags/Form.java` — 模板标签 `<@fly_form code="...">`
- `EmailService.sendNotifyEmail(...)`（原 `sendEmail` 只支持验证码模板）

> 设计决策：表单数据走**统一表 + JSON**，不动态建表。表单数据只用于「后台查看 + 导出」，
> 不需要按字段做索引查询；动态 DDL 会带来权限放大与字段漂移，收益不抵成本（与规划文档一致）。

### 阶段 H
- `ArticleService.getArticleAuditSwitch()` / `resolveArticleStatusByAuditSwitch()`
- `ArticleService.getArticleAuditPage()` / `auditArticle()` / `batchAuditArticle()` / `countPendingArticle()`
- `web/api/ApiAuditController.java` — `/api/system/audit/page|pendingCount|audit|batch|switch`
- 驳回必填原因，通过后重建 feed + 索引，驳回后撤 feed + 撤索引并**站内信通知作者**
- 兼容老键 `user_article_verify`（语义相反，做了反向映射，升级后行为不变）

## 五、SQL 迁移脚本

| 文件 | 内容 |
|------|------|
| `sql/stage-b.sql` | 评论/附件索引 + 菜单权限 900220~900254 + `fly_comment_audit` 配置键 |
| `sql/stage-f.sql` | `fly_form` / `fly_form_field` / `fly_form_data` 三张表 + 菜单权限 900260~900274 |
| `sql/stage-h-audit.sql` | `fly_article_audit` 配置键 + 待审索引 + 菜单权限 900280~900284 |

菜单走后端动态菜单（`getAllMenusApi` + `pageMap` 映射），**无需**在前端 `router/routes/modules` 里再注册。

## 六、前端

新增 API：`api/core/{comment,images,guide,links,form,audit}.ts`
新增页面：`views/system/{comment,images,guide,links,form,formdata,audit}/`

顺带修掉 3 个既有前端错误（会阻塞 `pnpm check:type`，其中 1 个是运行时崩溃）：

| 问题 | 位置 | 后果 |
|------|------|------|
| `Modal` 导入与本地同名常量冲突 | `template/skin-modal.vue`、`template/version-modal.vue` | 类型检查失败 |
| `row.desc` 不存在于 `TagManualRow` | `template/tag-manual-modal.vue` | 后端只返回 `usage`，搜索时 `r.desc.toLowerCase()` 抛 TypeError |
| SelectValue 强转 number | `channel/index.vue:272` | `undefined` 被当数字使用 |

## 七、验证

- 后端：`mvn -o compile` → BUILD SUCCESS
- 前端：`vue-tsc --noEmit --skipLibCheck` → 0 error

## 八、仍需人工处理

1. 三份 SQL 需按 `stage-b → stage-f → stage-h-audit` 顺序执行；`group_id`（超管组）以实际库为准。
2. 老库已存在的 `fly_article` 大表加索引 `idx_status_time` 会锁表，建议在低峰执行。
3. `fly_article_audit` 默认给 0（直接发布），与老行为一致；要启用先审后发需到「投稿审核」页手动打开。
