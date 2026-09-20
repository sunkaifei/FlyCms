package com.flycms.web.api;

import com.flycms.core.base.BaseController;
import com.flycms.core.utils.AdminSessionUtils;
import com.flycms.core.utils.CheckUrlUtils;
import com.flycms.module.admin.model.Admin;
import com.flycms.module.admin.model.Permission;
import com.flycms.module.admin.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * vben 前端 REST 接口基类。
 *
 * /api/** 不在 AdminInterceptor 拦截范围内（它只拦 /system/**，且未登录时以 302/{login_status:300}
 * 响应，vben 只认 HTTP 401），因此这里统一做两道守卫：
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
}
