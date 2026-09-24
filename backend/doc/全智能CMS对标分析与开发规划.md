# FlyCms 全智能 CMS 对标分析与开发规划

> 版本：v1.0（2026-09-21）
> 定位：**平台级规划文档**。回答三个问题——与主流 CMS（PHPCMS / 帝国CMS / DedeCMS 等）差距在哪、它们的历史问题如何规避、FlyCms 走"全智能后台（一切操作后台配置化完成，零代码建站）"路线的具体开发方案。
> 与《自定义模型系统开发手册.md》（领域专项，已落地）互补：模型系统是本规划的核心地基之一，本文不重复其细节。
> 对接规范（认证/权限码/代理）以 `docs/frontend-access-guide.md` 为准；开发规范以 `.agents/skills/flycms-dev` 铁律为准。

---

## 目录

1. [对标结论摘要](#1-对标结论摘要)
2. [对标对象 2026 年现状](#2-对标对象-2026-年现状)
3. [FlyCms 现状快照](#3-flycms-现状快照)
4. [功能差距矩阵](#4-功能差距矩阵)
5. [优缺点分析](#5-优缺点分析)
6. [主流 CMS 的历史问题与规避设计](#6-主流-cms-的历史问题与规避设计)
7. ["全智能后台"总体架构](#7-全智能后台总体架构)
8. [分阶段开发计划](#8-分阶段开发计划)
9. [里程碑总览](#9-里程碑总览)
10. [风险清单](#10-风险清单)
11. [附录](#11-附录)

---

## 1. 对标结论摘要

**一句话结论**：FlyCms 在架构代差（前后端分离新后台、现代技术栈、自定义模型动态 DDL）上已领先三家老牌 PHP CMS，但作为"通用建站 CMS"的内容结构层（统一栏目）、呈现配置层（模板版本化/碎片/推荐位）、运营层（SEO/审核流/表单）还有明确缺口；这些缺口全部可以做成**后台配置项**而非代码功能，正是"全智能后台"路线的施工图。

五条最关键的判断：

1. **最大结构性差距是"统一栏目体系"**。三家老 CMS 都以"栏目树"为建站骨架（栏目绑模型、栏目绑模板），FlyCms 目前各内容类型各自建分类表，没有统一的栏目/频道概念 → 阶段 C 补齐。
2. **当前已存在一个 P0 级安全隐患**：模板在线编辑（ApiWebsiteController）已能写任意 `.html` 到前台皮肤目录，但 Freemarker **未配置类解析沙箱**，拿到模板编辑权限（或被 XSS/提权）即可通过 `<#assign x="freemarker.template.utility.Execute"?new()>` 执行任意命令 → 阶段 A 第一件事就是加沙箱（一行配置级改动）。
3. **老 CMS 的死因不是功能，是模式**：模板文件直写不可回滚、标签方言学不会、静态化心智负担、升级毁站、停更+版权收费。FlyCms 的"全智能后台"必须在架构层面逐条规避（见 §6），而不是把老 CMS 的功能清单照抄一遍。
4. **自定义模型系统（已落地）是这个路线里最贵的一块，已经有了**——后台建模型/字段自动建表、自动出管理页、前台自动路由，对标 Dede 内容模型/帝国系统模型并更自动化。后续所有配置化能力（栏目、碎片、表单）都在它之上叠加。
5. **明确不做的事要写下来**：多站点、多语言、独立伪静态重写引擎、爬虫式采集——这四项是老 CMS 复杂度失控的主要来源，本期以决策记录形式排除（见 §7.4），防止路线膨胀成第二个 PHPCMS。

---

## 2. 对标对象 2026 年现状

| 系统 | 最新主流版本 | 维护状态 | 安全口碑 | 借鉴价值 |
|---|---|---|---|---|
| DedeCMS（织梦） | v5.7 SP2（2018-01） | **基本停更**；2023 年前后转向商业授权收费，社区萎缩 | 差：历史大量注入/上传漏洞，多次被批量挂马 | 标签易学、模型概念普及、"栏目-模板"直观绑定 |
| PHPCMS | v9（2017 年后停更） | **已死**，官方停止维护 | 差：停更后无法应对新漏洞 | 模块化设计、get 标签灵活度（同时也是注入源头）、缓存设计 |
| 帝国CMS | 7.5（低频更新） | **三者中唯一仍在维护**，节奏慢 | 好：以"最安全的 CMS"著称，版本长期稳定 | 系统模型、灵动标签、碎片管理、定时刷新/定时审核、参数白名单式安全设计 |
| WordPress（现代参照） | 6.x | 活跃 | 中：核心尚可，插件生态是重灾区 | 块编辑器可视化搭建、插件元数据机制、生态运营 |
| Halo / Strapi（现代参照） | Halo 2.x / Strapi v5 | 活跃 | 好 | Halo：主题包+插件的分发形态；Strapi：**内容类型构建器（后台可视化建模）与 FlyCms 自定义模型同思路**，验证了这条路线的正确性 |
| 迅睿CMS（延伸参照） | 持续更新 | PHPCMS 血统的延续者 | 中 | 证明了老 CMS 方法论在"持续维护"前提下仍有生命力 |

> 结论：三家国产老 CMS 的时代红利已过（两家事实停更），但它们沉淀二十年的**建站方法论**——模型 → 栏目 → 模板/标签 → 碎片 → 静态化——依然是"全智能后台"要实现的功能全集。FlyCms 的机会 = 这套方法论 × 现代架构（前后端分离 + 配置驱动 + 安全默认值），而不是复刻它们的实现。

参考来源见附录 D。

---

## 3. FlyCms 现状快照

### 3.1 已具备（达标或超前）

| 能力 | 现状 | 对标定位 |
|---|---|---|
| 自定义内容模型 | `fly_model` + `fly_model_field` + 动态 DDL（`ModelTableService` 自动建表/加列），15 种字段类型，前台自动路由 `/{modelCode}/**`，接口收敛 `/api/system/model/**` | 对标 Dede 内容模型/帝国系统模型，**自动建表比两者都进一步**（Dede 加字段要手工管理附加表） |
| 模板引擎与标签 | Freemarker 原生语法 + 48 个指令标签（`web/tags/`，`<@fly_xxx>` 自动注册），覆盖文章/问答/分享/专题/用户/模型全数据面 | 对标 Dede 标签、帝国灵动标签；**无方言、表达力即 Freemarker**，且可在线新建模板 |
| 模板在线编辑（雏形） | `ApiWebsiteController`：皮肤列表/切换、模板文件树、读/写/建/删（路径白名单 + canonical 校验 + 512KB 限制） | 对标三家后台模板编辑，安全边界已做路径层，**缺沙箱与版本化**（阶段 A/D） |
| 新管理后台 | vben v5 SPA：登录/动态菜单/树形授权/按钮权限已落地，`/api/**` REST 规范成型 | 架构代差：三家仍是 jQuery 多页后台 |
| 会员/互动体系 | user 模块 15 张表 + 积分/粉丝/邀请/收藏/消息/私信 | 超出常规 CMS 出厂能力 |
| 全文搜索 | Lucene + IK（正迁移 ES，`SearchService` 空壳接缝已留） | 老 CMS 站内搜索普遍较弱，此处超前 |
| 定时任务 | Quartz（fly_job/fly_job_log + 管理页） | 底座已有，缺"定时发布"等业务化封装（阶段 H） |
| 数据库备份 | MySQLAdminController（/system/tools） | 帝国同款能力，已有 |
| 导航/友链 | fly_guide、fly_links + 旧后台管理页 | 已有，待迁 vben |
| 富文本清洗 | jsoup 1.17.2 依赖就绪 | XSS 过滤底座 |

### 3.2 核心缺口（即本文施工范围）

1. **统一栏目体系**（无 channel 概念，article/topic/share/model 各自分类）→ 阶段 C
2. **模板中心**（无版本化/回滚/皮肤包导入导出/保存校验/标签面板）→ 阶段 D（沙箱前置在阶段 A）
3. **碎片/推荐位/广告位**（零）→ 阶段 E
4. **表单生成器**（无自定义表单/留言/问卷）→ 阶段 F
5. **SEO 中心**（仅全局 TDK 3 个配置键，无栏目级/内容级 sitemap/robots）→ 阶段 G
6. **评论后台审核**（前台可发、后台无管理页）→ 阶段 B
7. **审核流与定时发布**（status 字段语义已在，无流程页与定时 Job）→ 阶段 H
8. **操作审计日志**（无）→ 阶段 A
9. **静态化/页面缓存**（无；决策见 §7.3）→ 阶段 I（可选）
10. 采集、多站点、多语言、可视化拖拽搭建 → 明确缓做/不做（§7.4）

---

## 4. 功能差距矩阵

评级：✅ 达标/超前　🟡 部分　❌ 缺失。

| # | 功能域 | DedeCMS | 帝国CMS | PHPCMS | FlyCms 现状 | 差距动作 |
|---|---|---|---|---|---|---|
| 1 | 内容模型 | 内容模型+附加表 | 系统模型+字段管理 | 模型管理+附加表 | ✅ 元数据+动态 DDL+15 字段类型 | 补字段校验规则细化（随阶段 F 复用） |
| 2 | 栏目体系 | 栏目树绑模型/模板/封面 | 栏目绑模型+多访问端 | 栏目绑模型 | ❌ 各内容类型独立分类树，无统一栏目 | **阶段 C（最大单项）** |
| 3 | 模板引擎 | 自研标签 lib_* | 灵动标签 e:loop | get 标签（可写 SQL） | ✅ 原生 Freemarker + 48 指令标签 | 无 |
| 4 | 模板后台管理 | 在线编辑+模板组 | 在线编辑+方案管理 | 在线编辑 | 🟡 在线编辑/建/删/切肤已落地 | 阶段 D：版本化/回滚/皮肤包/校验 |
| 5 | 模板安全沙箱 | 无（历史 RCE 源） | 无 | 无 | ❌ **当前可写模板且无沙箱** | **阶段 A（P0）** |
| 6 | 碎片/广告位 | mytag+广告模块 | 碎片管理（强项） | 碎片+广告 | ❌ 无 | 阶段 E |
| 7 | 推荐位 | 推荐位 | 置顶/推荐级别 | 推荐位 | 🟡 article 有 recommend 权重字段 | 阶段 E 统一为推荐位条目 |
| 8 | 自定义表单 | 自定义表单 | 信息反馈表单 | 表单向导 | ❌ 无 | 阶段 F |
| 9 | 会员体系 | 有（依赖整合） | 有 | 有（Phpsso） | ✅ 自有全套+积分/粉丝/邀请 | 无 |
| 10 | 评论及审核 | 有 | 有 | 有 | 🟡 前台可发，后台无审核管理 | 阶段 B |
| 11 | 站内搜索 | 弱 | 弱 | 弱 | ✅ Lucene+IK（迁 ES 中） | 无 |
| 12 | SEO | 全站/栏目 TDK、sitemap、伪静态 | 同左+生成 sitemap | 同左 | 🟡 仅全局 TDK 3 键 | 阶段 G |
| 13 | 静态化 | 生成 HTML | 生成 HTML（心智负担重） | 生成 HTML | ❌ 动态渲染（决策见 §7.3） | 阶段 I（可选工具化） |
| 14 | 采集 | 有（漏洞重灾区） | 有 | 有 | ❌ 无 | 决策：不做，CSV 导入/API 投稿替代 |
| 15 | 定时任务 | 定时生成 | 定时刷新+定时审核 | 计划任务 | ✅ Quartz 底座 | 阶段 H 业务化（定时发布） |
| 16 | 投稿审核流 | 单级 | 单级+签名| 单级 | 🟡 status 字段语义已有 | 阶段 H |
| 17 | 附件管理 | 附件库 | 附件库 | 附件库 | 🟡 fly_images+引用计数，无后台统一浏览页 | 阶段 B 附带 |
| 18 | 专题 | 专题（内容型） | 自定义列表 | 专题 | 🟡 fly_topic 偏社区话题，非内容专题 | 阶段 C 聚合栏目承接大部分场景 |
| 19 | 多站点 | 弱（二级站） | 有（弱） | 多发布点 | ❌ 无 | 决策：不做（§7.4） |
| 20 | 多语言 | 多语言包 | 有 | 有 | ❌ 无 | 决策：不做（§7.4） |
| 21 | 权限/菜单 | RBAC | RBAC+操作日志 | RBAC | ✅ 菜单树+按钮权限+动态菜单（vben） | 补操作审计（阶段 A） |
| 22 | 后台形态 | jQuery 多页 | jQuery 多页 | jQuery 多页 | ✅ 前后端分离 SPA | 无（代差优势） |
| 23 | 数据表引擎 | MyISAM 为主 | MyISAM 混杂 | MyISAM 混杂 | 🟡 存量含 MyISAM/utf8 | **新表一律 InnoDB+utf8mb4**（各阶段 DDL 已按此写） |
| 24 | API 开放 | 无/后期补 | 弱 | 弱 | ✅ REST 体系成型 | 无 |

---

## 5. 优缺点分析

### 5.1 FlyCms 的优势

1. **架构代差**：管理端是前后端分离 SPA（vben + `/api/**` REST + Session 认证 + 动态菜单），前台 Freemarker SSR 职责单一。老 CMS 的"后台页面与服务端渲染耦死"在 FlyCms 不存在，后续所有新能力都只是"加接口 + 加配置页"。
2. **自定义模型一步到位**：后台建模型即自动建物理表（真实列、可索引）、自动出管理页面、自动有前台路由。这是"全智能后台"最核心、开发成本最高的一块，**已经落地**。Strapi 的核心卖点（内容类型构建器）同类。
3. **技术栈现代且自研可控**：Spring Boot 4.1.1 / JDK 25 / MyBatis / Caffeine / Quartz，全栈可维护。对标对象两家事实停更——**用停更的 CMS 起新站才是最大风险**（安全补丁无来源、版权收费不确定）。
4. **模板表达力即 Freemarker**：无自造标签方言，任何会 Freemarker 的人直接上手；标签插件按 bean 自动注册，新增数据标签成本极低。
5. **会员/互动开箱即用**：积分、粉丝、邀请、收藏、消息是老 CMS 要靠 UCenter 整合论坛才能拼出来的能力。
6. **安全基础动作规范**：SQL 全参数化、BCrypt 密码、URL 白名单权限、登录锁定——比三家老 CMS 的"历史上先出事再打补丁"起点高。

### 5.2 FlyCms 的劣势（诚实清单）

1. **生态为零**：没有模板市场、插件市场、社区教程。老 CMS 真正的护城河从来不是代码而是生态。→ 对策：皮肤包导入导出（阶段 D）为未来"模板市场"留接口，但本期不建设生态，只保证"包格式可分发"。
2. **建站骨架缺失**：没有统一栏目，"新建一个频道"目前仍要理解各内容模块的分类差异——这与"全智能后台"目标直接冲突，是阶段 C 重投的原因。
3. **运营层空白**：SEO、审核流、表单、碎片这些"网站上线后天天用"的东西都没有，运营人员目前离了开发做不了任何结构变更。
4. **内容量级未验证**：单表大数据量（百万级文章/模型数据）的查询与索引策略未做过压测；MyISAM 遗产表在大数据量下有坏表风险。
5. **文档/上手材料少**：目前两本手册（自定义模型、前端对接），面向开发者而非"运营/建站人员"。→ 阶段 D 的"在线标签手册 + 组件面板"就是给非程序员的文档形态。
6. **安全纵深刚起步**：预编译/BCrypt/权限闭环是"正确"，但缺审计、缺沙箱、缺上传纵深复查（见 §8 阶段 A）。

### 5.3 三家老 CMS 值得继承的优点（明确"抄"什么）

| 来源 | 值得继承 | 本规划的承接点 |
|---|---|---|
| 帝国CMS | 碎片管理（把"页面上一块会变的内容"抽象为后台可维护对象）、定时审核/定时刷新、**参数白名单式安全设计**、系统模型的字段管理交互 | 阶段 E 碎片系统；阶段 H 定时发布；模板编辑白名单（已做）；模型字段管理页（已做） |
| DedeCMS | 标签简单易学、栏目-模板直观绑定、内容模型概念的普及度 | 阶段 D 组件面板+在线手册；阶段 C 栏目绑模板交互 |
| PHPCMS | 栏目页/列表页/详情页三级模板分离的清晰度、缓存层次设计 | 阶段 C 模板覆盖链设计；阶段 I 缓存分层 |

---

## 6. 主流 CMS 的历史问题与规避设计

逐条：问题 → 根因 → FlyCms 的规避设计。这一节是"全智能后台"的**设计红线**，各阶段实现时必须遵守。

### 6.1 模板编辑 = 变相 RCE（三家共性，Dede 重灾区）

- **根因**：模板引擎可执行代码（Dede/帝国模板语法内嵌 PHP），后台模板编辑功能给了任何人写代码的通道。
- **规避**：
  1. Freemarker 配置类解析沙箱 `TemplateClassResolver.ALLOWS_NOTHING_RESOLVER`（禁 `?new`），模板内只允许调用已注册的 48 个指令标签和 Freemarker 内建——**阶段 A 落地，P0**；
  2. 模板编辑是独立按钮权限（不是"管理员"就默认能编），高危操作记审计日志；
  3. 模板保存强制语法 parse 校验，写坏不会导致全站 500（阶段 D）。

### 6.2 升级即毁站：模板/文件被覆盖（三家共性）

- **根因**：模板直接写在程序目录内，升级包覆盖即丢改动。
- **规避**：主题目录与代码天然隔离（`views/templates/pc_theme/{skin}/`，升级不动 views）；阶段 D 模板入库版本化，任何一次保存都有快照可回滚；皮肤包导入导出走标准 zip 格式，"换站 = 换皮肤包"。

### 6.3 标签方言黑盒：学不会、调试难（三家共性）

- **根因**：自造模板语法 + 无调试手段，用户只能背文档、抄论坛代码。
- **规避**：不发明方言，模板层就是 Freemarker 原生语法；数据获取统一走 `<@fly_xxx>` 指令标签（参数即方法签名，可查代码可查手册）；阶段 D 提供**组件面板**（后台点击插入标签代码骨架）和**在线标签手册页**（每个标签的真实参数与示例）；保存时 parse 校验把"模板写坏全站白屏"的调试成本降为零。

### 6.4 静态化心智负担：不刷新就不更新（帝国典型）

- **根因**：内容改动 → 必须手动/定时"生成 HTML"，忘记刷新就是线上事故；全站刷新又慢。
- **规避**：**动态渲染为默认**（改了即生效），Caffeine 缓存兜住性能，缓存粒度到"内容/栏目/碎片"精准失效；静态化降级为可选运维工具（阶段 I），仅用于纯浏览型大流量页，且给出明确使用边界。**"发布即生效"是产品承诺，不允许出现"记得去刷新首页"这种操作。**

### 6.5 栏目与模型绑死：换模型要清数据（帝国典型）

- **根因**：栏目必须绑定且仅绑定一个模型，中途换模型等于弃站重建。
- **规避**：栏目绑模型是**默认值而非约束**——允许不绑（单页/外链/聚合栏目），聚合栏目可混排多模型内容；解除栏目只解除引用、不删内容数据（阶段 C 验收项）。

### 6.6 后台功能堆砌，学习成本失控（帝国后台数百个功能项）

- **根因**：功能按"开发者视角"堆放，没有任务流组织。
- **规避**：新后台按"建站五面"组织菜单（内容/结构/呈现/交互/运营，见 §7.1）；每个管理页只保留高频操作，低频项收进设置；动态菜单已落地，后续新页面天然遵循统一信息架构。

### 6.7 二开必须改代码（三家共性）

- **根因**：内容呈现差异没有足够的配置面，最终都落到改 PHP。
- **规避**：把一切"呈现差异"收敛到四个配置面——**模型字段**（数据结构）、**模板**（页面骨架）、**碎片/推荐位**（动态区块）、**表单**（交互收集）。本规划的每个阶段都在为这四个面加能力，新增需求先问"能不能在四个面里解决"，答案为否才立项写代码。

### 6.8 数据结构不可演进：加字段锁死、删列丢数据

- **根因**：动态建表无纪律（直接 `DROP COLUMN`、无备份）。
- **规避**：沿用《自定义模型手册》§9 已定规范并重申——动态 DDL **只加列不删列**（删列=丢数据，改为标记禁用）；改表前自动快照备份；列名/类型白名单防注入；`fly_model_field.column_type` 冗余与物理表一致性校验。

### 6.9 MyISAM 习气：崩溃即坏表（三家共性，我们库里也有遗产）

- **根因**：2000 年代"读多写少 MyISAM 更快"的惯性。
- **规避**：**本期所有新表一律 InnoDB + utf8mb4**（本规划各阶段 DDL 已按此书写）；存量 MyISAM 表不动（不做顺手迁移，大表 ALTER 有锁表风险），单独立项评估后迁 InnoDB（见 §10 风险 4）。

### 6.10 停更/版权不可持续（Dede/PHPCMS 的死因本身）

- **根因**：闭源 + 单一维护方 + 商业化转向。
- **规避**：FlyCms 自研 + 全部配置入库（模型/栏目/模板/碎片/表单都是数据，换机器可完整重建）；本文档与两本手册构成知识资产，人员更替不停摆。

---

## 7. "全智能后台"总体架构

### 7.1 配置驱动"五面"

全智能后台 = 把建站拆成五个可完全后台配置的层面，任何运营动作都能落进某一面的配置操作：

```
┌─ 内容面：模型 + 字段（已落地 fly_model/fly_model_field，动态 DDL）
├─ 结构面：统一栏目树 + 导航（阶段 C；导航 fly_guide 已有）
├─ 呈现面：模板中心（版本/回滚/皮肤包）+ 碎片/推荐位（阶段 D/E）
├─ 交互面：表单生成器 + 评论审核 + 会员/权限（阶段 F/B；会员权限已落地）
└─ 运营面：SEO 中心 + 审核流/定时发布 + 审计 + 定时任务（阶段 G/H/A）
```

支撑设施（已就绪，不需要新建）：vben 新后台 + `/api/**` REST 规范、动态菜单 + 树形授权 + 按钮权限、Quartz、Caffeine、上传与附件（fly_images 引用计数）、jsoup 清洗、敏感词过滤（FilterKeywordService）。

### 7.2 "零代码上线一个新频道"的完整链路（目标场景验收基准）

以运营要上线"楼盘"频道为例，全程应只发生后台操作：

1. 【内容面】模型管理 → 新建模型 `loupan`，加字段：楼盘名(单行)/均价(数字)/户型(多选)/效果图(图组)/位置(地区)/详情(编辑器) → 系统自动建 `fly_cmodel_xxx` 表、自动出数据管理页；
2. 【结构面】栏目管理 → 新建栏目"楼盘"（目录 `loupan`，绑定模型 `loupan`，选列表/详情模板，填栏目 TDK）→ 前台 `/{目录}` 列表与详情即刻可达；
3. 【呈现面】模板中心 → 在线新建/编辑列表模板，从组件面板插入 `<@fly_listmodel>` 等标签骨架，保存自动语法校验；首页要一块"本周热盘" → 碎片管理建推荐位，添加条目（或从楼盘数据一键推荐）；
4. 【交互面】页脚放一个"购房登记"表单 → 表单生成器拖字段（复用 15 种字段类型）、设置提交频控/审核/邮件通知；
5. 【运营面】栏目 TDK 已在步骤 2 配置，sitemap 自动收录；设置"提交即审核"开关；授权"楼盘编辑"角色组勾选相关菜单/按钮；
6. 上线。**全程 0 行代码、0 次发版。**

这条链路就是各阶段验收的"总纲场景"，阶段 C/E/F 完成后应能完整走通。

### 7.3 渲染策略决策：动态为主、静态化可选

| 方案 | 采用 | 理由 |
|---|---|---|
| 动态渲染 + Caffeine 缓存（现状增强） | **默认** | 发布即生效，无刷新心智负担；缓存按内容/栏目/碎片粒度精准失效；运维零成本 |
| Nginx 反代缓存（proxy_cache） | 运维侧可选项 | 对应用零改动，适合大流量纯浏览场景；写入部署文档即可 |
| 程序生成 HTML（帝国式） | 降级为可选工具（阶段 I，P3） | 仅对"纯浏览列表/详情页"有价值；交互页（评论/计数/表单）不适用；作为容量兜底工具而非默认路径 |

配套要求：生产环境打开 Freemarker 模板缓存（`spring.freemarker.cache: true` + `template_update_delay: 0`，改模板仍即时生效但编译结果可复用）——当前 `cache: false` 仅适合开发期（`application.yml:22`）。

### 7.4 明确不做的事（决策记录，防止路线膨胀）

| 项 | 决策 | 理由 |
|---|---|---|
| 多站点 | **不做** | 与《自定义模型手册》决策二一致（单站点单语言）；引入 site_id 会让所有表和缓存键复杂化一个量级；真有多站需求时用"多实例 + 共库不同前缀"过渡 |
| 多语言 | **不做** | 同上；i18n 基础设施（messages）已存在，前台内容多语言是业务需求出现后再立项 |
| 独立伪静态重写引擎 | **不做** | URL 风格已短链化（`/ac/{shortUrl}.html`、`/{modelCode}/**`）；自造 rewrite 规则引擎是 PHPCMS 复杂度来源之一，收益低；存量 URL 迁移用 Nginx rewrite 配置解决（阶段 G 附示例） |
| 爬虫式采集 | **不做** | 合规风险（版权/robots/法律）+ 历史上是 Dede/PHPCMS 漏洞重灾区 + 维护成本高；替代：CSV 批量导入（阶段 J 可选）+ 投稿 API + 表单收集 |
| 多级审批工作流 | **不做**（仅单级审核开关） | 多级流是 OA 范畴；CMS 真实场景单级足够，帝国/Dede 也只做单级 |
| 可视化拖拽搭建页面 | **缓做**（阶段 J 远期） | "模板 + 组件面板 + 碎片"已覆盖约 80% 的诉求且实现风险小；拖拽编辑器投入大（对标 WordPress Gutenberg 级别工程），放远期按需立项 |

---

## 8. 分阶段开发计划

> 通用约定（每个阶段都要遵守，下文不再重复）：
> - 新接口一律 `web/api/ApiXxxController`、`/api/system/**`、返回 `DataVo`、分页用 `PageVo<T>`、端点内 `requirePermission`；
> - **上线检查单**：`sql/<阶段>.sql`（仓库根目录 `sql/`）建表/插权限行 → 执行 `GET /system/admin/permission_sync` 注册 action_key → 角色组勾选 → 冒烟（详见附录 C）；
> - 新表一律 InnoDB + utf8mb4、bigint 主键 `SnowFlake.nextId()`（除注明自增的日志类表沿用 AUTO_INCREMENT 亦可，与所属模块既有风格一致）；
> - 同步更新 `doc/flycms_date.sql` 基线；前端页面落 `frontend/apps/web-antd/src/views/system/<域>/`，API 封装落 `src/api/core/<域>.ts`；
> - 工作量为单人估计（人日），含联调与冒烟。

### 阶段 A：安全加固与操作审计（P0，先行，约 3~5 人日）

**目标**：消除模板编辑 RCE 面，补管理操作审计。**本阶段是阶段 D 的前置，必须最先做。**

| # | 事项 | 说明 |
|---|---|---|
| A1 | Freemarker 沙箱 | `config/` 新增配置：`FreeMarkerConfigCustomizer` Bean 调 `configuration.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER)`（禁所有 `?new`；模板只需指令标签，无需实例化任何类）。落地前 `grep -rn '?new' views/templates/` 确认存量模板无 `?new` 用法 |
| A2 | 会话 Cookie 加固 | `application.yml` `servlet.session.cookie.httpOnly` 生产置 `true`（当前为 false，`application.yml:30`；开发联调如需可留 profile 区分） |
| A3 | 审计日志表 | `fly_admin_log`（见下）+ HandlerInterceptor：对 `/api/**`、`/system/**` 的 POST 记录（管理员、路径、参数摘要 500 字截断、IP、耗时、结果码），异步写 |
| A4 | 审计查询页 | vben 页面 `views/system/log/`，接口 `/api/system/log/page`（按管理员/时间/路径过滤） |
| A5 | 上传纵深复查 | UpLoadController / CkeditorUp：扩展名 + MIME 双白名单、强制重命名、上传目录禁止脚本解析（部署文档附 Nginx 配置片段） |

```sql
CREATE TABLE `fly_admin_log` (
  `id` bigint(20) NOT NULL,
  `admin_id` bigint(20) NOT NULL,
  `admin_name` varchar(50) DEFAULT '',
  `method` varchar(10) DEFAULT '',
  `path` varchar(200) DEFAULT '',
  `query` varchar(500) DEFAULT '' COMMENT '参数摘要，截断500',
  `ip` varchar(50) DEFAULT '',
  `status` int(11) DEFAULT NULL COMMENT '响应码',
  `cost_ms` int(11) DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_admin_time` (`admin_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理操作审计';
```

**验收**：模板中写 `<#assign x="freemarker.template.utility.Execute"?new()>${x("whoami")}` 保存并访问 → 渲染报模板错误而非执行命令；后台任一保存操作在审计页可见记录。

### 阶段 B：新后台欠账迁移（P1，约 5~8 人日）

**目标**：把"日常运营天天要用、但还留在旧 HTML 后台或没有后台"的功能迁入 vben。全部是既有模式的复制，无架构新增。

| # | 模块 | 接口（`/api/system/...`） | 前端页面 | 说明 |
|---|---|---|---|---|
| B1 | 评论审核 | `comment/page`（join 文章标题）、`comment/audit`（status 0→1/2）、`comment/delete`、`comment/batch` | `views/system/comment/` | `fly_article_comment.status` 语义现成（0未审/1正常/2未通过/3删除），纯映射 |
| B2 | 附件库 | `images/page`、`images/deleteOrphan` | `views/system/images/` | fly_images 已有引用计数（模型手册 §8），补统一浏览+孤儿清理 |
| B3 | 导航管理 | `guide/**`（tree + CRUD） | `views/system/guide/` | fly_guide 已有表与旧后台，迁接口与页面 |
| B4 | 友情链接 | `links/**`（page + CRUD + status） | `views/system/links/` | 同上 |

**验收**：运营可在新后台完成"看评论→审/删、找图/清孤儿、调导航和友链"，旧后台对应入口标注"经典版"保留至阶段 B 验收后下线。

### 阶段 C：统一栏目体系（P1，本规划最大单项，约 8~12 人日）

**目标**：建立全站统一栏目树（对标三家 CMS 的"栏目"），作为建站结构骨架，与内容模型松耦合（规避 §6.5）。

**数据表**：

```sql
CREATE TABLE `fly_channel` (
  `id` bigint(20) NOT NULL,
  `father_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '父栏目，0=根',
  `channel_name` varchar(60) NOT NULL COMMENT '栏目名',
  `channel_dir` varchar(60) NOT NULL COMMENT 'URL目录名，唯一，字母/数字/-',
  `model_id` bigint(20) DEFAULT '0' COMMENT '绑定模型，0=不绑定（单页/外链/聚合）',
  `channel_type` tinyint(2) NOT NULL DEFAULT '0' COMMENT '0列表 1单页 2外链 3聚合(多模型混排)',
  `page_content` text COMMENT '单页内容(channel_type=1)',
  `out_url` varchar(500) DEFAULT '' COMMENT '外链地址(channel_type=2)',
  `list_template` varchar(100) DEFAULT '' COMMENT '列表模板，空=用模型默认',
  `detail_template` varchar(100) DEFAULT '' COMMENT '详情模板，空=用模型默认',
  `seo_title` varchar(200) DEFAULT '', `seo_keywords` varchar(200) DEFAULT '', `seo_description` varchar(500) DEFAULT '',
  `page_size` int(11) DEFAULT '20' COMMENT '列表每页条数',
  `status` tinyint(2) DEFAULT '1' COMMENT '0隐藏 1显示',
  `sort` int(11) DEFAULT '0',
  `create_time` datetime DEFAULT NULL, `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dir` (`channel_dir`),
  KEY `idx_father` (`father_id`,`status`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一栏目树';
```

**接口**（`/api/system/channel/`）：`tree`（栏目树）、`get`、`save`（含唯一性校验）、`delete`（只解除引用不删内容）、`status`、`sort`。

**关键设计与校验**：
- **URL 唯一性**：保存时校验 `channel_dir` 不与既有前台路由冲突——比对清单：`/{modelCode}`（fly_model.model_code）、front 控制器固定前缀（`ac/`、`qc/`、`sc/`、`topics/`、`search`、`ucenter` 等）、其他栏目 dir。冲突即拒绝保存。
- **路由**（新增 `front/ChannelController`）：列表/单页 `/{channelDir}/`、分页 `/{channelDir}/p{n}/`；外链 302。详情页仍走内容自身路由（`/ac/{shortUrl}.html`、`/{modelCode}/{shortUrl}.html`），**存量 URL 不变**（渐进，不毁 SEO）。
- **模板解析链**：栏目模板字段空值回退 → 模型默认模板（fly_model.list_template/detail_template）→ 主题默认模板（cmodel/list.html、detail.html）。三级回退继承 PHPCMS 的清晰度。
- **TDK 回退链**：内容级 → 栏目级 → 全局配置（阶段 G 汇总核查）。
- **聚合栏目**（channel_type=3）：`<@fly_listchannel channelIds="1,2,3">` 标签跨模型混排，承接原"专题"大部分场景。
- **删除语义**：删栏目仅删栏目行并迁移/隐藏子栏目归属（子栏目提升或一并隐藏，交互上让管理员选），**内容数据不动**。

**前端**：`views/system/channel/` 树形管理页（新增/编辑含模型绑定下拉、模板选择下拉（读模板文件树接口）、TDK、类型切换的动态表单；排序用 sort + 上移下移按钮，第一期不做拖拽）。

**标签**：新增 `Channeltree`（导航/侧栏栏目树）、`Channelinfo`（当前栏目信息）、`Listchannel`（聚合列表）。

**验收**（对齐 §7.2 总纲场景）：新建栏目→绑模型→前台列表/详情可达→TDK 按栏目输出→隐藏栏目 404/不可见→删除栏目后内容数据完好。

### 阶段 D：模板中心（P1，约 10~15 人日，依赖阶段 A）

**目标**：把"在线改模板"从雏形升级为可放心交付运营使用的模板中心，承接帝国"方案管理"、Dede"模板组"的能力并规避其升级毁站问题（§6.2/6.3）。

**数据表**：

```sql
CREATE TABLE `fly_template` (
  `id` bigint(20) NOT NULL,
  `skin` varchar(50) NOT NULL COMMENT '所属皮肤目录名',
  `file_path` varchar(200) NOT NULL COMMENT '皮肤内相对路径',
  `update_time` datetime DEFAULT NULL,
  `editor_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_skin_file` (`skin`,`file_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板登记（文件系统为事实源）';

CREATE TABLE `fly_template_version` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `template_id` bigint(20) NOT NULL,
  `version` int(11) NOT NULL COMMENT '递增版本号',
  `content` mediumtext COMMENT '版本快照',
  `remark` varchar(200) DEFAULT '' COMMENT '保存备注',
  `editor_id` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tpl_ver` (`template_id`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板版本快照（历史保留，不清理）';
```

**决策：文件系统为事实源，DB 只存登记与版本快照**。渲染链路（Freemarker loader 指向文件路径）零改动，且保留"FTP/手工兜底修改"的运维后门；版本快照解决"改坏回滚"。

**保存链路（改造 ApiWebsiteController.save）**：
1. 语法校验：`new Template(name, new StringReader(content), cfg)` 捕获 `ParseException` → 失败拒绝落盘并返回行号/错误信息（**阻止模板写坏全站 500**）；
2. 写版本快照（insert fly_template_version，version+1）；
3. 落盘写文件；
4. 若生产开了模板缓存：`freeMarkerConfigurer.getConfiguration().removeTemplate("pc_theme/{skin}/{file}")` 使之即时生效。

**功能清单**：

| # | 功能 | 接口（`/api/system/template|skin/...`） | 说明 |
|---|---|---|---|
| D1 | 版本历史/回滚 | `version/list`、`version/restore` | 恢复=把历史内容作为**新版本**写入，不丢历史 |
| D2 | 新建皮肤 | `skin/create`（从现有皮肤复制） | 换肤不影响在用皮肤 |
| D3 | 删除皮肤 | `skin/delete` | 校验非当前使用皮肤、非默认皮肤 |
| D4 | 皮肤包导出 | `skin/export`（zip 下载） | 限 `.html/.css/.js/图片` 条目，附 manifest.json（皮肤名/版本/导出时间）——未来模板市场的分发格式 |
| D5 | 皮肤包导入 | `skin/import`（zip 上传） | **zip-slip 防御**：逐 entry canonical 路径校验、后缀白名单、总大小 50MB、重名皮肤拒收或提示覆盖 |
| D6 | 组件面板 | 前端功能 | 把 48 个标签按"内容/用户/模型/通用"分组，点击插入带参数占位的标签代码骨架（附参数注释）；**这是给非程序员的模板生产方式** |
| D7 | 在线标签手册 | `tags/manual`（静态数据接口） | 每个标签：用途/参数表/最小示例代码；后台页面渲染。手册数据源先内置 Java 常量，后续可入库 |
| D8 | 模板试渲染（可选增强） | `preview` | 用当前栏目/模型的一行样例数据渲染预览（不落盘），所见即所得的折中实现 |

**验收**：故意写入语法错误模板被拒绝并提示行号；改模板→前台立即生效；回滚到 3 个版本前内容正确；导出的皮肤包导入另一环境（换 pc_theme 值）可用。

### 阶段 E：碎片 / 推荐位 / 广告位系统（P2，约 6~8 人日）

**目标**：对标帝国碎片管理（其公认强项），把"页面上一块会变的内容"变成后台对象，是首页/频道页运营的核心工具。

**数据表**：

```sql
CREATE TABLE `fly_block` (
  `id` bigint(20) NOT NULL,
  `block_key` varchar(50) NOT NULL COMMENT '调用键，模板 <@fly_block key="home_focus"/>',
  `block_name` varchar(60) NOT NULL,
  `block_type` tinyint(2) NOT NULL DEFAULT '0' COMMENT '0富文本 1图片 2推荐位列表 3模板碎片',
  `content` mediumtext COMMENT '富文本/模板内容(block_type=0/3)',
  `item_count` int(11) DEFAULT '10' COMMENT '推荐位展示条数',
  `cache_seconds` int(11) DEFAULT '300' COMMENT '渲染缓存秒数，0=不缓存',
  `status` tinyint(2) DEFAULT '1',
  `sort` int(11) DEFAULT '0',
  `create_time` datetime DEFAULT NULL, `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`block_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='碎片位';

CREATE TABLE `fly_block_item` (
  `id` bigint(20) NOT NULL,
  `block_id` bigint(20) NOT NULL,
  `title` varchar(200) DEFAULT '',
  `image` varchar(500) DEFAULT '',
  `url` varchar(500) DEFAULT '',
  `summary` varchar(500) DEFAULT '',
  `start_time` datetime DEFAULT NULL COMMENT '定时上线',
  `end_time` datetime DEFAULT NULL COMMENT '定时下线',
  `status` tinyint(2) DEFAULT '1',
  `sort` int(11) DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_block` (`block_id`,`status`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='碎片条目（推荐位/广告位条目）';
```

**接口**：`/api/system/block/` page/get/save/delete/status；`/api/system/blockItem/` page/save/delete/sort。
**标签**：`<@fly_block key="home_focus">`——按 block_type 渲染富文本/图片/条目列表/模板碎片，`cache_seconds` 走 Caffeine（缓存键 = blockKey + 条目版本号，条目变更即失效，落实 §6.4"精准失效"）。
**前端**：`views/system/block/` 碎片位列表 + 条目维护（条目支持定时上下线，交由渲染时按时间过滤，无需 Job）。
**二期增强**（不进本期）：内容编辑页"推荐到碎片位"快捷按钮（从内容生成条目）。

**验收**：首页模板挖一个洞放 `<@fly_block key="home_focus"/>`，运营在后台换图/换条目 10 秒内前台生效；过期条目自动消失。

### 阶段 F：表单生成器（P2，约 8~10 人日）

**目标**：对标 Dede 自定义表单/帝国信息反馈/PHPCMS 表单向导——后台自助建"留言/报名/问卷/购房登记"，承接 §7.2 步骤 4。

**数据表与决策**：

```sql
CREATE TABLE `fly_form` (
  `id` bigint(20) NOT NULL,
  `form_code` varchar(50) NOT NULL COMMENT '调用码，前台提交 /api/form/submit/{formCode}',
  `form_name` varchar(60) NOT NULL,
  `audit` tinyint(2) DEFAULT '0' COMMENT '提交是否需审核',
  `submit_limit` int(11) DEFAULT '1' COMMENT '同一用户/IP 每日限提次数',
  `need_captcha` tinyint(2) DEFAULT '1',
  `success_tip` varchar(200) DEFAULT '提交成功',
  `notify_email` varchar(200) DEFAULT '' COMMENT '提交通知邮箱，空=不通知',
  `status` tinyint(2) DEFAULT '1',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`form_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单';

CREATE TABLE `fly_form_field` (
  `id` bigint(20) NOT NULL,
  `form_id` bigint(20) NOT NULL,
  `field_code` varchar(50) NOT NULL,
  `field_name` varchar(60) NOT NULL,
  `field_type` varchar(20) NOT NULL COMMENT '复用 FieldTypeEnum 15 种',
  `required` tinyint(2) DEFAULT '0',
  `default_value` varchar(200) DEFAULT '',
  `placeholder` varchar(100) DEFAULT '',
  `options` varchar(1000) DEFAULT '' COMMENT 'select/radio/checkbox 选项，换行分隔',
  `sort` int(11) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_form` (`form_id`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单字段';

CREATE TABLE `fly_form_data` (
  `id` bigint(20) NOT NULL,
  `form_id` bigint(20) NOT NULL,
  `user_id` bigint(20) DEFAULT '0' COMMENT '0=匿名',
  `ip` varchar(50) DEFAULT '',
  `data_json` text COMMENT '字段值 JSON',
  `status` tinyint(2) DEFAULT '0' COMMENT '0待审 1正常 2不通过（audit=0 时直接1）',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_form_time` (`form_id`,`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单数据（统一表+JSON）';
```

> **为什么表单用统一表+JSON、而内容模型用物理表**（规避"一刀切"反模式）：模型数据要进列表查询/排序/筛选/SEO 详情页，必须真实列+索引；表单数据只需"后台查看+导出"，MySQL 5.7 `JSON_EXTRACT` 足够，统一表避免动态建表数量失控。**按查询需求选存储结构，而不是按习惯。**

**接口**：`/api/system/form/**`、`/api/system/formField/**`（CRUD）、`/api/system/formData/page|detail|delete|export`（CSV 导出，BOM 头防 Excel 乱码）；前台提交 `/api/form/submit/{formCode}`（公开端点：频控 submit_limit、need_captcha 走现有验证码、jsoup 清洗 + FilterKeywordService 敏感词、notify_email 走 EmailService 异步通知）。
**前端**：`views/system/form/`（表单管理 + 字段设计器，交互复用模型字段页面）+ `views/system/formdata/`（数据查看/导出）；前台表单页由模板 `<@fly_form code="liuyan">` 输出渲染（或模板内手写 form 表单指向提交接口）。

**验收**：10 分钟内建出"购房登记"表单（6 字段、限 1 次/日、需验证码、通知邮箱）→ 前台提交 → 后台可见/可导出 CSV。

### 阶段 G：SEO 中心（P2，约 5~7 人日）

**目标**：把 SEO 从"3 个全局配置键"补到主流 CMS 水位。

| # | 项 | 实现 |
|---|---|---|
| G1 | 栏目级 TDK | 已随阶段 C 落地（fly_channel 三字段）；补内容级回退链核查：内容 keywords/description → 栏目 → 全局（模板 head 输出处统一封装） |
| G2 | sitemap.xml | `front/SitemapController` 输出 `/sitemap.xml`（首页+栏目+最新 N 条内容，超量自动 sitemap index 分片）；开关与条数入 config（`fly_sitemap_status`/`fly_sitemap_limit`） |
| G3 | robots.txt | config 键 `fly_robots` + Controller 输出 `/robots.txt`，后台网站管理页加编辑框 |
| G4 | URL 风格 | 现状已短链化，**不做重写引擎**（§7.4）；文档附 Nginx rewrite 存量迁移示例（如 `rewrite ^/news/(\d+)\.html$ /ac/$1.html permanent;`） |
| G5 | 死链/收录体检（可选） | 后台"SEO 概览"页：各栏目可访问性自检、sitemap 条数、TDK 缺失内容统计 |

**验收**：新增内容 1 分钟内出现在 sitemap；任一页面 view-source 三层 TDK 正确回退；robots 后台改完即时生效。

### 阶段 H：审核流与定时发布（P2，约 4~6 人日）

**目标**：内容"先审后发/定时发布"闭环（对标帝国定时审核）。

- **状态语义**：沿用现有 0未审/1正常/2未通过/3删除，**新增 4=待定时发布**（article 与 fly_cmodel_* 表同语义，模型表建表时已含 status 列则直接复用）。
- **定时发布**：内容保存时可选 `publish_time`（status 存 4）；Quartz 新增 `TimingPublishJob`（每分钟）：`status=4 and publish_time<=now` → 置 1 并清缓存。复用 fly_job 登记，不写死 cron。
- **投稿审核开关**：config 键 `fly_article_audit`（0直接发/1先审后发），前台投稿入口按开关落 status。
- **后台**：待审列表页（`views/system/audit/`，或并入文章/模型数据管理页加状态 Tab）；批量通过/驳回（驳回必填原因，站内信通知作者——MessageService 已有）。

**验收**：投一篇稿→审核开关生效→驳回有通知；设 publish_time 为 10 分钟后→到点自动可见，无需任何人工"刷新"动作（§6.4 红线）。

### 阶段 I：静态化与页面缓存（P3，可选，约 5~8 人日）

**目标**：容量兜底工具，**不是默认路径**（§7.3 决策）。

1. 生产配置定稿：`spring.freemarker.cache: true` + `template_update_delay: 0`（改模板即时生效 + 编译缓存）；Caffeine 缓存键细化到"内容/栏目/碎片"粒度，内容保存/审核/删除时精准 evict（阶段 E 的碎片缓存同机制）。
2. 部署文档：Nginx `proxy_cache` 方案（对动态页做 60s 微缓存），对应用零改动。
3. （最后考虑）"全站/栏目生成 HTML"运维工具：仅处理纯浏览列表/详情页，输出 `views/static/html/`，Nginx `try_files` 静态优先。仅当真实流量证明需要时立项。

**验收**：内容发布即生效（缓存及时失效）；压测报告证明默认路径达标后，本阶段即视为可关闭项。

### 阶段 J：远期储备（不排期，按需立项）

| 项 | 说明 |
|---|---|
| Elasticsearch 接入 | 《自定义模型手册》决策五已定：实现 SearchService 索引同步与查询，调用方零改动 |
| CSV 批量导入 | 采集的合规替代：按模型映射 CSV→后台导入（复用元数据校验）；配合投稿 API |
| 可视化拖拽搭建 | 仅当"模板+组件面板+碎片"被证明不够用时立项；届时优先扩碎片能力而非自研编辑器 |
| 多站点/多语言 | 维持 §7.4 不做决策，除非真实业务驱动 |

---

## 9. 里程碑总览

| 阶段 | 内容 | 优先级 | 预估（人日） | 依赖 | 完成后解锁 |
|---|---|---|---|---|---|
| A | 安全加固+审计 | **P0** | 3~5 | 无 | 模板编辑可安全交付（D 的前置） |
| B | 新后台欠账迁移 | P1 | 5~8 | 无 | 运营日常自助 |
| C | 统一栏目体系 | P1 | 8~12 | 无（建议在 A 后） | 建站结构骨架、聚合/单页/外链栏目 |
| D | 模板中心 | P1 | 10~15 | A | 版本化/回滚/皮肤包/组件面板，非程序员可产模板 |
| E | 碎片/推荐位 | P2 | 6~8 | 无 | 页面区块运营化 |
| F | 表单生成器 | P2 | 8~10 | 无 | 交互收集零代码 |
| G | SEO 中心 | P2 | 5~7 | C（栏目 TDK） | 收录基础设施 |
| H | 审核流+定时发布 | P2 | 4~6 | 无 | 内容生产流程闭环 |
| I | 缓存/静态化 | P3 | 5~8 | D/E 的缓存键 | 容量兜底（可关闭项） |
| J | 远期储备 | — | — | — | 按需 |

> 关键路径：**A → D → C → E**，走完即可完整验证 §7.2 总纲场景（零代码上线新频道）；B/F/G/H 相互独立可穿插。

---

## 10. 风险清单

| # | 风险 | 等级 | 应对 |
|---|---|---|---|
| 1 | 模板编辑 RCE（现状已存在） | **高** | 阶段 A1 立即消除；残余风险=有权限管理员故意写入恶意模板——按钮权限独立 + 审计留痕 + （可选）模板保存双人复核 |
| 2 | 动态 DDL 误删列/并发改表 | 中 | 沿用模型手册 §9：只加不删、改前快照、白名单；阶段 C/E/F 不新增动态 DDL 面 |
| 3 | 栏目 URL 与既有路由冲突 | 中 | 阶段 C 保存校验清单（model_code + front 固定前缀 + 栏目 dir 三方比对） |
| 4 | 存量 MyISAM 表大容量坏表风险 | 中 | 本期不动（不做顺手迁移）；单独立项：低峰期 `ALTER TABLE ... ENGINE=InnoDB` 逐表迁移并验证 |
| 5 | MySQL 5.7 已过生命周期（EOL 2023-10） | 中 | 不并入本期；记入技术债台账，单独评估升 8.0 |
| 6 | 新旧双后台并存期操作混乱 | 低 | 迁移完成前旧后台菜单标注"经典版"；阶段 B 验收后按模块下线 |
| 7 | 皮肤包导入引入恶意文件 | 中 | 阶段 D5 zip-slip 防御 + 后缀白名单 + 导入后语法批量校验 |
| 8 | 表单公开端点被刷 | 中 | 频控 + 验证码 + 敏感词 + 审计；必要时加 IP 黑名单（复用 filter_keyword 表思路） |
| 9 | Freemarker 开启模板缓存后"改了没生效" | 低 | 阶段 D 保存链路第 4 步主动 `removeTemplate`；验收项覆盖 |

---

## 11. 附录

### 附录 A：现有模板标签清单（48 个，`web/tags/`，模板内 `<@fly_xxx>` 调用）

| 分组 | 标签 |
|---|---|
| 文章 | Articlepage、Articleinfo、Articletypelist、Articletypeinfo、Articlecommentpage |
| 自定义模型 | FieldsModel、InfoModel、ListModel、PageModel |
| 问答 | Questionpage、Questioninfo、Answerpage、Answerinfo、NewestAnswerinfo |
| 分享 | Sharepage、Shareinfo |
| 专题/话题 | Topicpage、Topicinfolist、Topicinfopage、Checkfollow、Checktagfollow |
| 用户/会员 | Userinfo、Usergroup、Usergrouplist、Userfanspage、Userhotpage、Useraccount、Useractivation、Usercount、Userpower、Fanspage、Avatar、Login、Markgroup、Markrole、Adminrole、Invitepage |
| 通用/其他 | Areaslist、Dateformat、Stringcut、Linkspage、Guidepage、Favoritepage、Feedpage、Infopage、Order、Scoredetailpage、Scorerulepage |

### 附录 B：数据库表现状

基线 `doc/flycms_date.sql` 含 59 张 `fly_` 表（admin 系 6、article 系 6、user 系 15、question/answer 系 6、topic 系 7、share 系 6、config/score/job/links/message/favorite/feed/filter/images/areas/templet 等）；自定义模型系 `fly_model`/`fly_model_field`/`fly_model_category`/`fly_cmodel_*` 见 `sql/custom-model.sql`；本规划新增表见各阶段 DDL（`fly_admin_log`、`fly_channel`、`fly_template`、`fly_template_version`、`fly_block`、`fly_block_item`、`fly_form`、`fly_form_field`、`fly_form_data`）。注意 `fly_templet` 为零引用遗留表，阶段 D 上线后评估废弃。

### 附录 C：每个阶段的上线检查单（固定动作）

1. `sql/<阶段名>.sql`（仓库根目录 `sql/`）：建表 + `fly_admin_permission` 插菜单/按钮行 + 超管组授权关联；
2. 更新 `doc/flycms_date.sql` 基线；
3. 执行 `GET /system/admin/permission_sync` 注册 action_key；
4. 角色组勾选新权限（超管全勾、运营角色按需）；
5. 后端 `mvn -q compile`；前端涉类型改动跑 `pnpm check:type`；
6. 冒烟：登录 → 菜单 → 新页面 CRUD → 未授权账号访问新端点应 403/401；
7. 涉及前台路由/模板的：`/ac/`、`/{modelCode}/` 既有 URL 回归一遍（不毁存量是红线）。

### 附录 D：对标信息来源

- 《PHPCMS、织梦DEDECMS、帝国CMS的选择与比较》（知更鸟，zmingcx.com）
- 《2025 年 3 类 14 大国产 CMS 内容管理系统盘点》（SO AI，2025）
- 知乎/阿里云开发者社区/主机测评网相关对比文（三家停更与安全现状交叉印证）
- 帝国CMS 官方站（phome.net）功能清单：碎片管理、定时刷新、系统模型
- DedeCMS 官方（dedecms.com）：内容模型、自定义表单、mytag 说明
- Strapi 文档（Content-Type Builder）、Halo 文档（主题/插件机制）：现代参照

---

*本文档由 FlyCms 开发组维护；改动"五面"边界、§7.4 决策或阶段优先级时，先修订本文档再动代码。*
