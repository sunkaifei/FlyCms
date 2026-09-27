# FlyCms 前端权限接入开发手册

> 适用范围：在 `frontend/`（vue-vben-admin v5）开发管理端登录、动态菜单、角色/权限/按钮控制，并为前端改造 `backend/`（Spring Boot）接口时使用。
> 本手册基于 2026-09 对两个代码库的实际调研，所有文件路径均可直接定位。
> 配套 ZCode skill：`.agents/skills/flycms-frontend-access/`（对话开发时的速查入口）。

---

## 1. 系统现状

### 1.1 后端（`backend/`，Spring Boot 4.1.1 / JDK 25，端口 80，无 context-path）

**认证方式：Servlet Session（JSESSIONID Cookie），不是 JWT。**

| 环节 | 位置 | 说明 |
|---|---|---|
| 会话读写 | `backend/src/main/java/com/flycms/core/utils/AdminSessionUtils.java` | `Const.SESSION_ADMIN` 存 `Admin` 对象 |
| 登录页 | `GET /system/login` | Freemarker 渲染 `user/login` |
| 登录接口 | `POST /system/login_act` | `IndexAdminController`；表单参数 `admin_name` / `password` / `captcha` / `redirectUrl` |
| 登录逻辑 | `AdminService.adminLogin()` | BCrypt 校验（`BCryptUtils.checkpw`），失败计 `attempts`，成功写入 session |
| 图形验证码 | `GET /captcha/default` | `CaptchaController`，GifCaptcha 4 位，session key 为 `kaptcha`，校验忽略大小写 |
| 登出 | `GET /system/logout` | 清 session 后 redirect |
| 鉴权拦截 | `com/flycms/interceptor/AdminInterceptor.java` | 拦 `/system/**`（排除 `login`、`logout`、`login_act`） |

**权限模型（5 张表，见 `sql/flycms_20260928_012316.sql`）：**

```
fly_admin                          管理员（BCrypt 密码、attempts 登录失败计数）
fly_admin_group                    角色/组（"超级管理员"、"CEO"、"CTO"、"小编"…）
fly_admin_group_merge              admin_id ↔ group_id（一人一组）
fly_admin_group_permission_merge   group_id ↔ permission_id
fly_admin_permission               权限节点：id, action_key, controller, remark
```

**关键认知：权限粒度是 URL，不是菜单树。**
`action_key` 形如 `/system/admin/admin_save`（路径参数在同步时被替换为 `*`）。
鉴权时 `AdminInterceptor` 拿到该管理员所属组的全部 `action_key`，逐条 `CheckUrlUtils.match(action_key, contextPath)`，命中才放行；未命中 302 到 `/403`；**Ajax 请求未登录时返回 HTTP 200 + JSON `{login_status: 300, message: "..."}`**（`FilterUtils.out`，不走 HTTP 401）。

**权限节点来源：** `GET /system/admin/permission_sync` 扫描 `RequestMappingHandlerMapping` 把所有 `/system/**` 路由自动注册为权限节点（`PermissionService.getSyncAllPermission()`）。

**响应封装 `DataVo`（`com/flycms/core/entity/DataVo.java`）：**

```json
{ "code": 0, "message": "操作成功", "url": "/system/index", "data": [] }
```

- 成功 `code=0`，失败 `code=-1`。
- ⚠️ **重载陷阱**：`DataVo.success(String)` 走的是「message」重载（`data` 为空数组）；`DataVo.success(Object)` 走的是「data」重载（`message=null`，且 `url="操作成功"`）。前端取值要兼容，后端新增接口建议统一 `success(message, data)`。

**现有管理端 JSON 端点（`@ResponseBody`，均受拦截器保护）**：管理员增删改查（`/system/admin/admin_save|admin_act|delAdmin|password_update`）、组管理（`add_group_save|update_group_save|group_del`）、权限管理（`permission_sync|permission_del|permission_update_save|group_markpermissions`）——页面渲染仍是 Freemarker（`views/admin/**`）。

