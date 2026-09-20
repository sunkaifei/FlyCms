package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.admin.model.Admin;
import com.flycms.module.admin.model.Group;
import com.flycms.module.admin.model.Permission;
import com.flycms.module.admin.service.AdminService;
import com.flycms.module.admin.service.GroupService;
import com.flycms.module.admin.service.PermissionService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * vben 前端系统管理 REST 接口：管理员、角色组、权限节点。
 *
 * 权限复用老后台 /system/admin/** 的 action_key（见 requirePermission），
 * 超级管理员组（id=1）的保护规则与老 AdminController 保持一致。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/api/system")
public class ApiSystemController extends ApiBaseController {

    @Autowired
    protected AdminService adminService;

    @Autowired
    protected GroupService groupService;

    @Autowired
    protected PermissionService permissionService;

    // ///////////////////////////////
    // /////       管理员       ////////
    // ///////////////////////////////

    /**
     * 管理员分页列表（密码散列不下发）
     */
    @ResponseBody
    @GetMapping("/admin/list")
    public DataVo adminList(@RequestParam(value = "adminName", required = false) String adminName,
                            @RequestParam(value = "p", defaultValue = "1") int pageNum) {
        requirePermission("/system/admin/admin_list");
        PageVo<Admin> pageVo = adminService.getAdminListPage(adminName, null, null, null, pageNum, 20);
        if (pageVo.getList() != null) {
            for (Admin admin : pageVo.getList()) {
                admin.setPassword(null);
            }
        }
        return DataVo.success("操作成功", pageVo);
    }

    /**
     * 新增管理员
     */
    @ResponseBody
    @PostMapping("/admin/save")
    public DataVo adminSave(@RequestParam(value = "adminName", required = false) String adminName,
                            @RequestParam(value = "password", required = false) String password,
                            @RequestParam(value = "repassword", required = false) String repassword,
                            @RequestParam(value = "nickName", required = false) String nickName,
                            @RequestParam(value = "roleId", defaultValue = "0") Long roleId) {
        requirePermission("/system/admin/admin_save");
        if (StringUtils.isBlank(adminName)) {
            return DataVo.failure("用户名不能为空");
        }
        if (StringUtils.isBlank(password)) {
            return DataVo.failure("密码不能为空");
        }
        if (!password.equals(repassword)) {
            return DataVo.failure("两次密码不一样");
        }
        if (adminService.checkAdminByName(adminName)) {
            return DataVo.failure("用户名已被占用！");
        }
        Admin admin = new Admin();
        admin.setAdminName(adminName);
        admin.setNickName(nickName);
        admin.setPassword(password);
        admin.setRepassword(repassword);
        admin.setRoleId(roleId);
        return adminService.addAdmin(admin);
    }

    /**
     * 编辑管理员（password 留空则不修改密码）
     */
    @ResponseBody
    @PostMapping("/admin/update")
    public DataVo adminUpdate(@RequestParam(value = "id", defaultValue = "0") Long id,
                              @RequestParam(value = "adminName", required = false) String adminName,
                              @RequestParam(value = "nickName", required = false) String nickName,
                              @RequestParam(value = "password", required = false) String password,
                              @RequestParam(value = "repassword", required = false) String repassword,
                              @RequestParam(value = "roleId", defaultValue = "0") Long roleId) {
        requirePermission("/system/admin/admin_act");
        if (id == null || id <= 0) {
            return DataVo.failure("参数传递错误");
        }
        if (StringUtils.isBlank(adminName)) {
            return DataVo.failure("用户名不能为空");
        }
        if (StringUtils.isNotBlank(password) && !password.equals(repassword)) {
            return DataVo.failure("两次密码不一样");
        }
        if (adminService.checkAdminByName(adminName, id)) {
            return DataVo.failure("管理员用户名已存在！");
        }
        Admin admin = new Admin();
        admin.setId(id);
        admin.setAdminName(adminName);
        admin.setNickName(nickName);
        admin.setPassword(StringUtils.isBlank(password) ? null : password);
        admin.setRoleId(roleId);
        return adminService.updateAdmin(admin);
    }

    /**
     * 删除管理员（超管保护与老后台一致）
     */
    @ResponseBody
    @PostMapping("/admin/delete")
    public DataVo adminDelete(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/system/admin/delAdmin");
        if (id == null || id <= 0) {
            return DataVo.failure("参数传递错误");
        }
        if (id == 1L) {
            return DataVo.failure("超级管理员组不能删除");
        }
        return adminService.deleteAdminById(id);
    }

    // ///////////////////////////////
    // /////       角色组       ////////
    // ///////////////////////////////

    /**
     * 全部角色组（列表与下拉通用）
     */
    @ResponseBody
    @GetMapping("/group/list")
    public DataVo groupList() {
        requirePermission("/system/admin/group_list");
        return DataVo.success("操作成功", groupService.getAllGroupList());
    }

    /**
     * 新增/编辑角色组（id 为空或 0 时新增）
     */
    @ResponseBody
    @PostMapping("/group/save")
    public DataVo groupSave(@RequestParam(value = "id", required = false) Long id,
                            @RequestParam(value = "name", required = false) String name) {
        requirePermission(StringUtils.isNotBlank(name) && id != null && id > 0
                ? "/system/admin/update_group_save"
                : "/system/admin/add_group_save");
        if (StringUtils.isBlank(name)) {
            return DataVo.failure("会员组名不能为空");
        }
        if (id != null && id > 0) {
            return groupService.updateGroup(name, id);
        }
        return groupService.addGroup(name);
    }

    /**
     * 删除角色组（超管保护与老后台一致）
     */
    @ResponseBody
    @PostMapping("/group/delete")
    public DataVo groupDelete(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/system/admin/group_del");
        if (id == null || id <= 0) {
            return DataVo.failure("参数传递错误");
        }
        if (id == 1L) {
            return DataVo.failure("超级管理员组不能删除");
        }
        if (groupService.deleteGroup(id)) {
            return DataVo.success("该权限组已删除");
        }
        return DataVo.failure("删除失败或者不存在！");
    }

    /**
     * 角色组已勾选的权限节点 id 列表（分配权限弹窗回显用）
     */
    @ResponseBody
    @GetMapping("/group/permissionIds")
    public DataVo groupPermissionIds(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/system/admin/group_assignPermissions/*");
        if (id == null || id <= 0) {
            return DataVo.failure("参数传递错误");
        }
        return DataVo.success("操作成功", groupService.findGroupPermissionIds(id));
    }

    /**
     * 保存角色组的权限勾选（全量提交，内部做增删差量）
     */
    @ResponseBody
    @PostMapping("/group/assignPermissions")
    public DataVo groupAssignPermissions(@RequestParam(value = "groupId", defaultValue = "0") Long groupId,
                                         @RequestParam(value = "permissionIds", required = false) List<Long> permissionIds) {
        requirePermission("/system/admin/group_markpermissions");
        if (groupId == null || groupId <= 0) {
            return DataVo.failure("参数错误！");
        }
        if (groupId == 1L) {
            return DataVo.failure("超级管理员组权限不能修改");
        }
        List<Long> oldIds = groupService.findGroupPermissionIds(groupId);
        List<Long> newIds = permissionIds == null ? java.util.Collections.emptyList() : permissionIds;
        int changed = 0;
        for (Long oldId : oldIds) {
            if (!newIds.contains(oldId) && groupService.deleteGroupPermission(groupId, oldId)) {
                changed++;
            }
        }
        for (Long newId : newIds) {
            if (!oldIds.contains(newId) && groupService.addGroupPermission(groupId, newId)) {
                changed++;
            }
        }
        return DataVo.success("已保存，变更 " + changed + " 项");
    }

    // ///////////////////////////////
    // /////       权限节点      ////////
    // ///////////////////////////////

    /**
     * 权限节点分页列表
     */
    @ResponseBody
    @GetMapping("/permission/list")
    public DataVo permissionList(@RequestParam(value = "p", defaultValue = "1") int pageNum) {
        requirePermission("/system/admin/permission_list");
        return DataVo.success("操作成功", permissionService.getPermissionListPage(pageNum, 20));
    }

    /**
     * 全部权限节点（分配权限弹窗与下拉通用）
     */
    @ResponseBody
    @GetMapping("/permission/all")
    public DataVo permissionAll() {
        requirePermission("/system/admin/permission_list");
        return DataVo.success("操作成功", permissionService.getAllPermissions());
    }

    /**
     * 同步全部权限节点（扫描 /system/** 路由注册 action_key）
     */
    @ResponseBody
    @PostMapping("/permission/sync")
    public DataVo permissionSync() {
        requirePermission("/system/admin/permission_sync");
        if (permissionService.getSyncAllPermission()) {
            return DataVo.success("同步权限成功");
        }
        return DataVo.failure("同步权限失败");
    }

    /**
     * 删除权限节点
     */
    @ResponseBody
    @PostMapping("/permission/delete")
    public DataVo permissionDelete(@RequestParam(value = "id", defaultValue = "0") Long id) {
        requirePermission("/system/admin/permission_del");
        if (id == null || id <= 0) {
            return DataVo.failure("参数传递错误");
        }
        if (permissionService.deletePermission(id)) {
            return DataVo.success("该权限已删除");
        }
        return DataVo.failure("删除失败或者不存在！");
    }

    /**
     * 编辑权限节点（action_key / 备注）
     */
    @ResponseBody
    @PostMapping("/permission/update")
    public DataVo permissionUpdate(@RequestParam(value = "id", defaultValue = "0") Long id,
                                   @RequestParam(value = "actionKey", required = false) String actionKey,
                                   @RequestParam(value = "remark", required = false) String remark) {
        requirePermission("/system/admin/permission_update_save");
        if (id == null || id <= 0 || StringUtils.isBlank(actionKey)) {
            return DataVo.failure("参数传递错误");
        }
        Permission permission = new Permission();
        permission.setId(id);
        permission.setActionKey(actionKey);
        permission.setRemark(remark);
        return permissionService.updatePermissions(permission);
    }
}
