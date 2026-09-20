# 后端开发规范与模块 SOP

以"新增一个管理模块"为完整动线，代码模板均提取自本项目现有代码（AdminService/AdminDao/AdminController 的真实写法），照抄结构、替换业务字段即可。

## 0. 动线总览

```
建表(SQL入doc/) → model → dao接口+XML → service → controller(/system 或 /api)
→ permission_sync 注册权限 + 角色组勾权 → 前端页面/路由/v-access（见 docs/frontend-access-guide.md §4）
```

## 1. 建表约定

- 表名 `fly_` 前缀；主键 `bigint unsigned`，应用层 `SnowFlake.nextId()` 生成（参照 `fly_admin` 的 seed 数据形态：272835742965968896）。
- 关联用 merge 表（如 `fly_admin_group_merge`），主键即业务联合主键——这是本项目的既有关联风格，不要引入中间表新花样。
- SQL 变更同步补进 `doc/`（保持 `doc/flycms_date.sql` 可建完整库）。
- 注意：遗留表存在 `createAt`（驼峰）与 `last_login_time`（下划线）混用；**新表统一下划线**，model 字段映射在 XML 里显式 `resultMap`/别名，不依赖全局驼峰转换。

## 2. Model

```java
@Setter
@Getter
public class FlyDemo implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    @NotEmpty(message = "名称不能为空")          // jakarta.validation，配合 controller 的 @Valid
    private String name;
    private int status;
    private Date createAt;
}
```

## 3. DAO（接口 + XML 同目录）

`src/main/java/com/flycms/module/demo/dao/DemoDao.java`：

```java
@Repository
public interface DemoDao {
    public int addDemo(FlyDemo demo);
    public int updateDemo(FlyDemo demo);
    public int deleteDemoById(@Param("id") Long id);
    public FlyDemo findDemoById(@Param("id") Long id, @Param("status") int status);
    public List<FlyDemo> getDemoList(@Param("offset") int offset, @Param("rows") int rows);
    public int getDemoCount();
}
```

同目录 `DemoDao.xml`（**必须**与接口同包，否则 mapper-locations 扫不到）：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.flycms.module.demo.dao.DemoDao">
    <sql id="base_column">id, name, status, create_at AS createAt</sql>

    <select id="findDemoById" resultType="com.flycms.module.demo.model.FlyDemo">
        select <include refid="base_column"/> from fly_demo
        where id = #{id}
        <if test="status != 0">and status = #{status}</if>
    </select>

    <select id="getDemoList" resultType="com.flycms.module.demo.model.FlyDemo">
        select <include refid="base_column"/> from fly_demo
        order by id desc limit #{offset}, #{rows}
    </select>

    <select id="getDemoCount" resultType="int">select count(*) from fly_demo</select>

    <insert id="addDemo">
        insert into fly_demo (id, name, status, create_at)
        values (#{id}, #{name}, #{status}, #{createAt})
    </insert>
</mapper>
```

要点：参数一律 `#{}`（预编译）；列表查询手写 `limit #{offset}, #{rows}` 与 PageVo 配合；条件用 `<if>` 动态拼接。

## 4. Service

```java
@Service
public class DemoService {
    @Resource private DemoDao demoDao;
    @Resource private SnowFlake snowFlake;      // 已注册为 bean，直接注入

    public DataVo addDemo(FlyDemo demo) {
        if (demoDao.findDemoByName(demo.getName()) != null) {
            return DataVo.failure("名称已存在");
        }
        demo.setId(snowFlake.nextId());
        demo.setCreateAt(new Date());
        return demoDao.addDemo(demo) > 0 ? DataVo.success("添加成功") : DataVo.failure("添加失败");
    }

    public PageVo<FlyDemo> getDemoListPage(String name, int pageNum, int rows) {
        PageVo<FlyDemo> pageVo = new PageVo<>(pageNum);
        pageVo.setRows(rows);
        pageVo.setList(demoDao.getDemoList(name, pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(demoDao.getDemoCount(name));
        return pageVo;
    }
}
```

**响应重载铁律**：`DataVo.success(String)` 是填 message；`DataVo.success(Object)` 是填 data（且 message=null）；带数据返回统一 `DataVo.success("消息", data)` 两参重载。

## 5. Controller（二选一或并存）

**A. 管理后台页面型（web/system，Freemarker + 局部 JSON，既有风格）：**

```java
@Controller
@RequestMapping("/system/demo")
public class DemoAdminController extends BaseController {
    @Resource private DemoService demoService;

    @GetMapping("/demo_list")                    // 页面渲染，自动受 AdminInterceptor URL 权限保护
    public String demoList(@RequestParam(value = "p", defaultValue = "1") int pageNum, ModelMap modelMap) {
        modelMap.put("pageVo", demoService.getDemoListPage(pageNum, 20));
        modelMap.addAttribute("admin", getAdminUser());          // 模板必需
        return theme.getAdminTemplate("demo/demo_list");         // 视图文件 views/<模板>/demo/demo_list.ftl
    }

    @ResponseBody
    @PostMapping("/demo_save")                   // JSON 动作
    public DataVo demoSave(@Valid FlyDemo demo, BindingResult result) {
        if (result.hasErrors()) {
            return DataVo.failure(result.getAllErrors().get(0).getDefaultMessage());
        }
        return demoService.addDemo(demo);
    }
}
```

BaseController 已注入 `request/response/session/theme` 并提供 `getAdminUser()`/`getUser()`，直接用，不要自己再注入一份。

**B. 给 vben 前端的 REST 型（web/api，/api/**）：**

- 路径 `/api/<域>/...`，全部 `@ResponseBody` 返回 `DataVo`；
- **每个端点自查登录**：`AdminSessionUtils.getLoginMember(request)` 为空时 `throw new ResponseStatusException(HttpStatus.UNAUTHORIZED)`（vben 前端靠 HTTP 401 触发重新登录，老 `/system/**` 的 `login_status:300` 它不认）；
- 骨架见 `docs/frontend-access-guide.md` §2.4 的 ApiAuthController。

## 6. 权限闭环（/system/** 端点必做，漏了上线即 403）

1. 启动后以超管调用 `GET /system/admin/permission_sync`——框架扫描 `RequestMappingHandlerMapping` 自动把新路由注册为权限节点（路径参数替换为 `*`）。
2. 管理后台"权限组管理"把新 action_key 勾给对应角色组（超级管理员组 id=1 硬编码全量放行）。
3. 前端按钮控制用同一个 action_key 字符串（见 frontend-access 手册）。

## 7. 自检清单（提交前）

- [ ] XML 与 DAO 接口同目录，`mvn -q compile` 通过
- [ ] 新表 SQL 已进 `doc/`
- [ ] controller 归位正确；/api/** 端点有 401 自查
- [ ] /system/** 新路由已 permission_sync + 角色组授权
- [ ] 无 `org.apache.commons.lang`（2.x）新引用、无字符串拼 SQL、密码经 BCryptUtils
- [ ] 冒烟：登录 → 菜单 → 新页面/接口可用
