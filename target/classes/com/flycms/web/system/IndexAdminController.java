package com.flycms.web.system;

import com.flycms.core.utils.AdminSessionUtils;
import com.flycms.constant.Const;
import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.module.admin.model.Admin;
import com.flycms.module.admin.service.AdminService;
import com.flycms.module.question.service.AnswerService;
import com.flycms.module.question.service.QuestionService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.Properties;

/**
 * 开发公司：28844.com<br/>
 * 版权：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: $time$ $date$
 */
@Controller
@RequestMapping("/system")
public class IndexAdminController extends BaseController {
    @Autowired
    protected AdminService adinsrv;
    @Autowired
    private QuestionService questionService;

    @Autowired
    private AnswerService answerService;
    /**
     * 首页
     *
     * @return
     */
    @GetMapping(value = "/index")
    public String index(ModelMap modelMap){
        Properties props = System.getProperties();
        int questionCount=questionService.getQuestionCount(null,null,null,0);
        modelMap.addAttribute("questionCount", questionCount);
        modelMap.addAttribute("os", props.getProperty("os.name"));
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("index");
    }

    @GetMapping(value = "/login")
    public String adminLogin(@RequestParam(value = "redirectUrl",required = false) String redirectUrl,ModelMap modelMap){
        if(getAdminUser() != null){
            return "redirect:/system/index";
        }
        modelMap.addAttribute("redirectUrl",redirectUrl);
        return theme.getAdminTemplate("user/login");
    }

    @ResponseBody
    @PostMapping(value = "/login_act")
    public DataVo userLogin(
            @RequestParam(value = "admin_name", required = false) String admin_name,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "redirectUrl",required = false) String redirectUrl,
            @RequestParam(value = "captcha", required = false) String captcha) {
        try {
            String kaptcha = (String) session.getAttribute("kaptcha");
            if (StringUtils.isBlank(admin_name)) {
                return DataVo.failure("用户名不能为空");
            }
            if (StringUtils.isBlank(password)) {
                return DataVo.failure("密码不能为空");
            } else if (password.length() < 6 && password.length() > 30) {
                return DataVo.failure("密码最少6个字符，最多30个字符");
            }
            // 校验验证码
            if (captcha != null) {
                if (!captcha.equalsIgnoreCase(kaptcha)) {
                    return DataVo.failure("验证码错误");
                }
            }else{
                return DataVo.failure("验证码不能为空");
            }
            Admin entity = adminService.adminLogin(admin_name,password,request);
            if(entity==null){
                return DataVo.failure("帐号或密码错误。");
            }else{
                session.removeAttribute(Const.KAPTCHA_SESSION_KEY);
                if (StringUtils.isNotEmpty(redirectUrl)){
                    return DataVo.jump("操作成功", redirectUrl);
                }
                return DataVo.jump("操作成功", "/system/index");
            }
        } catch (Exception e) {
            return DataVo.failure("帐号或密码错误。");
        }
    }

    /**
     * 管理员退出登录
     *
     */
    @GetMapping(value = "/logout")
    public String logout() {
        AdminSessionUtils.setLoginMember(request,null);
        return "redirect:/system/login";
    }
}