**CORS**（`CorsConfig.java`）：仅放行 `localhost`/`127.0.0.1`/`28844.com` 等；开发走 vite proxy 同源转发不受影响，生产前后端分域部署时需在此扩充。

### 1.2 前端（`frontend/`，vue-vben-admin v5 pnpm monorepo + turbo）

- 应用：`apps/web-antd`（Ant Design Vue，**推荐作为主力应用**）、`apps/web-ele`、`apps/web-naive`、`apps/web-tdesign`、`apps/backend-mock`（Nitro mock，端口 5320）。
- 要求：Node `^22.18 || ^24.12`，pnpm ≥ 11。
- 常用命令（根目录）：`pnpm install`、`pnpm dev:antd`（端口 5666）、`pnpm build:antd`。

**请求层 `apps/web-antd/src/api/request.ts`：**

```ts
client.addResponseInterceptor(
  defaultResponseInterceptor({ codeField: 'code', dataField: 'data', successCode: 0 }),
);
```

→ **与后端 `DataVo` 天然兼容**：`code===0` 时自动剥壳返回 `data` 字段，非 0 抛错并 `message.error(message)`。这是整个对接能低成本完成的核心原因。

请求拦截器自动附加 `Authorization: Bearer <accessToken>`（无 token 时不加）与 `Accept-Language`。

**登录流 `apps/web-antd/src/store/auth.ts` → `authLogin()`：**

```
loginApi(params) → { accessToken } → accessStore.setAccessToken
  → 并行 getUserInfoApi() + getAccessCodesApi()
  → userStore.setUserInfo(userInfo) + accessStore.setAccessCodes(codes)
  → router.push(userInfo.homePath || defaultHomePath)
```

对应 API 定义（`apps/web-antd/src/api/core/`）：
- `auth.ts`：`POST /auth/login`、`POST /auth/refresh`、`POST /auth/logout`、`GET /auth/codes`
- `user.ts`：`GET /user/info` → vben `UserInfo` 类型
- `menu.ts`：`GET /menu/all` → `RouteRecordStringComponent[]`

**两种权限模式 `preferences.app.accessMode`（默认 `'frontend'`，配置于各 app 的 `src/preferences.ts`）：**

- `frontend`：菜单取自前端静态路由（`src/router/routes/modules/*.ts`），按 `userInfo.roles` 与路由 `meta.authority` 过滤。**不请求 `/menu/all`。**
- `backend`：菜单完全由 `GET /menu/all` 返回的字符串路由树生成。`component` 字段是字符串（`'BasicLayout'` / `'IFrameView'` / 页面路径），经 `pageMap = import.meta.glob('../views/**/*.vue')` 转换；**路径归一化规则**（`packages/utils/src/helpers/generate-routes-backend.ts`）：`/dashboard/analytics/index` → 匹配 `../views/dashboard/analytics/index.vue`，映射不到会 fallback 到 not-found 页。

**按钮级权限码：** `accessStore.accessCodes: string[]`，使用方式三选一：
`<AccessControl :codes="['x']">` 组件、`v-access:code="'x'"` 指令、`useAccess().hasAccessByCodes(['x'])`。

**开发代理 `apps/web-antd/vite.config.ts`：**

```ts
proxy: {
  '/api': {
    changeOrigin: true,
    rewrite: (path) => path.replace(/^\/api/, ''),  // 前端请求带 /api 前缀，转发时剥掉
    target: 'http://localhost:5320/api',             // 默认指向 mock
    ws: true,
  },
}
```

`VITE_GLOB_API_URL=/api`（`.env.development`），`VITE_NITRO_MOCK=true` 时 nitro mock 随 dev 启动。

---

## 2. 对接总方案

### 2.1 认证：Session-Cookie，不做 JWT 改造

