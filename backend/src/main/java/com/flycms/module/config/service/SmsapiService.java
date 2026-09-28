package com.flycms.module.config.service;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.MathUtils;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.config.dao.SmsapiDao;
import com.flycms.module.config.model.Smsapi;
import com.flycms.module.user.dao.UserDao;
import com.flycms.module.user.model.UserActivation;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

/**
 * 短信服务：五厂商可切换（阿里云 / 腾讯云 / 华为云 / 百度云 / 火山引擎）。
 *
 * 配置存于 fly_config_web（键前缀 fly_sms_）：
 *   fly_sms_provider          当前启用的厂商：aliyun/tencent/huawei/baidu/volc
 *   fly_sms_{P}_ak            访问密钥ID：阿里AccessKeyID/腾讯SecretID/华为APP_Key/百度INVOKE_ID/火山AccessKeyID
 *   fly_sms_{P}_sk            访问密钥Secret（设置页掩码显示，提交掩码/空则不更新）
 *   fly_sms_{P}_sign          短信签名（华为国内短信绑定通道号，此字段可空）
 *   fly_sms_{P}_account       应用/通道标识：腾讯SmsSdkAppId/华为短信通道号/火山SmsAccount，阿里/百度可空
 *   fly_sms_{P}_tpl_reg       注册验证码模板ID
 *   fly_sms_{P}_tpl_safe      绑定/安全手机验证码模板ID
 *   fly_sms_{P}_tpl_reset     找回密码验证码模板ID
 *
 * 场景类型沿用历史约定（UserService 调用方）：1 注册 / 2 安全手机 / 3 找回密码。
 * 各厂商模板内容需含变量 code。
 *
 * 非阿里云四家使用 Hutool HTTP + 官方签名算法直连，不引入额外 SDK 依赖。
 *
 * @author sun-kaifei
 * @version 2.0
 */
@Service
public class SmsapiService {
    private static final Logger log = LoggerFactory.getLogger(SmsapiService.class);

    @Autowired
    private SmsapiDao smsapiDao;
    @Autowired
    private UserDao userDao;
    @Autowired
    private ConfigService configService;

    /** 支持的厂商（key = fly_sms_provider 值，label 给前端展示） */
    public static final List<Map<String, String>> PROVIDERS = Arrays.asList(
            provider("aliyun", "阿里云"),
            provider("tencent", "腾讯云"),
            provider("huawei", "华为云"),
            provider("baidu", "百度云"),
            provider("volc", "火山引擎"));

    /** 每个厂商统一的配置字段 */
    public static final List<String> PROVIDER_FIELDS = Arrays.asList(
            "ak", "sk", "sign", "account", "tpl_reg", "tpl_safe", "tpl_reset");

    /** sk 掩码：GET 时替换真实值，保存时遇掩码/空则跳过 */
    public static final String SK_MASK = "******";

