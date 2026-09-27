package com.flycms.web.api;

import com.flycms.core.entity.DataVo;
import com.flycms.module.config.service.ConfigService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 网站管理 + 前台模板管理（参考帝国CMS/DedeCMS 范式）。
 *
 * 模板在线编辑的安全边界（必守）：
 * 1. 相对路径正则白名单（字母/数字/下划线/中划线/中文/斜杠，必须 .html 结尾，禁止 ..）；
 * 2. 解析后的绝对路径必须位于 views/templates/pc_theme/{当前皮肤}/ 规范路径内（防符号链/穿越）；
 * 3. 单文件大小上限 512KB；编码固定 UTF-8。
 * 网站配置只允许白名单键读写。SMTP 密码（授权码）特殊处理：读取时以 ****** 掩码返回，
 * 保存时提交掩码或空串则跳过不更新，避免明文回传浏览器。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/api")
public class ApiWebsiteController extends ApiBaseController {

    /** 网站管理页可编辑的配置键白名单 */
    private static final List<String> CONFIG_KEYS = Arrays.asList(
            "fly_title", "fly_url", "fly_logo", "fly_status",
            "fly_seo_title", "fly_seo_keywords", "fly_seo_description",
            "fly_robots", "fly_sitemap_status", "fly_sitemap_limit",
            "master", "qq", "email", "mobile", "phone", "address",
            "pc_theme", "m_theme",
            // 第三方邮箱（SMTP）：注册邮箱验证、找回密码、表单通知等共用
            "fly_smtp_server", "fly_smtp_port", "fly_smtp_ssl",
            "fly_smtp_usermail", "fly_smtp_password", "fly_smtp_fromname");

    /** SMTP 密码（授权码）掩码：GET 时用它替换真实值，保存时遇它跳过 */
    private static final String SMTP_PASSWORD_MASK = "******";

    private static final Pattern SAFE_TEMPLATE_PATH =
            Pattern.compile("^[\\w\\-/\\u4e00-\\u9fa5]+\\.html$");

    private static final long MAX_TEMPLATE_BYTES = 512 * 1024;

    @Autowired
    private ConfigService configService;

    @Autowired
    private com.flycms.module.other.service.EmailService emailService;

    @Autowired
    private com.flycms.module.template.service.TemplateCenterService templateCenterService;

    // /////////////////// 网站管理 ///////////////////

    /** 站点设置 + 可选皮肤列表 */
    @ResponseBody
    @GetMapping("/system/website/config")
    public DataVo config() {
        requirePermission("/api/system/website/config");
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, String> config = new LinkedHashMap<>();
        for (String key : CONFIG_KEYS) {
            config.put(key, configService.getStringByKey(key));
        }
        // SMTP 授权码不回传明文
        String smtpPassword = config.get("fly_smtp_password");
        if (smtpPassword != null && !smtpPassword.isEmpty()) {
            config.put("fly_smtp_password", SMTP_PASSWORD_MASK);
        }
        data.put("config", config);
        data.put("skins", listSkins());
        return DataVo.success("操作成功", data);
    }

    /** 保存站点设置：仅白名单键；主题切换即时生效（模板路径按配置解析） */
    @ResponseBody
    @PostMapping("/system/website/save")
    public DataVo saveConfig(@RequestParam Map<String, String> params) {
        requirePermission("/api/system/website/save");
        int changed = 0;
        for (String key : CONFIG_KEYS) {
            String v = params.get(key);
            if (v == null) {
                continue;
            }
            // SMTP 授权码：提交掩码或空串都不动已保存的真实值
            if (key.equals("fly_smtp_password")
                    && (v.trim().isEmpty() || SMTP_PASSWORD_MASK.equals(v.trim()))) {
                continue;
            }
            // 主题目录必须真实存在，防切到无效皮肤
            if (key.equals("pc_theme") && listSkins().stream().noneMatch(s -> s.equals(v.trim()))) {
                return DataVo.failure("PC 主题目录不存在：" + v);
            }
            configService.updagteConfigByKey(key, v.trim());
            changed++;
        }
        return DataVo.success("已保存 " + changed + " 项站点设置");
    }

    /** 用当前已保存的 SMTP 配置发一封测试邮件，验证第三方邮箱参数是否可用 */
    @ResponseBody
    @PostMapping("/system/website/testEmail")
    public DataVo testEmail(@RequestParam("toEmail") String toEmail) {
        requirePermission("/api/system/website/testEmail");
        String error = emailService.sendTestEmail(toEmail);
        if (error == null) {
            return DataVo.success("测试邮件已发送，请到 " + toEmail + " 收件箱（含垃圾邮件）查收");
        }
        return DataVo.failure("发送失败：" + error);
    }

    // /////////////////// 模板管理 ///////////////////

