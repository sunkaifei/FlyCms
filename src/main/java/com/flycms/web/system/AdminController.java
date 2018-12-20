package com.flycms.web.system;

import com.flycms.constant.Const;
import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.AdminSessionUtils;
import com.flycms.module.admin.model.Admin;
import com.flycms.module.admin.model.Group;
import com.flycms.module.admin.model.Permission;
import com.flycms.module.admin.service.AdminService;
import com.flycms.module.admin.service.GroupService;
import com.flycms.module.admin.service.PermissionService;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 开发公司：28844.com<br/>
 * 版权：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 2018-7-4
 */
@Controller
@RequestMapping("/system/admin")
public class AdminController extends BaseController {

    @Autowired
    protected AdminService adminService;

    @Autowired
    protected GroupService groupService;

    @Autowired
    protected PermissionService permissionService;
    //用户列表
    @GetMapping(value = "/admin_list")
    public String adminList(@RequestParam(value = "adminName", required = false) String adminName,
                            @RequestParam(value = "nickName", required = false) String nickName,
                            @RequestParam(value = "mobile", required = false) String mobile,
                            @RequestParam(value = "email", required = false) String email,
                            @RequestParam(value = "p", defaultValue = "1") int pageNum,
                            ModelMap modelMap){
        PageVo<Admin> pageVo=adminService.getAdminListPage(adminName, nickName, mobile, email,pageNum,20);
        List<Group> group=groupService.getAllGroupList();
        modelMap.put("group", group);
        modelMap.put("pageVo", pageVo);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("admin/admin_list");
    }

    //添加管理员
    @GetMapping(value = "/admin_add")
    public String adminAdd(ModelMap modelMap){
        List<Group> group=groupService.getAllGroupList();
        modelMap.put("group", group);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("admin/admin_add");
    }

