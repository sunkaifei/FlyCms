# FlyCms 后端代码审查报告

- 审查日期：2026-09-24
- 审查范围：`backend/` 共 343 个 Java 文件（拦截器、配置、上传链路、订单/积分业务、核心工具类）
- 方式：静态代码走查 + 修复（`mvn clean compile` 通过）

## 修复状态（均已改完并编译通过）

| 问题 | 状态 | 改动文件 |
|------|------|----------|
| 积分扣减符号反转 | 已修 | `OrderService.java`、`ScoreRuleService.java` |
| SnowFlake ID 重复 | 已修（改类级共享状态 + 静态同步，31 处调用点改为 `SnowFlake.getInstance()`） | `SnowFlake.java` 及 31 个调用类 |
| WebSocket 定向推送失效/NPE | 已修（Long→String 比较、判空、去 System.out） | `WebSocketService.java` |
| WebSocket `allowedOrigins("*")` | 已修（改白名单前缀匹配） | `WebSocketConfig.java` |
| CORS 端口不匹配 | 已修（改 `addAllowedOriginPattern` 放行白名单域名任意端口） | `CorsConfig.java` |
| `/system/upload` NPE / mkdirs / success 矛盾 | 已修 | `ImageUtils.java`、`UpLoadController.java`、`DataVo.java` |
| logger 类名写错 | 已修 | `UpLoadController.java` |
| PageVo 除零 | 已修 | `PageVo.java` |
| FileUtils 流泄漏 | 已修（try-with-resources） | `FileUtils.java` |
| 拦截器潜在 NPE | 已修 | `AdminInterceptor.java`、`UserInterceptor.java` |
| **pom 依赖坐标错误（新增发现，导致项目根本无法构建）** | 已修 | `pom.xml` |

> 新增发现：`pom.xml` 中阿里短信 SDK 的 groupId 写作 `com.alibaba.aliyun.dysmsapi`，
> 该坐标在中央仓库不存在，Maven 直接解析失败。已改为官方坐标 `com.aliyun`，编译通过。

**未改动（需业务/运维决策，已说明）**
- `SearchService.searchInfo` 空实现：搜索功能需按索引方案重新实现，非单纯 Bug 修复。
- `Const.UPLOAD_PATH = "./uploadfiles"` 相对路径、application.yml 中 `devtools.enabled` 与明文数据库口令：属部署配置，建议上线前处理。

---

## 一、严重 Bug（建议优先修复）

### 1. 积分扣减符号反转 —— 购买资源反而加积分

**位置**
- `module/order/service/OrderService.java:57,62`
- `module/score/service/ScoreRuleService.java:219-220`
- SQL：`module/user/dao/UserDao.xml:274-286`

```java
// OrderService.addSharOrdere —— 购买分享资源，本应扣分
scoreDetail.setScore(-share.getNeedmoney());   // 已是负数
scoreDetailService.saveScoreDetail(scoreDetail, "reduce");
```

```xml
<!-- updateUserAccountScore -->
<when test="calculate == 'reduce'">score = score - #{score}</when>
```

`reduce` 分支执行 `score - (-needmoney)` = **`score + needmoney`**，用户购买资源后积分不减反增。
且前面 `account.getScore() < share.getNeedmoney()` 的余额校验形同虚设。

同样的写法出现在撤销奖励逻辑中：

```java
// ScoreRuleService.scoreRuleCancelBonus —— 撤销奖励，本应扣回
scoreDetail.setScore(-scoreDetail.getScore());
scoreDetailService.saveScoreDetail(scoreDetail, "reduce");   // 结果：再加一次
```

撤销奖励会**再给用户加一次积分**。

**正确约定**（参考 `ScoreRuleService:197` 的正向用法）：
- 加积分 → 正数 + `"plus"`
- 扣积分 → 正数 + `"reduce"`（或负数 + `"plus"`）

**修复建议**：两处均改为 `setScore(正值)` 配 `"reduce"`。

---

### 2. SnowFlake 全局 ID 会重复

**位置**：全项目 **38 处** `new SnowFlake(2, 3)`，均为方法内临时新建，无 Spring 单例。

```java
// ScoreDetailService.saveScoreDetail:57
SnowFlake snowFlake = new SnowFlake(2, 3);
scoreDetail.setId(snowFlake.nextId());
```

`SnowFlake` 的实例字段 `sequence = 0`、`lastStmp = -1`。每次 `new` 都会重置：
同一毫秒内两个不同实例生成的时间戳段、机器段完全相同且 `sequence` 均为 0，
产生**完全相同的 ID** → 主键冲突 / 数据覆盖。批量插入或并发下必然触发。

**修复建议**：注册为 `@Bean` 单例（注意 `nextId()` 已 `synchronized`）：

```java
@Bean
public SnowFlake snowFlake() { return new SnowFlake(2, 3); }
```

并全局替换为注入使用，去掉 38 处 `new`。

---

