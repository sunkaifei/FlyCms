package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.module.config.service.SmsapiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 短信设置 REST（阶段 N）：五厂商（阿里云/腾讯云/华为云/百度云/火山引擎）可切换，
 * 每厂商独立 ak/sk/签名/应用标识 + 三场景验证码模板（注册/绑定手机/找回密码）。
 *
 * sk（访问密钥 Secret）安全约定与 SMTP 授权码一致：GET 掩码回显、提交掩码或空串不更新。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/api")
public class ApiSmsController extends ApiBaseController {

    @Autowired
    private SmsapiService smsapiService;

    /** 短信设置：当前厂商 + 厂商清单 + 五厂商分组配置（sk 掩码） */
    @ResponseBody
    @GetMapping("/system/sms/config")
    public DataVo config() {
        requirePermission("/api/system/sms/config");
        return DataVo.success("操作成功", smsapiService.getSmsConfig());
    }

    /** 保存短信设置：fly_sms_provider + fly_sms_{厂商}_{字段} 白名单键，upsert */
    @ResponseBody
    @PostMapping("/system/sms/save")
    public DataVo save(@RequestParam Map<String, String> params) {
        requirePermission("/api/system/sms/save");
        return smsapiService.saveSmsConfig(params);
    }

    /** 发送测试短信：用当前厂商的注册验证码模板向指定手机号发一条验证码 */
    @ResponseBody
    @PostMapping("/system/sms/test")
    public DataVo test(@RequestParam("phone") String phone) {
        requirePermission("/api/system/sms/test");
        String error = smsapiService.testSend(phone);
        if (error == null) {
            return DataVo.success("测试短信已提交，请查收手机短信");
        }
        return DataVo.failure("发送失败：" + error);
    }
}
