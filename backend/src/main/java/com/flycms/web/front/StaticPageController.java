package com.flycms.web.front;

import com.flycms.constant.Const;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 静态页访问路由：/html/** → ./html 目录（织梦式"生成 HTML"，StaticPageService 产物）。
 *
 * <p><b>访问形式</b>（v1.1 起全部支持）：
 * <pre>
 *   /html  /html/              → index.html
 *   /html/{dir}  /html/{dir}/  → {dir}/index.html（目录式访问）
 *   /html/{dir}/p{n}.html      → {dir}/p{n}.html
 *   /html/{model}/{short}.html → {model}/{short}.html
 * </pre>
 * 注意 Spring 6 默认不匹配尾斜杠（/html/news/ 不会命中 /html/{*path}），因此目录式
 * 访问必须显式声明 "/html/{dir}/" 映射，不能只靠捕获一切变量兜底。
 *
 * <p><b>回退规则（混合模式）</b>：静态文件不存在时不 404，按 URL↔动态路由映射表
 * 转发到动态渲染——首页 → /、{dir}/index.html → /{dir}、{dir}/p{n}.html → /{dir}/p{n}、
 * 其余（含 {model}/{short}.html）原样转发。转发路径一律不带尾斜杠（Spring 6 约束）。
 * 因此可以放心整站引用 /html/ 前缀：已生成的走静态，没生成的走动态。
 *
 * <p><b>安全</b>：路径白名单字符校验 + normalize 后必须落在静态根目录内（防穿越）。
 *
 * @author sun-kaifei
 * @version 1.1
 */
@Controller
public class StaticPageController {

    /** 仅允许字母数字、下划线、中划线、斜杠和 .html 后缀 */
    private static final Pattern SAFE_PATH = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_\\-/]*\\.html$");
    private static final Pattern PAGED = Pattern.compile("^(.+)/p(\\d+)\\.html$");

    /** 首页快照：/html、/html/ → index.html */
    @GetMapping({"/html", "/html/"})
    public void index(HttpServletRequest request, HttpServletResponse response)
            throws IOException, jakarta.servlet.ServletException {
        serveRelative("index.html", request, response);
    }

    /** 目录式访问（带尾斜杠）：/html/news/ → news/index.html */
    @GetMapping("/html/{dir}/")
    public void dirTrailingSlash(@PathVariable("dir") String dir,
                                 HttpServletRequest request, HttpServletResponse response)
            throws IOException, jakarta.servlet.ServletException {
        serveRelative(dir + "/index.html", request, response);
    }

    /** 其余：/html/xxx.html、/html/{dir}/p2.html、/html/{model}/{short}.html（也兜住无尾斜杠目录式 /html/news） */
    @GetMapping("/html/{*path}")
    public void serve(@PathVariable("path") String path,
                      HttpServletRequest request, HttpServletResponse response)
            throws IOException, jakarta.servlet.ServletException {
        String p = path == null ? "" : path.replaceFirst("^/", "");
        if (!p.isEmpty() && !p.endsWith(".html")) {
            // 目录式（无尾斜杠）：/html/news → news/index.html
            p = (p.endsWith("/") ? p : p + "/") + "index.html";
        }
        serveRelative(p, request, response);
    }

    private void serveRelative(String p, HttpServletRequest request, HttpServletResponse response)
            throws IOException, jakarta.servlet.ServletException {
        if (p.isEmpty() || !SAFE_PATH.matcher(p).matches()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Path root = Paths.get(Const.STATIC_HTML_PATH).toAbsolutePath().normalize();
        Path target = root.resolve(p).normalize();
        if (!target.startsWith(root)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!Files.isRegularFile(target)) {
            // 静态文件不存在 → 回退动态路由
            request.getRequestDispatcher(dynamicPath(p)).forward(request, response);
            return;
        }
        response.setContentType("text/html;charset=UTF-8");
        response.setDateHeader("Last-Modified", Files.getLastModifiedTime(target).toMillis());
        response.setContentLengthLong(Files.size(target));
        Files.copy(target, response.getOutputStream());
    }

    /** 静态相对路径 → 动态路由（与 StaticPageService 的映射表互逆；一律不带尾斜杠） */
    private String dynamicPath(String p) {
        if ("index.html".equals(p)) {
            return "/";
        }
        if (p.endsWith("/index.html")) {
            // v1.1 修复：去掉尾斜杠，Spring 6 下 "/news/" 匹配不到任何路由
            return "/" + p.substring(0, p.length() - "/index.html".length());
        }
        Matcher m = PAGED.matcher(p);
        if (m.matches()) {
            return "/" + m.group(1) + "/p" + m.group(2);
        }
        return "/" + p;
    }
}