## 二、功能性问题

### 3. WebSocket 定向推送失效，且存在 NPE

**位置**：`module/websocket/service/WebSocketService.java:86`

```java
if (user.getAttributes().get("accountId").equals(userId)) {
```

- `accountId` 在 `WebSocketInterceptor:32` 存的是 `user.getUserId()`，类型为 **Long**
- 参数 `userId` 是 **String**
- `Long.equals(String)` 恒为 `false` → **定向推送永远发不出去**
- 若会话没有该属性（如 SockJS 分支）→ 直接 **NPE**

**修复建议**：`String.valueOf(user.getAttributes().get("accountId")).equals(userId)`，并做 null 判断。

附带：`WebSocketConfig:30` 使用 `setAllowedOrigins("*")`，任意站点都可建立 WebSocket 连接，建议改为白名单。

---

### 4. CORS 白名单无法匹配带端口的前端地址

**位置**：`config/CorsConfig.java:12-25`

```java
private static String[] orginVal = { "www.28844.com", "28844.com", "localhost", "127.0.0.1" };
corsConfiguration.addAllowedOrigin("http://" + origin);   // e.g. http://localhost
```

Origin 必须**精确匹配协议+域名+端口**。后端 `server.port=80`，前端开发服务通常跑在
3000/8080，其 Origin 是 `http://localhost:3000`，与 `http://localhost` 不匹配 → **跨域被拦截**。

**修复建议**：`corsConfiguration.addAllowedOriginPattern("*")`（配合 `setAllowCredentials(true)`），
或把开发环境前端端口显式写进白名单。

---

### 5. `/system/upload` 接口的几处问题

**位置**：`core/controller/UpLoadController.java:140-169`、`core/utils/ImageUtils.java:249-285`

1. `ImageUtils.uploadFile` 在文件校验失败时 **返回 `null`**（第 259 行），
   Controller 第 149 行直接 `file.getImgurl()` → **NPE**（被 catch 吞掉，对外表现为"操作失败"）。
2. 第 266-268 行 `dest.mkdirs()` 创建的是**包含文件名的目录**，随后再 `delete()` 掉，
   属于错误写法（当前侥幸可用，但语义混乱）。应改为 `dest.getParentFile().mkdirs()`。
3. 上传失败分支返回 `DataVo.success("上传失败", msg)`，状态码与文案矛盾，前端无法据此判断成败。
4. 第 264 行遗留 `System.out.println` 调试输出。

---

## 三、次要问题

| # | 位置 | 问题 |
|---|------|------|
| 6 | `core/controller/UpLoadController.java:50` | logger 写成了 `CaptchaController.class`（复制粘贴错误），日志归类错乱 |
| 7 | `interceptor/AdminInterceptor.java:89-90`、`UserInterceptor.java:89-90` | `isLoginRequest()` 恒返回 `false`，但 `\|\|` 右侧一旦为真则 `admin.getId()` / `user.getUserId()` 会 NPE（潜在） |
| 8 | `core/entity/PageVo.java:96` | `getPageCount()` 在 `rows == 0` 时除零抛 `ArithmeticException` |
| 9 | `module/search/service/SearchService.java:56-62` | `searchInfo` 是空实现，永远返回空列表（搜索功能不可用） |
| 10 | `constant/Const.java:52` | `UPLOAD_PATH = "./uploadfiles"` 依赖进程工作目录，部署切换目录会导致上传丢失 |
| 11 | `src/main/resources/application.yml` | `devtools.enabled: true`、数据库口令明文（`root/123456`），生产环境需关闭/外置 |
| 12 | `core/utils/FileUtils.java:440-459` | `copyFile` 中 `FileOutputStream` 未关闭，资源泄漏 |
| 13 | `module/order/service/OrderService.java:69` | 遗留 `System.out.println` 调试输出 |

---

## 四、已确认加固到位（无需修改）

- **ORDER BY 注入**：所有 `${orderby}` / `${order}` 拼接点均经 `OrderbyUtils.check` 正则白名单校验，
  非法值回落默认值。
- **动态建表 / 列名注入**：`SqlSafeUtil.safeColumnName`（正则白名单+保留字黑名单）与
  `safeNumber`（纯数字）已覆盖 `ModelDataDao.xml` 的 DDL/DML 拼接。
- **文件上传**：`UploadSafeUtil.safeImage` 做了扩展名 + MIME + 文件头魔数三重校验，
  并强制重命名丢弃原始文件名，路径穿越风险已封堵。

---

## 五、修复优先级建议

1. **P0** 积分符号反转（`OrderService` / `ScoreRuleService`）—— 直接造成资金/积分错误
2. **P0** SnowFlake 单例化 —— 并发下主键冲突
3. **P1** WebSocket 定向推送类型不匹配 + NPE
4. **P1** CORS 端口匹配
5. **P2** `/system/upload` NPE 与错误返回、logger 类名、除零、搜索空实现等
