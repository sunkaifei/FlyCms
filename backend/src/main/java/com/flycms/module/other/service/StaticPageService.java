package com.flycms.module.other.service;

import com.flycms.constant.Const;
import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.channel.model.Channel;
import com.flycms.module.channel.service.ChannelService;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.service.ModelService;
import com.flycms.module.model.service.ModelDataService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 整页静态化服务（织梦式"生成 HTML"，v1）。
 *
 * <p><b>实现策略：自请求抓取</b>——直接 HTTP 请求本机动态路由，把渲染结果字节写入
 * {@code ./html} 目录。与动态渲染共用同一条 FreeMarker 管线（标签、主题、模板层级、
 * 缓存全部一致），规避了"无 Servlet 上下文离线渲染模板"这一整类问题。
 *
 * <p><b>URL ↔ 文件映射</b>（静态访问路由 {@code /html/**}，StaticPageController）：
 * <pre>
 *   动态 /                      → html/index.html
 *   动态 /{dir}                 → html/{dir}/index.html
 *   动态 /{dir}/p{n}            → html/{dir}/p{n}.html
 *   动态 /{model}/{short}.html  → html/{model}/{short}.html
 * </pre>
 * 静态页内链指向动态路由（混合模式）：静态文件是快照，链接点击走动态渲染仍可用；
 * 静态文件缺失时 /html/** 自动回退转发动态路由，不会 404。
 *
 * <p><b>安全</b>：写入路径白名单字符 + normalize 后必须落在根目录内（防路径穿越）；
 * 自请求仅打 127.0.0.1，不经过外部网络。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class StaticPageService {

    private static final Logger log = LoggerFactory.getLogger(StaticPageService.class);

    /** 相对文件名白名单：段名 + 目录分隔，shortUrl 为 base62 可能含大写 */
    private static final Pattern SAFE_FILE = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_\\-/]*\\.html$");

    /** 详情页单次抓取上限，防止恶意大库拖死生成请求 */
    private static final int MAX_DETAIL_ROWS = 10000;

    @Autowired
    private ModelService modelService;
    @Autowired
    private ModelDataService modelDataService;
    @Autowired
    private ChannelService channelService;

    @Value("${server.port:80}")
    private int port;

    private Path root() {
        return Paths.get(Const.STATIC_HTML_PATH).toAbsolutePath().normalize();
    }

    // /////////////////// 生成入口 ///////////////////

    /** 全量生成：首页 + 全部启用栏目（含分页）+ 全部启用模型的已发布详情页 */
    public DataVo generateAll() {
        List<String> files = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try {
            files.add(fetchTo("/", "index.html"));
        } catch (Exception e) {
            errors.add("首页: " + e.getMessage());
        }
        for (Channel ch : channelService.tree()) {
            if (ch.getStatus() != 1 || ch.getChannelType() == 2) {
                continue;
            }
            DataVo r = generateChannel(ch);
            collect(r, files, errors);
        }
        for (Model m : modelService.getEnabledModels()) {
            DataVo r = generateModelDetails(m.getCode());
            collect(r, files, errors);
        }
        return finish(files, errors);
    }

    /** 首页 → html/index.html */
    public DataVo generateHome() {
        try {
            return DataVo.success("首页已生成", List.of(fetchTo("/", "index.html")));
        } catch (Exception e) {
            return DataVo.failure("首页生成失败：" + e.getMessage());
        }
    }

    /** 栏目页（含分页）→ html/{dir}/index.html、html/{dir}/p{n}.html */
    public DataVo generateChannel(String dir) {
        if (dir == null || !dir.matches("^[a-z][a-z0-9_\\-]{0,49}$")) {
            return DataVo.failure("非法栏目目录：" + dir);
        }
        Channel ch = channelService.findByDir(dir);
        if (ch == null) {
            return DataVo.failure("栏目不存在：" + dir);
        }
        if (ch.getChannelType() == 2) {
            return DataVo.failure("外链栏目不生成静态页");
        }
        return generateChannel(ch);
    }

    /** 模型详情页（已发布）→ html/{model}/{short}.html */
    public DataVo generateModelDetails(String code) {
        Model model = modelService.findModelByCode(StringUtils.trimToEmpty(code).toLowerCase());
        if (model == null) {
            return DataVo.failure("模型不存在：" + code);
        }
        List<String> files = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        PageVo<Map<String, Object>> page = modelDataService.selectPage(
                model.getId(), null, null, 1, null, null, null, 1, MAX_DETAIL_ROWS, null, true);
        if (page.getList() == null || page.getList().isEmpty()) {
            return DataVo.success("模型[" + code + "]暂无已发布内容", files);
        }
        for (Map<String, Object> row : page.getList()) {
            Object shortUrl = row.get("short_url");
            if (shortUrl == null || StringUtils.isBlank(shortUrl.toString())) {
                continue;
            }
            String name = model.getCode() + "/" + shortUrl + ".html";
            try {
                files.add(fetchTo("/" + name, name));
            } catch (Exception e) {
                errors.add(name + ": " + e.getMessage());
            }
        }
        return finish(files, errors);
    }

    // /////////////////// 内部 ///////////////////

    private DataVo generateChannel(Channel ch) {
        List<String> files = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try {
            files.add(fetchTo("/" + ch.getChannelDir(), ch.getChannelDir() + "/index.html"));
        } catch (Exception e) {
            errors.add(ch.getChannelDir() + ": " + e.getMessage());
            return finish(files, errors);
        }
        // 列表型栏目按真实行数生成分页
        if (ch.getModelId() != null && ch.getModelId() > 0) {
            int pageSize = ch.getPageSize() > 0 ? ch.getPageSize() : 20;
            PageVo<Map<String, Object>> page = modelDataService.selectPage(
                    ch.getModelId(), null, null, 1, null, null, null, 1, 1, null, true);
            int pages = (int) Math.ceil(page.getCount() / (double) pageSize);
            for (int i = 2; i <= pages; i++) {
                String name = ch.getChannelDir() + "/p" + i + ".html";
                try {
                    files.add(fetchTo("/" + ch.getChannelDir() + "/p" + i, name));
                } catch (Exception e) {
                    errors.add(name + ": " + e.getMessage());
                }
            }
        }
        return finish(files, errors);
    }

    /**
     * 抓取动态 URL 渲染结果写盘。
     *
     * @param dynamicUrl   本机动态路由（如 /news、/articles/xx.html）
     * @param relativeFile html 目录下的相对文件名（如 news/index.html）
     */
    private String fetchTo(String dynamicUrl, String relativeFile) throws Exception {
        if (relativeFile == null || !SAFE_FILE.matcher(relativeFile).matches()) {
            throw new IllegalArgumentException("非法静态文件名：" + relativeFile);
        }
        Path root = root();
        Path target = root.resolve(relativeFile).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("静态文件路径越界：" + relativeFile);
        }
        // Hutool HTTP：对 Jetty chunked 响应更宽容（JDK HttpClient 会抛
        // "chunked transfer encoding, state: READING_DATA"）
        try (cn.hutool.http.HttpResponse resp = cn.hutool.http.HttpRequest
                .get("http://127.0.0.1:" + port + dynamicUrl)
                .header("X-Static-Gen", "1")
                .timeout(15000)
                .execute()) {
            if (resp.getStatus() != 200) {
                throw new IllegalStateException(dynamicUrl + " → HTTP " + resp.getStatus());
            }
            Files.createDirectories(target.getParent());
            Files.write(target, resp.bodyBytes());
        }
        return relativeFile;
    }

    private void collect(DataVo r, List<String> files, List<String> errors) {
        if (r.getCode() != DataVo.CODE_SUCCESS) {
            errors.add(r.getMessage());
            return;
        }
        Object data = r.getData();
        if (data instanceof List) {
            for (Object o : (List<?>) data) {
                files.add(String.valueOf(o));
            }
        }
    }

    private DataVo finish(List<String> files, List<String> errors) {
        if (errors.isEmpty()) {
            return DataVo.success("生成完成，共 " + files.size() + " 个页面", files);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("files", files);
        data.put("errors", errors);
        return DataVo.failure("生成 " + files.size() + " 个页面，" + errors.size() + " 个失败", data);
    }
}
