# FlyCms 主流 CMS 对标与全项目优化开发方案

> 版本：v1.0（2026-09-28）
> 定位：**平台级复评 + 对标融合施工图**。回答四个问题——
> ① 当前程序有哪些真实缺陷与框架/路径层面的优化点；
> ② 2026 年主流 CMS（国际 + 国内）各自的最佳解法是什么；
> ③ FlyCms 与它们的差距具体在哪些能力项上；
> ④ 每一项差距，**抄谁的解法、怎么落到 FlyCms 代码里**。
>
> 与既有文档的关系：
> - 《全智能CMS对标分析与开发规划.md》是本方案的**前置**（A–J 阶段，已基本落地）——本文不重复其内容，只在必要处修订其过期结论（见 §2）。
> - 《前台模板引擎重新设计开发方案.md》《模板引擎补齐执行清单.md》覆盖呈现面，本文**不重复**，只在其上补"呈现面 → 主流 CMS 水位"的差距（§5 第 9、14 项）。
> - 本文产出的新批次为 **K–R 阶段**，与 A–J 连续编号。

---

## 目录

1. [一句话结论](#1-一句话结论)
2. [复评方法与既往文档对账](#2-复评方法与既往文档对账)
3. [现状快照](#3-现状快照)
4. [缺陷与优化清单（含证据）](#4-缺陷与优化清单含证据)
5. [2026 主流 CMS 对标](#5-2026-主流-cms-对标)
6. [差距矩阵：FlyCms vs 主流最佳实践](#6-差距矩阵flycms-vs-主流最佳实践)
7. [最优解融合方案（逐项施工设计）](#7-最优解融合方案逐项施工设计)
8. [架构与路径优化建议](#8-架构与路径优化建议)
9. [分阶段实施计划（K–R）](#9-分阶段实施计划kr)
10. [决策复审：原"不做清单"的再评估](#10-决策复审原不做清单的再评估)
11. [验收与上线检查单](#11-验收与上线检查单)
12. [附录](#12-附录)

---

## 1. 一句话结论

> **总纲（2026-09-28 定调，优先级高于本文一切分项）**：FlyCms 的核心技术就是**自定义模型引擎**——系统的一切内容模块（文章、图片、下载、问答、链接、话题……以及未来任何模块）都由后台**在线建模**生产，不做第二套硬编码实现。现存硬编码内容模块（article/share/question/topic/links/announcement/message/guide/favorite 等）属于历史包袱，按**阶段 U** 逐个退役：数据迁入 `fly_cmodel_*`、页面收敛到通用 `modeldata` 管理与 `cmodel` 模板通道、旧代码删除。目标形态 = **万能系统**：用户只靠后台设置就能生成任何想要的程序模块。

**FlyCms 在"架构代差"上依然领先所有对标对象（前后端分离 SPA + 配置驱动建模 + 现代 JVM 栈），A–J 阶段补齐后，"通用建站 CMS 的功能全集"已基本闭合；当前真正的短板从"功能缺失"转移到了三个新层面：**

1. **工程健壮性债务**（零单元测试、无全局异常契约、无 CSRF/安全头、循环依赖）——这是"能演示"与"能交付"的分界线；
2. **内容治理深度**（内容版本/草稿、字段级权限、可复用区块、可视化预览）——这是 WordPress 之外的现代 CMS（Drupal/Strapi/Directus/Payload/Craft）在过去三年集体拉高的水位线；
3. **AI 原生能力为零**（2026 年 CMS 竞争的新主战场：内容原生向量、schema-aware AI 写入、MCP）——这与 FlyCms 自称的"**全智能** CMS"定位存在**名实差距**。

因此本方案的施工重点不是再加功能页，而是：**补工程底座（K）→ 补内容治理（L/M/N）→ 落 AI 原生（P）**，同时用 §8 的架构与路径优化消化历史习气。

---

## 2. 复评方法与既往文档对账

### 2.1 取证方式

本轮**不采信任何文档自述**，全部结论来自现场取证：

| 取证面 | 手段 |
|---|---|
| 代码结构与命名 | 遍历 `backend/src/main/java/com/flycms/**`（361 Java + 43 MyBatis XML） |
| 配置与框架接缝 | 逐读 `application.yml`、`config/*.java`、`filter/*.java`、`interceptor/*.java` |
| 缺陷模式 | 全仓 grep（`printStackTrace` / `System.out` / `@ControllerAdvice` / `select *` / `@Transactional` / 循环依赖） |
| 文档一致性 | 逐份核对 `docs/` 下 11 份文档的引用路径、版本号、数量声明与代码/DB 实际值 |
| 数据库落地 | 直连 MySQL 5.7（`--default-character-set=utf8mb4`）查 `information_schema` |
| 对标素材 | 2026 年公开资料（WordPress/FSE、Drupal 11、Strapi v5、Directus v11、Payload v3、Sanity、Craft 5、Ghost、迅睿/帝国/织梦现状、AI-native CMS 生态） |

### 2.2 与《全智能CMS对标分析与开发规划》的对账（发现 6 处过期）

该文档停留在 A–J 阶段的"施工前"视角，A–J 落地后未回改，产生如下**实证不一致**（全部已在本轮核实）：

| # | 文档原文（位置） | 代码/DB 实际 | 性质 |
|---|---|---|---|
| 1 | "Spring Boot 4.1.1 / **JDK 25**"（§5.1 第 3 条，行 128） | `pom.xml:17` → `<java.version>24</java.version>` | 版本号写错 |
| 2 | "**48 个**指令标签"（§3.1 行 64、§4 行 97、§6.3 行 160、附录 A 行 600、D6 行 408，共 5 处） | `web/tags/*.java` 共 56 个，扣基类 `AbstractTagPlugin` = **55 个真实标签** | 数量过期（D 阶段后新增未回写） |
| 3 | "基线含 **59 张** `fly_` 表"（附录 B 行 614） | 快照 `sql/flycms_20260928_093335.sql` 实测 **78 张** | 数量过期 |
| 4 | "对接规范以 `docs/frontend-access-guide.md` 为准"（头部行 6；另《内容体系收敛》行 6 同） | **该文件不存在**，实际是 `docs/02-手册/前端权限接入开发手册.md`（docs 目录已重构分类） | 失效引用 |
| 5 | "自定义模型系见 `sql/custom-model.sql`"（附录 B 行 614） | **该文件不存在**；sql/ 下只有全量快照 + `migrations/` 两份增量 | 失效引用 |
| 6 | "`application.yml:22`""`application.yml:30`"（行 248、279） | 行号已漂移（现 cache 在 `:24`、cookie 在 `:32`），且 A2「httpOnly 生产置 true」**已落地** | 行号失效 + 状态过期 |

> **对账结论**：《全智能CMS对标分析与开发规划》的功能判断仍然成立（其 §4 差距矩阵与 §6 设计红线未过时），但**其状态陈述已不可作为验收依据**。本方案 §4 的缺陷清单才是当前唯一可信的现状基线。

---

## 3. 现状快照

### 3.1 技术底座（实测）

| 层 | 现状 |
|---|---|
| 后端 | Spring Boot **4.1.1** + JDK **24** + MyBatis（`mybatis-spring-boot-starter 4.1.0`）+ Jetty 12 + FreeMarker |
| 数据 | MySQL **5.7.44**（绿色版，EOL 2023-10）+ Druid 1.2.28（`filters: stat,wall`） |
| 缓存 | Caffeine（`EhCacheConfig` 类名遗留，实现已是 Caffeine）；MyBatis `cache-enabled: true` |
| 检索 | Lucene **7.4.0**（2018 年版本）+ IK 分词；`SearchService` 留了 ES 接缝 |
| 任务 | Quartz 2.3.2 + `@EnableScheduling` |
| 序列化 | fastjson2 2.0.57（Dependabot 修复后）+ Jackson（Long→String 全局） |
| 前端 | pnpm + turbo monorepo，vue-vben-admin **v5.7.0**（`apps/web-antd`），Vue 3.5.41 / Vite 8 |
| 规模 | 后端 361 Java / 43 XML / 26 模块；web/api 22 控制器 + web/front 16 控制器 + web/tags 55 标签 |
| 数据面 | 78 张表（`fly_*`） |

### 3.2 能力面（A–J 阶段落地后的真实现状）

已具备：自定义模型（动态 DDL）、统一栏目树、模板中心（版本/回滚/皮肤包/组件面板/在线手册）、碎片与推荐位、表单生成器、SEO 中心（栏目 TDK / sitemap / robots）、审核流与定时发布、操作审计、Freemarker 沙箱、静态化工具、前后端分离后台、会员互动体系、REST API 体系。

**已跃迁为"功能全集基本闭合"**——这正是本方案把重心从"功能"转向"健壮性 / 治理 / AI"的原因。

---

## 4. 缺陷与优化清单（含证据）

分级：**P0 阻断交付 / P1 高危 / P2 中等 / P3 技术债**。每条含「证据 → 影响 → 建议方案」。

### 4.1 P0

#### P0-1　全局异常契约缺失（错误处理无统一出口）

- **证据**：
  - 全仓 `grep -rn "@ControllerAdvice\|@RestControllerAdvice"` → **0 命中**（唯一的字符串命中还是 `TemplateCenterService.java:866` 的注释）。
  - `Application.java`：`@EnableAutoConfiguration(exclude = {ErrorMvcAutoConfiguration.class, ...})` —— **主动排除了 Spring Boot 的错误处理自动配置**（即 `BasicErrorController` 与 `/error` 映射一并消失）。
  - `ApiBaseController.requireAdmin()` 抛 `ResponseStatusException(401)`、`requirePermission()` 抛 `403`，而成功路径返回 `DataVo`（`code: 0 / -1`）——**HTTP 状态码语义与业务码语义双轨并存，无统一信封**。
- **影响**：
  - API 抛未捕获异常时不由 Spring 输出结构化 JSON，退化为 **Jetty 容器默认错误页（HTML）**；前端 `request.ts` 的响应拦截器拿不到预期结构，只能落到通用的"请求失败"分支，排障成本高。
  - 生产环境存在泄漏堆栈/内部路径的风险（默认错误页在未定制时可能带异常信息）。
  - 这是"能演示"与"能交付"的分界线，**列为 P0**。
- **方案**：见 §7 第 1 项（`@RestControllerAdvice` + 统一 `ErrorVo` + 恢复 `ErrorMvcAutoConfiguration`）。

### 4.2 P1

#### P1-1　Cookie-Session 认证无 CSRF 防护，且 CORS 白名单过宽

- **证据**：
  - `application.yml:32` 仅设 `httpOnly: true`，**未设 `sameSite`**；无 CSRF token 机制（全仓无 `CsrfToken` / `SameSite` 相关代码）。
  - `CorsConfig.addAllowedOrigins()`：对白名单域名（含 `localhost`、`127.0.0.1`）放行 `http://host:*` / `https://host:*` 的**任意端口**通配，同时 `setAllowCredentials(true)` + `addAllowedHeader("*")` + `addAllowedMethod("*")`。
  - 认证完全依赖 Cookie（`JSESSIONID`）——浏览器的自动携带特性使所有状态变更端点（`/api/system/**` 的 POST）天然暴露于 CSRF。
- **影响**：管理员在已登录状态下访问任一恶意页面，即可被伪造提交删除栏目/改配置/删数据等请求。CORS 通配 + 凭据的组合进一步放宽了可利用面。
- **方案**：见 §7 第 2 项（`SameSite=Lax` + CSRF 双提交 Token + 安全响应头 + CORS 白名单收敛并 profile 化）。

#### P1-2　`WebMvcConfig extends WebMvcConfigurationSupport`（框架反模式）

- **证据**：`config/WebMvcConfig.java:32` → `public class WebMvcConfig extends WebMvcConfigurationSupport`。
- **影响**：继承该类会**关闭 Spring Boot 的 `WebMvcAutoConfiguration`**，必须手工补回一切（这正是该文件里要手写 `extendMessageConverters` 替换 Jackson 转换器、手写 `addResourceHandlers` 的原因）。副作用包括：Boot 默认的消息转换器链、`PathMatchConfigurer`、favicon、静态资源缓存策略、`spring.mvc.*` 配置项大部分**静默失效**。这是 Spring 官方明确不推荐的写法，属**架构级技术债**。
- **方案**：见 §7 第 3 项（改 `implements WebMvcConfigurer`，用 `WebMvcConfigurer` 的回调替代 `super.*` 调用，回归 Boot 默认装配）。

#### P1-3　零单元测试 + 后端无 CI 门禁

- **证据**：
  - `backend/src/test` → **目录不存在**（361 个 Java 文件，0 个测试）。
  - 仓库根无 `.github/workflows`（仅 `frontend/.github`，是 vben 上游自带）。
  - 现有回归手段是 `tools/e2e_template_check.py`（PASS 51）与 `tools/audit_extra_check.py`（PASS 16）——都是**黑盒 e2e，且 `tools/` 被 `.gitignore` 排除**（只存在于本地工作区，随仓库分发即丢失）。
- **影响**：任何重构（含本方案 §8 的路径调整）都没有自动化安全网；回归依赖"本地起 MySQL + 起后端 + 手工跑脚本"，无法在 CI 上把关；`tools/` 不入库使协作者拿不到验收基线。
- **方案**：见 §7 第 4 项（JUnit 5 + Testcontainers 打底 + GitHub Actions 门禁 + 验收脚本入库）。

#### P1-4　`allow-circular-references: true`（循环依赖债务）

- **证据**：`application.yml:8` `spring.main.allow-circular-references: true`，注释自陈"老项目存在 userService ↔ userSessionUtils 等循环依赖"。
- **影响**：Bean 装配顺序脆弱、单元测试无法单独实例化、AOP 代理行为不确定。
- **方案**：见 §8 第 2 项（按"下沉共享状态到独立组件"的方式拆环，拆完即删除该开关）。

### 4.3 P2

| # | 问题 | 证据 | 影响 | 方向 |
|---|---|---|---|---|
| P2-1 | **静默吞异常清理口径不一致** | `core/utils/**` 仍有 **29 处 `printStackTrace`**（lucbir 7、ImageUtils 4、DateUtils 4 等）；`core/**`+`filter/**` 仍有 12+ 处 `System.out.println`（FlyFilter 4、StringHelperUtils 4、LucbirSearcher 3…） | P4 批次只清了 `web/module/interceptor/job`，`core/` 未覆盖；生产 stdout 污染 + 异常无 logger 上下文 | 统一替换为 `log.debug/warn`，按包补扫 |
| P2-2 | **权限查询无缓存** | `PermissionService.findPermissionByUserId()` 直接落 DAO，无 `@Cacheable`；`ApiBaseController.requirePermission()` **每个受保护 API 调用一次** | 高频接口每请求多一次 DB 往返；权限变更时若加缓存需配套 evict | 加 `@Cacheable("permission")` + 授/撤权时 evict |
| P2-3 | **107 处 `select *`** | `grep -c "select \*" --include=*.xml` → 107 | 无法形成覆盖索引（尤其 `fly_article.content` 等 mediumtext 被无谓传输）；schema 变更易断映射 | 收敛列表查询为显式列；详情页保留 `*` |
| P2-4 | **仓库卫生** | `backend/views/static/` **1747 个文件 / 39MB 老主题静态资源入库**（含 `jquery-migrate-1.2.1`、`html5shiv`）；`.git` **101MB**；`backend/views/templates/_testdata/wpdemo` 测试残留；`frontend/apps/backend-mock` 上游样本未清理 | clone 慢、仓库膨胀、误引过时前端库 | `.gitignore` 收敛 + `git rm --cached` + 清理残留 |
| P2-5 | **命名与依赖债** | `config/EhCacheConfig.java`（类名 vs Caffeine 实现）；`filter/CorsFilter.java` 与 `config/CorsConfig` 提供的 `CorsFilter` Bean **重名**；Lucene **7.4.0**（2018）配 JDK 24 | 认知负担 + 旧库安全/兼容风险 | 重命名 + 去重 + 评估 Lucene 升级或直接接 ES |
| P2-6 | **MyBatis XML 布局非标准** | 43 个 Mapper XML 位于 `src/main/java/com/flycms/module/**/dao/*.xml`，靠 `pom.xml` 把 `src/main/java` 也当 `<resource>` 才打包 | 违反 Maven 约定；IDE/构建工具易误判；`application.yml` 里 `mapper-locations: classpath:com/flycms/module/**/dao/*.xml` 与源码路径耦合 | 迁 `src/main/resources/mapper/**`（需一次性全量调整 + 回归） |
| P2-7 | **根级路由冲突面** | 根级单段映射并列：`ChannelController` `/{channelDir}`、`ModelController` `/{modelCode}/`、`IndexController` `/index-{sort}`；`ChannelController` 源码注释自陈曾因 pattern 重复导致 **Ambiguous mapping 退服** | 依赖 Spring 精确度排序"侥幸可用"，新增根级映射极易再次踩雷 | 统一前缀（`/c/{dir}`）或集中注册 + 冲突启动自检 |

### 4.4 P3

| # | 问题 | 说明 |
|---|---|---|
| P3-1 | MySQL 5.7 EOL + MyISAM 遗产表 | 5.7 已于 2023-10 停止支持；存量含 MyISAM（崩溃易坏表）。既往决策为"本期不动"，建议单独立项评估升 8.0 + 逐表转 InnoDB |
| P3-2 | 静态化产物概念混淆 | `views/static/`（主题静态资源，入库）与 `backend/html/`（静态化输出，已 ignore）职责易混；`StaticPageController` 只映射 `/html/**` |
| P3-3 | 配置硬编码 | DB 口令明文在 `application.yml`；`CorsConfig` 白名单硬编码（`www.28844.com` 等）；`devtools.enabled: true` 在**默认 profile**（而非仅 dev） |
| P3-4 | `fly_templet` 遗留表 | 零引用，建议评估废弃 |
| P3-5 | 遗留注释噪音 | `FlyFilter` 内多处注释掉的 `System.out`；`WebMvcConfig`/`BaseController` 保留 2018 年 "28844.com/开源中国" 版权头，与项目现状不符 |

---

## 5. 2026 主流 CMS 对标

### 5.1 对标对象与定位

| 系统 | 类型 | 2026 定位 | 值得 FlyCms 抄的"最优解" |
|---|---|---|---|
| **WordPress 6.8+ / FSE** | 传统 + 混合 | 43% 市场份额，生态绝对第一 | `theme.json` 设计令牌体系；Block/FSE 结构化编辑；`_fields` 稀疏字段集 + `_embed`；Nonce 式权限回调（端点级不可省） |
| **Drupal 11 / Drupal CMS** | 传统企业级 | 政府/高校/多语言首选，结构化最深 | Entity+Field 原生建模；**原生三层多语言**（内容/界面/配置）；Content Moderation 独立工作流实体；JSON:API 规范；**Config Management（配置可版本化）**；Canvas 可视化 |
| **Joomla 5/6** | 传统 | 灵活性与易用性平衡 | 内置 ACL 层级与用户组 |
| **Ghost** | 出版 | 独立媒体/订阅 | 开箱 SEO（自动 canonical + 结构化数据 + sitemap）；性能优先架构；会员订阅 + Newsletter |
| **Strapi v5** | Headless | 安装量最大，编辑器友好 | Content-Type Builder（FlyCms 已有）；**Components / Dynamic Zones**（可复用字段组 + 动态区块）；per-content-type RBAC；插件市场 |
| **Directus v11** | Headless（数据库优先） | 包裹既有库即出 API+Admin | **Flows 可视化自动化**；**字段级权限**（最细）；Revisions 内容版本；实时订阅 |
| **Payload v3** | Headless（TS-first） | 开发者首选，势头最快 | **Config-as-code schema**（可入版本库）；**versions + drafts + 回滚**；字段级 access；Local API |
| **Sanity** | AI-native | "AI 时代的内容操作系统" | **内容原生 Embeddings Index API**（向量与内容同步，无需外部管道）；**Portable Text**（结构化富文本，chunk 后结构不丢）；**Agent Actions**（schema-aware AI 写入）；Content Releases |
| **Contentful** | SaaS 企业 | 治理成熟 | 环境/角色/审计；App Framework |
| **Storyblok** | 可视化 | 编辑器体验 | Visual Editor 真·可视化 |
| **Craft CMS 5/6** | 开发者体验 | 精品站首选 | **Matrix 字段**（内容内嵌可复用区块）；**Live Preview**；Project Config（配置即代码）；原生多站点 |
| **Wagtail** | Django 系 | 结构化 + 编辑体验 | **StreamField**（内容流式区块）；Page 树 + 图片 rendition |
| **TYPO3 / Umbraco / Concrete** | 企业/其他 | 区域市场 | Workspaces 工作区（草稿-发布隔离）；块编辑 |
| **CrafterCMS AI** | Agentic（Java） | 首个开源 Agentic CMS | **Spring AI + OpenSearch 向量**（Java 栈最直接参照）；MCP server/client；内容生成/翻译/摘要/配图内嵌工作流 |
| **迅睿 CMS** | 国产现代（CI 框架） | 替代织梦的主流方案 | 模块化按需安装；**原生多语言 + 多站点 + RBAC + 自定义 API**（很多是内置而非插件）；插件规范统一 |
| **帝国 CMS** | 国产企业 | 大型门户 | 碎片管理（FlyCms 阶段 E 已对标）；定时刷新/审核（阶段 H 已对标）；参数白名单安全设计 |
| **织梦 DedeCMS** | 国产老牌（停更） | 历史资产迁移 | 标签易学、模型概念普及度 |
| **PHPCMS** | 已停更 | — | 三级模板分离的清晰度、缓存分层 |
| **Halo 2.x** | Java 现代 | 博客/内容站 | **主题包 + 插件包的分发形态**（FlyCms 皮肤包已有雏形） |

### 5.2 能力矩阵（✅ 完备 / 🟡 部分 / ❌ 缺失 / — 不适用）

| 能力维度 | WordPress | Drupal 11 | Strapi | Directus | Payload | Sanity | Craft | Ghost | 迅睿 | 帝国 | **FlyCms** |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 可视化建模 | 🟡(ACF) | ✅ | ✅ | ✅ | ✅(代码) | ✅ | ✅ | ❌ | ✅ | ✅ | **✅** |
| 内容版本/回滚 | 🟡(插件) | ✅ | 🟡 | ✅ | ✅ | ✅ | ✅ | 🟡 | 🟡 | 🟡 | **🟡(仅模板)** |
| 草稿-发布工作流 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | **🟡(单级)** |
| 字段级权限 | 🟡 | ✅ | 🟡 | ✅ | ✅ | ✅ | ✅ | ❌ | 🟡 | 🟡 | **❌** |
| 可复用区块/动态区块 | ✅(Block) | ✅(Paragraphs) | ✅(Dyn Zone) | 🟡 | ✅(Blocks) | ✅ | ✅(Matrix) | ❌ | 🟡 | 🟡 | **❌** |
| 可视化预览 | ✅ | ✅(Canvas) | 🟡 | ✅ | ✅ | ✅ | ✅(Live) | 🟡 | 🟡 | 🟡 | **🟡(试渲染)** |
| 多语言 | 🟡(插件) | ✅(原生三层) | ✅ | ✅ | ✅ | ✅ | ✅ | 🟡 | ✅ | ✅ | **❌(决策不做)** |
| 多站点 | ✅(Multisite) | ✅ | 🟡 | ✅ | 🟡 | ✅ | ✅ | ❌ | ✅ | 🟡 | **❌(决策不做)** |
| 配置即代码 | 🟡 | ✅(CMI) | 🟡 | 🟡 | ✅ | ✅ | ✅ | — | 🟡 | 🟡 | **❌** |
| 插件/扩展体系 | ✅✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ | 🟡 | **❌(无插件机制)** |
| API 版本化 | ✅(/v2) | ✅(JSON:API) | ✅ | ✅ | ✅ | ✅ | 🟡 | ✅ | ✅ | 🟡 | **❌** |
| 稀疏字段集 | ✅(_fields) | ✅(sparse) | ✅ | ✅ | ✅ | ✅ | 🟡 | ✅ | 🟡 | 🟡 | **❌** |
| 权限/角色 | 🟡(6 角色) | ✅(细粒度) | ✅ | ✅ | ✅ | ✅ | ✅ | 🟡 | ✅ | ✅ | **✅** |
| SEO 开箱 | 🟡(插件) | ✅ | 🟡 | 🟡 | 🟡 | 🟡 | 🟡 | ✅✅ | ✅ | ✅ | **✅** |
| 语义搜索/RAG | 🟡(插件) | 🟡 | 🟡 | 🟡 | 🟡 | ✅✅(原生向量) | 🟡 | ❌ | ❌ | ❌ | **❌(仅关键词)** |
| AI 内容助手 | 🟡(插件) | ✅ | ✅ | ✅ | 🟡 | ✅✅ | 🟡 | ❌ | ❌ | ❌ | **❌** |
| MCP 接入 | 🟡 | ✅ | 🟡 | 🟡 | 🟡 | ✅ | 🟡 | ❌ | ❌ | ❌ | **❌** |
| 自动化编排 | 🟡 | ✅ | 🟡 | ✅(Flows) | ✅(Hooks) | ✅ | 🟡 | 🟡 | 🟡 | 🟡 | **❌** |
| 自动化测试/CI | ✅ | ✅✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 🟡 | ❌ | **❌(0 单测)** |
| 前后端分离后台 | — | 🟡 | ✅ | ✅ | ✅ | ✅ | 🟡 | 🟡 | 🟡 | ❌ | **✅** |
| 动态建物理表 | ❌ | 🟡 | ❌ | ✅(镜像) | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ | **✅** |

### 5.3 "最优解"提取（本方案要抄的 20 条）

按对 FlyCms 的价值排序，标注**是否已具备**：

| 序 | 最优解 | 来源 | FlyCms |
|---|---|---|---|
| 1 | 统一错误信封 + 正确状态码语义 | REST 2026 通行实践 | ❌ |
| 2 | CSRF token + `SameSite` + CSP/HSTS 安全头 | OWASP 2026 / WP Nonce | ❌ |
| 3 | API 路径版本化 `/api/v1` | WordPress / REST 2026 | ❌ |
| 4 | 稀疏字段集 `?fields=` | WordPress `_fields` / JSON:API | ❌ |
| 5 | 内容版本 + 草稿 + 回滚 | Payload / Directus Revisions | 🟡 仅模板 |
| 6 | 可配置内容工作流状态机 | Drupal Content Moderation | 🟡 单级 |
| 7 | 字段级权限 | Directus / Payload | ❌ |
| 8 | 可复用区块 / 动态区块 | Strapi Dyn Zone / Craft Matrix / Wagtail StreamField | ❌ |
| 9 | Live Preview（真·所见即所得） | Craft / Storyblok | 🟡 试渲染 |
| 10 | 配置即代码（模型定义可入 git） | Payload / Drupal CMI | ❌ |
| 11 | 内容原生 embeddings（语义搜索/RAG） | Sanity | ❌ |
| 12 | schema-aware AI 内容助手（写入走草稿） | Sanity Agent Actions / CrafterCMS AI | ❌ |
| 13 | MCP server（让外部 Agent 读写内容） | Sanity / CrafterCMS / Drupal | ❌ |
| 14 | 设计令牌（`theme.json` 式） | WordPress | ❌ |
| 15 | 事件-动作自动化编排（Flows） | Directus / Payload Hooks | ❌ |
| 16 | 主题/插件包分发形态 | Halo / WP 插件元数据 | 🟡 皮肤包 |
| 17 | 对象缓存（两级 + 精准失效） | WP Object Cache / Drupal | 🟡 单机 Caffeine |
| 18 | `/health` `/ready` 探针 | REST 2026 / K8s | ❌ |
| 19 | 测试金字塔 + CI 门禁 | 全行业 | ❌ |
| 20 | 富文本结构化（chunk 不丢结构） | Sanity Portable Text | ❌ 现为 HTML |

---

## 6. 差距矩阵：FlyCms vs 主流最佳实践

把 §4 的**内部缺陷**与 §5.3 的**外部差距**合并成一张可施工的表。**这是本方案的核心产出。**

评级：`P0` 阻断 / `P1` 高危 / `P2` 中等 / `P3` 技术债。

| ID | 差距/缺陷 | 现状证据 | 主流最优解（抄谁） | 归属阶段 |
|---|---|---|---|---|
| **G1** | 无全局异常契约 | grep `@ControllerAdvice` = 0；`ErrorMvcAutoConfiguration` 被 exclude | REST 统一错误信封 | **K** |
| **G2** | 无 CSRF / 安全头 / SameSite | `application.yml:32` 仅 httpOnly | OWASP + WP Nonce | **K** |
| **G3** | `extends WebMvcConfigurationSupport` | `WebMvcConfig.java:32` | Spring 官方推荐 `WebMvcConfigurer` | **K** |
| **G4** | 零单测 + 无 CI + 验收脚本不入库 | `src/test` 不存在；`tools/` 被 ignore | 测试金字塔 + CI 门禁 | **K** |
| **G5** | 循环依赖开关 | `application.yml:8` | 构造注入拆环 | **K** |
| **G6** | API 无版本化 | 全部 `/api/**` 无版本段 | WordPress `/wp/v2` | **L** |
| **G7** | 无稀疏字段集 | 列表接口整行返回 | WP `_fields` | **L** |
| **G8** | 无 `/health` `/ready` | 无探针 | REST 2026 | **L** |
| **G9** | `core/` 29 处 `printStackTrace` + 12 处 `System.out` | grep 实测 | 全行业日志规范 | **L** |
| **G10** | 权限查询无缓存 | `PermissionService` 无 `@Cacheable` | 两级缓存 + 精准失效 | **L** |
| **G11** | 107 处 `select *` | grep 实测 | 显式列 + 覆盖索引 | **L** |
| **G12** | 内容无版本/草稿 | 仅 `fly_template_version` | Payload / Directus Revisions | **M** |
| **G13** | 工作流仅单级开关 | `fly_article_audit` 0/1 | Drupal Content Moderation | **M** |
| **G14** | 无字段级权限 | 仅菜单/按钮级 `action_key` | Directus / Payload | **M** |
| **G15** | 无可复用区块 | 模型字段是扁平列表 | Strapi Dyn Zone / Craft Matrix | **M** |
| **G16** | 无 Live Preview | 仅 `preview` 试渲染 | Craft Live Preview | **N** |
| **G17** | 无事件-动作编排 | 无 webhook/规则 | Directus Flows / Payload Hooks | **N** |
| **G18** | 无插件机制 | 唯一扩展方式是改代码/加标签 | WP Hook / Payload Plugins / Halo 插件包 | **N** |
| **G19** | 无设计令牌 | 主题 CSS 硬编码 | WordPress `theme.json` | **N** |
| **G20** | 模型定义不可版本化 | 仅入库，无法 diff/回放 | Payload config-as-code / Drupal CMI | **N** |
| **G21** | 无语义搜索/RAG | Lucene 7.4.0 关键词 | Sanity 内容原生 embeddings | **P** |
| **G22** | 无 AI 内容助手 | 无 | Sanity Agent Actions / CrafterCMS AI | **P** |
| **G23** | 无 MCP 接入 | 无 | Sanity / CrafterCMS / Drupal | **P** |
| **G24** | 富文本为 HTML blob | `mediumtext` 存 HTML | Sanity Portable Text | **P**（配合 G21） |
| **G25** | 仓库卫生（39MB 静态资源 / 101MB .git / 测试残留 / mock app） | 实测 | 仓库瘦身惯例 | **R** |
| **G26** | MyBatis XML 在 `src/main/java` | pom `<resource>` 双挂 | Maven 标准布局 | **R** |
| **G27** | 根级路由冲突面 | `/{channelDir}` vs `/{modelCode}/` | 集中注册 + 启动自检 | **R** |
| **G28** | 命名/依赖债（EhCache 类名、双 CorsFilter、Lucene 7.4） | 实测 | 重构 + 升级 | **R** |
| **G29** | 配置硬编码（DB 口令、CORS 白名单、devtools 默认开） | 实测 | 12-factor / profile 化 | **R** |
| **G30** | MySQL 5.7 EOL + MyISAM 遗产 | 实测 | 升 8.0 + InnoDB | **R**（单独立项） |

---

## 7. 最优解融合方案（逐项施工设计）

> 通用约定沿用《全智能CMS对标分析与开发规划》§8：新接口落 `web/api/ApiXxxController`、`/api/**`、返回 `DataVo`、分页 `PageVo<T>`、端点内 `requirePermission`；上线走"建表 → `permission_sync` → 角色组勾选 → 冒烟"检查单。

### 7.1 G1　统一错误契约（P0，抄 REST 2026 通行实践）

**统一信封**（新增 `core/entity/ErrorVo.java`）：

```json
{
  "code": 40001,
  "message": "参数校验失败",
  "status": 400,
  "path": "/api/system/channel/save",
  "traceId": "3f2a...",
  "errors": [ { "field": "channelDir", "message": "目录名已存在" } ],
  "timestamp": "2026-09-28T11:20:00+08:00"
}
```

**新增 `core/exception/GlobalExceptionHandler.java`**：

```java
@RestControllerAdvice(basePackages = "com.flycms.web.api")
public class GlobalExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)   // 承接 requireAdmin/requirePermission 的 401/403
    @ExceptionHandler(MethodArgumentNotValidException.class) // @Valid 校验 → 400 + errors[]
    @ExceptionHandler(BusinessException.class)         // 业务异常 → 200 + code!=0（沿用 DataVo 语义）
    @ExceptionHandler(Exception.class)                 // 兜底 → 500，**不外泄堆栈**
}
```

要点：
- **保留 `DataVo` 语义不动**（`code 0/-1`），只在"异常路径"补 `ErrorVo`；两者通过 HTTP 状态区分（异常走 4xx/5xx，业务失败仍 200 + `code=-1`）。这样 **17 个已用 `DataVo` 的控制器零改动**。
- 每个响应带 `traceId`（MDC 注入），与前端的错误定位打通。
- `Application.java` 的 `exclude` 中**恢复 `ErrorMvcAutoConfiguration`**（前台 Freemarker 错误页需要它），或改为显式声明 `ErrorPageRegistrar`。
- `@RestControllerAdvice(basePackages="com.flycms.web.api")` 限定包名，**避免污染前台 `web/front` 的 Freemarker 错误页行为**（前台仍走 `theme.getPcTemplate("404")`）。

### 7.2 G2　CSRF / 安全头 / SameSite（P1，抄 OWASP 2026 + WP Nonce）

三步，可独立上线：

1. **Cookie 加固**：`application.yml` 加 `sameSite: lax`（管理端足够；如需 `strict` 需评估从外部链接跳回后台的体验）。生产 profile 追加 `secure: true`。
2. **CSRF 双提交 Token**：
   - 新增 `config/CsrfConfig`：注册 `CookieCsrfTokenRepository.withHttpOnlyFalse()`（前端需读 Cookie 回填请求头）+ `CsrfFilter`，仅作用于 `/api/**` 的 **非 GET** 方法。
   - 前端 `apps/web-antd/src/api/request.ts` 拦截器自动带 `X-XSRF-TOKEN`。
   - **灰度策略**：先以"仅记录不拦截"（监听模式）跑一周，确认无漏网端点后切强制。
3. **安全响应头过滤器**（新增 `filter/SecurityHeaderFilter.java`）：

```java
X-Content-Type-Options: nosniff
X-Frame-Options: SAMEORIGIN          // 后台禁被嵌套，防点击劫持
Referrer-Policy: strict-origin-when-cross-origin
Content-Security-Policy: default-src 'self'; img-src 'self' data: https:; ...（后台 SPA 单独放宽）
Strict-Transport-Security: max-age=31536000; includeSubDomains   // 仅 HTTPS 生效
```

4. **CORS 白名单收敛**：`CorsConfig` 白名单改为读配置（`flycms.cors.allowed-origins`），**默认仅 dev profile 放行 `localhost:*`**；生产不出现 `localhost`。

### 7.3 G3　MVC 配置回归 Boot 装配（P1）

```java
// 前
public class WebMvcConfig extends WebMvcConfigurationSupport { ... super.extendMessageConverters(...) ... }

// 后
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override public void addInterceptors(InterceptorRegistry registry) { ... }
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry) { ... }
    @Override public void extendMessageConverters(List<HttpMessageConverter<?>> converters) { ... } // 去掉 super 调用
}
```

**验收红线**：改造后回归 `e2e PASS 51` + 静态资源可达 + Long→String 序列化仍生效（雪花 ID 末位不丢）。

### 7.4 G4　测试与 CI 门禁（P1）

| 层 | 内容 |
|---|---|
| 单元测试 | JUnit 5 + Mockito：优先覆盖 `core/utils/**`（纯函数、易测）、`TemplateCenterService` 的路径白名单/zip-slip 防御、`ModelTableService` 的 DDL 白名单 |
| 集成测试 | `spring-boot-test` + **Testcontainers MySQL 5.7**：覆盖 Mapper 层（`select *` 改造后必需）+ 关键 API 的 401/403/200 三态 |
| E2E | 现有 `tools/*.py` **移入仓库**（`.gitignore` 改 `tools/*` + `!tools/e2e_template_check.py` + `!tools/audit_extra_check.py` + `!tools/CapSolver.java`），作为 release 门禁 |
| CI | GitHub Actions 三 job：`backend-test`（mvn test）、`backend-build`（mvn package）、`frontend-check`（pnpm check:type + build）；PR 必须全绿 |

**目标基线**：核心工具类行覆盖 ≥ 60%；关键 API 三态用例 ≥ 30 条。

### 7.5 G5　拆循环依赖（P1）

`userService ↔ userSessionUtils`：把"会话读写"职责从 `UserSessionUtils` 下沉到独立的 `UserSessionStore`（不依赖 `UserService`），`UserService` 再依赖它。拆完删掉 `allow-circular-references`，由启动失败自证拆干净。

### 7.6 G6–G8　API 治理（P2，抄 WordPress `/wp/v2` 与 REST 2026）

- **版本化**：新增 `/api/v1/system/**` 映射（`@RequestMapping({"/api", "/api/v1"})` 双注册，保证存量前端零改动），并在网关/文档标注 v1 为稳定版。
- **稀疏字段集**：`PageVo` 增加 `fields` 支持——查询参数 `?fields=id,title,createTime` 经统一后置处理器裁剪 JSON（**不改 SQL**，兼容 107 处 `select *`）。
- **探针**：`GET /api/health`（进程存活）与 `GET /api/ready`（DB 连通 + 关键缓存可用），供负载均衡/K8s 使用。

### 7.7 G9–G11　一致性与性能（P2）

- G9：`core/**` + `filter/**` 的 29 处 `printStackTrace` 与 12 处 `System.out` 全量替换为 `log.debug/warn`（补扫口径与 P4 对齐，完成后 grep 归零）。
- G10：`PermissionService.findPermissionByUserId` 加 `@Cacheable(value="permission", key="#userId")`；在角色-权限关联变更处 `@CacheEvict`。
- G11：按"高频列表查询"优先级收敛 `select *`——先做 `fly_article`、`fly_cmodel_*`、`fly_channel`（这三个是列表页热点），详情查询保留 `*`。

### 7.8 G12　内容版本与草稿（P2，抄 Payload versions+drafts / Directus Revisions）

**表**（沿用既有雪花 ID 风格）：

```sql
CREATE TABLE `fly_content_version` (
  `id` bigint(20) NOT NULL,
  `target_type` varchar(30) NOT NULL COMMENT 'article/question/share/cmodel',
  `target_id` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `content_json` mediumtext COMMENT '整条内容快照（含自定义字段）',
  `status` tinyint(2) DEFAULT '0' COMMENT '0草稿 1已发布 2历史',
  `editor_id` bigint(20) DEFAULT NULL,
  `remark` varchar(200) DEFAULT '',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_target_ver` (`target_type`,`target_id`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容版本快照';
```

**与模板版本的差异（关键设计）**：模板版本是"文件为事实源 + DB 存快照"；**内容版本反过来——DB 为事实源，快照即版本**。保存链路：写 `fly_content_version`（version+1）→ 更新主表 → 清缓存。

**配套**：`content/version/list`、`content/version/diff`、`content/version/restore`（恢复=写入新版本，不丢历史，与模板版本同语义）。

### 7.9 G13　可配置工作流状态机（P2，抄 Drupal Content Moderation）

不做多级审批流（尊重原决策），但把**硬编码的 0/1 审核开关升级为可配置状态集**：

```sql
CREATE TABLE `fly_content_workflow` (
  `id` bigint(20) NOT NULL,
  `workflow_code` varchar(40) NOT NULL,
  `target_type` varchar(30) NOT NULL,
  `states` varchar(500) DEFAULT '' COMMENT 'JSON：[{code,name,color,allowed_roles}]',
  `transitions` varchar(1000) DEFAULT '' COMMENT 'JSON：[{from,to,roles,notify}]',
  `is_default` tinyint(2) DEFAULT '0',
  PRIMARY KEY (`id`), UNIQUE KEY `uk_code` (`workflow_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容工作流定义';
```

默认只装一条"未审 → 通过 / 驳回"工作流（与现状等价），需要时后台加状态。**保留 `fly_article_audit` 开关作为快捷方式**（向后兼容）。

### 7.10 G14　字段级权限（P2，抄 Directus / Payload）

`fly_admin_permission.action_key` 已支持 `模块:动作` 形态（`CheckUrlUtils.match` 支持通配）。扩展为支持 **字段粒度**：`article:edit:field:price`。

- 后端：`ApiBaseController` 增加 `requireFieldPermission(actionKey, field)`；列表/详情响应经统一后置处理器**按角色剔除无权字段**（ResponseBodyAdvice）。
- 前端：表单按 `accessCodes` 隐藏/只读无权字段（复用现有 `useAccess`）。

### 7.11 G15　可复用区块（P2，抄 Strapi Dynamic Zones / Craft Matrix / Wagtail StreamField）

**问题**：`fly_model_field` 是扁平列表，做不出"文章里插一段图文块/引用块/商品卡"。

**方案**：新增"区块类型"概念——

```sql
CREATE TABLE `fly_block_type` (
  `id` bigint(20) NOT NULL,
  `type_code` varchar(40) NOT NULL,
  `type_name` varchar(60) NOT NULL,
  `icon` varchar(60) DEFAULT '',
  `fields_json` text COMMENT '区块内字段定义（复用 FieldTypeEnum）',
  `tpl_snippet` mediumtext COMMENT '前台渲染片段（Freemarker）',
  `status` tinyint(2) DEFAULT '1',
  PRIMARY KEY (`id`), UNIQUE KEY `uk_type_code` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='可复用区块类型';
```

模型的 `FieldTypeEnum` 增加第 16 种字段类型 `BLOCKS`（存区块实例数组 JSON）；模板用 `<@fly_blocks value=... />` 遍历渲染。**这正是 WordPress Block / Craft Matrix 的同构能力**，也是"全智能后台"从"字段可配"走向"结构可配"的关键一步。

### 7.12 G16　Live Preview（P2，抄 Craft CMS）

现有 `preview`（模板试渲染）升级为**草稿真预览**：

1. 后台编辑页右侧 iframe 指向 `/{前台URL}?__preview=1&__token=<一次性 token>`；
2. 前台 `ChannelRenderService`/`ModelController` 检测到预览 token → 从 `fly_content_version` 取**最新草稿**而非已发布数据渲染；
3. token 由 `TokenUtils` 签发（短时效、绑定 adminId + targetId），**不进搜索引擎**（响应头 `X-Robots-Tag: noindex`）。

### 7.13 G17　事件-动作编排（P2，抄 Directus Flows）

不做可视化拖拽编排器（防止复杂度爆炸），做**规则表 + 内置动作集**：

```sql
CREATE TABLE `fly_automation_rule` (
  `id` bigint(20) NOT NULL,
  `rule_name` varchar(60) NOT NULL,
  `event` varchar(40) NOT NULL COMMENT 'content.created/content.updated/comment.posted/form.submitted/...',
  `conditions` varchar(1000) DEFAULT '' COMMENT 'JSON 条件（字段=值 / 状态=值）',
  `actions` varchar(1000) DEFAULT '' COMMENT 'JSON：[{type:webhook|notify|reindex|cache_evict, params:{...}}]',
  `status` tinyint(2) DEFAULT '1',
  PRIMARY KEY (`id`), KEY `idx_event` (`event`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动化规则';
```

发布事件 → 异步匹配规则 → 执行动作。**这是"插件机制的轻量替代"**：把"二开"从改代码降到配规则，直接回应《全智能CMS对标分析与开发规划》§6.7 的目标。

### 7.14 G18　插件机制（P3→远期，抄 WordPress Hook + Halo 插件包）

**结论：本期不实现完整插件系统**（投入巨大、生态为零时无收益），但**必须先留扩展点**：
- 在核心服务（内容保存、评论、表单提交、渲染）预留 **Spring 事件发布**（`ApplicationEventPublisher`），作为未来"事件型插件"的稳定契约；
- 皮肤包（已实现）作为"插件包"分发的形态先例，格式上加 `manifest.json` 的 `type: theme|plugin` 预留位。

> 理由与《全智能CMS对标分析与开发规划》§5.2「生态为零」判断一致：没有生态时先做插件系统是空转。**先留扩展点，等有第三方需求再开。**

### 7.15 G19　设计令牌（P3，抄 WordPress `theme.json`）

主题目录新增 `theme.json`（`views/templates/pc_theme/{skin}/theme.json`）：

```json
{
  "version": 1,
  "settings": {
    "color": { "palette": [ {"slug":"primary","color":"#…","name":"主色"} ], "custom": false },
    "typography": { "fontSizes": [...] },
    "spacing": { "spacingSizes": [...] },
    "layout": { "contentSize": "720px", "wideSize": "1200px" }
  }
}
```

前台以 CSS 变量输出（`--wp--preset--color--primary` 同构写法），模板改色不再改 CSS 文件。**`custom: false` 语义照抄**：锁死品牌色，防运营改坏。

### 7.16 G20　配置即代码（P3，抄 Payload / Drupal CMI）

新增 `model/export`（导出当前模型/字段/栏目/区块/工作流定义为**单个 JSON/YAML**）与 `model/import`（幂等导入）。产出落 `config/site-model.yaml` 入 git → 模型变更有 diff、可回放、可跨环境同步。**这是把"配置在库里"升级为"配置在版本控制里"的关键**（也是 Drupal CMI 与 Payload 相对 WP 的结构性优势）。

### 7.17 G21–G24　AI 原生（P3，抄 Sanity + CrafterCMS AI）

> **这是"全智能 CMS"名实相符的核心**。CrafterCMS AI 用 Spring AI + OpenSearch 的 Java 栈实现，是 FlyCms（同为 JVM）最直接的参照。

| 子项 | 方案 |
|---|---|
| **G24 富文本结构化** | 新增 `rich_text` 存储形态：编辑器输出 JSON 块结构（而非 HTML blob），渲染时转 HTML。**这是 RAG 的前提**——HTML blob 在 chunk 时会丢标题层级/列表边界。参考 Sanity Portable Text 的"marks + blocks"模型 |
| **G21 语义搜索** | `SearchService` 已有 ES 接缝：接入 **Spring AI + 向量库**（优先 OpenSearch/Elasticsearch 向量检索，与 CrafterCMS 一致；或 pgvector 作为轻量选项）。内容发布事件（G17）触发 embedding 同步，**向量与内容天然同步**（Sanity Embeddings Index 的思路） |
| **G22 AI 内容助手** | 编辑器内 AI：生成/改写标题、摘要、标签、SEO 描述、翻译、配图提示词。**硬红线：AI 输出一律落 `status=0` 草稿**，人工审核后才发布（Sanity/Contentful 的一致做法，规避幻觉与合规风险）。模型接入走可配置 provider（OpenAI / Anthropic / DeepSeek / 通义），密钥入配置不入库 |
| **G23 MCP server** | 新增 MCP 端点，暴露 `content.search / content.get / content.create(draft) / content.update`，让外部 Agent（Claude/Cursor 等）能安全读写内容。**写入强制走草稿**。这是 2026 年 CMS 的新标准接口 |

**落地顺序建议**：G24 → G21 → G22 → G23（结构先行，检索次之，生成再次，Agent 最后）。

### 7.18 G25–G30　工程债清偿（P3，见 §8）

---

## 8. 架构与路径优化建议

### 8.1 包结构：`core` 职责过载

```
现状：core/{base, controller, entity, exception(空), service, utils}
问题：utils 下混着 30+ 工具类 + lucbir 图像检索 + captcha 实现，无内聚边界
建议：
  core/
    ├─ web/        BaseController, AbstractTagPlugin, Plugin
    ├─ entity/     DataVo, PageVo, ErrorVo
    ├─ exception/  GlobalExceptionHandler, BusinessException
    └─ support/    utils 按域拆：support/security(BCrypt/Token/Upload/SqlSafe)
                              support/media(Image/ScaleImage/File)
                              support/text(String/Filter/Jsoup/SymbolConvert)
                              support/misc(Date/Ip/Math/SnowFlake/SpringContext)
  （lucbir / captcha 抽为独立 feature 包，不进 core）
```

### 8.2 循环依赖拆除

见 §7.5。拆环后删除 `allow-circular-references`，用启动失败自证。

### 8.3 MyBatis XML 布局归位

43 个 Mapper XML 从 `src/main/java/com/flycms/module/**/dao/` 迁至 `src/main/resources/mapper/<module>/`，并：
- `pom.xml` **移除** `<resource><directory>src/main/java</directory>` 的非标准配置；
- `application.yml` `mapper-locations: classpath:mapper/**/*.xml`；
- 风险与对策：这是一次**大面积机械改动**，必须靠 §7.4 的 Testcontainers 集成测试兜底；建议在 K 阶段（测试就位）之后单独排一个批次做。

### 8.4 路由收敛

根级单段路由（`/{channelDir}`、`/{modelCode}/`、`/index-{sort}`、`/people/{shortUrl}` …）依赖 Spring 精确度排序。建议：
- 新增 **启动自检**：`ApplicationReadyEvent` 里遍历 `RequestMappingHandlerMapping`，命中"同形状（相同段数、同位置变量）"的 pattern 即 `log.error` 报警（不阻断启动，但能在日志里第一时间发现）；
- 中期方案：栏目统一前缀 `/c/{dir}`（保留 `/{dir}` 301 重定向 3 个月过渡，不毁 SEO）。

### 8.5 配置外部化（12-factor）

| 项 | 现状 | 目标 |
|---|---|---|
| DB 口令 | `application.yml` 明文 | 环境变量 `FLYCMS_DB_PASSWORD` + profile |
| CORS 白名单 | `CorsConfig` 硬编码 | `flycms.cors.allowed-origins` 配置项 |
| `devtools` | 默认开 | 仅 `dev` profile 开 |
| 上传路径/大小 | 部分硬编码 | 统一 `flycms.upload.*` |

### 8.6 仓库瘦身

| 动作 | 说明 |
|---|---|
| `backend/views/static/` | 1747 文件/39MB 老主题资源。**方案**：保留主题实际引用的资源，删除 `jquery-migrate-1.2.1`、`html5shiv` 等死引用；或整体迁到 theme 目录随皮肤包分发 |
| `_testdata/wpdemo` | 删除（WP 转换测试残留） |
| `frontend/apps/backend-mock` | 删除（vben 上游样本，未使用） |
| `.git` 101MB | 上述清理 + `git gc --aggressive`；如需彻底瘦身需 `filter-repo` 重写历史（**风险高，需团队确认**） |
| `tools/` | 改 `.gitignore` 为 `tools/*` + 3 条否定规则，让验收脚本入库 |

### 8.7 依赖与命名

| 项 | 动作 |
|---|---|
| Lucene 7.4.0 | 短期不动（仅 lucbir 图像检索用，非主链路）；中期随 G21 一起换成 ES/OpenSearch 向量检索 |
| `EhCacheConfig` | 重命名 `CacheConfig` |
| `filter/CorsFilter` | 删除重复类，统一走 `config/CorsConfig` 的 Bean |
| `fly_templet` | 评估废弃（零引用） |

---

## 9. 分阶段实施计划（K–R）

> 优先级：**K/P1 最先行**（工程底座是所有后续的安全网）；L/N/P 可穿插；R 消化历史习气。

| 阶段 | 名称 | 覆盖 | 优先级 | 依赖 | 完成后解锁 |
|---|---|---|---|---|---|
| **K** | **工程底座补齐** | G1–G5 | **P1** | 无 | 全局错误契约、CSRF、Boot 装配回归、测试与 CI、拆环 → **后续所有阶段的安全网** |
| **L** | **API 治理与一致性** | G6–G11 | P2 | K | 版本化、稀疏字段、探针、日志口径统一、权限缓存、`select *` 收敛 |
| **M** | **内容治理深化** | G12–G15 | P2 | K | 内容版本/草稿、可配置工作流、字段级权限、可复用区块 |
| **N** | **编辑体验与可配置性** | G16–G20 | P2/P3 | M | Live Preview、自动化规则、插件扩展点、设计令牌、配置即代码 |
| **P** | **AI 原生能力** | G21–G24 | P3 | N（G24 可先行） | 语义搜索/RAG、AI 内容助手、MCP 接入 → **"全智能"名实相符** |
| **R** | **工程债清偿** | G25–G30 | P3 | K（测试兜底） | 仓库瘦身、XML 归位、路由收敛、配置外部化、依赖升级 |
| **U** | **旧模块退役与统一生产（宗旨落地）** | U1–U6 | **P1** | L（引擎 E1/E3 已验收） | 硬编码内容模块全部退役，一切内容由自定义模型生产 → **万能系统形态** |

**关键路径**：`K → （L / M 并行） → N → P`；R 全程穿插（但 **XML 迁移必须在 K 之后**，否则无测试兜底）；**U 与 M 并行推进**（U 是产品总纲，M 的内容治理能力必须落在自定义模型通道上，不为旧模块重复建设）。

### 9.1 阶段 K 详表（最先行）

| # | 事项 | 交付物 | 验收 |
|---|---|---|---|
| K1 | 全局异常契约（G1） | `ErrorVo` + `GlobalExceptionHandler` + 恢复 `ErrorMvcAutoConfiguration` | API 抛异常返回结构化 JSON（含 traceId）；前台 404/500 页仍正常 |
| K2 | CSRF + 安全头（G2） | `CsrfConfig` + `SecurityHeaderFilter` + `sameSite` + CORS 配置化 | 无 token 的 POST 被拒；响应含 CSP/X-Frame-Options；前端登录后正常提交 |
| K3 | MVC 装配回归（G3） | `WebMvcConfig implements WebMvcConfigurer` | `e2e PASS 51` + 静态资源可达 + 雪花 ID 不丢精度 |
| K4 | 测试与 CI（G4） | JUnit + Testcontainers + GH Actions + `tools/` 入库 | CI 全绿；核心工具类覆盖 ≥ 60% |
| K5 | 拆循环依赖（G5） | `UserSessionStore` + 删除 `allow-circular-references` | 应用正常启动（无循环依赖报错） |

### 9.2 里程碑与工作量（单人估算，人日）

| 阶段 | 工作量 | 说明 |
|---|---|---|
| K | 8~12 | 含 CSRF 灰度观察期 |
| L | 6~9 | `select *` 收敛按热点分批 |
| M | 14~20 | 内容版本 + 工作流 + 字段权限 + 区块（区块最重） |
| N | 12~18 | 自动化规则 + 设计令牌 + 配置导出 + Live Preview |
| P | 20~30 | 富文本结构化改造最重（涉及存量数据） |
| R | 8~14 | XML 迁移 + 仓库瘦身 + 配置外部化 |

### 9.3 实施进度

> 图例：`[x]` 已完成并验收 / `[~]` 部分完成 / `[ ]` 未开始

#### 阶段 K —— 工程底座补齐（**已完成**）

| # | 事项 | 状态 | 交付物与验收证据 |
|---|---|---|---|
| K1 | 全局异常契约（G1） | `[x]` | 新增 `core/entity/ErrorVo`、`core/exception/GlobalExceptionHandler`（`@RestControllerAdvice(basePackages="com.flycms.web.api")`）、`core/exception/BusinessException`、`filter/TraceIdFilter`；`Application` 恢复 `ErrorMvcAutoConfiguration`。**实测**：未登录 `GET /api/auth/codes` → `401` + `{"code":40101,"message":"未登录或登录态已失效","path":...,"status":401,"timestamp":...,"traceId":"..."}`，响应头含 `X-Trace-Id`；前台 `/403`、`/404`、`/500` 主题页与首页 200 全部不受影响 |
| K2 | CSRF + 安全头 + SameSite + CORS 配置化（G2） | `[x]` | 新增 `config/CsrfConfig`、`filter/CsrfFilter`（双提交 Cookie，**默认监听模式**，实测 85 条 `CSRF 监听` 日志）、`filter/SecurityHeaderFilter`；`session.cookie` 加 `sameSite=lax`，prod 加 `secure=true`；`CorsConfig` 白名单改为 `flycms.cors.allowed-hosts` 配置项（dev 仅本机 / prod 仅真实域名）并改用 `FilterRegistrationBean` 置顶；**删除**重复的 `filter/CorsFilter`（原全局 `Access-Control-Allow-Origin: *`）。实测响应含 `X-Content-Type-Options` / `X-Frame-Options` / `Referrer-Policy` / `Content-Security-Policy` 与 `XSRF-TOKEN` Cookie |
| K3 | MVC 装配回归（G3） | `[x]` | `WebMvcConfig` 由 `extends WebMvcConfigurationSupport` 改为 `implements WebMvcConfigurer`；**连带修正 `WebSocketConfig` 同类继承**（两个子类各继承一份 `@Bean localeResolver` → `BeanDefinitionOverrideException`）。验收：**e2e PASS 51 / FAIL 0**、静态资源 200（`/assets/**`）、雪花 ID 序列化未回退 |
| K4 | 测试与 CI（G4） | `[x]` | 新增 `src/test`：**31 个用例（30 通过 / 1 按环境跳过）**——`CheckUrlUtilsTest`（权限通配符，越权红线）、`SqlSafeUtilTest`（动态 SQL 标识符）、`UploadSafeUtilTest`（上传三重校验/双后缀绕过）、`DataVoTest`、`ErrorVoTest`，以及 `ApiErrorContractIntegrationTest`（Testcontainers MySQL 5.7，无 Docker 自动跳过，CI 上执行）；新增 `.github/workflows/ci.yml` 三 job（backend-test / backend-build / frontend-check）；`.gitignore` 改为 `tools/*` + 4 条否定规则，验收脚本入库 |
| K5 | 拆循环依赖（G5） | `[x]` | 删除 `spring.main.allow-circular-references`，并**拆掉由此暴露的三组循环**：① `UserService ↔ UserSessionUtils` → 新增 `UserSessionStore`（只依赖 DAO）；② `QuestionService ↔ AnswerService` → `AnswerService` 改用已有 `questionDao`；③ `AbstractTagPlugin.init()` 在 `@PostConstruct` 中 `getBean(自身)` 的自循环 → 改用 `this`。验收：**无该项配置下应用正常启动**（启动失败自证拆干净） |

#### 阶段 L —— API 治理与一致性（**已完成**）

| # | 事项 | 状态 | 交付物与验收证据 |
|---|---|---|---|
| G6 | API 版本化 | `[x]` | 新增 `filter/ApiVersionFilter`（`@Order(HIGHEST_PRECEDENCE+15)`）：把 `/api/v1/**` **重写**为 `/api/**`（`HttpServletRequestWrapper` 覆写 `getRequestURI`/`getServletPath`/`getRequestURL`）。**刻意不用 `@RequestMapping` 双注册**——双注册会调 `getSyncAllPermission()` 重复登记权限行。**实测**：`GET /api/v1/system/modelData/list/3` → 200 且载荷与 `/api/**` 完全一致 |
| G7 | 稀疏字段集（`_fields`） | `[x]` | 新增 `web/api/SparseFieldAdvice`（`@ControllerAdvice(basePackages="com.flycms.web.api")` + `ResponseBodyAdvice`）：解析 `?fields=a,b,c`，对 `DataVo.data` 的 JSON 树裁剪顶层键；识别分页信封（含 `list` 数组）只裁 `list` 内行。**实测**：`GET /api/system/modelData/list/3?fields=id,title` → 行键恰为 `['id','title']`；不带 `fields` 时 10 列 |
| G8 | 健康/就绪探针 | `[x]` | 新增 `web/api/ApiHealthController`：`GET /api/health`（存活，不查依赖）与 `GET /api/ready`（就绪，`DataSource.isValid(2)` + `CacheManager` 非空，失败 503）。**实测**：`/api/health` → `{"status":"UP","app":"FlyCms"}`；`/api/ready` → `{"status":"UP","checks":{"database":"UP","cache":"UP"}}` |
| G9 | 日志口径统一 | `[x]` | `core/**` + `filter/**` 共 29 处 `printStackTrace` → `log.warn("操作异常，已降级处理", e)`、14 处 `System.out/err.print*` → `log.debug/ info`（17 个文件）。补扫阶段再清 6 处真实残留（`SnowFlake.main` / `ImagesService.main` 两处死代码 `main` 直接删除；`MyJobRunner` 补 `@Slf4j` 后 `log.info`；`MyTaskTest` 用已有 `log`）。**验收**：排除注释后 `printStackTrace|System.out.print|System.err.print` 命中 **0** |
| G10 | 权限查询缓存 | `[x]` | `PermissionService.findPermissionByUserId` 加 `@Cacheable(value="permission", key="#userId")`；`getSyncAllPermission` / `deletePermission` / `updatePermissions` 各加 `@CacheEvict(value="permission", allEntries=true)`（写侧精准失效） |
| G11 | `select *` 收敛（热点列表） | `[x]` | ① `fly_cmodel_*`：`ModelDataDao.selectPage` 由 `SELECT *` 改为**列白名单投影**（`<foreach>` + `<choose>` 空则回退全列），列清单由 `ModelDataService.listColumns()` 依「固定列常量 + 启用模型字段（排除 `TEXTAREA` 与 `content` 通道）」生成并过 `SqlSafeUtil.safeColumnName`；② `fly_article`：新增 `articleListColumn(-Aliased)` 片段，`getArticleList`（**保留 content**：`type_article.html` 用 `fly_stringcut` 出摘要）改显式列，`getArticleIndexList` / `getArticleAuditList` 剔除 longtext `content`；③ `fly_channel`：`ChannelDao.xml` 原已用 `<sql id="ch_column">` 显式列，登记为已满足。详情查询（`findDataById` / `findByShortUrl` / `findArticleByShorturl` / `findArticleById` / `findArticleByPK`）按规划**保留 `*`**。**实测**：`fly_cmodel_articles` 16 行、其中 14 行 `content` 非空，`GET /api/system/modelData/list/3?rows=20` 返回 16 行**含 content 的为 0 行**；`/articles/`、`/downloads/`、`/images/`、`/testdemo/` 列表页全 200；**e2e PASS 51 / FAIL 0**、**audit PASS 16 / FAIL 0** |

---

## 10. 决策复审：原"不做清单"的再评估

《全智能CMS对标分析与开发规划》§7.4 排除了 6 项。对标 2026 主流后重新评估如下（**结论：维持 5 项，调整 1 项**）：

| 项 | 原决策 | 复审结论 | 理由 |
|---|---|---|---|
| 多站点 | 不做 | **维持不做** | Craft/Drupal 有原生多站点，但引入 `site_id` 会让全部表/缓存键复杂化一个量级；FlyCms 单站定位清晰，多站需求用"多实例共库"过渡 |
| 多语言 | 不做 | **调整为"细分决策"** | 关键新信息：**Drupal 原生三层多语言、迅睿 CMS 原生多语言且无商业授权风险**——外贸站是国产 CMS 的真实主战场。建议：**内容级 i18n 延后，但先做"栏目级 locale 前缀 + 界面翻译"**（i18n 基础设施已存在，成本可控）；§12 附录给出最小方案 |
| 独立伪静态重写引擎 | 不做 | **维持不做** | 判断未变；URL 已短链化，存量迁移用 Nginx rewrite |
| 爬虫式采集 | 不做 | **维持不做** | 合规风险与漏洞史不变；CSV 导入 + 投稿 API + 表单已覆盖 |
| 多级审批工作流 | 不做 | **维持不做**，但升级为**可配置状态机** | §7.9：状态集可配 ≠ 多级审批流；既保留"单级够用"的判断，又给未来留了空间 |
| 可视化拖拽搭建 | 缓做 | **维持缓做**，但先落 **G15 可复用区块** | WordPress Block / Craft Matrix 证明"区块化"是拖拽的 80% 价值且成本 20%；拖拽编辑器仍留远期 |

**新增的"明确不做"（防止路线膨胀）**：

| 项 | 决策 | 理由 |
|---|---|---|
| 完整插件市场 | **本期不做** | 无生态时做插件系统是空转；先留 Spring 事件扩展点（§7.14） |
| 可视化自动化编排器 | **不做** | Directus Flows 的可视化画布投入大；用规则表 + 内置动作集覆盖 90% 场景（§7.13） |
| 自研向量数据库 | **不做** | 直接用 ES/OpenSearch 或 pgvector，不重复造轮子 |

---

## 11. 验收与上线检查单

沿用《全智能CMS对标分析与开发规划》附录 C，并补充本批次新增项：

1. `sql/<阶段>.sql`：建表 + `fly_admin_permission` 插菜单/按钮行 + 超管组授权；
2. 更新基线快照 `sql/flycms_20260928_*.sql`（**每次备份后必须同步文档中所有旧文件名引用**——这是本轮 §2 发现的教训）；
3. `GET /system/admin/permission_sync` 注册 action_key；
4. 角色组勾选新权限；
5. 后端 `mvn -o -B compile` SUCCESS；前端涉类型改动跑 `pnpm check:type`；
6. **新增**：`mvn test` 全绿（K 阶段后为强制项）；
7. **新增**：`tools/e2e_template_check.py` PASS + `tools/audit_extra_check.py` PASS；
8. 冒烟：登录 → 菜单 → 新页面 CRUD → 未授权账号访问新端点应 403/401；
9. 涉及前台路由/模板的：`/ac/`、`/{modelCode}/` 既有 URL 全回归（**不毁存量是红线**）；
10. **新增**：启动日志无 `Ambiguous mapping` / 循环依赖告警；`/api/health`、`/api/ready` 返回 200。

---

## 12. 附录

### 附录 A：本轮实证发现汇总（证据索引）

| 类别 | 数量 | 关键证据 |
|---|---|---|
| 文档与代码不一致 | 6 类 | JDK 25 vs 24；48 tags vs 56；59 表 vs 78；`frontend-access-guide.md` 失效；`sql/custom-model.sql` 失效；`application.yml:22/30` 行号漂移 |
| P0 缺陷 | 1 | `@ControllerAdvice` 0 命中 + `ErrorMvcAutoConfiguration` 被 exclude |
| P1 缺陷 | 4 | 无 CSRF/SameSite；`extends WebMvcConfigurationSupport`；0 单测 0 CI；`allow-circular-references` |
| P2 缺陷 | 7 | 29 处 printStackTrace（core）；权限无缓存；107 处 `select *`；39MB/1747 文件入库；类名/双 CorsFilter/Lucene 7.4；XML 在 src/main/java；根级路由冲突面 |
| P3 技术债 | 5 | MySQL 5.7 EOL；静态化概念混淆；配置硬编码；`fly_templet`；注释噪音 |

### 附录 B：对标信息来源（2026）

- WordPress 6.8+/FSE 与 `theme.json` 设计令牌、Block 开发、`_fields` 稀疏字段集（WordPress 官方文档 + 2026 企业级插件开发指南）
- Drupal 11 / Drupal CMS：Entity-Field 建模、原生三层多语言、Content Moderation、JSON:API、Config Management、Canvas（Drupal 官方 + DXP Scorecard 2026）
- Strapi v5 / Directus v11 / Payload v3 三方对比（Content-Type Builder、Components/Dynamic Zones、Flows、Revisions、versions+drafts、字段级权限、config-as-code）
- Sanity 的 AI-native 定位：Embeddings Index API、Portable Text、Agent Actions、Content Releases（llmcms.org 2026 AI CMS 评测）
- CrafterCMS AI：Spring AI + OpenSearch 向量、MCP integration、多 LLM provider（开源 Agentic CMS 发布说明）
- Craft CMS 5/6 Roadmap：Matrix 字段、Live Preview、Project Config、多站点
- Ghost：开箱 SEO 与性能、会员订阅（Ghost Review 2026）
- 国产 CMS 现状：迅睿 CMS（原生多语言/多站点/RBAC/插件规范）、帝国 CMS（碎片/定时/参数白名单）、织梦 DedeCMS（停更与安全）、PHPCMS（停更）（电云 IDC / CSDN / 博客园 2026 对比文）
- 安全最佳实践：OWASP Top 10 2025/2026、Patchstack《State of WordPress Security 2026》（91% 漏洞在插件）、Verizon DBIR 2026（31% 泄露源自软件漏洞）
- REST API 2026：资源化命名、状态码语义、版本化、稀疏字段集、游标分页、统一错误信封、`/health` `/ready`

### 附录 C：最小多语言方案（若 §10 决策启动）

```sql
-- 不引入 site_id，只在"需要多语言的实体"上加 locale 维度
ALTER TABLE fly_channel     ADD COLUMN locale varchar(10) DEFAULT 'zh-CN';
ALTER TABLE fly_article     ADD COLUMN locale varchar(10) DEFAULT 'zh-CN';
ALTER TABLE fly_cmodel_*    ADD COLUMN locale varchar(10) DEFAULT 'zh-CN';  -- 逐模型
CREATE TABLE fly_content_i18n (
  id bigint(20) NOT NULL,
  target_type varchar(30) NOT NULL,
  target_id bigint(20) NOT NULL,
  locale varchar(10) NOT NULL,
  title varchar(200) DEFAULT '', summary varchar(500) DEFAULT '', content mediumtext,
  PRIMARY KEY (id), UNIQUE KEY uk_target_locale (target_type, target_id, locale)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

前台路由加 locale 前缀 `/{locale}/{channelDir}`（默认 locale 不带前缀，保证存量 URL 不变）。**成本可控点**：i18n 基础设施（`spring.messages`）已存在；菜单/模板文案走 messages，内容走 `fly_content_i18n`。

---

## 9.4 阶段 U —— 旧模块退役与统一生产（宗旨落地，新增于 2026-09-28）

> **宗旨**：自定义模型引擎是本系统核心技术；一切内容模块由后台在线建模生产（万能系统）。
> 引擎前置能力已验收：E1 关联引用（relate/relates，含目标存在性/发布状态校验 + `{field}Obj`/`{field}List` 读取展开）、E3 URL 型图片/文件（image_url/file_url，varchar(500) + XSS 字符拦截）——探针 11/11 PASS。
> 退役路径（每个旧模块四步）：① `fly_model` 注册模型 + 种子字段 → ② 存量数据迁 `fly_cmodel_{code}`（有量才迁）→ ③ 后台页面收敛到通用 `modeldata`、前台模板走 `cmodel` 通道 → ④ 删除旧代码（module/ + web.front/ + web.api/ + 前端页面 + 旧模板 + 专用标签引用）。

### U1 退役矩阵（实证于 2026-09-28 现场取证）

| 旧模块 | 表与存量 | 目标自定义模型 | 特殊字段需求 | 退役难度 |
|---|---|---|---|---|
| 文章 article | `fly_article`(0 行) + `fly_article_category_merge` | `articles`（已注册 id=3） | 分类字段、tag、评论/顶踩走平台能力（E4/E7） | **高**（**U3 已退役** ✅ 评论由 E4 `fly_comment` 承接） |
| 图片 images | `fly_images` = **附件库基础设施**（引用计数/孤儿清理，供模型引擎 IMAGE/FILE 字段与上传服务使用） | **不退役**——`ApiImagesController`/`ImagesService`（已迁 `module/images`）是引擎能力的一部分 | — | —（U2 现场修正：fly_images 不是内容表） |
| 分享 share | `fly_share`(0 行) | `shares`（已建 id=10） | files 字段已有 | 低（**U2 已退役** ✅） |
| 问答 question/answer | `fly_question`(0 行)/`fly_answer`(0 行) | `questions`（id=11）+ `answers`（id=12，relate→questions + relate 自关联 parent） | E1 已具备 | 中（悬赏/采纳是业务逻辑，先用 number 字段表达；**U2 已退役** ✅） |
| 话题 topic | `fly_topic`(**18 行**) | `topics`（已建 id=9，18 行已迁入 ✅） | 标签系统已在改造后走 TagService 跨模型聚合，不再依赖 fly_topic | **U3 已退役** ✅（`fly_topic` 更名留档，侧栏热议话题走 `fly_list_model model="topics"`） |
| 链接 links | `fly_links`(**1 行**) | `links`（新建） | E3 image_url（link_logo） | **低（试点首选，U1 已退役）** |
| 公告 announcement | `fly_announcement`(0 行) | `announcements`（已建 id=7） | 无 | 低（**U2 已退役** ✅） |
| 帮助/导航 guide | `fly_guide`(**3 行**) | `guides`（已建 id=6，3 行已迁入 ✅） | 无 | 低（**U2 已退役** ✅） |
| 留言 message | `fly_message`(**2 行**) | `messages`（已建 id=8，2 行已迁入 ✅） | 回复关系 = relate 自关联（E1 已具备） | 低（**U2 已退役** ✅） |
| 话题聚合 topics（front） | — | 走 `fly_list_model` + 模型 code | — | 随 topic 退役 |
| **保留例外** | `fly_favorite`/关注/Feed/积分/订单 | **不建模**——用户行为与交易属平台能力，不是内容模块 | — | — |

### U2 施工顺序

> **进度（2026-09-28）**：**U1 试点已完成并验收**——links 模块按四步法端到端退役：旧代码删除（`module/links`、`ApiLinksController`、`Linkspage` 标签、前端 `views/system/links` + `api/core/links.ts`、黑名单移除 `links`）→ 在线建模 `links` 模型（id=5，字段 link_url/link_logo(image_url)/link_type/sort）→ 存量 1 行迁入 `fly_cmodel_links`（旧表更名 `fly_retired_links_20260928` 留档）→ footer 模板改走 `<@fly_list_model model="links">`。**验收：首页 200、0 条 FreeMarker template error、友情链接正常渲染迁移数据；e2e PASS 51 / audit PASS 16。**
>
> **U2 批量已完成并验收（2026-09-28）**：guide / announcement / message / share / question+answer 五组退役端到端落地。在线建模 7 模型（guides=6 / announcements=7 / messages=8 / topics=9 / shares=10 / questions=11 / answers=12）+ 种子字段；存量迁移 guide 3 行、message 2 行、topic 18 行（topics 表保留供标签系统直至 U3）；旧主表 6 张更名 `fly_retired_*_20260928` + 关联表 10 张（share_category/share_comment/share_count/share_order/share_votes/question_count/question_follow_merge/answer_count/answer_votes）留档；删除 `module/{announcement,message,share,question}`（附件库迁出为 `module/images` 独立包）、`Api{Announcement,Guide,Message}Controller`、前台 `{Help,Message,Question,Share}Controller`、12 个旧标签、前端 `views/system/{guide,announcement,message}` + `api/core/{guide,message}.ts`、权限/菜单节点 16 条；模板层 question/share 目录删除，people 列表页改走 `<@fly_page_model>` 通用通道，feed 场景 type 0/2 占位；标签手册同步移除 11 个退役标签骨架（55→44）。**验收：probe_u2 25/25、e2e PASS 51、audit PASS 16、vue-tsc 通过。** 附带修复：ModelController 404 兜底补 `setStatus(404)`（此前返回 200 的 404 页）。

1. **U1 试点** ✅：`links`（1 行数据、模板简单）端到端跑通四步退役法，沉淀操作手册；
2. **U2 批量** ✅：guide / announcement / message / shares / questions+answers；**images 行现场修正为「附件库基础设施，保留」**（`fly_images` 是模型引擎 IMAGE/FILE 字段的附件表，不是内容表）；**topic 顺延 U3**（`fly_topic` 兼作 article 标签存储，TopicService/TagController/ArticleService 的 tag 链路在 article 退役前必须存活）；
3. **U3 收尾** ✅（2026-09-29 完成并验收）：`article` + `topic` + `weight`（权重仅 MyTaskTest 引用，随文章退役）+ `search`（Solr 空壳，`Infopage` 标签一并删）四模块退役。删除 `module/{article,topic,weight,search}`、前台 `ArticleController`/`TopicsController`、10 个旧标签（Articlepage/Articleinfo/Articletypeinfo/Articletypelist/Articlecommentpage/Topicpage/Topicinfolist/Topicinfopage/Checktagfollow/Infopage）；`FavoriteService` 收藏目标校验改走 ModelDataService、`SearchController` 去 SearchService 依赖、`TimingPublishJob` 删 fly_article 硬编码块（cmodel 循环已覆盖）、`UserDao` 文章计数改源 `fly_cmodel_articles`；模板层 `article/`、`topics/`、`search/detail.html`、`common/type_{article,question,share}.html` 删除，explore 页走 `<@fly_page_model model="articles">` + 公告/热议话题侧栏走 `fly_list_model`，people×7 侧栏热议话题同改，people 文章页改 `userId` 参数（**修复 U2 潜伏 bug：`user_id` 参数此前落不进 is_filter 字段被静默忽略**，`selectPage` 新增固定列 userId 过滤）；13 张旧表更名 `fly_retired_*_20260928`；老后台 `/system/{article,question,share,topic}` 死权限节点与前台 `/ucenter/{article,share,topics,answer,question}` 权限行清理；标签手册同步（44→35，新增 `fly_commentpage`）。**验收：e2e PASS 51 / audit PASS 16 / probe_u3 10/10、12 个前台页面 200 零模板错误、`/a/`、`/ac/`、`/article/**`、`/topics/{shortUrl}`（旧路由）均 404。**
4. **U4 平台能力承接** ✅（E4 评论部分，2026-09-29）：新增多态评论表 `fly_comment`（target_model+target_id 引用任意模型内容）+ `module/comment`（CommentService/Dao）+ 前台 `POST /ucenter/comment/save`（session 用户，沿用 `fly_comment_audit` 先审后显开关，目标存在性/发布态校验与 E1 同口径）+ 模板标签 `<@fly_commentpage/>` + `detail-articles.html` 评论区（列表/分页/发表，内容删除级联清评论、审核通过联动 count_comment±1）；管理端 `ApiCommentController` 重写为平台评论管理（端点路径不变、行内附 targetTitle），`ApiAuditController` 重写为**模型驱动内容审核**（按模型分页/待审角标跨模型求和/审核即 `ModelDataService.updateStatus`，开关沿用 `fly_article_audit` 键）；前端 audit 页加模型下拉、comment 页列改 targetTitle。E7 收藏沿用 `fly_favorite` 表（本次仅解耦 ArticleService 依赖，平台化改造仍待做）。
5. **U5 清理** ✅：见 U3（权限节点/标签手册已随批完成）。**附带修复**：FlyFilter 记住我自动登录对已删用户/缺失 `fly_user_account` 行的 NPE（两分支补判空）——由 probe 暴露的既有隐患。

**表单布局补齐（2026-09-29，用户点名能力）**：字段级 `fly_model_field.is_form`（0=表单隐藏：不渲染/不校验/不提交，语义同条件显隐未命中的静态版）；模型级 `fly_model.use_content`/`use_seo`（关闭「详细内容」「SEO 设置」基础页签，链接/导航/留言类模型无正文场景）；字段编辑弹窗加「表单显示」勾选、模型编辑弹窗加两页签开关、动态表单按开关渲染/过滤空页签。

**字段绑定数据源（B1，2026-09-29，用户点名能力）**：新增 `user`（用户选择器：表单可搜索下拉显示昵称、提交存 user_id，对标 ACF User 字段）与 `category`（分类选择器：绑定模型的分类树下拉、提交存分类 id，对标 ACF Taxonomy 字段；绑定模型复用 relate_model，留空=本模型，可做跨模型副分类）两种字段类型。写侧校验实体存在性与归属（user 必须存在、category 必须属于绑定模型分类树），读侧批量展开 `{field}Obj`（用户含 userId/nickName/userName/avatar/shortUrl 供模板直出人员链接；分类含 id/name/fatherId），列表/详情/前台模板零二次查询。数据源选项走 `/api/system/options/{users,categories}`（管理端权限行已登记授权）。固定「分类」表单项同步升级为 TreeSelect 树形下拉。管理端列表 cellText 优先渲染展开名（昵称/分类名）而非裸 id。

---

## 9.5 阶段 M 部分落地 + 前台投稿重建 + R 快赢（2026-09-29）

### M/G12 内容版本与草稿回滚 ✅（对标 Payload versions / Directus Revisions）

- 新表 `fly_content_version`（target_model+target_id+version，快照整行含自定义字段；DB 为事实源，快照即版本）；
- 快照钩子全量接入：内容新建（v1）/更新（version+1）/状态切换（留痕"状态切换→N"）/恢复（另存新版本，历史不丢）；
- 管理端 API：`GET /api/system/modelData/version/list/{modelId}/{id}`、`POST /api/system/modelData/version/restore`（复用模型发布权限 `save@{modelId}`）；
- 前端：内容列表新增「版本」按钮 → 版本抽屉（版本/状态/备注/时间/一键恢复）。

### M/G14 字段级权限 ✅（对标 Directus/Payload 字段级 access）

- 锁行约定：`fly_admin_permission` 存在精确行 `/api/system/modelData/field/{modelId}/{fieldName}` 即视为锁定（行不存在=字段开放，向后兼容）；
- 读侧剔除：formMeta（表单不渲染即不提交）、列表/详情（字段连同 `{f}Obj` 等展开键一并剔除）；
- 写侧拒绝：无权字段非空提交直接 `DataVo.failure("无权限提交字段：…")`；
- 授权复用既有角色组勾选（尾星号通配兼容）；锁行增删后需 `POST /api/system/permission/sync` 清缓存。

### 前台投稿通道重建 ✅（万能系统最后缺口收口）

- `POST /ucenter/submit/{modelCode}`：会话用户投稿，任意启用模型皆可开放；
- 字段白名单 = 启用且 is_form=1 的自定义字段 + 固有列（title/categoryId/thumbnail/keywords/description/content/publish_time），未知键与表单隐藏字段丢弃，status 不可由前台指定；
- 状态由审核开关 `fly_article_audit` 决定（1→待审 0→直接发布）；落库走 insertData（字段校验/唯一约束/附件引用计数/G12 快照全继承）；
- 权限行 `/ucenter/submit/*` 已登记并全组授权。

### 顺手修复（探针暴露）

- **permission/sync Spring 6 NPE**：`getPatternsCondition()` 在 Boot 4（PathPatternParser 为主）恒为 null——补 `getPathPatternsCondition()` 优先分支（该端点自旧后台下线后实际已损坏）；
- **MyBatis 动态绑定三连坑**（restoreColumns 路径）：① `#{params.${c}}` 占位符 token 内的 `${}` 不被替换；② `#{params[c]}` 无引号索引解析不到静默绑 null（UPDATE 空转不报错）；③ 多 @Param 方法绑定名须带 `params.` 前缀。最终按项目 whereSql 惯例：Service 拼白名单 setSql + `#{params.pN}` 索引化参数。

### R 快赢 ✅

- 删除 `backend/views/templates/_testdata`（WP 转换测试残留）与 `frontend/apps/backend-mock`（vben 上游样本，git rm --cached + 本地删除 + turbo 任务清理）。

### M 阶段剩余

- G13 可配置工作流状态机：现有单级审核（audit 开关 + status 语义）保持默认决策不变，状态集可配置按需后置；
- G15 可复用字段组（Component）：与 `fly_block_type` 一并实现的合并方案维持，排下一批。

**验收：probe_m 15/15、投稿探针全过（user_id 归属/审核开关/注入防护/版本联动）、e2e PASS 51、audit PASS 16、B1 探针 11/11、vue-tsc 通过、7 个前台页面 200 零模板错误。**

---

## 9.6 N 批次部分落地 + 工程收尾（2026-09-29）

### G20 模型导入导出 ✅（schema-as-code，对标 Payload config-as-code / Drupal CMI）

- `ModelTransferService`：`exportModel`（模型元数据+字段定义含 GROUP 子字段按 parentFieldName 关联+分类树按 fatherName 关联的单个 JSON）与 `importModel`（幂等：模型按 code 对齐——存在更新元数据/缺失新建自动建表+骨架；字段按 fieldName 对齐——存在更新可变属性（类型不可变）/缺失新增建列，子字段两遍导入；分类按「同父下名称」对齐）；
- 端点：`GET /api/system/model/export/{id}`（复用 list 权限）、`POST /api/system/model/import`（复用 save 权限）；外部 relate 依赖缺失时报 `missingModelDeps` 清单（先导依赖再导本模型）；
- 前端：模型列表行「导出」下载 `{code}-model.json`；工具栏「导入模型」（选文件→确认→按 code 更新或新建，内容数据不受影响）。

### G15 可复用字段组 ✅（复制式 Component，M 阶段收尾）

- 新表 `fly_component`（code/name/remark/fields_json，fields 与导出 fields 同构）；字段管理页对 group 字段「存为字段组」入库；「字段组库」抽屉列出全部组件、「应用到本模型」= 新建 group 字段（fieldName=组件 code，冲突自动 _copy 后缀）+ 子字段逐个复制（同名跳过，复制式不做联动更新）；
- 端点：`/api/system/component/{list,save,del,apply,pickup}`（复用 modelField 权限）。

### G18 事件扩展点 ✅（§7.14 契约先行）

- 新增 `core/event/ContentChangedEvent`（action/modelCode/ids/operatorId/time）；发布点覆盖 ModelDataService 的 insert/update/delete/status 与 CommentService 的发表/审核/删除；发布侧 try/catch、监听侧异常不阻断主流程。本期零监听者——为 G17 自动化与未来事件型插件提供稳定契约。

### G27 路由冲突自检 ✅

- `RouteConflictChecker`（ContextRefreshedEvent）：扫描全变量同形根级路由按段数分组，≥2 个 handler 即 log.error 报警（不阻断启动）。已验证：启动日志正确报出既有的单段全变量路由组（/{channelDir} 与 /{modelCode} 等）。

### G28/G29 工程小债 ✅

- `EhCacheConfig` → `CacheConfig` 正名（实现早已是 Caffeine）；
- devtools 默认关闭、新增 dev profile 开启（`mvn spring-boot:run -Dspring-boot.run.profiles=dev`）；
- DB 账号口令环境变量化：`FLYCMS_DB_USER` / `FLYCMS_DB_PASSWORD`（缺省回退开发值，生产无需改代码即可外部注入）。

**验收：G20 导出/幂等导入/改码新建探针全过（含 fatherName 分类树还原）、G15 入库/应用/复制 2 子字段/重复跳过全过、G27 启动报警 1 组、e2e PASS 51、audit PASS 16、B1 11/11、M 15/15、vue-tsc 通过。**

---

## 9.7 N/R 收尾批次（2026-09-29 第二轮）

### G17 自动化规则 ✅（Directus Flows 简化版：规则表+内置动作，非可视化编排）

- 新表 `fly_automation_rule`（event/model_code/conditions/actions/status）；`AutomationRuleService` 监听 G18 `ContentChangedEvent`——事件+模型匹配 → conditions 对首行求值（eq/neq/gt/lt/contains）→ 依次执行内置动作（`webhook` POST JSON 5s 超时 / `log`）；
- 管理端：`/api/system/automation/{list,save,del}`（权限行已登记授权）+ 新页面 `views/system/automation/index.vue`（菜单「系统管理→自动化规则」，菜单行 id 2760100000000005001）。

### G16 Live Preview ✅（草稿真预览，对标 Craft Live Preview）

- `PreviewTokenUtils`：HMAC-SHA256 短时效（30 分钟）令牌，绑定（adminId, modelCode, targetId），密钥每次启动随机（重启失效为可接受语义）；
- `fly_info_model` 标签预览旁路：request 参数 `__preview=1&__pid&__ptoken` 校验通过 → 按 id 取任意状态行（含待审草稿），失败安全回退；任何详情模板零改动生效；
- `ModelController` 对预览响应设 `X-Robots-Tag: noindex`；`GET /api/system/modelData/previewToken` 签发（复用 save@model 权限）；内容列表新增「预览」按钮。

### E6 公式字段 ✅（P3 提前落地）

- `FieldTypeEnum.FORMULA`（虚拟列）+ `fly_model_field.formula`；表达式白名单校验（仅字段名/数字/四则/括号）；读取时 `expandFormulas` 以行 Map 为根做 SpEL `SimpleEvaluationContext` 受限求值（标识符统一转 `#root['字段']` 索引），失败安全降级 null；
- 顺手修复 `updateField` 对虚拟字段（columnType NULL）的 NPE。

### E9 模型级评论开关 ✅（部分）

- `fly_model.enable_comment`（缺省 1）：0 时前台发表评论直接拒绝；模型编辑弹窗「前台评论」开关。enable_version 等其余开关维持按需后置。

### G19 设计令牌 ✅（最小闭环）

- theme.json 的 settings 数据层此前已存在（ThemeRegistry 解析）；本轮补齐消费层：新标签 `<@fly_theme_vars />`（ThemeVars）输出 `:root{--fly-color-*; --fly-font-*; --fly-layout-*}`，`common/header.html` 全局引入——模板/自定义 CSS 一律消费令牌，改色只改 theme.json（模板中心文件编辑器可直接编辑）。

### R：G26 MyBatis XML 归位 ✅（P2 技术债清偿）

- 35 个 Mapper XML 从 `src/main/java/**/dao/` 迁至 `src/main/resources/mapper/`（平铺命名 `模块__XxxDao.xml`）；`mapper-locations` 改 `classpath:mapper/**/*.xml`；`pom.xml` 移除把 `src/main/java` 当资源目录的非标准配置；**开发约定同步变更（flycms-dev skill 铁律 #1 已改）**。全量回归通过。

### 本轮修复

- `TagVars` 命名坑：标签类名无驼峰（Themevars）时 `toUnderline` 不产下划线——注册名与模板引用名不一致且**渲染中途抛 InvalidReference 导致 chunked 响应截断**（前端表现为响应体不完整）；已正名 `ThemeVars`。

**验收：e2e PASS 51 / audit PASS 16 / B1 11/11 / M 15/15 / 本批综合探针 7/7（G16 含 noindex 与伪造令牌拒绝、G17 规则 CRUD、E9 开关、E6 公式 100.00）/ vue-tsc 通过。**

---

## 9.8 P 阶段可自研部分落地 + G25 审计（2026-09-29 第三轮）

### G22 AI 内容助手 ✅（OpenAI 兼容 provider 抽象）

- `AiProviderService`：chat / embeddings 客户端（`java.net.http`，60s 超时），配置三键存系统参数 `fly_ai_base_url / fly_ai_api_key / fly_ai_model`（DeepSeek/通义/OpenAI 等 OpenAI 兼容服务均可）；未配置时全部 AI 端点返回可读提示，其余功能零影响；
- `POST /api/system/ai/generate`：任务集 summary / keywords / title（5 候选）/ translate；输出**只回填表单由人工审核保存**（红线：服务端不直接改写已发布内容）；
- 前端：内容表单标题下方 AI 按钮组（未配置时不显示）——摘要/关键词直接填入 SEO 页签字段，标题建议弹层点选采用；
- 配置四键已入 `fly_config_web`（key 空值占位，部署方填值即可点亮）。

### G21 语义搜索 ✅（MySQL 存向量，小站规模免向量库）

- 新表 `fly_embedding`（target_model+target_id+chunk 唯一，vector_json + embed_model）；`EmbeddingService`：发布事件（G18）异步向量化（标题+正文按段落分块 ≤1200 字 ×8），删除/下架删向量；`@EnableAsync` 已开启（事件消费不阻塞保存）；
- 检索：查询向量化 → 目标模型全量余弦 → topK；`GET /api/system/ai/semanticSearch` 与手动 `POST /api/system/ai/embedSync`（均复用 AI 权限）；
- 量级边界：内容到十万级迁 ES/pgvector（DAO 已按 target_model 隔离，迁移面收敛在 EmbeddingDao）。

### G25 静态资源审计 ✅（报告产出，删除人工执行）

- 新工具 `tools/static_asset_audit.py`：模板/js/css 直链 + url() + sea.js use/require 闭包展开（alias 两份配置）；
- **报告结论：views/static 共 1744 个文件（35.0MB），被引用仅 30 个——98%（1714 个 / 34.5MB）为旧主题死资源**；报告含未引用 TOP30 与全量清单（`docs/03-报告/静态资源引用审计-2026-09-29.md`）。删除决策人工执行：sea.js 存在运行时拼路径场景，须对候选清单抽查后再批量删除。

### 本轮修复

- 无新增缺陷；AI 链路未配置态全端点优雅降级（探针 3/3）。

**验收：AI 探针 3/3（status/未配置提示×2）、e2e PASS 51 / audit PASS 16 / B1 11/11 / M 15/15 / vue-tsc 通过。**

### G23 MCP server ✅（轻量自实现 JSON-RPC，Streamable HTTP）

- `web/mcp/McpController`（POST /mcp）：initialize 握手 / tools/list / tools/call / ping，无状态无 SSE（工具型调用无需服务端推送）；不依赖 Spring AI SDK；
- 4 个工具：`model_list`（启用模型）、`content_search`（关键词跨模型聚合，仅已发布）、`content_get`（按 model+id 读已发布内容，含自定义字段与关联展开）、`content_create_draft`（**MCP 写入红线：强制 status=0 草稿**，字段校验/唯一约束/版本快照全继承）；
- 门禁：系统参数 `fly_mcp_token`（已入站点参数白名单，后台可直接管理）——留空 = /mcp 404 关闭；非空时请求须带 `Authorization: Bearer <token>`（常量时间比较），错误令牌 401；
- 探针 10/11（唯一"失败"为探针断言过窄：content_search 正确返回了 brands/downloads 的匹配行）。

### G25 静态资源隔离执行 ⚠️ 已执行并回滚（结论：清理转人工）

- 按 G25 审计报告执行自动隔离（1714 个文件移入 views_static_quarantine），前台资产完整性验证连续暴露**三类静态分析盲区**：
  ① FreeMarker 条件分支内的字面路径（`src="<#if>…<#else>/assets/.../avatar/128x128.jpg"`）；
  ② theme.json 元数据字段引用（thumbnail: screenshot.png）；
  ③ 宏动态拼接族（`cover-{模型码}.svg`，按模型码任意扩展）；
- 按设计**整体回滚**（隔离目录移回，e2e 恢复 51/0）——报告保留作**人工清理指南**：建议清理时按「先改模板消除条件内路径 → theme.json 引用纳入清单 → cover 族建齐或改占位」三步收敛后再批量删除。

### 第五轮：配置 UI 化 + 语义搜索前台化 + 版本 diff（2026-09-29 末批）

- **AI/MCP 配置 UI 化**：`fly_ai_base_url/api_key/model/embed_model` 与 `fly_mcp_token` 全部纳入站点参数白名单——后台「网站设置→AI 与自动化」分组直接填写即可点亮 AI/语义搜索/MCP，无需 SQL；`fly_ai_api_key` 与 `fly_mcp_token` 回显掩码化（`******`），掩码/空串提交不覆盖真实值（SMTP 授权码同口径）；
- **G21 前台语义搜索通道**：`fly_search_page` 标签在 AI 向量模型就位时自动切语义通道（查询向量化 → 全模型余弦 → topK 单页，行含缩略图展开，模板零改动），未配置/异常一律回退关键词聚合——搜索永不因 AI 缺席而空转；G21 探针 3/3；
- **G12 版本 diff**：`GET /api/system/modelData/version/diff`（逐字段 from/to，排除 id/short_url）+ 版本抽屉每行「对比」按钮（与上一版逐字段差异表）；
- 验收：综合探针 8/8（配置回显/掩码/掩码不覆盖真值/配置后走真实调用/搜索回退/diff/恢复/清理）、e2e PASS 51、audit PASS 16、B1 11/11、M 15/15、vue-tsc 通过。

### 9.9 收尾批次落地（2026-09-30 ~ 2026-10-01，第五/六/七轮）

- **数据字典** ✅（2026-09-30，若依式）：`fly_dict_type/fly_dict_data` + 字段 select/radio/checkbox 绑 `dict_type`（候选优先级=字典>field.options），管理页 `/system/dict`，菜单 900350~900357；下载模型三字段预绑定；
- **广告系统** ✅（2026-09-30，对标帝国/Dede/PHPCMS）：`fly_ad_position`（ad_key 不可改）+ `fly_ad`（image/text/code 三类型、权重、时间窗、展现/点击双写按日统计）+ 前台标签 `fly_ad` + **联盟式 JS 分发 `/ad/js/{adKey}`**（跨站可投）+ 管理页取码弹窗；手册《广告系统手册》；
- **发布页布局设计** ✅（2026-09-30）：独立设计页 `/system/model/layout/:modelId`（页签增删排序/字段归属/默认页签，`fly_model.form_tabs`+`form_default_tab`）；
- **站点导航管理** ✅（2026-10-01）：`fly_guide` 重建为树形+三源绑定（自定义链接/栏目/模型分类，url 运行时计算）+ `ApiGuideController`（种子节点 900240~900245）+ 前台标签 `fly_guide` + 管理页 `/system/guide`；同批补齐**删模型级联清理**（分类/规则/版本/收藏/栏目解绑）与**模型导入菜单同步**；
- **P4-9 浏览器走查补验** ✅（2026-10-01）：模板引擎执行清单 26 项全部 `[x]` 收口（布局管理/图案库/主题市场三页干净环境走查）。

### 全项目剩余（2026-10-01 复核，全部需用户决策或外部条件）

- **G22/G21 点亮**：后台「网站设置→AI 与自动化」分组直接填写（无需 SQL）。**2026-10-01 实测：`fly_ai_base_url/model/embed_model` 与 `fly_mcp_token` 仍为空**（仅 api_key 有值）——AI 助手/语义搜索/MCP 均未点亮，等用户开通服务填键；
- **G24 富文本结构化**：涉及存量 HTML 迁移，建议随 AI 语义检索的段落分块（已实现）演进，暂缓存储改造；
- **G25 清理执行**：按审计报告人工执行（直接删除模式——先按 §9.8 G25 结论收敛三类盲区路径再删，可回收 34.5MB）；隔离区已按设计整体回滚（views_static_quarantine 已不存在）；
- **G13 可配置工作流**：单级审核保持默认决策（用户已定），状态集可配置按需后置；
- **G30 MySQL 8.0 升级**：生产操作，需停机窗口与回滚预案（当前仍 5.7.44）；
- **G28 剩余**：Lucene 升级随语义检索规模演进（当前关键词+语义双通道已覆盖）。

**验收口径**：`module/` 下不再有 article/share/question/topic/links/announcement/message/guide 内容模块目录（**2026-10-01 复核达成**：module/ 仅剩平台能力 ad/admin/adminlog/ai/block/channel/comment/config/dict/favorite/form/guide(导航管理)/images/job/model/order/other/score/tag/template/user/websocket）；后台「内容」分组只剩「内容模型 + 内容数据（通用）+ 栏目」；前台任意主题模板用 `<@fly_page_model model="{code}">` 可渲染全部内容类型；e2e 与 audit 脚本全绿。

---

*本文档由 FlyCms 开发组维护。修订"五面"边界、§10 决策或阶段优先级时，先改本文档再动代码；每完成一个阶段，在 §9 表内标注状态并同步 `docs/README.md` 索引。*


---

## 13. 自定义模型引擎全景：发布表单、详情模板与万能标签（对标市面主流程序）

> **本章定位**：回应用户总纲——"程序核心 = 后台在线建模生产一切内容模块"。建模层（37 字段类型 / 结构层 / 关系层）已在《自定义模型引擎对标调研与万能建模完善方案》对标闭环；本章补齐**建模之后的三件事**的整合视图与差距批次 **V**：
> ① **发布表单**（建模后内容怎么录——后台表单布局 + 前台投稿）；
> ② **详情/列表模板**（内容怎么显示——解析链 / 指派 / 速查）；
> ③ **万能标签**（内容怎么被任何页面调用——一套参数通吃所有模型）。
> 此前三份文档（前台模板引擎 / 万能建模完善方案 / 自定义模型系统开发手册）仍是细节归属地，本章是**总纲视图**，冲突时以各归属地 + 代码为准。

### 13.1 三层能力框架与现状（全绿盘点）

| 层 | 回答的问题 | FlyCms 实现 | 状态 |
|---|---|---|---|
| **建模层** | 模块有哪些字段/结构 | 37 字段类型（含 user/category 绑定、GROUP/REPEATER、relate/relates/m2a、公式、slug/rating…）；is_unique / min-max / 条件显隐 visible_when / 字段级权限 | ✅ 对标闭环（§9.5–9.7） |
| **发布层（后台）** | 内容怎么录入 | 动态表单全元数据驱动：tab_name 选项卡分组、sort 排序、is_form 表单隐藏、必填/正则/唯一校验、字段级权限剔除、「预览表单」即时预览、AI 助手（摘要/关键词/标题） | ✅ |
| **发布层（前台）** | 访客/会员怎么投稿 | `POST /ucenter/submit/{modelCode}`（审核开关 + 字段白名单 + user 归属），任意模型开放 | 🟡 API 已通；**前台投稿表单页模板未模板化**（V2） |
| **展示层** | 前台怎么显示 | 模板解析链 `detail-{code}.html → detail.html` / `list-{code}.html → list.html`；模型级指派（MODEL LIST/DETAIL）+ 内容级指派（CONTENT DETAIL）；重新生成骨架（含逐字段取值速查注释块） | ✅ |
| **调用层** | 任何页面怎么调用任何内容 | `AbstractModelTag` 万能标签族：`fly_page_model / fly_list_model / fly_info_model / fly_fields_model / fly_category_model / fly_rel_model / fly_hot_model / fly_search_page / fly_commentpage / fly_theme_vars / fly_stats_model`，参数通吃所有模型 | ✅ 全量（V1–V5 已闭环，V3 缓存/V4 聚合/V5 片段库均落地） |

### 13.2 市面程序逐家对标（自定义模型 + 发布/详情模板 + 标签）

| 程序 | 自定义模型机制 | 发布/详情模板 | 标签体系 | 值得吸取的优点 → FlyCms 落点 |
|---|---|---|---|---|
| **帝国 CMS** | 系统模型（内置表加字段）+ 自定义模型（新建表），字段分「系统/附加」 | 每模型独立**发布模板**（后台录入界面模板）与**内容模板/列表模板**，支持按栏目再覆盖 | **万能标签 `ecmsinfo`**（SQL 级参数组合）+ **灵动标签 `e:loop`**（任意表 SQL 循环）；标签带缓存时间参数 | ① 发布模板=我们的动态表单（✅ 更强：tab/隐藏/条件显隐）；② 内容模板=detail-{code}.html（✅ + 内容级指派）；③ **标签缓存时间参数** → V3；④ **自定义属性(flag) 位筛选** → V1（用 checkbox 字段 + is_filter 即建模配方，暂不进引擎）；⑤ 灵动标签任意表循环 → 安全代价大，以「多模型标签 + 模型可无限建」替代（定案不做任意 SQL） |
| **DedeCMS** | 频道模型（channel/diyform），字段用 `addtable` | 每模型独立 list/detail 模板，栏目可再覆盖 | **`arclist`**（flag/typeid/infolen/subday 等丰富参数）+ `channelartlist` + `likearticle` | ① arclist 的 **infolen 摘要截断** → 模板侧已可 `fly_stringcut`（✅）；② **flag 属性筛选**（头条/h/p/f）→ 同 V1 配方；③ **likearticle 相关** → `fly_rel_model` 已覆盖（✅ 且含 m2a 手选增强）；④ `subday` 时效 → E8 timeField 已覆盖（✅ 更通用） |
| **PHPCMS** | 模型 + 字段管理 | 每模型三套模板 | **get 标签**：模板里写只读 SQL 直查任意表 | 灵活度天花板但 = 模板层 SQL 注入面；**定案不做**（用「模型可无限建 + relate 关联」替代；若未来确需，走只读白名单视图 V6 备选） |
| **WordPress** | CPT（自定义文章类型）+ ACF/Meta Box 字段组 | 模板层级（single-{type}.php 等）+ Page Template 下拉 + Block Patterns | **Loop / WP_Query**（参数化查询循环），Gutenberg 区块 | ① CPT ≈ 模型（✅）；② **single-{type} 解析链**（✅ 同构）；③ Page Template 下拉 = 内容级模板指派（✅ P7 已做）；④ **Block Patterns 区块模式** → 面板/碎片已有雏形（theme.json + area 区块），增强方向见 V5 |
| **Strapi v5** | Content-Type Builder + Components/Dynamic Zone | 无前台模板（headless） | REST `populate/filter/sort/pagination` 参数化查询 | headless 语义由 REST API + 稀疏字段承担：SparseFieldAdvice（✅）/ filters（is_filter 筛选字段 ✅）；Dynamic Zone ≈ REPEATER + m2a 组合（✅） |
| **Directus** | 数据库优先建模 + Flows | 无前台模板 | Filter/Sort/Aggregate REST 参数 | **Flows 事件编排** → G17 自动化规则（✅ 简化版）；**Aggregate(group by) 聚合查询** → V4 标签化 |
| **Payload** | config-as-code 建模 + Blocks | 无前台模板（Next.js 渲染） | Local API `where/depth` | **depth=N 关联深度展开** → 读侧 expand 已做（relate/relates/m2a/backs，✅）；**config 导出** → G20 model/export ✅ |
| **Craft CMS** | Section + Matrix 字段 | 每 Section 模板 + Live Preview | Element Query（entry query 全参数） | **Live Preview** → G16 草稿预览（✅）；Matrix ≈ REPEATER（✅） |
| **Joomla** | 文章+自定义字段（扩展继承） | 每分类模板覆盖 | 模块系统 | 分类覆盖模板 → 模板指派已含栏目级（✅） |
| **迅睿 CMS** | 模块化应用 + 自定义字段 | 每模块模板 | `chann/list/order` 等标签 | ① **按模型粒度授权**（✅ save@{modelId} 已做）；② **前台投稿含自定义字段** → V2；③ 多站点/多语言 → 定案不做 |
| **Halo 2.x** | 主题包 + 插件包 | 主题级模板声明 | — | 主题包分发 ✅（皮肤包 zip + manifest） |

**对标结论**：三层框架已对齐市面第一梯队；**真实缺口收敛为 5 项**（V1–V5，见 13.4），其余"灵活度天花板"类能力（任意 SQL 循环、可视化编排）均属定案不做或已有更安全替代。

### 13.3 万能标签设计规范（定案，新增标签必须遵守）

1. **注册命名**：类名 `驼峰式`，注册名 `fly_` + `toUnderline(首字母小写类名)`——**类名必须含真实驼峰分词**（ThemeVars→fly_theme_vars ✅；Themevars→fly_themevars ❌ 曾致渲染截断，2026-09-29 教训）；
2. **模型参数通吃**：所有内容类标签以 `model="{code}"` 为第一参数——**一个标签家族调用任何模型**，不出现 `fly_article_list` 这类专模型标签（旧 9 个已随 U3 退役）；
3. **参数全景约定**（AbstractModelTag 框架参数）：`category / title / p / rows / orderby / order / notid / userId / timeField+timeFrom+timeTo(E8)`；其余参数自动成为 is_filter 字段筛选；`q/semantic` 为 SearchPage 专属；
4. **输出变量约定**：列表族 `dataList + pageHtml（或 search_page）`；详情族 `info + fields`；关联展开 `{field}Obj / {field}List / {field}Url(s) / {field}Backs`；绑定实体 `{field}Obj`（user/category）；
5. **安全边界**：动态标识符一律 `SqlSafeUtil` 白名单；业务值恒 `#{}` 预编译；`fly_search_page` 语义通道失败必须回退关键词（搜索不因 AI 空转）；
6. **手册同源**：新标签必须在 `TagManualService` 登记 SCOPES + 手册骨架（e2e 校验「手册覆盖全部真实标签」，当前 36）+ 在线手册可见；
7. **分类覆盖通道**：模型专属模板放主题目录 `{code}/list.html|detail.html`，解析链 `detail-{code}-{id} → detail-{code} → detail → {code}/detail → cmodel/detail`。

### 13.4 差距清单与 V 批次（模板/标签补全，全部可自研）

| # | 缺口 | 对标来源 | 生产机制 | 优先级 |
|---|---|---|---|---|
| **V1** | **flag/属性位筛选**（头条/推荐/幻灯等位标签筛选） | 帝国自定义属性 / Dede `flag` | ✅ **配方已验证（2026-09-29）**：radio 字段（options 合法 JSON）+ `is_filter=1` → `<@fly_page_model model="articles" style="科技">` 筛选命中（探针验证）；注意 options 必须存合法 JSON（裸逗号串会致筛选失配，已修正 articles.style） | P2 ✅（配方） |
| **V2** | **前台投稿表单模板化**（每模型可绑定投稿页模板） | Dede diyform / 迅睿投稿 | ✅ **已落地（2026-09-29）**：`GET /ucenter/submit/{code}` 渲染 `submit-{code}.html`（缺省 `submit.html` 由 formMeta 字段驱动，options 解析 optionsList 按 radio/checkbox/select 渲染）；`fly_model.enable_submit` 模型级开关（编辑页可控，关闭后投稿页 404/提交拒绝）；探针 6/6 | **P2 ✅** |
| **V3** | **标签微缓存**（列表标签 cache 参数） | 帝国标签缓存时间 | ✅ **已落地（2026-09-30）**：`AbstractModelTag.tagCache`（TTL 指纹缓存，>512 条全清防膨胀），`fly_page_model / fly_list_model` 支持 `cache="秒"`；G18 内容事件按模型精准失效 | **P3 ✅** |
| **V4** | **聚合统计标签** `fly_stats_model`（按分类/作者/模型计数） | Directus Aggregate / 帝国统计函数 | ✅ **已落地（2026-09-30）**：`ModelDataDao.statsGroup`（groupCol 白名单 category_id/user_id，恒定过滤已发布）+ 标签输出 `statsList`（key/name/count，name 解析分类名或作者昵称）；by=category 与 by=author 两通道预览验证 | **P3 ✅** |
| **V5** | **区块模式库**（Block Patterns） | WP Block Patterns | ✅ **已落地（2026-09-30）**：`pattern/save`/`pattern/delete` 端点（自定义图案保存/删除，path-traversal 防护 + SAFE_NAME 白名单）+ 前端「存为图案」弹层与图案库删除按钮；图案库弹窗 3 个种子图案 + 搜索 + 一键插入（2026-10-01 浏览器走查复核） | **P3 ✅** |

> V 批次 **V1–V5 全部闭环（2026-09-30 收口）**：「后台建模 → 前台发布 → 前台展示 → 任意调用」四环全部模板化/数据驱动，万能系统闭环无引擎缺口。

### 13.5 建模配方速查（每类站怎么用现有引擎建出来）

| 想做的模块 | 建模配方（现有 37 类型足够） | 备注 |
|---|---|---|
| 文章/资讯 | articles 模型（已建）：style(radio)+color(color)+tags(input) 已补 | 分类树 + flag 属性走 V1 配方 |
| 下载 | downloads（已建）：file/attachment_pack(files)/needmoney(number)/hide(radio) 已补 | 积分核扣属交易系统后置 |
| 图集 | images（已建）：images/shoot_time/location/camera | 附件平台层不下放（定案） |
| 商品 | products：brand_id(relate→brands)、price(decimal)、stock(number)、gallery(images)、规格(REPEATER: 名称+值) | E1 已具备 |
| 课程 | courses：teacher_id(relate→teachers 自建)、duration(number)、level(select) | 同上 |
| 问答/回答 | questions / answers（已建）：answers.parent_id(relate→answers 自关联树) | 采纳/悬赏用 number 字段表达 |
| 话题 | topics（已建）：count_follow(number)、isgood(radio) | 话题聚合走 TagService |
| 品牌库 | brands（已建）：logo(image)、website(url) | 零代码生产已验证 |
| FAQ | questions 模型 + 关闭前台投稿（E9 enableComment 独立，投稿开关同理） | 或独立 faq 模型 |
| 单页（关于我们） | 单条内容 + detail-{code}.html 模板 | P3 单页型模型（single type）备选 |

> 本章由用户总纲触发补写（2026-09-29）；**V 批次 V1–V5 已全部落地（2026-09-30 收口，状态见表），§9.9 已回写。**
