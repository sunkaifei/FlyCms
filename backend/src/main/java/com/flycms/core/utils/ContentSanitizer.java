package com.flycms.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;

import java.util.Locale;
import java.util.Set;

/**
 * 富文本正文入库前 XSS 清洗（2026-10-23）。
 *
 * <p><b>为什么必须在服务端写侧做</b>：前台模板用 {@code ${info.content}} 原样输出正文
 * （富文本必须如此，转义就没法排版了），所以只要正文里带 {@code <script>}、
 * {@code onerror}、{@code javascript:} 伪协议，就是一条完整的存储型 XSS 通道 ——
 * 审核员在后台看投稿、管理员在后台改配置，都在同一会话域里。
 * 此前只有审核详情抽屉在「渲染侧」挡了一下，写库的内容本身仍是脏的。
 *
 * <p>与 {@link JsoupUtils}（评论用，{@code basicWithImages}）的分工：
 * 正文需要 h1-h6 / table / 视频嵌入等排版标签，basic 白名单会把它们全删掉 ——
 * 那才是「改变存量语义」的真正来源。这里用 relaxed 基线 + 显式增补，
 * 只删危险的东西，不删排版能力。
 *
 * <p>清洗是<b>幂等</b>的：同一段干净正文清洗前后完全一致（prettyPrint=false 且不做格式化），
 * 因此可以安全地对存量重复执行。
 *
 * @author sun-kaifei
 * @version 2.0
 */
@Slf4j
public final class ContentSanitizer {

    /** 站内相对协议基址（用于 absUrl 解析，不参与输出） */
    private static final String BASE_URI = "http://localhost/";

    /** 允许嵌入的视频/音频域名（iframe 与 source 共用） */
    private static final Set<String> MEDIA_HOSTS = Set.of(
            "player.bilibili.com", "www.bilibili.com", "bilibili.com",
            "player.youku.com", "v.youku.com", "player.iqiyi.com", "open.iqiyi.com",
            "v.qq.com", "videos.qq.com", "www.youtube.com", "youtube.com", "youtu.be",
            "player.vimeo.com", "vimeo.com", "music.163.com", "www.w3schools.com");

    private static final Safelist SAFELIST;
    private static final Document.OutputSettings OUTPUT =
            new Document.OutputSettings().prettyPrint(false);

    static {
        Safelist base = Safelist.relaxed();
        // 排版标签：relaxed 已含 h1-h6/table/blockquote/pre/code，这里补齐富文本常见项
        base.addTags("section", "article", "figure", "figcaption", "hr", "iframe",
                "video", "audio", "source", "embed");
        // 富文本的对齐/配色依赖 class 与内联 style（relaxed 默认不保留 class）
        base.addAttributes(":all", "class", "style", "id");
        base.addAttributes("iframe", "src", "width", "height", "frameborder", "allowfullscreen",
                "allow", "loading", "title", "sandbox", "referrerpolicy");
        base.addAttributes("video", "src", "controls", "width", "height", "poster", "preload", "loop", "muted");
        base.addAttributes("audio", "src", "controls", "preload", "loop", "muted");
        base.addAttributes("source", "src", "type");
        base.addAttributes("embed", "src", "type", "width", "height");
        base.addAttributes("img", "loading", "decoding", "srcset", "sizes");
        base.addAttributes("a", "target", "rel");
        base.addAttributes("td", "colspan", "rowspan");
        base.addAttributes("th", "colspan", "rowspan");
        // 协议白名单：挡掉 javascript: / data: 伪协议
        base.addProtocols("a", "href", "http", "https", "mailto", "tel");
        base.addProtocols("img", "src", "http", "https");
        base.addProtocols("iframe", "src", "http", "https");
        base.addProtocols("video", "src", "http", "https");
        base.addProtocols("audio", "src", "http", "https");
        base.addProtocols("source", "src", "http", "https");
        base.addProtocols("embed", "src", "http", "https");
        SAFELIST = base;
    }

    private ContentSanitizer() {
    }

    /**
     * 清洗富文本正文。空白原样返回（不要把 null 变成空串，调用方依赖 null 判定不写列）。
     *
     * <p>清洗失败时按「宁可空、不可脏」原则返回 null 并记日志 —— 一段正文丢失可以重填，
     * 一段带 XSS 的正文进库会持续危害所有人。
     */
    public static String sanitize(String html) {
        if (StringUtils.isBlank(html)) {
            return html;
        }
        try {
            String cleaned = Jsoup.clean(html, BASE_URI, SAFELIST, OUTPUT);
            Document doc = Jsoup.parseBodyFragment(cleaned, BASE_URI);
            // 二次收口：jsoup 的白名单只管标签与协议，管不了「合法协议 + 恶意域名」
            for (Element el : doc.select("iframe")) {
                if (!mediaHostAllowed(el.absUrl("src"))) {
                    el.remove();
                }
            }
            for (Element el : doc.select("video, audio")) {
                String src = el.absUrl("src");
                if (StringUtils.isNotBlank(src) && !mediaHostAllowed(src)) {
                    el.remove();
                }
            }
            // target=_blank 一律补 rel，避免反向标签页劫持
            for (Element a : doc.select("a[target=_blank]")) {
                a.attr("rel", "noopener noreferrer");
            }
            return doc.body().html();
        } catch (Exception e) {
            log.warn("正文清洗失败，按空处理：{}", e.getMessage());
            return null;
        }
    }

    /** 清洗后是否与原文不同（存量扫描用，避免无谓 UPDATE） */
    public static boolean changed(String html) {
        if (StringUtils.isBlank(html)) {
            return false;
        }
        String cleaned = sanitize(html);
        return cleaned == null || !cleaned.equals(html);
    }

    private static boolean mediaHostAllowed(String absUrl) {
        if (StringUtils.isBlank(absUrl)) {
            // 相对地址视为站内自己的播放器
            return true;
        }
        String url = absUrl.toLowerCase(Locale.ROOT);
        int schemeAt = url.indexOf("://");
        if (schemeAt < 0) {
            return true;
        }
        String rest = url.substring(schemeAt + 3);
        int slash = rest.indexOf('/');
        String host = slash < 0 ? rest : rest.substring(0, slash);
        int at = host.indexOf('@');
        if (at >= 0) {
            // user@host 形式：取 @ 之后
            host = host.substring(at + 1);
        }
        int colon = host.indexOf(':');
        if (colon >= 0) {
            host = host.substring(0, colon);
        }
        if (host.isEmpty()) {
            return false;
        }
        if (MEDIA_HOSTS.contains(host)) {
            return true;
        }
        // 站内域名（同 BASE_URI）放行
        return "localhost".equals(host) || "127.0.0.1".equals(host);
    }
}
