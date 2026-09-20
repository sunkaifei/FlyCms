package com.flycms.web.api;

import com.flycms.constant.Const;
import com.flycms.core.entity.DataVo;
import com.flycms.core.utils.AdminSessionUtils;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * vben 前端认证接口。
 *
 * 认证方式为 Session-Cookie（JSESSIONID 经 vite/nginx 同域代理透传），
 * accessToken 仅返回占位符以满足前端登录流的非空校验，后端不做校验。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/api")
public class ApiAuthController extends ApiBaseController {

    @Autowired
    protected AdminService adminService;

    @Autowired
    protected GroupService groupService;

    @Autowired
    protected PermissionService permissionService;

    /**
     * 登录：BCrypt 校验 + 图形验证码，成功后管理员信息写入 session（沿用 adminLogin 内部逻辑）
     */
    @ResponseBody
    @PostMapping("/auth/login")
    public DataVo login(@RequestParam(value = "admin_name", required = false) String adminName,
                        @RequestParam(value = "password", required = false) String password,
                        @RequestParam(value = "captcha", required = false) String captcha) {
        String kaptcha = (String) session.getAttribute(Const.KAPTCHA_SESSION_KEY);
        if (StringUtils.isBlank(adminName)) {
            return DataVo.failure("用户名不能为空");
        }
        if (StringUtils.isBlank(password)) {
            return DataVo.failure("密码不能为空");
        }
        if (StringUtils.isBlank(captcha)) {
            return DataVo.failure("验证码不能为空");
        }
        if (kaptcha == null || !captcha.equalsIgnoreCase(kaptcha)) {
            return DataVo.failure("验证码错误");
        }
        Admin admin = adminService.adminLogin(adminName, password, request);
        if (admin == null) {
            return DataVo.failure("帐号或密码错误。");
        }
        session.removeAttribute(Const.KAPTCHA_SESSION_KEY);
        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", "session");
        return DataVo.success("操作成功", data);
    }

    /**
     * 退出登录：清空管理员会话
     */
    @ResponseBody
    @PostMapping("/auth/logout")
    public DataVo logout() {
        AdminSessionUtils.setLoginMember(request, null);
        try {
            session.invalidate();
        } catch (IllegalStateException ignored) {
            // 会话已失效，无需处理
        }
        return DataVo.success("操作成功");
    }

    /**
     * 当前登录管理员信息（vben UserInfo 结构，roles 为角色组名）
     */
    @ResponseBody
    @GetMapping("/user/info")
    public DataVo userInfo() {
        Admin admin = requireAdmin();
        // session 中是登录时的完整行，密码散列绝不能下发
        admin.setPassword(null);
        Map<String, Object> info = new HashMap<>();
        info.put("userId", String.valueOf(admin.getId()));
        info.put("username", admin.getAdminName());
        info.put("realName", admin.getNickName() == null ? admin.getAdminName() : admin.getNickName());
        info.put("avatar", admin.getAvatar() == null ? "" : admin.getAvatar());
        info.put("desc", "");
        info.put("token", "session");
        info.put("homePath", "/dashboard");

        Integer roleId = groupService.findUserAndGroupById(admin.getId());
        Group group = roleId == null ? null : groupService.findGroupById(roleId.longValue());
        info.put("roles", group == null
                ? Collections.emptyList()
                : Collections.singletonList(group.getName()));
        return DataVo.success("操作成功", info);
    }

    /**
     * 当前管理员的权限码列表（action_key，与老后台权限节点同源，前端按钮显隐使用）
     */
    @ResponseBody
    @GetMapping("/auth/codes")
    public DataVo codes() {
        Admin admin = requireAdmin();
        List<Permission> permissions = permissionService.findPermissionByUserId(admin.getId());
        List<String> codes = new ArrayList<>();
        if (permissions != null) {
            for (Permission permission : permissions) {
                codes.add(permission.getActionKey());
            }
        }
        return DataVo.success("操作成功", codes);
    }
}