    private static Map<String, String> provider(String key, String label) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("label", label);
        return m;
    }

    // ///////////////////////////////
    // /////       发送       ////////
    // ///////////////////////////////

    /**
     * 发送手机验证码并保存激活记录（多厂商分发）。
     *
     * @param phoneNumber 手机号
     * @param code        验证码
     * @param type        1注册 2安全手机 3找回密码
     * @return null=成功；否则返回错误信息（含厂商返回码）
     */
    public String sendVerifyCode(String phoneNumber, String code, int type) {
        String provider = currentProvider();
        String tpl = templateOf(provider, type);
        if (tpl == null || tpl.trim().isEmpty()) {
            return "未配置「" + provider + "」的场景模板ID（type=" + type + "）";
        }
        String tplId = tpl.trim();
        String error;
        switch (provider) {
            case "tencent":
                error = sendByTencent(phoneNumber, code, tplId);
                break;
            case "huawei":
                error = sendByHuawei(phoneNumber, code, tplId);
                break;
            case "baidu":
                error = sendByBaidu(phoneNumber, code, tplId);
                break;
            case "volc":
                error = sendByVolc(phoneNumber, code, tplId);
                break;
            case "aliyun":
            default:
                error = sendByAliyun(phoneNumber, code, tplId);
                break;
        }
        if (error == null) {
            saveActivation(phoneNumber, code, type);
        }
        return error;
    }

    /** 历史兼容入口：UserService 注册/绑定/找回密码验证码 */
    @Transactional
    public boolean mobileRegisterCode(String phoneNumber, String code, int type) {
        return sendVerifyCode(phoneNumber, code, type) == null;
    }

    // ///////////////////////////////
    // /////     厂商实现      ////////
    // ///////////////////////////////

    /** 阿里云（官方 SDK，dysmsapi 2017-05-25） */
    private String sendByAliyun(String phone, String code, String tpl) {
        try {
            String ak = configOf("aliyun", "ak");
            String sk = configOf("aliyun", "sk");
            String sign = configOf("aliyun", "sign");
            if (ak.isEmpty() || sk.isEmpty()) {
                return "阿里云未配置 AccessKey";
            }
            System.setProperty("sun.net.client.defaultConnectTimeout", "10000");
            System.setProperty("sun.net.client.defaultReadTimeout", "10000");
            IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", ak, sk);
            DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", "Dysmsapi", "dysmsapi.aliyuncs.com");
            IAcsClient client = new DefaultAcsClient(profile);
            SendSmsRequest request = new SendSmsRequest();
            request.setPhoneNumbers(phone);
            request.setSignName(sign);
            request.setTemplateCode(tpl);
            request.setTemplateParam("{\"code\":\"" + code + "\"}");
            SendSmsResponse resp = client.getAcsResponse(request);
            return "OK".equals(resp.getCode()) ? null
                    : "阿里云返回 " + resp.getCode() + (resp.getMessage() == null ? "" : " " + resp.getMessage());
        } catch (Exception e) {
            log.error("阿里云短信发送失败", e);
            return "阿里云请求异常：" + e.getMessage();
        }
    }

    /** 腾讯云（TC3-HMAC-SHA256 签名，sms 2021-01-11） */
    private String sendByTencent(String phone, String code, String tpl) {
        try {
            String ak = configOf("tencent", "ak");
            String sk = configOf("tencent", "sk");
            String sign = configOf("tencent", "sign");
            String account = configOf("tencent", "account");
            if (ak.isEmpty() || sk.isEmpty() || account.isEmpty()) {
                return "腾讯云未配置 SecretId/SecretKey/SmsSdkAppId";
            }
            String host = "sms.tencentcloudapi.com";
            String body = "{\"PhoneNumberSet\":[\"+86" + phone + "\"],\"SmsSdkAppId\":\"" + account
                    + "\",\"SignName\":\"" + sign + "\",\"TemplateId\":\"" + tpl
                    + "\",\"TemplateParamSet\":[\"" + code + "\"]}";
            Map<String, String> headers = tc3Headers(host, "SendSms", "2021-01-11", body, ak, sk);
            HttpRequest request = HttpRequest.post("https://" + host + "/")
                    .header("Content-Type", "application/json; charset=utf-8")
                    .body(body)
                    .timeout(10000);
            headers.forEach(request::header);
            try (HttpResponse resp = request.execute()) {
                JSONObject root = JSON.parseObject(resp.body());
                JSONObject response = root.getJSONObject("Response");
                if (response == null || response.getJSONArray("Result") == null
                        || response.getJSONArray("Result").isEmpty()) {
                    JSONObject err = response == null ? null : response.getJSONObject("Error");
                    return "腾讯云返回 " + (err == null ? resp.body() :
                            err.getString("Code") + " " + err.getString("Message"));
                }
                JSONObject result = response.getJSONArray("Result").getJSONObject(0);
                return "Ok".equals(result.getString("Code")) ? null
                        : "腾讯云返回 " + result.getString("Code") + " " + result.getString("Message");
            }
        } catch (Exception e) {
            log.error("腾讯云短信发送失败", e);
            return "腾讯云请求异常：" + e.getMessage();
        }
    }

    /** 华为云（WSSE 签名，/sms/batchSendSms/v1/） */
    private String sendByHuawei(String phone, String code, String tpl) {
        try {
            String ak = configOf("huawei", "ak");
            String sk = configOf("huawei", "sk");
            String sender = configOf("huawei", "account");
            if (ak.isEmpty() || sk.isEmpty() || sender.isEmpty()) {
                return "华为云未配置 APP_Key/APP_Secret/短信通道号";
            }
            // WSSE PasswordDigest = Base64(SHA256(nonce + created + secret))
            String created = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date());
            String nonce = UUID.randomUUID().toString().replace("-", "");
            String digest = cn.hutool.core.codec.Base64.encode(
                    SecureUtil.sha256(nonce + created + sk));
            String wsse = "WSSE UsernameToken Username=\"" + ak
                    + "\",PasswordDigest=\"" + digest
                    + "\",Nonce=\"" + nonce + "\",Created=\"" + created + "\"";
            String paras = "[{\"code\":\"" + code + "\"}]";
            String form = "from=" + java.net.URLEncoder.encode(sender, "UTF-8")
                    + "&to=" + java.net.URLEncoder.encode("+" + phone, "UTF-8")
                    + "&templateId=" + java.net.URLEncoder.encode(tpl, "UTF-8")
                    + "&templateParas=" + java.net.URLEncoder.encode(paras, "UTF-8");
            try (HttpResponse resp = HttpRequest.post(
                            "https://smsapi.cn-north-4.myhuaweicloud.com:443/sms/batchSendSms/v1/")
                    .header("Authorization", wsse)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body(form)
                    .timeout(10000)
                    .execute()) {
                JSONObject root = JSON.parseObject(resp.body());
                com.alibaba.fastjson2.JSONArray results = root.getJSONArray("result");
                if (results == null || results.isEmpty()) {
                    return "华为云返回 HTTP " + resp.getStatus() + " " + resp.body();
                }
                JSONObject result = results.getJSONObject(0);
                return "0".equals(result.getString("status"))
                        ? null
                        : "华为云返回状态码 " + result.getString("status")
                                + " " + result.getString("desc");
            }
        } catch (Exception e) {
            log.error("华为云短信发送失败", e);
            return "华为云请求异常：" + e.getMessage();
        }
    }

    /** 百度云（BCE bce-auth-v1 签名，SMS v3） */
    private String sendByBaidu(String phone, String code, String tpl) {
        try {
            String invokeId = configOf("baidu", "ak");
            String sk = configOf("baidu", "sk");
            String signId = configOf("baidu", "sign");
            if (invokeId.isEmpty() || sk.isEmpty()) {
                return "百度云未配置 INVOKE_ID/Secret Key";
            }
            String host = "smsv3.bj.baidubce.com";
            String path = "/v1/message";
            String body = "{\"invokeId\":\"" + invokeId + "\",\"phoneNumber\":\"" + phone
                    + "\",\"template\":\"" + tpl + "\",\"contentVar\":\"{\\\"code\\\":\\\"" + code
                    + "\\\"}\""
                    + (signId.isEmpty() ? "" : ",\"signatureId\":\"" + signId + "\"}") + "}";
            // BCE 签名：签派生密钥 = HMAC-SHA256(sk, bceDate)；签名 = HMAC(派生密钥, stringToSign)
            SimpleDateFormat bceFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            bceFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            String bceDate = bceFormat.format(new Date());
            String signKey = SecureUtil.hmacSha256(sk).digestHex(bceDate);
            // CanonicalRequest = METHOD\nURI\nQUERY(空)\nHEADERS(空)\n
            String canonicalRequest = "POST\n" + path + "\n\n\n";
            String stringToSign = "bce-auth-v1/" + invokeId + "/" + bceDate + "/1800//" + canonicalRequest;
            String signature = SecureUtil.hmacSha256(signKey).digestHex(stringToSign);
            String auth = "bce-auth-v1/" + invokeId + "/" + bceDate + "/1800//" + signature;
            try (HttpResponse resp = HttpRequest.post("https://" + host + path)
                    .header("Authorization", auth)
                    .header("x-bce-date", bceDate)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .timeout(10000)
                    .execute()) {
                JSONObject root = JSON.parseObject(resp.body());
                return "100000".equals(root.getString("code")) ? null
                        : "百度云返回 " + root.getString("code") + " " + root.getString("message");
            }
        } catch (Exception e) {
            log.error("百度云短信发送失败", e);
            return "百度云请求异常：" + e.getMessage();
        }
    }

    /** 火山引擎（HMAC-SHA256 签名，Action=SendSms&Version=2020-01-01） */
    private String sendByVolc(String phone, String code, String tpl) {
        try {
            String ak = configOf("volc", "ak");
            String sk = configOf("volc", "sk");
            String sign = configOf("volc", "sign");
            String account = configOf("volc", "account");
            if (ak.isEmpty() || sk.isEmpty()) {
                return "火山引擎未配置 AccessKey";
            }
            String host = "sms.volcengineapi.com";
            String query = "Action=SendSms&Version=2020-01-01";
            String body = "{\"SmsAccount\":\"" + account + "\",\"Sign\":\"" + sign
                    + "\",\"TemplateID\":\"" + tpl + "\",\"TemplateParam\":\"{\\\"code\\\":\\\""
                    + code + "\\\"}\",\"PhoneNumbers\":\"" + phone + "\"}";
            // 火山引擎签名：与 AWS SigV4 同构
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String xDate = sdf.format(new Date());
            String date = xDate.substring(0, 8);
            String bodyHash = sha256Hex(body);
            String canonicalRequest = "POST\n/\n" + query + "\n"
                    + "content-type:application/json\nhost:" + host + "\nx-content-sha256:" + bodyHash
                    + "\nx-date:" + xDate + "\n\ncontent-type;host;x-content-sha256;x-date\n" + bodyHash;
            String credentialScope = date + "/sms/request";
            String stringToSign = "HMAC-SHA256\n" + xDate + "\n" + credentialScope + "\n"
                    + sha256Hex(canonicalRequest);
            byte[] kDate = hmacSha256(sk.getBytes(StandardCharsets.UTF_8), date);
            byte[] kRegion = hmacSha256(kDate, "sms");
            byte[] kService = hmacSha256(kRegion, "request");
            byte[] kSigning = hmacSha256(kService, "hmac_sha256");
            String signature = hex(hmacSha256(kSigning, stringToSign));
            String auth = "HMAC-SHA256 Credential=" + ak + "/" + credentialScope
                    + ", SignedHeaders=content-type;host;x-content-sha256;x-date, Signature=" + signature;
            try (HttpResponse resp = HttpRequest.post("https://" + host + "/?" + query)
                    .header("Content-Type", "application/json")
                    .header("Host", host)
                    .header("X-Content-Sha256", bodyHash)
                    .header("X-Date", xDate)
                    .header("Authorization", auth)
                    .body(body)
                    .timeout(10000)
                    .execute()) {
                JSONObject root = JSON.parseObject(resp.body());
                JSONObject meta = root.getJSONObject("ResponseMetadata");
                JSONObject err = meta == null ? null : meta.getJSONObject("Error");
                return err == null ? null
                        : "火山引擎返回 " + err.getString("Code") + " " + err.getString("Message");
            }
        } catch (Exception e) {
            log.error("火山引擎短信发送失败", e);
            return "火山引擎请求异常：" + e.getMessage();
        }
    }

    // ///////////////////////////////
    // /////   签名工具方法    ////////
    // ///////////////////////////////

    /** 腾讯云 TC3-HMAC-SHA256 请求头 */
    private Map<String, String> tc3Headers(String host, String action, String version, String body, String ak, String sk) throws Exception {
        long ts = System.currentTimeMillis() / 1000;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = sdf.format(new Date(ts * 1000));
        String bodyHash = sha256Hex(body);
        String canonicalRequest = "POST\n/\n\n"
                + "content-type:application/json; charset=utf-8\nhost:" + host
                + "\nx-tc-action:" + action.toLowerCase() + "\n\n"
                + "content-type;host;x-tc-action\n" + bodyHash;
        String stringToSign = "TC3-HMAC-SHA256\n" + ts + "\n" + date + "/sms/tc3_request\n"
                + sha256Hex(canonicalRequest);
        byte[] kDate = hmacSha256(("TC3" + sk).getBytes(StandardCharsets.UTF_8), date);
        byte[] kService = hmacSha256(kDate, "sms");
        byte[] kSigning = hmacSha256(kService, "tc3_request");
        String signature = hex(hmacSha256(kSigning, stringToSign));
        Map<String, String> headers = new HashMap<>();
        headers.put("X-TC-Action", action);
        headers.put("X-TC-Version", version);
        headers.put("X-TC-Timestamp", String.valueOf(ts));
        headers.put("Host", host);
        headers.put("Authorization", "TC3-HMAC-SHA256 Credential=" + ak + "/" + date + "/sms/tc3_request"
                + ", SignedHeaders=content-type;host;x-tc-action, Signature=" + signature);
        return headers;
    }

    private static byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(String data) {
        return SecureUtil.sha256().digestHex(data);
    }

    private static String hex(byte[] bytes) {
        try (Formatter f = new Formatter()) {
            for (byte b : bytes) {
                f.format("%02x", b);
            }
            return f.toString();
        }
    }

    // ///////////////////////////////
    // /////   配置读写(设置页) //////
    // ///////////////////////////////

    /** 当前启用的厂商 key */
    public String currentProvider() {
        String p = configService.getStringByKey("fly_sms_provider");
        return (p == null || p.trim().isEmpty()) ? "aliyun" : p.trim();
    }

    /** 场景 → 模板配置键：1 注册 / 2 安全手机 / 3 找回密码 */
    private String templateOf(String provider, int type) {
        String scene;
        if (type == 2) {
            scene = "tpl_safe";
        } else if (type == 3) {
            scene = "tpl_reset";
        } else {
            scene = "tpl_reg";
        }
        return configService.getStringByKey("fly_sms_" + provider + "_" + scene);
    }

    /** 读取厂商字段值（空安全） */
    private String configOf(String provider, String field) {
        String v = configService.getStringByKey("fly_sms_" + provider + "_" + field);
        return v == null ? "" : v.trim();
    }

    /** 短信设置页：分组返回全部配置（sk 掩码） */
    public Map<String, Object> getSmsConfig() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("provider", currentProvider());
        data.put("providers", PROVIDERS);
        Map<String, Map<String, String>> config = new LinkedHashMap<>();
        for (Map<String, String> p : PROVIDERS) {
            String key = p.get("key");
            Map<String, String> fields = new LinkedHashMap<>();
            for (String field : PROVIDER_FIELDS) {
                String v = configOf(key, field);
                if ("sk".equals(field) && !v.isEmpty()) {
                    v = SK_MASK;
                }
                fields.put(field, v);
            }
            config.put(key, fields);
        }
        data.put("config", config);
        return data;
    }

    /** 短信设置页：保存（sk 提交掩码/空串则跳过，其余 upsert） */
    public DataVo saveSmsConfig(Map<String, String> params) {
        String provider = params.get("fly_sms_provider");
        if (provider != null && !provider.trim().isEmpty()
                && PROVIDERS.stream().noneMatch(p -> p.get("key").equals(provider.trim()))) {
            return DataVo.failure("不支持的短信厂商：" + provider);
        }
        int changed = 0;
        for (Map<String, String> p : PROVIDERS) {
            String key = p.get("key");
            for (String field : PROVIDER_FIELDS) {
                String name = "fly_sms_" + key + "_" + field;
                String v = params.get(name);
                if (v == null) {
                    continue;
                }
                if ("sk".equals(field) && (v.trim().isEmpty() || SK_MASK.equals(v.trim()))) {
                    continue;
                }
                configService.updagteConfigByKey(name, v.trim());
                changed++;
            }
        }
        if (provider != null && !provider.trim().isEmpty()) {
            configService.updagteConfigByKey("fly_sms_provider", provider.trim());
            changed++;
        }
        return DataVo.success("已保存 " + changed + " 项短信设置");
    }

    /** 短信设置页的「发送测试短信」：用当前厂商的注册模板发验证码 */
    public String testSend(String phone) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            return "请填写正确的 11 位手机号";
        }
        String code = MathUtils.getRandomCode(6);
        return sendVerifyCode(phone.trim(), code, 1);
    }

    /** 发送成功后落激活表（与历史逻辑一致） */
    private void saveActivation(String phoneNumber, String code, int type) {
        try {
            UserActivation activation = new UserActivation();
            activation.setId(SnowFlake.getInstance().nextId());
            activation.setUserName(phoneNumber);
            activation.setCode(code);
            activation.setCodeType(type);
            activation.setReferStatus(0);
            activation.setReferTime(new Date());
            userDao.addUserActivation(activation);
        } catch (Exception e) {
            log.error("验证码激活记录保存失败", e);
        }
    }

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    // 更新短信api接口信息（老单行表，保留兼容）
    @Transactional
    public DataVo updateSmsapi(Smsapi smsapi){
        DataVo data = DataVo.failure("更新失败");
        int totalCount = smsapiDao.updagteSmsapiById(smsapi);
        if(totalCount > 0){
            data = DataVo.jump("已成功修改","/admin/site/smsapi_edit");
        }
        return data;
    }

    // ///////////////////////////////
    // /////       查询       ////////
    // ///////////////////////////////
    // 查询短信接口信息（老单行表，保留兼容）
    public Smsapi findSmsapiByid(Integer id) {
        return smsapiDao.findSmsapiByid(id);
    }
}
