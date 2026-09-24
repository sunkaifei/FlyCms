# 技术债台账与架构决策记录（ADR）

两份清单：**已知债**（改不改、何时改由任务驱动，不做"顺手重构"）与**已定决策**（翻案前必须读理由）。任何一条变化都直接更新本文件。

## 一、已知技术债（按优先级）

| # | 债务 | 现状与风险 | 处置建议 |
|---|---|---|---|
| 1 | 无自动化测试 | `src/test` 不存在；回归全靠手工冒烟 | 不必补大而全单测；但 core/utils（BCryptUtils、CheckUrlUtils、SnowFlake）和权限链路值得先补 JUnit，改一处坏全局的风险最高 |
| 2 | commons-lang 2.x 残留 | 如 `AdminController` 里 `org.apache.commons.lang.math.NumberUtils`（2.x，早已 EOL） | 遇到即替换为 commons-lang3 等价物，不专门立项 |
| 3 | ~~前端 vben 完整 monorepo 未裁剪~~ | **已解决（2026-09-21）**：删除 apps/web-ele、web-naive、web-tdesign、web-antdv-next、playground、docs 及 vben 官方 deploy.yml，workspace/scripts/code-workspace 引用已清理 | 剩余 apps/web-antd（主力）与 apps/backend-mock（mock 契约参考，`VITE_NITRO_MOCK=false` 下不启动）；后续若确认不再需要 mock 参照可一并删除 |
| 4 | 遗留表字段命名混用 | `fly_admin.createAt` 驼峰 vs `last_login_time` 下划线 | 新表统一下划线；存量不动（改表影响 Freemarker 模板与 XML，收益低） |
| 5 | Session 认证的横向扩展局限 | 多实例部署需 session 共享（Spring Session + Redis） | 单实例部署下不是问题；上集群前解决，见决策 #1 |
| 6 | ~~Solr 移除残留~~ **已解决（2026-09-21）** | SolrService/SolrAdminController/SolrConst 及 solr 日期工具已删除，调用方（article/question/share/SearchController/Infopage）切换到技术中立空壳 `SearchService`，编译通过 | 未来接入 **Elasticsearch**：实现 `SearchService` 同名方法即可，消费方零改动（见自定义模型手册 §8.2） |
| 7 | 老管理端 Freemarker 与新 vben 并存 | 两套管理 UI 长期并存会造成权限/功能双维护 | 明确 vben 为目标形态；老模板只修 bug 不加新功能 |
| 8 | `WebMvcConfig` 的 `excludePathPatterns("/*")` 语义模糊 | 单层通配，读者易误解为全豁免 | 新增豁免一律写完整路径并加注释 |
| 9 | 开发机 node v25 超出 vben engines（^22.18 \|\| ^24.12） | 直接 `pnpm install` 的 postinstall 崩溃（rolldown 并发 abort/爆内存） | 已用便携 node 22（`~/node22/`，PATH 前置，未动系统）+ `--ignore-scripts` + 串行 stub 绕过，见手册 §6；系统 node 升级计划由用户决定。**补充（2026-09-21）**：`pnpm dev:antd` 的 vite 预构建也会触发 rolldown 原生层内存分配崩溃（两次复现，16GB 内存剩 4.8GB 时仍崩），疑似本机 rolldown 稳定性问题——重启机器或关闭大内存程序后重试；后端与数据库不受影响 |
| 10 | `/api/system/**` 的 403 校验按老 action_key 映射 | `ApiSystemController.requirePermission` 用常量映射老权限节点，API 路径与权限节点无自动同步（新增 API 端点须记得加 requirePermission） | 可接受；若 API 面扩大，考虑把 `/api/**` 纳入 permission_sync 与统一拦截器 |

## 二、架构决策记录（ADR，翻案先读理由）

### D1. 认证保持 Servlet Session-Cookie，不引入 JWT（2026-09-21）
- **理由**：单实例部署；dev/生产均可同域反代，cookie 透传零成本；后端登录/拦截器/验证码全链路已按 session 建成，改 JWT 需重写拦截器与前后端两处契约，收益为零。
- **翻案条件**：生产必须前后端分域且无法反代、或需要多实例/跨系统共享登录态——届时引入 Spring Session 或 JWT，作为独立任务。

### D2. 前端权限码 = 后端 action_key（URL 字符串）（2026-09-21）
- **理由**：后端权限粒度本就是 URL（`fly_admin_permission.action_key`），复用同一字符串做前端按钮码，两端语义强一致，无需维护翻译层。
- **代价（已知并接受）**：controller 重构 URL 时会同时影响权限数据与前端按钮码——所以路由路径变更属破坏性变更，需同步 permission_sync + 角色组 + 前端 v-access。