    //处理和保存管理员信息
    @PostMapping("/admin_save")
    @ResponseBody
    public DataVo addAdminSave(@Valid Admin admin, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            data = adminService.addAdmin(admin);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //编辑用户
    @GetMapping(value = "/admin_edit/{adminId}")
    public String adminEdit(@PathVariable(value = "adminId", required = false) Long adminId,ModelMap modelMap){
        Admin user=adminService.findAdminById(adminId,0);
        if(user==null){
            modelMap.addAttribute("message", "用户不存在！");
            return theme.getAdminTemplate("common/message_tip");
        }
        int roleId=groupService.findUserAndGroupById(adminId);
        List<Group> role=groupService.getAllGroupList();
        modelMap.put("roleId", roleId);
        modelMap.put("role", role);
        modelMap.addAttribute("admin", getAdminUser());
        modelMap.addAttribute("user", user);
        return theme.getAdminTemplate("admin/admin_edit");
    }

    //处理和保存更新后管理员信息
    @PostMapping("/admin_act")
    @ResponseBody
    public DataVo addAdminAct(@Valid Admin admin, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            data = adminService.updateAdmin(admin);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //删除管理员
    @PostMapping("/delAdmin")
    @ResponseBody
    public DataVo deleteAdminById(@RequestParam(value = "id") Long id){
        DataVo data = DataVo.failure("操作失败");
        if(id==1){
            return data = DataVo.failure("超级管理员组不能删除");
        }
        data = adminService.deleteAdminById(id);
        return data;
    }

    //我的密码修改
    @GetMapping(value = "/admin_password")
    public String userPassword(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("admin/admin_password");
    }

    @ResponseBody
    @PostMapping(value = "/password_update")
    public DataVo updatePassword(
            @RequestParam(value = "old_password", required = false) String old_password,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "password_confirmation", required = false) String password_confirmation) {
        String kaptcha = (String) session.getAttribute("kaptcha");
        DataVo data = DataVo.failure("操作失败");
        try {
            if (StringUtils.isBlank(old_password)) {
                return DataVo.failure("原来密码不能为空");
            } else if (old_password.length() < 6 && old_password.length() >= 32) {
                return DataVo.failure("密码最少6个字符，最多32个字符");
            }
            if (StringUtils.isBlank(password)) {
                return DataVo.failure("新密码不能为空");
            } else if (password.length() < 6 && password.length() >= 32) {
                return DataVo.failure("密码最少6个字符，最多32个字符");
            }
            if (!password.equals(password_confirmation)) {
                return DataVo.failure("两次密码必须一样");
            }
            data=adminService.updateAdminPassword(getAdminUser().getId(), old_password, password);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //权限列表
    @GetMapping(value = "/permission_list")
    public String permissionList(@RequestParam(value = "p", defaultValue = "1") int pageNum, ModelMap modelMap){
        PageVo<Permission> pageVo=permissionService.getPermissionListPage(pageNum,20);
        modelMap.put("pageVo", pageVo);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("admin/admin_permission_list");
    }

    //同步所有权限
    @GetMapping("/permission_sync")
    @ResponseBody
    public DataVo getSyncAllPermission(){
        if(permissionService.getSyncAllPermission()){
            return DataVo.success("同步权限成功");
        }
        return DataVo.failure("同步权限失败");
    }

    //删除权限节点
    @PostMapping("/permission_del")
    @ResponseBody
    public DataVo deletePermission(@RequestParam(value = "id") Long id){
        DataVo data = DataVo.failure("操作失败");
        if(permissionService.deletePermission(id)){
            data = DataVo.success("该权限已删除");
        }else{
            data = DataVo.failure("删除失败或者不存在！");
        }
        return data;
    }

    @GetMapping(value = "/permission_update/{id}")
    public String updatePermissions(@PathVariable Long id, ModelMap modelMap){
        Permission permission = permissionService.findPermissionById(id);
        if(permission==null){
            modelMap.addAttribute("message", "该权限不存在");
            return theme.getAdminTemplate("common/message_tip");
        }
        modelMap.put("permission", permission);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("admin/admin_permission_update");
    }

    //处理用户组信息
    @PostMapping("/permission_update_save")
    @ResponseBody
    public DataVo updatePermissionsSave(@Valid Permission permission, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            data = permissionService.updatePermissions(permission);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }


    //添加用户权限
    @GetMapping(value = "/add_group")
    public String getAddGroup(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("admin/group_add");
    }

    //处理用户组信息
    @PostMapping("/add_group_save")
    @ResponseBody
    public DataVo getAddGroupSave(@RequestParam(value = "name", required = false) String name){
        if (StringUtils.isBlank(name)) {
            return DataVo.failure("会员组名不能为空");
        }
        return groupService.addGroup(name);
    }

    //删除权限组
    @PostMapping("/group_del")
    @ResponseBody
    public DataVo deleteGroup(@RequestParam(value = "id") Long id){
        DataVo data = DataVo.failure("操作失败");
        if(id==1){
            return data = DataVo.failure("超级管理员组不能删除");
        }
        if(groupService.deleteGroup(id)){
            data = DataVo.success("该权限组已删除");
        }else{
            data = DataVo.failure("删除失败或者不存在！");
        }
        return data;
    }

    @GetMapping(value = "/group_update/{id}")
    public String updateGroup(@PathVariable Long id, ModelMap modelMap){
        Group group = groupService.findGroupById(id);
        if(group==null){
            modelMap.addAttribute("message", "该管理组不存在");
            return theme.getAdminTemplate("common/message_tip");
        }
        modelMap.put("group", group);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("admin/group_update");
    }

    //处理用户组信息
    @PostMapping("/update_group_save")
    @ResponseBody
    public DataVo updateGroup(@RequestParam(value = "id", required = false) String id,@RequestParam(value = "name", required = false) String name){
        if (StringUtils.isBlank(name)) {
            return DataVo.failure("会员组名不能为空");
        }
        if (!NumberUtils.isNumber(id)) {
            return DataVo.failure("参数传递错误");
        }
        return groupService.updateGroup(name,Long.parseLong(id));
    }

    @GetMapping(value = "/group_list")
    public String roleList(@RequestParam(value = "p", defaultValue = "1") int pageNum,ModelMap modelMap){
        PageVo<Group> pageVo=groupService.getGroupListPage(pageNum,20);
        modelMap.put("pageVo", pageVo);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("admin/group_list");
    }


    @GetMapping(value = "/group_assignPermissions/{id}")
    public String assignPermissions(@PathVariable Long id, ModelMap modelMap){
        Group group = groupService.findGroupById(id);
        if(group==null){
            modelMap.addAttribute("message", "该管理组不存在");
            return theme.getAdminTemplate("common/message_tip");
        }
        List<Permission> permissionList = permissionService.getAllPermissions();
        LinkedHashMap<String, List<Permission>> permissionMap = permissionService.groupByController(permissionList);
        modelMap.put("group", group);
        modelMap.put("permissionMap", permissionMap);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("admin/group_assign_permissions");
    }

    @ResponseBody
    @PostMapping(value = "/group_markpermissions")
    public DataVo addGroup(@RequestParam(value = "groupId", defaultValue = "0") Long groupId,@RequestParam(value = "permissionId", defaultValue = "0") Long permissionId) {
        DataVo data = DataVo.failure("操作失败");
        if(groupId==1){
            return data = DataVo.failure("超级管理员组权限不能修改");
        }
        if(groupId==0 || permissionId==0){
            return data.failure("参数错误！");
        }
        if(permissionService.markAssignedPermissions(groupId,permissionId)){
            if(groupService.deleteGroupPermission(groupId,permissionId)){
                return data.success("删除成功！");
            }else{
                return data.failure("删除失败！");
            }
        }else{
            if(groupService.addGroupPermission(groupId,permissionId)){
                return DataVo.success("添加成功！");
            }else{
                return DataVo.failure("添加失败！");
            }
        }
    }
}
