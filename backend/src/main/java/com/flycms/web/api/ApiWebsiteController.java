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
 * 网站配置只允许白名单键读写（绝不暴露 SMTP 密码等敏感键）。
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
            "master", "qq", "email", "mobile", "phone", "address",
            "pc_theme", "m_theme");

    private static final Pattern SAFE_TEMPLATE_PATH =
            Pattern.compile("^[\\w\\-/\\u4e00-\\u9fa5]+\\.html$");

    private static final long MAX_TEMPLATE_BYTES = 512 * 1024;

    @Autowired
    private ConfigService configService;

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
            // 主题目录必须真实存在，防切到无效皮肤
            if (key.equals("pc_theme") && listSkins().stream().noneMatch(s -> s.equals(v.trim()))) {
                return DataVo.failure("PC 主题目录不存在：" + v);
            }
            configService.updagteConfigByKey(key, v.trim());
            changed++;
        }
        return DataVo.success("已保存 " + changed + " 项站点设置");
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

    /** 保存模板内容（在线编辑） */
    @ResponseBody
    @PostMapping("/system/template/save")
    public DataVo save(@RequestParam("file") String file,
                       @RequestParam(value = "content", required = false) String content) {
        requirePermission("/api/system/template/save");
        File f = resolve(file);
        if (f == null) {
            return DataVo.failure("非法模板路径");
        }
        byte[] bytes = (content == null ? "" : content).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_TEMPLATE_BYTES) {
            return DataVo.failure("模板内容超过 512KB 上限");
        }
        try {
            Files.write(f.toPath(), bytes);
            return DataVo.success("模板已保存");
        } catch (IOException e) {
            return DataVo.failure("保存失败：" + e.getMessage());
        }
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
            return DataVo.success("模板已创建");
        } catch (IOException e) {
            return DataVo.failure("创建失败：" + e.getMessage());
        }
    }

    /** 删除模板 */
    @ResponseBody
    @PostMapping("/system/template/delete")
    public DataVo delete(@RequestParam("file") String file) {
        requirePermission("/api/system/template/delete");
        File f = resolve(file);
        if (f == null || !f.isFile()) {
            return DataVo.failure("模板文件不存在");
        }
        if (!f.delete()) {
            return DataVo.failure("删除失败");
        }
        return DataVo.success("模板已删除");
    }

    // /////////////////// 内部 ///////////////////

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
