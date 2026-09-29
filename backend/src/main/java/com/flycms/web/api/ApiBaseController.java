package com.flycms.web.api;

import com.flycms.core.base.BaseController;
import com.flycms.core.utils.AdminSessionUtils;
import com.flycms.core.utils.CheckUrlUtils;
import com.flycms.module.admin.model.Admin;
import com.flycms.module.admin.model.Permission;
import com.flycms.core.entity.DataVo;
import com.flycms.module.admin.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * vben 前端 REST 接口基类。
 *
 * 旧 FreeMarker 后台的 AdminInterceptor 已随旧后台下线，API 鉴权统一由本基类做两道守卫：
 * 1. requireAdmin()：未登录抛 401，前端收到后走重新登录流程；
 * 2. requirePermission(actionKey)：已登录但角色组未勾选对应权限时抛 403。
 *    actionKey 直接复用老后台 /system/** 的同名权限节点（permission_sync 已注册），
 *    与前端 accessCodes 同源，按钮显隐和服务端放行天然一致。
 *
 * @author sun-kaifei
 * @version 1.0
 */
public abstract class ApiBaseController extends BaseController {

    @Autowired
    protected PermissionService permissionService;

    /**
     * 校验登录态，未登录返回 401
     */
    protected Admin requireAdmin() {
        Admin admin = AdminSessionUtils.getLoginMember(request);
        if (admin == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return admin;
    }

    /**
     * 当前登录管理员 id，未登录返回 null（供审计/站内信等记录操作人）
     */
    protected Long getLoginUserId() {
        Admin admin = AdminSessionUtils.getLoginMember(request);
        return admin == null ? null : admin.getId();
    }

    /**
     * 校验当前管理员是否拥有指定权限节点（与老后台 action_key 同源）
     */
    protected void requirePermission(String actionKey) {
        Admin admin = requireAdmin();
        List<Permission> permissions = permissionService.findPermissionByUserId(admin.getId());
        if (permissions != null) {
            for (Permission permission : permissions) {
                if (CheckUrlUtils.match(permission.getActionKey(), actionKey)) {
                    return;
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    /**
     * G14 字段级权限：当前管理员是否被授权指定节点（不抛异常的布尔变体）。
     */
    protected boolean hasPermission(String actionKey) {
        Admin admin = AdminSessionUtils.getLoginMember(request);
        if (admin == null) {
            return false;
        }
        List<Permission> permissions = permissionService.findPermissionByUserId(admin.getId());
        if (permissions != null) {
            for (Permission permission : permissions) {
                if (CheckUrlUtils.match(permission.getActionKey(), actionKey)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * G14 字段锁判定：fly_admin_permission 存在精确行 `/api/system/modelData/field/{modelId}/{fieldName}`
     * 即视为「锁定」——未授权角色不可见（读剔除）不可写（写拒绝）；行不存在则字段开放（向后兼容）。
     */
    protected boolean isFieldLocked(Long modelId, String fieldName) {
        String key = "/api/system/modelData/field/" + modelId + "/" + fieldName;
        return permissionService.existsPermissionRow(key) && !hasPermission(key);
    }

    /**
     * G14：从字段元数据清单中剔除当前管理员无权查看的字段（formMeta 表单渲染用）。
     */
    protected List<com.flycms.module.model.model.ModelField> filterFieldsByPermission(
            Long modelId, List<com.flycms.module.model.model.ModelField> fields) {
        List<com.flycms.module.model.model.ModelField> out = new ArrayList<>();
        if (fields == null) {
            return out;
        }
        for (com.flycms.module.model.model.ModelField f : fields) {
            if (!isFieldLocked(modelId, f.getFieldName())) {
                out.add(f);
            }
        }
        return out;
    }

    /**
     * G14：从行 Map 中剔除无权字段的键及其读侧展开键（{f}Obj/{f}List/{f}Url/{f}Urls/{f}Backs）。
     */
    protected void stripRowByPermission(Long modelId, Map<String, Object> row,
                                        List<com.flycms.module.model.model.ModelField> fields) {
        if (row == null || fields == null) {
            return;
        }
        for (com.flycms.module.model.model.ModelField f : fields) {
            if (!isFieldLocked(modelId, f.getFieldName())) {
                continue;
            }
            String name = f.getFieldName();
            row.remove(name);
            row.remove(name + "Obj");
            row.remove(name + "List");
            row.remove(name + "Url");
            row.remove(name + "Urls");
            row.remove(name + "Backs");
        }
    }

    /**
     * G14：写侧拦截——表单里出现无权字段（非空值）直接业务失败（DataVo.failure）。
     */
    protected DataVo assertFieldsWritable(Long modelId, Map<String, String> form,
                                          List<com.flycms.module.model.model.ModelField> fields) {
        if (form == null || fields == null) {
            return null;
        }
        for (com.flycms.module.model.model.ModelField f : fields) {
            if (!isFieldLocked(modelId, f.getFieldName())) {
                continue;
            }
            String v = form.get(f.getFieldName());
            if (v != null && !v.isBlank()) {
                return DataVo.failure("无权限提交字段：" + f.getFieldLabel());
            }
        }
        return null;
    }
}
