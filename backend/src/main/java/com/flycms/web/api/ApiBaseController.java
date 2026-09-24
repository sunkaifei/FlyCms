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
}