dev 环境浏览器只和 vite（5666）同源通信，`/api/**` 经 proxy 转发到后端（80），**Set-Cookie / JSESSIONID 由 proxy 自动透传**，前端代码无需处理凭证，后端无需任何 token 改造。

- vben 的 `authLogin` 要求登录返回 `accessToken` 才继续 → 后端新登录端点返回占位值 `accessToken: 'session'` 即可（它只是个非空字符串标记，请求头里会带一个无意义的 `Bearer session`，后端不校验、无影响）。
- 生产部署：前后端同域反代（nginx 把 `/api` 转后端）即同样成立；若必须分域，再考虑 JWT（见 §7）。

### 2.2 响应契约：零改动复用

后端新端点直接返回 `DataVo`，前端 `requestClient` 自动剥壳。唯一要处理的差异是**会话过期信号**：

- 后端现有 Ajax 未登录响应是 `HTTP 200 + {login_status: 300}`，而 vben 只在 **HTTP 401** 时触发重新登录流程。
- **对策（二选一，推荐 A）**：
  - **A. 给前端专用端点返回真 401**：新增的 `/api/**` 端点自行判断 session，未登录返回 `HTTP 401`，复用 vben 的 `authenticateResponseInterceptor`（自动清 store、弹登录过期弹窗/跳登录页）。
  - B. 在 `request.ts` 加一层自定义拦截器，识别 `login_status === 300` 时调用 `doReAuthenticate`（适合继续复用老 `/system/**` 端点的场合）。

### 2.3 权限映射（本方案的核心决策）

| 后端概念 | 前端概念 | 载体 |
|---|---|---|
| `fly_admin_group`（角色组） | `userInfo.roles: string[]` | `GET /api/user/info` 返回组名/组 ID |
| `fly_admin_permission.action_key`（URL） | `accessCodes: string[]`（权限码） | `GET /api/auth/codes` |
| `AdminInterceptor` URL 匹配 | 前端菜单过滤 + 按钮显隐 | `meta.authority` + `v-access:code` |

**权限码直接用 action_key 字符串**（如 `/system/admin/admin_save`），前后端语义一致、零翻译成本：后端拦截器拦的是这个 URL，前端按钮控制的也是这个 URL，天然不会出现"按钮显示了但请求 403"以外的错位。

**菜单起步采用 `frontend`（或 `mixed`）模式**：后端没有菜单表，菜单树先在前端静态定义（对应后端 controller 页面结构），按 roles 过滤；按钮码走真实 action_key。后期要"后端可配菜单"再演进 backend 模式（§7）。

### 2.4 后端需新增的最小端点集

新建一个不受 Freemarker 管辖的 REST 控制器（建议 `backend/src/main/java/com/flycms/web/api/ApiAuthController.java`，`@RequestMapping("/api")`），全部返回 `DataVo`，未登录统一 `HTTP 401`：

| 端点 | 作用 | 返回 data |
|---|---|---|
| `POST /api/auth/login` | 复用 `adminService.adminLogin` + kaptcha 校验；成功 `AdminSessionUtils.setLoginMember` | `{ "accessToken": "session" }` |
| `POST /api/auth/logout` | 清 session | — |
| `GET /api/user/info` | 读 session 组装 vben `UserInfo` | `{ userId, username, realName, avatar, roles: [组名...], homePath }` |
| `GET /api/auth/codes` | `permissionService.findPermissionByUserId(admin.getId())` 映射为 `action_key` 列表 | `["/system/index", "/system/admin/admin_save", ...]` |

骨架（省略 import）：

