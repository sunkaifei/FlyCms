# FlyCms

> 一套**前后端分离**的开源内容建站系统：后台是 Vue 3 单页控制台，前台是 Freemarker 模板站。
> 既可以直接搭建「问答 + 文章 + 分享」的社区，也可以用**自定义内容模型**搭楼盘、商品、案例等任意结构化站点。

```
后端 346 个 Java 文件 / 25 个业务模块 / 21 个 REST 控制器
前端 Vue 3 + TypeScript + Ant Design Vue，73 个页面组件
```

---

## 一、它现在是什么

FlyCms 最早是一个仿知乎的问答社区程序。经过持续重构，现在的内核是一套**五面分离**的 CMS 底座：

| 面 | 解决什么 | 对应能力 |
|----|---------|---------|
| **内容面** | 内容从哪来、长什么样 | 自定义内容模型（建模 + 字段 + 数据）、文章、问答、分享、话题、评论 |
| **结构面** | 内容挂在哪、URL 怎么走 | 统一栏目树（列表 / 单页 / 外链 / 聚合，聚合可跨模型混排） |
| **呈现面** | 页面怎么渲染 | 模板中心（在线编辑 / 版本快照 / 一键回滚 / 皮肤导入导出）、碎片位与推荐位、模板标签手册 |
| **交互面** | 访客怎么参与 | 表单系统（可视化配字段）、站内信、公告、收藏、关注与 Feed、WebSocket 推送 |
| **运营面** | 站点怎么管 | SEO（sitemap / robots）、投稿与评论审核、敏感词过滤、审计日志、定时任务、菜单 / 角色 / 权限 |

---

## 二、技术栈

### 后端

| 组件 | 版本 / 选型 | 说明 |
|------|------------|------|
| 运行时 | **JDK 24** | `maven-compiler-plugin` 开启 `proc:full` 以支持 Lombok |
| 框架 | **Spring Boot 4.1.1** | Jakarta 命名空间 |
| ORM | MyBatis（`mybatis-spring-boot-starter` 4.1.0） | XML 与 Mapper 同目录打包 |
| 模板 | Freemarker | 前台站点渲染，开启 `ALLOWS_NOTHING_RESOLVER` 沙箱 |
| 缓存 | **Caffeine** | 已替代原 EhCache 2 |
| 连接池 | Druid 1.2.28 | Boot 4 下由 `DruidConfig` 手动装配 |
| 调度 | Quartz 2.3.2 | 后台可启停任务、查看执行日志 |
| 数据库 | MySQL 8（utf8mb4） | SQL 仅适配 MySQL |
| 检索 | Lucene 7.4 | **当前为技术中立接缝**：原 Solr 已移除，`SearchService` 保留空实现，规划接入 Elasticsearch |
| 其它 | Hutool 5.8、jsoup 1.17、BCrypt、Log4j2、阿里云短信 SDK | 验证码、XSS 清洗、密码散列 |

### 前端

| 组件 | 版本 |
|------|------|
| Vue | 3.5.41 |
| TypeScript | 6.0 |
| Vite | 8.3 |
| Ant Design Vue | 4.2 |
| 状态 / 路由 | Pinia 4、Vue Router 5 |
| 基座 | Vue Vben Admin 5（`@vben/*` 内部包） |
| 工程 | pnpm 11 workspace + turbo，Node `^22.18 \|\| ^24.12` |

后台是 SPA（`apps/web-antd`），通过 `/api/**` 与后端通信；前台仍是服务端渲染的 Freemarker 模板站，两者互不干扰。

---

## 三、功能清单

### 前台

- **栏目**：列表 / 单页 / 外链 / 聚合四种形态，目录名即 URL，支持无限层级与移动改挂
- **内容模型**：后台建模后自动生成数据表，前台按模型 `code` 路由，列表自带分页条
- **问答**：发布问题、悬赏积分邀请回答、答案列表与统计
- **文章**：分类无限级、话题聚合、评论、顶踩、收藏
- **分享**：分享资源地址赚取积分，支持积分兑换
- **表单**：`<@fly_form code="...">` 输出表单，提交走 `POST /api/form/submit/{code}`
- **用户**：注册（手机 / 邮箱）、登录验证码与失败次数限制、密码找回、手机 / 邮箱绑定、邀请注册奖励、积分充值
- **互动**：关注、粉丝、Feed 动态、收藏、站内信、WebSocket 实时推送
- **SEO**：`/sitemap.xml`、`/robots.txt` 由配置键 `fly_sitemap_status` / `fly_sitemap_limit` / `fly_robots` 控制

