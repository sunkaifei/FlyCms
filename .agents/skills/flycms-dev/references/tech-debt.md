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
| 6 | Solr 移除残留 | 升级时已移除依赖，源码/配置可能仍有 SolrConst、search 模块残留分支 | 触碰相关代码时顺手清理并编译验证 |
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
