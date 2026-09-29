---
name: flycms-dev
description: FlyCms 项目（backend/ Spring Boot 4.1.1 + frontend/ vue-vben-admin v5）的架构地图与开发规范总纲。凡在本仓库做任何开发——写后端接口、新增/修改模块、管理页面、数据库变更、修 bug、重构——都要先加载本 skill 再动手，即使用户只说"改一下""加个功能"；前端登录/菜单/权限专项另见 flycms-frontend-access skill。
---

# FlyCms 开发总纲

目标是让每次改动都长得像这个项目已有的代码，而不是像通用教程。细节 SOP 和决策记录在 references/ 里，先按需读：

- 新增后端模块/接口/管理页面 → 读 `references/backend-conventions.md`（含可直接套用的代码模板）
- 想引入新框架、改架构、或发现代码"不顺眼"想顺手重构 → 先读 `references/tech-debt.md`（技术债台账 + 已定的架构决策，多数"顺手改进"已被明确否决过）
- 前端登录/菜单/权限/按钮控制 → 触发 `flycms-frontend-access` skill，完整手册在 `docs/frontend-access-guide.md`

## 项目一张图

```
FlyCms/
├── backend/    Spring Boot 4.1.1 / JDK 25 / MyBatis(XML) / Freemarker / Ehcache / MySQL，端口 80
│   └── src/main/java/com/flycms/
│       ├── module/<域>/{dao,model,service}/   # 业务三层，DAO 接口与 XML 同目录
│       ├── web/front|system/                  # 前台页面 / 管理后台（Freemarker 渲染）
│       ├── web/api/                           # 给 vben 前端的 REST（/api/**，DataVo）
│       ├── interceptor/                       # AdminInterceptor（/system/** URL 权限）、UserInterceptor
│       ├── core/{base,controller,entity,utils}/
│       └── config/                            # WebMvcConfig(拦截器注册)、CorsConfig、Druid…
├── frontend/   vue-vben-admin v5 pnpm monorepo；主力应用 apps/web-antd，dev 端口 5666
└── docs/frontend-access-guide.md               # 前后端权限对接手册（唯一信息源）
```

## 铁律（每条都有原因，违反即返工）

1. **DAO XML 统一放 `src/main/resources/mapper/`**（2026-09-29 G26 归位后的现行约定；文件名用 `模块__XxxDao.xml` 平铺）。`application.yml` 的 `mapper-locations` 扫 `classpath:mapper/**/*.xml`；`pom.xml` 已不再把 `src/main/java` 当资源目录——新 Mapper XML 放回 java 目录会静默不打包，启动报绑定失败。
2. **Controller 归位**：前台页面 → `web/front`；管理后台页面（含 Freemarker 渲染 + 局部 @ResponseBody）→ `web/system`，视图一律 `theme.getAdminTemplate("admin/xxx")` 返回；给 vben 前端的新 REST → `web/api`，路径 `/api/**`，返回 `DataVo`。不要在老 controller 里继续堆纯 JSON 端点。
3. **响应与分页**：JSON 一律 `DataVo`（成功 `DataVo.success(message, data)` 两参重载——单参重载有 message/data 分派陷阱）；分页一律 `PageVo<T>`（service 里 `new PageVo<>(pageNum)`、`setRows`、`setList(dao.getXxx(pageVo.getOffset(), pageVo.getRows()))`、`setCount(dao.countXxx())`，XML 里手写 `limit #{offset}, #{rows}`）。
4. **ID 与表**：主键 `SnowFlake.nextId()`（`core/utils/SnowFlake.java`），表名 `fly_` 前缀、bigint 主键。SQL 全部走 DAO XML 的 `#{}` 预编译，禁止字符串拼接。
5. **校验与密码**：入参用 jakarta validation 注解 + `@Valid ... BindingResult` 逐条 `DataVo.failure(error.getDefaultMessage())`；密码存取只经 `BCryptUtils`；登录失败锁定逻辑（attempts）沿用 AdminService 现状，不要另造。
6. **权限闭环**：`/system/**` 下的新端点自动被 AdminInterceptor 按 URL 权限拦截——**上线前必须** `GET /system/admin/permission_sync` 注册 action_key 并在角色组勾选，否则所有人 403；`/api/**` 不在拦截范围，端点内必须自查 session 并对未登录抛 HTTP 401（vben 只认 401）。
7. **依赖卫生**：新代码用 commons-lang3 / commons-io 等现代坐标；`org.apache.commons.lang`（2.x）只出现在遗留代码里，不要模仿。

## 改完怎么验证（本项目无自动化测试，验证 = 编译 + 冒烟）

1. 后端：`cd backend && mvn -q compile` 过了再谈下一步；涉及启动行为的，`mvn spring-boot:run` 起服务后按"登录 → 菜单 → 目标页面/接口"冒烟。
2. 前端：`cd frontend && pnpm dev:antd` 起开发服（5666），改动涉及类型的跑 `pnpm check:type`；涉及登录/权限的按手册 §3 验收。
3. 数据库变更：在 `doc/` 下同步维护 SQL 变更说明，保持 `doc/flycms_date.sql` 是可建库的完整基线。

## 禁止事项（防腐化边界）

- 不引入新框架/新 ORM/新安全框架（Shiro、Spring Security、MyBatis-Plus、JWT…）。现行选型及理由见 `references/tech-debt.md` 的决策记录；确有必要，先在对话中提出权衡再动。
- 不改 `DataVo` 字段语义、拦截器路径规则、`/api` 代理契约——前端手册与 skill 都建立在这些契约上。
- 不做"顺手大重构"。发现坏味道记入 tech-debt 台账，单独立任务处理。
- 提交信息沿用仓库惯例：中文一行式动词开头（如"修复表名BUG"）。
