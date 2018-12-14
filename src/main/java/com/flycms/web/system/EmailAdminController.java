package com.flycms.web.system;

import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.config.service.ConfigService;
import com.flycms.module.other.model.Email;
import com.flycms.module.other.service.EmailService;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 开发公司：28844.com<br/>
 * 版权：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 2018-06-30
 */
@Controller
@RequestMapping("/system/email")
public class EmailAdminController extends BaseController {
    protected final static Logger logger = Logger.getLogger(EmailAdminController.class);
    @Autowired
    protected ConfigService configService;

    @Autowired
    protected EmailService emailService;

    @GetMapping(value = "/list_email")
    public String shareList(@RequestParam(value = "p", defaultValue = "1") int p, ModelMap modelMap){
        PageVo<Email> pageVo=emailService.getEmailTempletPage(p,20);
        modelMap.addAttribute("pageVo", pageVo);
        modelMap.addAttribute("p", p);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("webconfig/list_email");
    }

    //邮箱信息设置
    @ResponseBody
    @PostMapping(value = "/email_conf_updagte")
    public DataVo updagteEmailConf(
            @RequestParam(value = "fly_smtp_server") String fly_smtp_server,
            @RequestParam(value = "fly_smtp_port") String fly_smtp_port,
            @RequestParam(value = "fly_smtp_usermail") String fly_smtp_usermail,
            @RequestParam(value = "fly_smtp_password") String fly_smtp_password) {
        DataVo data = DataVo.failure("操作失败");
       try {
            if (StringUtils.isBlank(fly_smtp_server)) {
                return DataVo.failure("邮件服务器地址不能为空！");
            }
            if (StringUtils.isBlank(fly_smtp_port)) {
                return DataVo.failure("邮件服务器端口不能为空！");
            }
            if (StringUtils.isBlank(fly_smtp_usermail)) {
                return DataVo.failure("邮箱登录名不能为空");
            }
            if (StringUtils.isBlank(fly_smtp_password)) {
                return DataVo.failure("邮箱登录密码不能为空");
            }

            configService.updagteConfigByKey("fly_smtp_server", fly_smtp_server);
            configService.updagteConfigByKey("fly_smtp_port", fly_smtp_port);
            configService.updagteConfigByKey("fly_smtp_usermail", fly_smtp_usermail);
            configService.updagteConfigByKey("fly_smtp_password", fly_smtp_password);
            return DataVo.success("操作成功", DataVo.NOOP);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            DataVo.failure("未知错误！");
        }
        return data;
    }

    //邮箱信息设置
    @ResponseBody
    @PostMapping(value = "/testemail")
    public DataVo testEmail(@RequestParam(value = "test_address") String test_address) {
        DataVo data = DataVo.failure("操作失败");
        try {
            if (StringUtils.isBlank(test_address)) {
                return DataVo.failure("邮件服务器地址不能为空！");
            }
            String mailTitle="邮件测试标题";
            StringBuffer mailBody = new StringBuffer();
            mailBody.append("邮件测试内容");
            emailService.sendEmail(test_address, null, "tese_email");
            return DataVo.success("操作成功", DataVo.NOOP);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            DataVo.failure("未知错误！");
        }
        return data;
    }

    //编辑邮件模板
    @GetMapping(value = "/edit_templets_email/{id}")
    public String updateEmailTemplets(@PathVariable(value = "id", required = false) int id,ModelMap modelMap){
        Email email=emailService.findEmailTempletById(id);
        if(email==null){
            return theme.getAdminTemplate("404");
        }
        modelMap.put("email", email);
        modelMap.addAttribute("admin", getAdminUser());
        return theme.getAdminTemplate("webconfig/edit_email");
    }

    //处理编辑邮件模板
    @PostMapping("/email_templets_update")
    @ResponseBody
    public DataVo updateEmailTemplets(@Valid Email email, BindingResult result){
        DataVo data = DataVo.failure("操作失败");
        try {
            if (result.hasErrors()) {
                List<ObjectError> list = result.getAllErrors();
                for (ObjectError error : list) {
                    return DataVo.failure(error.getDefaultMessage());
                }
                return null;
            }
            data = emailService.updateEmailTempletsById(email);
        } catch (Exception e) {
            data = DataVo.failure(e.getMessage());
        }
        return data;
    }
}