```java
@Controller
@RequestMapping("/api")
public class ApiAuthController extends BaseController {

    @Autowired private AdminService adminService;
    @Autowired private PermissionService permissionService;
    @Autowired private GroupService groupService;

    @PostMapping("/auth/login")
    @ResponseBody
    public DataVo login(@RequestParam String admin_name, @RequestParam String password,
                        @RequestParam(required = false) String captcha) {
        String kaptcha = (String) session.getAttribute("kaptcha");
        if (captcha == null || kaptcha == null || !captcha.equalsIgnoreCase(kaptcha)) {
            return DataVo.failure("验证码错误");
        }
        session.removeAttribute(Const.KAPTCHA_SESSION_KEY);
        Admin admin = adminService.adminLogin(admin_name, password, request);
        if (admin == null) {
            return DataVo.failure("帐号或密码错误。");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", "session");   // Session-Cookie 方案的占位 token
        return DataVo.success("操作成功", data);
    }

    private Admin requireAdmin() {           // 401 语义：给 vben 的重新登录流程用
        Admin admin = AdminSessionUtils.getLoginMember(request);
        if (admin == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return admin;
    }

    @GetMapping("/user/info")
    @ResponseBody
    public DataVo userInfo() {
        Admin admin = requireAdmin();
        Map<String, Object> info = new HashMap<>();
        info.put("userId", String.valueOf(admin.getId()));
        info.put("username", admin.getAdminName());
        info.put("realName", admin.getNickName());
        info.put("avatar", admin.getAvatar());
        int roleId = groupService.findUserAndGroupById(admin.getId());
        Group group = groupService.findGroupById(roleId);
        info.put("roles", group == null ? List.of() : List.of(group.getName()));
        info.put("homePath", "/system/index");   // 与前端路由对齐后调整
        return DataVo.success("操作成功", info);
    }

    @GetMapping("/auth/codes")
    @ResponseBody
    public DataVo codes() {
        Admin admin = requireAdmin();
        List<String> codes = permissionService.findPermissionByUserId(admin.getId())
                .stream().map(Permission::getActionKey).collect(Collectors.toList());
        return DataVo.success("操作成功", codes);
    }
}
```

注意：`/api/**` 目前不在 `AdminInterceptor` 的拦截范围（只拦 `/system/**`），所以每个端点必须用 `requireAdmin()` 自查；也可以在 `WebMvcConfig.addInterceptors` 给 `/api/**` 追加拦截并让拦截器对 `/api/**` 返回 401 而非 302。

> **实现状态（2026-09-21，已全部落地）**：
> - `web/api/ApiBaseController.java` — `requireAdmin()`（401）与 `requirePermission(actionKey)`（403，复用老后台 action_key 校验）；
> - `web/api/ApiAuthController.java` — login / logout / user/info / auth/codes 四端点；
> - `web/api/ApiSystemController.java` — 管理员、角色组、权限节点共 14 个端点（列表/增删改/分配权限/同步权限），超管保护与老控制器一致；
> - `GroupDao/GroupService` 新增 `findPermissionIdsByGroupId`（分配权限回显）。

### 2.5 验证码

`POST /api/auth/login` 沿用 session 里的 `kaptcha`（由 `GET /captcha/default` 写入，proxy 同源下 cookie 自动携带）。前端登录页把 vben 默认的 `SliderCaptcha` 滑块替换为图形验证码：`<img :src="'/captcha/default?t=' + t">` 点击刷新 + 文本输入框，字段名保持 `captcha`。后端校验失败不销毁旧码，**前端每次提交后必须刷新图片**。

---

## 3. 前端改造步骤（`apps/web-antd`，已完成于 2026-09-21）

实际落地与初稿的两处关键勘误（**都是踩过的**）：

- **API 路径不带 `/api` 前缀**：vben 的 `requestClient` baseURL 已是 `VITE_GLOB_API_URL=/api`，API 函数里再写 `/api/xxx` 会拼成 `/api/api/xxx`。函数保持 vben 惯例写 `/auth/login`、`/system/admin/list`。
- **POST 必须用 `URLSearchParams`（form-urlencoded）**：后端端点全部用 `@RequestParam` 读表单参数，`requestClient.post(url, {对象})` 发的是 JSON body，后端读不到。工具函数 `toForm()` 在 `api/core/auth.ts`、`api/core/system.ts`。

已完成的实际步骤（供复核与后续扩展对照）：

