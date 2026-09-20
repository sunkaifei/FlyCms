---
name: flycms-frontend-access
description: FlyCms 前端（frontend/，vue-vben-admin v5）与后端（backend/，Spring Boot Session 认证）对接的领域知识与开发规范。凡在本仓库开发管理端登录、动态菜单、角色/权限/按钮控制、新增给前端调用的后端接口、或新建任何 /system 管理页面时都要使用——即使用户只说"加个页面""接一下登录""加个权限"，也应主动加载本 skill。
---

# FlyCms 前端权限对接

对接方案的完整设计、端点清单、代码骨架和踩坑记录在 **`docs/frontend-access-guide.md`**——动手前先读它，本文件只是速查。

> 本 skill 是"前端权限"专项。项目级通用规范（分层约定、验证流程、防腐化边界、技术债与架构决策）见 **flycms-dev** skill；两者冲突时以本专项为准。

## 先读手册

- 规划对接/改造 → 读手册 §2（对接总方案）和 §3（前端改造步骤）
- 新建管理页面 → 读手册 §4（SOP checklist）
- 调试异常 → 读手册 §5（已知坑表），先对照再排查
- 拿不准后端行为 → 按 §1.1 的文件路径直接读后端源码确认，不要凭记忆

## 核心契约速查

| 事项 | 结论 |
|---|---|
| 认证 | Session-Cookie（JSESSIONID），dev 靠 vite proxy 同源透传，**不做 JWT** |
| accessToken | 占位字符串 `'session'`，仅满足 vben `authLogin` 的非空判断 |
| 响应格式 | 后端 `DataVo{code:0成功/-1失败, message, url, data}` ≈ vben 拦截器 `{codeField:'code', dataField:'data', successCode:0}`，**天然兼容，零改动** |
| 未登录信号 | 后端新 `/api/**` 端点必须抛 **HTTP 401**（`ResponseStatusException`）；老 `/system/**` 是 200+`{login_status:300}`，vben 不认 |
| 权限码 | = 后端 `fly_admin_permission.action_key`（URL 字符串，如 `/system/admin/admin_save`），经 `GET /api/auth/codes` 下发，前端 `v-access:code` 使用，**两边一字不差** |
| 角色 | = `fly_admin_group.name`，经 `GET /api/user/info` 的 `roles` 下发，前端路由 `meta.authority` 过滤 |
| 菜单 | 起步用 `accessMode: 'frontend'`（前端静态路由），后端无菜单表 |
| 验证码 | `GET /captcha/default`（session key `kaptcha`），前端登录页用图形验证码替换 vben 滑块，**每次提交后刷新图片** |
| 登录参数 | `admin_name` / `password` / `captcha`（下划线命名，不是 username） |

## 关键文件地图

```
backend/src/main/java/com/flycms/
├── web/system/IndexAdminController.java     # POST /system/login_act、logout
├── web/system/AdminController.java          # 管理员/角色组/权限管理端点
├── interceptor/AdminInterceptor.java        # /system/** URL 权限拦截
├── core/entity/DataVo.java                  # 响应封装（注意 success 重载陷阱）
├── core/utils/AdminSessionUtils.java        # 会话读写
├── module/admin/service/{Admin,Group,Permission}Service.java
└── config/WebMvcConfig.java                 # 拦截器注册、豁免路径

frontend/apps/web-antd/
├── src/api/request.ts                       # 拦截器链（DataVo 兼容点）
├── src/api/core/{auth,user,menu}.ts         # 登录/用户/菜单 API 定义
├── src/store/auth.ts                        # authLogin 登录流
├── src/views/_core/authentication/login.vue # 登录页（滑块→图形验证码）
├── src/preferences.ts                       # accessMode、app.name
├── src/router/routes/modules/               # 静态菜单路由
├── vite.config.ts                           # /api proxy target（默认 mock:5320）
└── .env.development                         # VITE_NITRO_MOCK、VITE_GLOB_API_URL

packages/utils/src/helpers/generate-routes-backend.ts   # backend 菜单模式的组件路径归一化
```

## 硬性约定

- 新后端端点放 `/api/**`（新控制器 `web/api/`），返回 `DataVo`，成功统一 `DataVo.success(message, data)` 两参重载；`/api/**` 不在 AdminInterceptor 范围内，端点内必须自查 session（401）。
- 后端能力先行：新页面对应的 action_key 要先 `GET /system/admin/permission_sync` 同步、再在角色组勾权限，前端才有码可用。
- 别改 `docs/frontend-access-guide.md` 之外另立对接文档；方案变更直接更新手册。