### 后台（Vue 控制台）

| 分组 | 功能 |
|------|------|
| 系统管理 | 管理员、角色组、菜单权限（后端动态菜单）、站内信、公告、审计日志、附件库、导航、友情链接 |
| 内容 | 内容模型（模型 / 字段 / 数据 CRUD）、栏目、文章、评论审核、**投稿审核**（先审后发开关 + 批量通过 / 驳回） |
| 呈现 | 模板中心（文件树编辑、版本快照与回滚、皮肤导入导出、标签手册）、碎片位 / 推荐位 |
| 交互 | 表单管理（表单 + 字段 + 数据 + CSV 导出） |
| 运营 | 网站配置、定时任务、积分规则、订单、敏感词过滤、用户管理 |
| 统计 | 仪表盘 |

### 平台能力

- **动态菜单与权限**：菜单、角色、权限点全部数据驱动，`actionKey` 同时用于前端按钮显隐（`accessCodes`）和后端 403 兜底
- **模板版本化**：每次保存自动快照，改坏模板可一键回滚
- **定时发布**：`TimingPublishJob` + `publish_time` 字段，`status=4` 为待定时发布
- **审计日志**：`AdminLogInterceptor` 记录后台操作，含敏感字段脱敏
- **定时任务**：后台启停、Cron 配置、执行日志

---

## 四、安全设计

| 风险 | 处理 |
|------|------|
| 模板 RCE | Freemarker `TemplateClassResolver.ALLOWS_NOTHING_RESOLVER` 沙箱 |
| 上传 getshell | `UploadSafeUtil.safeImage`：扩展名 + MIME + **文件头魔数**三重白名单，强制重命名丢弃原始文件名 |
| SQL 注入 | ORDER BY 走 `OrderbyUtils.check` 白名单；动态建表 / 列名走 `SqlSafeUtil.safeColumnName` / `safeNumber` |
| XSS | 提交内容用 jsoup `Safelist.none()` 清洗（表单模块），页面输出 HTML 转义 |
| 越权 | Session 认证 + `requireAdmin()`（401）/ `requirePermission()`（403）双重校验 |
| 爆破 | 登录验证码 + 失败次数限制；密码 BCrypt 散列 |
| 权限放大 | 表单数据走统一表 + JSON，**不做动态建表**，避免 DDL 权限外溢 |

---

## 五、目录结构

```
FlyCms/
├── backend/                       # Java 后端
│   ├── src/main/java/com/flycms/
│   │   ├── config/                # MVC、CORS、WebSocket、Freemarker 沙箱、Druid
│   │   ├── core/                  # 实体、工具类、全局标签、上传安全
│   │   ├── interceptor/           # 登录拦截、审计日志
│   │   ├── module/                # 26 个业务模块（model / channel / template / form ...）
│   │   └── web/
│   │       ├── api/               # /api/** REST（后台 SPA 用）
│   │       ├── front/             # 前台页面控制器
│   │       └── tags/              # Freemarker 自定义标签
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── i18n/                  # 国际化
│   └── doc/                       # 开发规划、模型手册、改造方案
├── frontend/                      # pnpm workspace
│   ├── apps/web-antd/             # 后台控制台（Vue 3）
│   ├── packages/@core/            # 基础能力包
│   └── internal/                  # 构建配置
├── sql/                           # 建库与分阶段迁移脚本
└── docs/                          # 审查报告、运维配置
```

---

## 六、快速开始

### 环境要求

- JDK 24、Maven 3.6+
- MySQL 8（字符集 `utf8mb4`）
- Node `^22.18 || ^24.12`、pnpm ≥ 11

### 1. 数据库

```bash
mysql -u root -p -e "CREATE DATABASE flycms DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p --default-character-set=utf8mb4 flycms < sql/flycms_full.sql
```

分阶段能力（评论审核 / 表单 / 投稿审核等）按需要执行增量脚本：

```bash
mysql -u root -p --default-character-set=utf8mb4 flycms < sql/stage-b.sql
mysql -u root -p --default-character-set=utf8mb4 flycms < sql/stage-f.sql
mysql -u root -p --default-character-set=utf8mb4 flycms < sql/stage-h-audit.sql
```

> `stage-*.sql` 默认给超管组授权，`group_id` 请先用 `SELECT * FROM fly_admin_group;` 确认后替换。

### 2. 后端

修改 `backend/src/main/resources/application.yml` 的数据库连接，然后：

```bash
cd backend
mvn clean compile        # 编译
mvn spring-boot:run      # 运行
```