1. **切断 mock**：`.env.development` → `VITE_NITRO_MOCK=false`；应用标题 `.env` → `VITE_APP_TITLE=FlyCms Console`。
2. **代理指向后端**（`vite.config.ts`）：
   - `/api` → `target: 'http://localhost:80/api'`（保留 rewrite 剥除，净映射 `/api/**` → 后端 `/api/**`）；
   - 新增 `/captcha` → `http://localhost:80` 直连（后端验证码无 `/api` 前缀，共用一条代理会 404）。
3. **API 层**（`src/api/core/`）：`auth.ts` 重写（login/logout/codes + 字段名映射 `username→admin_name`；`refreshTokenApi` 保留但 `enableRefreshToken` 默认 false 不会调用，`request.ts` 零改动）；新增 `system.ts`（管理员/角色组/权限全部端点与行类型）。
4. **登录页**（`src/views/_core/authentication/`）：`login.vue` 移除 vben 演示的选择账号/滑块验证码，新增 `captcha-input.vue` 图形验证码组件（`GET /captcha/default`，点击刷新；表单绑定遵循 adapter 的 `v-model:value`）；提交后（无论成败）递增 `captchaKey` 刷新图片。
5. **权限模式**（`src/preferences.ts`）：`app.accessMode: 'frontend'` 显式声明。
6. **路由**（`src/router/routes/modules/system.ts`）：系统管理菜单（管理员/角色组/权限三个页面）；删除 `demos.ts`、`vben.ts` 官方演示菜单（模块目录被 `import.meta.glob` 自动注册，删除文件即生效）。路由**不设 `meta.authority`**——角色组名是数据驱动的，页面控制交给按钮权限码 + 后端 403 兜底。
7. **页面**（`src/views/system/`）：`admin/`（列表+新增编辑弹窗）、`group/`（列表+弹窗+分配权限弹窗）、`permission/`（列表+同步+删除）。均用 `useVbenVxeGrid`（代理响应映射 `{items,total}`）+ `useVbenModal`（`connectedComponent` + `destroyOnClose`，弹窗 `onMounted` 里 `getData()` 初始化，成功后回调 `onSaved` 刷新表格）。
8. **按钮权限**：`useAccess().hasAccessByCodes(['/system/admin/delAdmin'])` 控制显隐，码 = 后端 action_key，一字不差。

## 4. 新页面开发 SOP（Checklist）

1. 后端有对应 controller + `@ResponseBody` 端点吗？没有 → 先建后端端点（返回 `DataVo`）。
2. 后端 `GET /system/admin/permission_sync` 同步出新 action_key，并在 `group_assignPermissions` 页面把权限勾给对应角色组。
3. 前端 `views/system/<module>/` 建页面组件。
4. `router/routes/modules/system.ts` 加路由（name 唯一、meta.title/icon/authority）。
5. 页面内按钮套 `v-access:code`（action_key）。
6. 联调：确认后端拦截器放行（所属组已勾选该权限）→ 前端菜单可见 → 按钮显隐正确。

## 5. 已知坑（踩过的都在这）

