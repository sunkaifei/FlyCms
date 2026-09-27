package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.utils.CheckUrlUtils;
import com.flycms.module.admin.model.Admin;
import com.flycms.module.admin.model.Permission;
import com.flycms.module.admin.service.PermissionService;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.service.ModelService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单管理（若依式单表：fly_admin_permission = 菜单/按钮树）+ 后端动态菜单。
 *
 * 节点类型：M 目录（无 actionKey，可见性由子节点推导）/ C 菜单（actionKey=授权锚点，
 * path/component 为前端路由与视图）/ F 按钮（actionKey 即前端权限码）。
 * /api/menu/all 输出 M/C 树（vben backend 模式），并遍历启用模型在 /modelData 目录下
 * 追加各模型的内容管理入口（ADR D3 翻案落地）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/api")
public class ApiMenuController extends ApiBaseController {

    /** 旧后台接口容器节点（见 sql/menu-management.sql），sync 新增的 F 行归入其下 */
    private static final long LEGACY_PARENT_ID = 900130L;

    @Autowired
    private com.flycms.module.admin.dao.PermissionDao permissionDao;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private ModelService modelService;

    // /////////////////// 菜单 CRUD ///////////////////

    @ResponseBody
    @GetMapping("/system/menu/list")
    public DataVo list() {
        requirePermission("/api/system/menu/list");
        return DataVo.success("操作成功", permissionDao.getAllMenuNodes());
    }

    @ResponseBody
    @PostMapping("/system/menu/save")
    public DataVo save(@RequestParam Map<String, String> params) {
        requirePermission("/api/system/menu/save");
        Permission node = new Permission();
        fill(node, params);
        if (StringUtils.isBlank(node.getMenuName())) {
            return DataVo.failure("菜单名称不能为空");
        }
        if ("C".equals(node.getMenuType()) && StringUtils.isBlank(node.getActionKey())) {
            return DataVo.failure("菜单节点必须填写权限标识（actionKey）");
        }
        if (!"F".equals(node.getMenuType()) && StringUtils.isBlank(node.getPath())) {
            return DataVo.failure("目录/菜单节点必须填写路由路径");
        }
        node.setId(new com.flycms.core.utils.SnowFlake(2, 3).nextId());
        permissionDao.addPermission(node);
        return DataVo.success("菜单已添加");
    }

    @ResponseBody
    @PostMapping("/system/menu/update")
    public DataVo update(@RequestParam Map<String, String> params) {
        requirePermission("/api/system/menu/update");
        Long id = parseLong(params.get("id"));
        if (id == null) {
            return DataVo.failure("参数传递错误");
        }
        Permission node = new Permission();
        node.setId(id);
        fill(node, params);
        permissionDao.updateMenuRow(node);
        return DataVo.success("菜单已更新");
    }

    @ResponseBody
    @PostMapping("/system/menu/del")
    public DataVo del(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/api/system/menu/del");
        if (permissionDao.hasMenuChildren(id)) {
            return DataVo.failure("请先删除下级节点");
        }
        permissionDao.deletePermission(id);
        permissionDao.deleteRolePermission(id);
        return DataVo.success("菜单已删除");
    }

    // /////////////////// 后端动态菜单（vben backend 模式） ///////////////////

    @ResponseBody
    @GetMapping("/menu/all")
    public DataVo menuAll() {
        Admin admin = requireAdmin();
        List<String> codes = new ArrayList<>();
        for (com.flycms.module.admin.model.Permission p : permissionService.findPermissionByUserId(admin.getId())) {
            codes.add(p.getActionKey());
        }
        List<Permission> nodes = permissionDao.getMenuNodes();
        List<Map<String, Object>> routes = buildRoutes(nodes, codes);
        return DataVo.success("操作成功", routes);
    }