默认监听 `http://localhost:80`，前台 `http://localhost/`；管理控制台是前端 Vue SPA（见下一步）。

### 3. 前端控制台

```bash
cd frontend
pnpm install
pnpm dev:antd            # 开发端口 5666
```

`vite.config.ts` 已配置代理：`/api/**` → 后端 80 端口，`/captcha` 直连（验证码接口无 `/api` 前缀）。

### 4. 常用命令

| 命令 | 作用 |
|------|------|
| `mvn -o compile` | 后端编译校验 |
| `pnpm dev:antd` | 启动后台开发服务 |
| `pnpm build:antd` | 构建后台 |
| `pnpm check:type` | 前端类型检查（`vue-tsc`） |
| `pnpm check` | 循环依赖 + 依赖 + 类型 + 拼写全量检查 |

---

## 七、打包部署

```bash
cd backend
mvn clean package                     # 产出 target/FlyCms.jar
java -jar target/FlyCms.jar --spring.profiles.active=prod > FlyCms.log 2>&1 &
```

- 配置外置：复制 `application.yml` 为 `application-prod.yml` 后修改（**务必改掉默认数据库口令**）
- 静态资源与上传目录在 `application.yml` 的 `spring.web.resources.static-locations`：`./views/static/`、`./uploadfiles/`
- 模板目录：`./views/templates/`
- 停止：`ps -ef | grep FlyCms.jar | grep -v grep | cut -c 9-15 | xargs kill -s 9`；看日志：`tail -200f FlyCms.log`

> 短信 SDK 使用官方坐标 `com.aliyun`，Maven 可直接解析，无需手动安装 jar。
> Solr 不再需要——检索模块已改为技术中立接缝，不接 ES 也能正常跑（搜索返回空结果）。

---

## 八、前台模板与标签

模板放在 `views/templates/{主题}/`，后台「模板中心」可在线编辑、保存即生成版本快照。

```html
<!-- 模型分页列表，自带分页条 -->
<@fly_page_model model="loupan" rows="10">
  <#list dataList as row>
    <a href="/loupan/${row.shortUrl}.html">${row.title}</a>
  </#list>
  ${pageHtml!''}
</@fly_page_model>

<!-- 碎片位 / 推荐位 -->
<@fly_block key="home_focus">
  <#list block.items as it><img src="${it.image}"></#list>
</@fly_block>

<!-- 表单 -->
<@fly_form code="feedback">
  <form action="/api/form/submit/${form.formCode}" method="post">
    <#list form.fields as f>
      <input name="${f.fieldCode}" placeholder="${f.placeholder!''}">
    </#list>
    <#if form.needCaptcha == 1><img src="/captcha"><input name="captcha"></#if>
  </form>
</@fly_form>

<!-- 按角色显隐 -->
<@fly_userpower groupName="技术专家组">
  <a href="/ucenter/article/add">发布文章</a>
</@fly_userpower>
```

完整标签清单见后台「模板中心 → 标签手册」，或 `TagManualService.java`。

---

## 九、开发文档

| 文档 | 内容 |
|------|------|
| `backend/doc/全智能CMS对标分析与开发规划.md` | 阶段 A~J 的整体演进规划与设计决策 |
| `backend/doc/自定义模型系统开发手册.md` | 内容模型建模、字段类型、数据表生成规则 |
| `backend/doc/内容体系收敛与模型命名改造开发方案.md` | 表名 / 主键策略的 ADR 与迁移步骤 |
| `docs/backend-bug-review-2026-09-24.md` | 后端审查报告与已修 Bug 清单 |
| `docs/stage-gap-fill-2026-09-24.md` | 规划对标后的漏开发补齐记录 |
| `docs/frontend-access-guide.md` | 前端权限码与按钮显隐约定 |
| `docs/nginx-upload-security.conf` | 上传目录的 Nginx 加固示例 |

---

## 十、已知边界

- **检索**：`SearchService` 目前是空实现（方法签名保留），站内搜索返回空结果；接入 Elasticsearch 时替换实现类即可，调用方无需改动
- **数据库**：SQL 仅适配 MySQL，切库需自行改写 Mapper
- **配置文件**：`application.yml` 内含明文数据库口令与 `devtools.enabled: true`，生产环境务必外置并关闭
- **上传目录**：`Const.UPLOAD_PATH` 是相对路径 `./uploadfiles`，部署换工作目录会导致上传丢失，建议配合 `docs/nginx-upload-security.conf` 做目录约束

---

## 十一、贡献

欢迎提 issue 与 PR。提问题时请附上复现步骤、环境版本与日志片段。

## License

MIT
