package com.flycms.web.api;

import com.flycms.constant.Const;
import com.flycms.core.base.BaseController;
import com.flycms.core.entity.DataVo;
import com.flycms.module.form.service.FormService;
import com.flycms.module.user.utils.UserSessionUtils;
import com.flycms.module.user.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 表单前台公开提交端点（规划阶段 F）
 *
 * 这是少数不需要管理员登录的 /api/** 接口，因此**不继承 ApiBaseController**
 * （基类会强制 requireAdmin）。防护全部下沉到 FormService.submit：
 * 表单启用校验、验证码、IP/用户每日频控、必填校验、jsoup XSS 清洗、敏感词过滤。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/api/form")
public class ApiFormSubmitController extends BaseController {

    @Autowired
    private FormService formService;

    /**
     * 提交表单
     *
     * @param formCode 表单编码
     * @param params   除 code/captcha 外的其余参数均视为字段值（key=fieldCode）
     */
    @ResponseBody
    @PostMapping("/submit/{formCode}")
    public DataVo submit(@PathVariable("formCode") String formCode,
                         @RequestParam Map<String, String> params) {
        Map<String, String> values = new HashMap<String, String>();
        if (params != null) {
            for (Map.Entry<String, String> e : params.entrySet()) {
                if ("captcha".equals(e.getKey()) || "code".equals(e.getKey())) {
                    continue;
                }
                values.put(e.getKey(), e.getValue());
            }
        }
        String captcha = params == null ? null : params.get("captcha");
        Object sessionCaptcha = session.getAttribute(Const.KAPTCHA_SESSION_KEY);
        // 验证码一次性：校验后无论成败都清除，防止复用
        String expected = sessionCaptcha == null ? null : String.valueOf(sessionCaptcha);

        Long userId = null;
        User user = UserSessionUtils.getLoginMember(request);
        if (user != null) {
            userId = user.getUserId();
        }
        DataVo result = formService.submit(request, formCode, values, captcha, expected, userId);
        // 验证码使用后立即失效
        if (sessionCaptcha != null) {
            session.removeAttribute(Const.KAPTCHA_SESSION_KEY);
        }
        return result;
    }
}
