package com.flycms.web.system;

import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.user.model.User;
import com.flycms.module.user.model.UserGroup;
import com.flycms.module.user.model.UserPermission;
import com.flycms.module.user.service.UserGroupService;
import com.flycms.module.user.service.UserPermissionService;
import com.flycms.module.user.service.UserService;
import org.apache.commons.lang.math.NumberUtils;
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
@RequestMapping("/system/user")
public class UserAdminController extends BaseController {
    @Autowired
    protected UserService userService;
    @Autowired
    protected UserGroupService userGroupService;
    @Autowired
    protected UserPermissionService userPermissionService;

    //同步所有权限
    @GetMapping("/permission_sync")
    @ResponseBody
    public DataVo getSyncAllPermission(){
        if(userPermissionService.getSyncAllPermission()){
            return DataVo.success("同步权限成功");
        }
        return DataVo.failure("同步权限失败");
    }

    @GetMapping(value = "/assignPermissions/{id}")
    public String assignPermissions(@PathVariable Long id, ModelMap modelMap){
        UserGroup group = userGroupService.findUserGroupByid(id);
        List<UserPermission> permissionList = userPermissionService.getAllPermissions();
        LinkedHashMap<String, List<UserPermission>> permissionMap = userPermissionService.groupByController(permissionList);
        modelMap.put("group", group);
        modelMap.put("permissionMap", permissionMap);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("user/assign_permissions");
    }

    @ResponseBody
    @PostMapping(value = "/markpermissions")
    public DataVo addRole(@RequestParam(value = "groupId", defaultValue = "0") Long groupId,@RequestParam(value = "permissionId", defaultValue = "0") Long permissionId) {
        DataVo data = DataVo.failure("操作失败");
        if(groupId==0 || permissionId==0){
            return data.failure("参数错误！");
        }
        if(userPermissionService.markAssignedPermissions(groupId,permissionId)){
            if(userGroupService.deleteUserGroupPermission(groupId,permissionId)){
                return data.success("删除成功！");
            }else{
                return data.failure("删除失败！");
            }
        }else{
            if(userGroupService.addUserGroupPermission(groupId ,permissionId)){
                return DataVo.success("添加成功！");
            }else{
                return DataVo.failure("添加失败！");
            }
        }
    }

    //删除权限节点
    @PostMapping("/permission_del")
    @ResponseBody
    public DataVo deletePermission(@RequestParam(value = "id") Long id){
        DataVo data = DataVo.failure("操作失败");
        if(userPermissionService.deletePermission(id)){
            data = DataVo.success("该权限已删除");
        }else{
            data = DataVo.failure("删除失败或者不存在！");
        }
        return data;
    }

    @GetMapping(value = "/permission_update/{id}")
    public String updateGrouupPermissions(@PathVariable Long id, ModelMap modelMap){
        UserPermission permission = userPermissionService.findPermissionById(id);
        modelMap.put("permission", permission);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("user/group_role_update");
    }

    //处理用户组信息
    @PostMapping("/permission_update_save")
    @ResponseBody
    public DataVo updateGrouupPermissionsSave(@Valid UserPermission permission, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            data = userPermissionService.updatePermissions(permission);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //用户组排序处理
    @PostMapping("/groupsort_update")
    @ResponseBody
    public DataVo updateGroupSort(@RequestParam(value = "id", required = false) String id,@RequestParam(value = "sort", required = false) String sort){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (!NumberUtils.isNumber(id)) {
                return data=DataVo.failure("话题参数错误");
            }
            if (!NumberUtils.isNumber(sort)) {
                return data=DataVo.failure("话题参数错误");
            }
            data = userGroupService.updateGroupSort(Long.parseLong(id),Integer.valueOf(sort));
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    @GetMapping(value = "/permissionlist")
    public String permissionList(@RequestParam(value = "p", defaultValue = "1") int pageNum, ModelMap modelMap){
        PageVo<UserPermission> pageVo=userPermissionService.getPermissionListPage(pageNum,20);
        modelMap.put("pageVo", pageVo);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("user/permission_list");
    }

    //用户列表
    @GetMapping(value = "/user_list")
    public String userList(@RequestParam(value = "username", required = false) String username,
                           @RequestParam(value = "trueName", required = false) String trueName,
                           @RequestParam(value = "mobile", required = false) String mobile,
                           @RequestParam(value = "email", required = false) String email,
                           @RequestParam(value = "p", defaultValue = "1") int pageNum,
                           ModelMap modelMap){
        PageVo<User> pageVo=userService.getUserListPage(username, trueName, mobile, email,null,null,pageNum,20);
        modelMap.put("pageVo", pageVo);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("user/user_list");
    }

    //添加用户
    @GetMapping(value = "/user_add")
    public String userAdd(ModelMap modelMap){
        List<UserGroup> group=userGroupService.getAllUserGroupList();
        modelMap.addAttribute("group",group);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("user/user_add");
    }

    //增加新用户信息
    @PostMapping("/user_save")
    @ResponseBody
    public DataVo userAdd(@Valid User user, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            data = userService.adminAddUser(user);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //删除权限组
    @PostMapping("/group_del")
    @ResponseBody
    public DataVo deleteRole(@RequestParam(value = "id") Long id){
        DataVo data = DataVo.failure("操作失败");
        if(id==1){
            return data = DataVo.failure("超级管理员组不能删除");
        }
        data = userService.deleteUserById(id);
        return data;
    }

    //编辑用户
    @GetMapping(value = "/user_edit/{id}")
    public String userEdit(@PathVariable Long id,ModelMap modelMap){
        User user=userService.findUserById(id,0);
        if(user==null){
            return theme.errorTips(modelMap,"用户不存在");
        }
        modelMap.addAttribute("user",user);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("user/user_edit");
    }

    //处理用户组信息
    @PostMapping("/user_update")
    @ResponseBody
    public DataVo updateUser(@Valid User user, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            data = userService.updateUser(user);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //用户组列表
    @GetMapping(value = "/group_list")
    public String groupList(@RequestParam(value = "p", defaultValue = "1") int pageNum, ModelMap modelMap){
        PageVo<UserGroup> pageVo=userGroupService.getUserGroupListPage(pageNum,20);
        modelMap.put("pageVo", pageVo);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("user/group_list");
    }

    //添加用户组
    @GetMapping(value = "/group_add")
    public String groupAdd(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("user/group_add");
    }

    //处理用户组信息
    @PostMapping("/group_update")
    @ResponseBody
    public DataVo updateUserGroupSave(@Valid UserGroup group, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            data = userGroupService.updateUserGroup(group);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //修改用户组
    @GetMapping(value = "/group_edit/{id}")
    public String groupEdit(@PathVariable Long id,ModelMap modelMap){
        UserGroup group=userGroupService.findUserGroupByid(id);
        if(group==null){
            return theme.errorTips(modelMap,"权限组不存在");
        }
        modelMap.addAttribute("group", group);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("user/group_edit");
    }

    //处理用户组信息
    @PostMapping("/group_save")
    @ResponseBody
    public DataVo addUserGroupSave(@Valid UserGroup group, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            data = userGroupService.addUserGroup(group);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }

    //提现申请列表
    @GetMapping(value = "/withdraw_list")
    public String withdrawList(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("user/withdraw_list");
    }

    //会员消息列表
    @GetMapping(value = "/message_list")
    public String messageList(ModelMap modelMap){
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("user/message_list");
    }
}