### D3. 菜单走 vben frontend 模式（前端静态路由），不建菜单表（2026-09-21）
- **理由**：后端无菜单数据，运营自配菜单非当前诉求；frontend 模式按 roles 过滤起步最快。
- **翻案条件**：出现"运营侧需要动态增删菜单/排序"的真实需求，再按手册 §7 演进 backend 模式。

### D4. 新 REST 统一收敛到 /api/**（web/api 控制器），老 /system/** 不再加纯 JSON 端点（2026-09-21）
- **理由**：/system/** 与 Freemarker 页面、AdminInterceptor 302 语义绑定；vben 需要纯 JSON + HTTP 401 语义。分离才能各自演化，老端点保持稳定。
- **注意**：/api/** 不在拦截器范围，401 自查是每个端点的强制动作。

### D5. 技术选型冻结（2026-09-21）
- 后端：Spring Boot 4.1.1 / JDK 25 / MyBatis XML / Freemarker / Ehcache / Druid，不新增 ORM、安全框架、缓存中间件。
- 前端：vben v5 + apps/web-antd 单主力应用；不并存第二套 UI 技术栈。
- 引入任何新依赖需先在对话中给出权衡（解决什么问题、有无既有能力替代、维护成本），达成一致再进 pom/package.json。

### D6. 搜索：移除 Solr，规划接入 Elasticsearch（2026-09-21，用户定案）
- Solr 依赖与代码已整体删除；`module/search/service/SearchService` 为技术中立空壳（原 SolrService 方法签名），文章/问答/分享的同步调用点原样保留。
- 全文搜索整体延后至 Elasticsearch 接入专项；自定义模型系统第一期不含搜索（`is_search` 元数据位保留）。
- 接入方式：实现 `SearchService` 同名方法替换空壳，消费方（Search.java、Infopage 标签、前台搜索页）零改动。

### D7. 模型物理表名 = `fly_cmodel_` + 模型 code（2026-09-24，用户定案）
- **方案**：物理表名 `fly_cmodel_{code}`，如 `fly_cmodel_articles`。`code` 同时承担三种角色：前台路由目录名、后台模板目录名、物理表名后缀。**已实施**（`sql/model-naming-migration.sql`）。
- **理由**：表名可读性是运维刚需——备份、排查慢查询、数据修复、跨环境迁移时 `fly_cmodel_articles` 一眼可知；雪花 id 表名须先查 `fly_model` 反查。Dede/帝国/PHPCMS 均为"模型标识即表名"。
- **代价（已知并接受）**：① `code` 一经创建**永久锁定**，变更 = 改表名 + 全量迁移；② `code` 必须严格白名单化（`^[a-z][a-z0-9_]{1,31}$`），否则表名成为 SQL 注入面；③ 需解决与存量表撞名（`uk_code` 防不了，故新增 `tableExistsBySuffix` 预检）。
- **翻案条件**：出现"创建后改 code"的真实诉求——正解是做「改 code = RENAME TABLE + 同步模板目录 + 写 301」的一键工具，而非回退 id 命名。

### D8. `fly_model` / `fly_model_field` 主键改自增，业务数据表保持雪花（2026-09-24，用户定案）
- **方案**：两表 `id` 改 `bigint AUTO_INCREMENT`；`fly_cmodel_*` 业务数据 id 一律继续雪花。**已实施**。
- **理由**：模型元数据是配置数据（单库低频、行数量级 10~100），雪花的跨库唯一/高并发优势无用武之地，而 id 不可读的代价每天都在付（沟通时说"模型 900000000000000003"）。业务数据表是并发写入主体且未来可能分片，雪花是正确选择。
- **实施坑（重要）**：仅 `ALTER AUTO_INCREMENT=1` **无效**——现有 id 是雪花值（9e17），MySQL 计数器不允许低于 `MAX(id)`，新模型 id 会续在雪花后（实测 1299270553286488065）。必须把历史雪花 id **重写为小整数**（1/2/3/4）并同步 `fly_model_field.model_id`、`fly_model_category.model_id`、`fly_channel.model_id` 三张引用表。
- **边界**：本决策**只覆盖模型元数据两张表**，不是"全项目改自增"的信号。其他表需单独立项。
- **翻案条件**：模型元数据需要跨库同步或多主写入——当前无此场景。

### D9. 存量 article 模块冻结并收敛为内置模型（2026-09-24）
- **方案**：`article` 独立模块冻结新增需求；`articles` 模型提升为唯一文章承载层；存量数据迁移后旧模块标记"经典版"过渡，最终下线。
- **理由**：两套并行实现违反 DRY；老后台（FreeMarker HTML）与 vben 新后台并存造成权限/功能双维护（本台账 #7）。
- **实测修正**：`fly_article` 实为 **0 行**（连同 share/question/answer/comment 全为 0）——收敛退化为"改路由 + 删代码"，不涉及数据迁移，风险与工作量大幅低于原估。
- **不做的事**：不删 `fly_article_comment`（评论属平台能力，见 D11）；不引入多站点/多语言。

### D10. 表名白名单独立化，禁止复用 `safeModelCode`（2026-09-24）
- **方案**：`SqlSafeUtil.safeTableSuffix(String)` 专用于表名后缀校验，与 `safeModelCode`（路由/模板目录用）**分开**，即使当前规则相同。**已实施**。
- **理由**：两者安全边界不同——`safeModelCode` 出错的后果是"路由 404 / 模板找不到"，`safeTableSuffix` 出错是"SQL 注入 + DROP TABLE"。安全边界不同的校验必须独立命名、独立演进、独立测试。

### D11. 评论能力属平台层，不下放给自定义模型（2026-09-24）
- **方案**：模型系统**不实现**评论子表/评论字段类型；评论走已有平台表，多态关联任意模型内容。模型侧只保留"是否开启评论"开关与冗余计数列 `count_comment`。
- **理由**：评论是横切能力（审核、敏感词、用户权限、通知、防刷），不属任何单一内容模型。若每模型自建评论表，则审核策略/反垃圾/统计要复制 N 份，跨模型审核无法统一。
- **翻案条件**：出现"某模型需要与平台评论语义完全不同的独立评论"（如商品问答）——届时建独立模型承载，而非给模型系统加评论表。

### D12. 模型标签收敛为单一通用标签族，禁止继续增生（2026-09-24）
- **方案**：新增 `web/tags/AbstractModelTag.java` 承载公共参数解析/模型解析/输出包装/分页条；7 个模型标签（`InfoModel`/`ListModel`/`PageModel`/`CategoryModel`/`RelModel`/`HotModel`/`FieldsModel`）改为继承基类，各留差异化逻辑。**已实施**。
- **理由**：改造前 7 个标签各有一份**逐字复制**的 `if ("model".equals(str))` 参数解析（每份约 30 行），改一处要改七处。表名/主键改造同时动到标签的模型解析链路，必须趁此收敛，否则改造面翻七倍。
- **兼容性**：标签名与参数名全部保持原样（`fly_info_model`/`fly_page_model`/…），模板零改动。这是内部实现重构，不是对外契约变更。
- **翻案条件**：出现某标签语义无法用族内参数表达的诉求——应扩展参数而非新增标签类。

### D13. 模型创建时自动生成"带注释的可用标签骨架"模板（2026-09-24，待实施）
- **方案**：`ModelService.generateDefaultTemplates` 生成 `{code}/list.html`、`detail.html` 时按该模型**实际字段**生成（列表页逐字段 `<th>`，详情页逐字段标签行），并在模板顶部注入**本模型可用标签清单 + 参数说明 + 复制即用的示例注释块**。
- **理由**：改造前生成的模板是通用静态骨架，与新模型字段无关，运营/AI 接手要翻手册查标签用法。自动生成"字段感知 + 标签注释"骨架，把"新建模块 → 打开模板 → 立即可用"闭合成环。
- **边界**：生成物是**骨架 + 注释**，非成品页面；字段变更时**不自动覆盖**已有模板，仅新建时生成，另提供"重新生成骨架"手动按钮（带覆盖确认）。
- **翻案条件**：出现可视化拖拽编排模板的能力——届时退化为"初始草稿"。

### D14. 补齐 6 类字段类型，其中 RELATE/RELATES 为 P1（2026-09-24，待实施）
- **方案**：现有 15 种 `FieldTypeEnum` 基础上补——**P1**：`relate`（单选引用其他模型记录）、`relates`（多选引用）；**P2**：`tree_category`（树形分类，复用 `fly_category`）、`json`；**P3**：`formula`（计算列）、`version`（版本号）。
- **理由**："关联引用"是内容建模刚需（文章→作者、商品→品牌、问题→答案），缺失会导致用户用 `input` 手填 id，既易错又无法做友好选择器。树形分类是栏目/多级分类刚需。
- **实现代价**：需在 `fly_model_field` 增列 `relate_model`（被引用模型的 code），并配套前台选择器组件与动态表单渲染分支；存储层存被引用记录 `id`（多值逗号分隔，与现有 images/files 一致）。
- **翻案条件**：无——这是能力补齐，只可能追加更高级类型。