    /** 当前皮肤下的模板文件树（相对路径 + 大小 + 修改时间） */
    @ResponseBody
    @GetMapping("/system/template/files")
    public DataVo files() {
        requirePermission("/api/system/template/files");
        File dir = templateRoot();
        List<Map<String, Object>> files = new ArrayList<>();
        if (dir.exists()) {
            collect(dir, dir, files);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("skin", configService.getStringByKey("pc_theme"));
        data.put("files", files);
        return DataVo.success("操作成功", data);
    }

    /** 读取模板内容 */
    @ResponseBody
    @GetMapping("/system/template/read")
    public DataVo read(@RequestParam("file") String file) {
        requirePermission("/api/system/template/files");
        File f = resolve(file);
        if (f == null || !f.isFile()) {
            return DataVo.failure("模板文件不存在");
        }
        try {
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("file", file);
            data.put("content", content);
            return DataVo.success("操作成功", data);
        } catch (IOException e) {
            return DataVo.failure("读取失败：" + e.getMessage());
        }
    }

    /**
     * 保存模板内容（在线编辑）。
     *
     * 阶段 D：整条保存链路下沉到 TemplateCenterService ——
     * 语法 parse 校验（写坏不上线）→ 版本快照 → 落盘 → 失效模板缓存（改了立即生效）。
     * 这是"敢把模板编辑交给运营"的前提，也是 §6.2/§6.3 的规避落地。
     */
    @ResponseBody
    @PostMapping("/system/template/save")
    public DataVo save(@RequestParam("file") String file,
                       @RequestParam(value = "content", required = false) String content,
                       @RequestParam(value = "remark", required = false) String remark) {
        requirePermission("/api/system/template/save");
        Long adminId = null;
        com.flycms.module.admin.model.Admin admin =
                com.flycms.core.utils.AdminSessionUtils.getLoginMember(request);
        if (admin != null) {
            adminId = admin.getId();
        }
        return templateCenterService.save(currentSkin(), file, content, remark, adminId);
    }

    /** 新建模板（可带子目录，自动创建） */
    @ResponseBody
    @PostMapping("/system/template/create")
    public DataVo create(@RequestParam("file") String file) {
        requirePermission("/api/system/template/create");
        File f = resolve(file);
        if (f == null) {
            return DataVo.failure("非法模板路径");
        }
        if (f.exists()) {
            return DataVo.failure("文件已存在");
        }
        try {
            Files.createDirectories(f.toPath().getParent());
            Files.write(f.toPath(), defaultTemplateBody(file).getBytes(StandardCharsets.UTF_8));
            templateCenterService.evictTemplate(currentSkin(), file);
            return DataVo.success("模板已创建");
        } catch (IOException e) {
            return DataVo.failure("创建失败：" + e.getMessage());
        }
    }

    /** 删除模板（连同登记与版本历史） */
    @ResponseBody
    @PostMapping("/system/template/delete")
    public DataVo delete(@RequestParam("file") String file) {
        requirePermission("/api/system/template/delete");
        return templateCenterService.delete(currentSkin(), file);
    }

    // /////////////////// 内部 ///////////////////

    /** 当前启用皮肤名（历史默认值拼写为 defalut，保留兼容） */
    private String currentSkin() {
        return StringUtils.defaultIfBlank(configService.getStringByKey("pc_theme"), "defalut");
    }

    /** 当前皮肤模板根目录 */
    private File templateRoot() {
        String skin = StringUtils.defaultIfBlank(configService.getStringByKey("pc_theme"), "defalut");
        return new File("views/templates/pc_theme/" + skin);
    }

    /** 可选皮肤 = pc_theme 下的目录列表 */
    private List<String> listSkins() {
        File root = new File("views/templates/pc_theme");
        List<String> skins = new ArrayList<>();
        File[] dirs = root.listFiles(File::isDirectory);
        if (dirs != null) {
            for (File d : dirs) {
                skins.add(d.getName());
            }
        }
        return skins;
    }

    private void collect(File root, File dir, List<Map<String, Object>> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        Arrays.sort(children, (a, b) -> a.getPath().compareTo(b.getPath()));
        for (File f : children) {
            if (f.isDirectory()) {
                collect(root, f, out);
            } else if (f.getName().endsWith(".html")) {
                String rel = root.toPath().relativize(f.toPath()).toString().replace('\\', '/');
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("file", rel);
                item.put("size", f.length());
                item.put("lastModified", f.lastModified());
                out.add(item);
            }
        }
    }

    /**
     * 相对路径安全解析：正则白名单 + 规范路径前缀校验，非法返回 null
     */
    private File resolve(String file) {
        if (StringUtils.isBlank(file) || file.contains("..")
                || !SAFE_TEMPLATE_PATH.matcher(file).matches()) {
            return null;
        }
        File root = templateRoot();
        File f = new File(root, file);
        try {
            String canonicalRoot = root.getCanonicalPath() + File.separator;
            if (!f.getCanonicalPath().startsWith(canonicalRoot)) {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
        return f;
    }

    /** 新建模板的骨架内容（FreeMarker 尖括号语法） */
    private String defaultTemplateBody(String file) {
        String name = file.substring(file.lastIndexOf('/') + 1);
        return "<#-- " + name + "（在线新建） -->\n"
                + "<!DOCTYPE html>\n<html lang=\"zh\">\n<head>\n<meta charset=\"UTF-8\">\n<title>模板</title>\n"
                + "</head>\n<body>\n\n</body>\n</html>\n";
    }
}