    /**
     * 组装 vben RouteRecordStringComponent 树（M/C 任意层级递归）：
     * - M → BasicLayout；C → 页面组件（visible=0 输出 hideInMenu，仅注册路由）
     * - C/M 可见性：actionKey 为空登录即可见；否则授权码精确或通配命中
     * - F 按钮节点不参与路由（getMenuNodes 已过滤，仅作权限锚点）
     * - /modelData 目录下追加启用模型的动态内容管理入口
     *
     * <p>2026-09-27 由「仅两级」改为递归：原实现只输出 M 的直接子节点，
     * 挂在 C 菜单下的 C 孙子节点会被无声丢弃（stage-j2 的 900188 主题市场
     * 挂在 900160 模板管理下即踩中此坑，页面始终不出现）。
     */
    private List<Map<String, Object>> buildRoutes(List<Permission> nodes, List<String> codes) {
        Map<Long, Permission> byId = new HashMap<>();
        for (Permission n : nodes) {
            byId.put(n.getId(), n);
        }
        Map<Long, List<Permission>> children = new HashMap<>();
        List<Permission> roots = new ArrayList<>();
        for (Permission n : nodes) {
            Permission parent = n.getParentId() == null ? null : byId.get(n.getParentId());
            if (parent == null) {
                roots.add(n);
            } else {
                children.computeIfAbsent(parent.getId(), k -> new ArrayList<>()).add(n);
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Permission root : roots) {
            Map<String, Object> route = buildRouteTree(root, children, codes);
            if (route != null) {
                out.add(route);
            }
        }
        return out;
    }

    /**
     * 递归组装单棵路由子树。返回 null 表示该子树整体不输出
     * （空目录：M 无可见子节点且自身无权限锚点）。
     */
    private Map<String, Object> buildRouteTree(Permission node, Map<Long, List<Permission>> children,
                                               List<String> codes) {
        Map<String, Object> route = toRoute(node);
        List<Map<String, Object>> childRoutes = new ArrayList<>();
        for (Permission k : children.getOrDefault(node.getId(), new ArrayList<>())) {
            if (!nodeAllowed(k, codes)) {
                continue;
            }
            Map<String, Object> child = buildRouteTree(k, children, codes);
            if (child != null) {
                childRoutes.add(child);
            }
        }
        // 模型动态菜单挂到 /modelData 目录
        if ("/modelData".equals(node.getPath())) {
            for (Model model : modelService.getEnabledModels()) {
                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("title", model.getName() + "管理");
                meta.put("icon", StringUtils.defaultIfBlank(model.getIcon(), "lucide:file-text"));
                Map<String, Object> child = new LinkedHashMap<>();
                child.put("name", "ModelData-" + model.getCode());
                child.put("path", "/modelData/" + model.getCode());
                child.put("component", "/system/modeldata/list");
                child.put("meta", meta);
                childRoutes.add(child);
            }
        }
        if (!childRoutes.isEmpty()) {
            route.put("children", childRoutes);
            return route;
        }
        // 叶子 M 目录：无权限锚点则不输出（与原「空目录不输出」行为一致）
        if ("M".equals(node.getMenuType()) && StringUtils.isBlank(node.getActionKey())) {
            return null;
        }
        return route;
    }

    private Map<String, Object> toRoute(Permission node) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("title", StringUtils.defaultIfBlank(node.getMenuName(), node.getActionKey()));
        if (node.getIcon() != null) {
            meta.put("icon", node.getIcon());
        }
        meta.put("order", node.getSort() == null ? 0 : node.getSort());
        if (node.getVisible() != null && node.getVisible() == 0) {
            meta.put("hideInMenu", true);
        }
        Map<String, Object> route = new LinkedHashMap<>();
        route.put("name", "Menu-" + node.getId());
        route.put("path", StringUtils.defaultIfBlank(node.getPath(), "/menu-" + node.getId()));
        route.put("meta", meta);
        if ("M".equals(node.getMenuType())) {
            route.put("component", "BasicLayout");
        } else if (node.getComponent() != null) {
            route.put("component", node.getComponent());
        }
        return route;
    }

    private boolean nodeAllowed(Permission node, List<String> codes) {
        String key = node.getActionKey();
        if (StringUtils.isBlank(key)) {
            return true;
        }
        for (String code : codes) {
            if (code == null) {
                continue;
            }
            if (code.equals(key) || CheckUrlUtils.match(code, key)) {
                return true;
            }
        }
        return false;
    }

    private void fill(Permission node, Map<String, String> params) {
        node.setParentId(parseLong(params.get("parentId")));
        node.setMenuType(StringUtils.defaultIfBlank(params.get("menuType"), "F"));
        node.setMenuName(params.get("menuName"));
        node.setActionKey(StringUtils.trimToNull(params.get("actionKey")));
        node.setPath(StringUtils.trimToNull(params.get("path")));
        node.setComponent(StringUtils.trimToNull(params.get("component")));
        node.setIcon(StringUtils.trimToNull(params.get("icon")));
        node.setSort(parseInt(params.get("sort"), 0));
        node.setVisible(parseInt(params.get("visible"), 1));
        node.setRemark(params.get("remark"));
    }

    private Long parseLong(String v) {
        try {
            return v == null ? null : Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInt(String v, int def) {
        try {
            return v == null ? def : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