| # | 坑 | 说明/对策 |
|---|---|---|
| 1 | `DataVo.success(...)` 重载分派 | `success(String)` 填 message；`success(Object)` 填 data 且 `message=null`。前端错误提示读 `message` 字段要判空；后端写新接口统一 `success(message, data)` |
| 2 | 未登录 Ajax 返回 200+`login_status:300` | vben 只认 HTTP 401；新 `/api/**` 端点务必返回真 401（§2.2 方案 A） |
| 3 | 验证码不自动销毁 | 登录失败旧验证码仍有效、成功才清除；前端提交后必须刷新图片 |
| 4 | backend 菜单模式的组件路径归一化 | component 字符串映射不到 `views/**` 会静默 fallback 到 not-found 页；路径必须以 `/` 开头且能对上 `../views/**` 去掉 `/views` 前缀后的文件 |
| 5 | `/*` excludePathPatterns 只匹配单层 | `WebMvcConfig` 里 `excludePathPatterns("/*")` 不会豁免 `/system/xxx` 深层路径；新增豁免要写全路径 |
| 6 | vite proxy 的 `/api` 前缀 | 后端路由本身没有 `/api` 前缀（靠 rewrite 剥掉）；直接写 `VITE_GLOB_API_URL=http://localhost:80` 会跨域 + cookie 丢失，别这么配 |
| 7 | 超级管理员组 id=1 不可删/不可改权限 | 后端硬编码 `groupId==1` 保护；前端对应按钮可直接藏 |
| 8 | 登录失败锁定 | `attempts`/`attempts_time` 计数在 `fly_admin`，联调乱试密码被锁后清表字段即可 |
| 9 | 生产分域 | 必须同域反代；分域要扩 `CorsConfig` 且 cookie 跨站受限，届时换 JWT（§7） |
| 10 | JSON body vs 表单参数 | `requestClient.post(url, {对象})` 发 JSON，后端 `@RequestParam` 读不到——POST 一律 `URLSearchParams`（§3 勘误） |
| 11 | API 函数双前缀 | baseURL 已含 `/api`，API 函数里再写 `/api/xxx` 会变成 `/api/api/xxx`（§3 勘误） |
| 12 | 验证码代理 | 后端 `/captcha/default` 无 `/api` 前缀，必须独立代理规则，与 `/api` 共用会 404 |
| 13 | node 版本 | vben engines 要求 `^22.18 \|\| ^24.12`，开发机 node v25 会导致 postinstall 崩溃——用便携 node 22（见 §6） |
| 14 | 表单 POST 的 Content-Type | vben 的 axios 实例默认 `Content-Type: application/json`，会覆盖 URLSearchParams 的自动检测，后端 `@RequestParam` 全部读不到（症状：后端报第一个参数为空）。所有表单 POST 必须显式传 `headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8' }`（见 `api/core/system.ts` 的 `postForm`） |
| 15 | 自定义表单组件的 v-model 绑定 | 未注册进 adapter 的 `markRaw` 自定义组件，vben 表单内核走**默认 `modelValue`** 绑定（`resolveModelPropName` 只对字符串组件查 `componentBindEventMap`）——组件要用 `modelValue`/`update:modelValue`，并在 schema 字段上显式 `modelPropName: 'modelValue'`。症状：字段值恒空、提交报"请先完成验证" |

## 6. 环境与启动

```bash
# 后端（需先导入 MySQL：mysql < sql/flycms_20260928_012316.sql，配置 application.yml 数据源）
cd backend && mvn spring-boot:run          # 端口 80

# 前端（开发机 node 是 v25，超出 vben engines 范围，用便携 node 22）
export PATH="$HOME/node22/node-v22.18.0-win-x64:$PATH"
cd frontend
pnpm install --ignore-scripts                        # 并发 postinstall 在 Windows 上会崩，跳过
pnpm -r run --if-present stub --workspace-concurrency=1   # 串行补跑内部包构建
pnpm dev:antd                                        # http://localhost:5666
```

默认管理员账号在 `fly_admin` 表（密码 BCrypt，seed 数据见 SQL 文件）。登录后到「权限管理」先执行一次"同步权限"，再到「角色组管理 → 分配权限」勾选权限，菜单按钮才可用。

## 7. 演进路线

- **backend 菜单模式**：新建菜单表（或由 `fly_admin_permission.controller` 字段推导分组）→ 后端提供 `GET /api/menu/all` 返回 vben `RouteRecordStringComponent[]` → 前端 `accessMode: 'backend'`。适合"运营可配菜单"的诉求，成本较高，非必需。
- **JWT 化**：仅当生产必须前后端分域时考虑——后端签发 token + 过滤器校验，前端改动极小（`accessToken` 变真值、恢复 refreshToken 流程）。当前 Session-Cookie 方案下不要提前做。
