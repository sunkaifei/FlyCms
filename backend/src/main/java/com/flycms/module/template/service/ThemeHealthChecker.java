package com.flycms.module.template.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.flycms.module.channel.model.Channel;
import com.flycms.module.channel.service.ChannelService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 主题切换真实探活（规划 §6.4 / §15.3；对应审查项 D1）。
 *
 * <p>切换主题后，从应用内部向自己发一次<b>真实 HTTP 请求</b>，用"页面到底能不能正常吐出来"
 * 来判定新主题是否可用。取代原先只判断 {@code index.html} <b>文件是否存在</b>的伪探活——
 * 文件在，但模板语法写错、标签不存在、运行期抛异常，这些只有真请求一次才会暴露，
 * 而正是它们会让线上白屏。
 *
 * <p><b>判定策略</b>
 * <ul>
 *   <li><b>硬探活</b>：首页 {@code /}。非 200，或返回 200 但响应体里带 FreeMarker 报错标记 → 主题不可用。</li>
 *   <li><b>软探活</b>：第一个可见的列表型栏目页。只记警告，不参与回滚判定，
 *       避免"栏目还没配"这类与主题无关的原因把好主题回滚掉。</li>
 *   <li><b>fail-open</b>：请求压根没到达应用（连接被拒 / 超时）时，判定为"探活机制自身不可用"，
 *       返回 {@code conclusive=false} 且<b>不触发回滚</b>。否则一旦探活自身出问题（端口不对、
 *       网络策略变化），任何主题都会切不上去——那比不做探活还糟。</li>
 * </ul>
 *
 * <p><b>为什么用 Hutool 而不是 JDK HttpClient</b>：本项目已验证 JDK HttpClient 抓 Jetty 的
 * chunked 响应会抛 {@code READING_DATA}（整页静态化抓取时踩过，当时也是改用 Hutool）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class ThemeHealthChecker {

    private static final Logger logger = LoggerFactory.getLogger(ThemeHealthChecker.class);

    /** 单次自请求超时（毫秒）。首页要跑完 DB 查询 + 完整模板渲染，留足余量 */
    private static final int TIMEOUT_MS = 10_000;

    /**
     * FreeMarker 的 template_exception_handler 默认为 DEBUG：模板报错时<b>不抛异常</b>，
     * 而是把错误信息内联写进响应体，同时 HTTP 状态码仍然是 200。
     * 因此只看状态码会漏判，必须同时检查响应体。
     */
    private static final String TPL_ERROR_MARK = "FreeMarker template error";

    /** 应用端口，与运行参数保持一致（如 --server.port=80） */
    @Value("${server.port:80}")
    private int port;

    @Autowired(required = false)
    private ChannelService channelService;

    /** 一次探活的结果 */
    public static class ProbeResult {
        /** 主题是否可用：true 表示可以保留这次切换 */
        public final boolean ok;
        /**
         * 结论是否可靠。false 表示请求没能到达应用，无法据此判断主题好坏，
         * 调用方应放行并告警，而不是回滚。
         */
        public final boolean conclusive;
        public final String message;

        ProbeResult(boolean ok, boolean conclusive, String message) {
            this.ok = ok;
            this.conclusive = conclusive;
            this.message = message;
        }
    }

    /**
     * 探活入口：先探首页（硬性，决定成败），再探列表页（仅告警）。
     *
     * @return 首页的探活结果；列表页的异常只记日志
     */
    public ProbeResult probe() {
        ProbeResult home = probePath("/");
        if (!home.ok || !home.conclusive) {
            return home;
        }
        String listPath = firstListChannelPath();
        if (listPath == null) {
            logger.info("[theme-probe] 未找到可见的列表型栏目，跳过列表页探活");
            return home;
        }
        ProbeResult list = probePath(listPath);
        if (!list.ok) {
            logger.warn("[theme-probe] 列表页探活未通过（仅告警，不触发回滚）：{}", list.message);
        }
        return home;
    }

    /**
     * 请求单个路径并判定。
     *
     * @param path 以 / 开头的路径
     * @return 探活结果
     */
    public ProbeResult probePath(String path) {
        String url = "http://127.0.0.1:" + port + path;
        try (HttpResponse resp = HttpRequest.get(url)
                .timeout(TIMEOUT_MS)
                .setFollowRedirects(true)
                .execute()) {
            int status = resp.getStatus();
            if (status != 200) {
                return new ProbeResult(false, true, "探活失败：" + path + " 返回 " + status);
            }
            String body = resp.body();
            if (body != null && body.contains(TPL_ERROR_MARK)) {
                return new ProbeResult(false, true,
                        "探活失败：" + path + " 返回 200，但页面内含 FreeMarker 报错（模板未能正常渲染）");
            }
            logger.info("[theme-probe] {} → 200 OK", url);
            return new ProbeResult(true, true, "探活通过：" + path);
        } catch (Exception e) {
            // 请求没到达应用：属于探活机制自身的问题，不能据此判定主题有问题
            logger.error("[theme-probe] 探活请求未能完成：{}（{}）", url, e.getMessage());
            return new ProbeResult(true, false,
                    "探活请求未能完成：" + path + "（" + e.getMessage() + "）");
        }
    }

    /**
     * 推导第一个可见列表栏目的访问路径。
     *
     * <p>注意路径<b>不补尾斜杠</b>：Spring 6 默认不匹配尾斜杠，
     * 前台栏目路由是 {@code /{channelDir}}，带斜杠会 404 从而误判。
     *
     * @return 形如 {@code /news} 的路径；推导不出则返回 null
     */
    private String firstListChannelPath() {
        if (channelService == null) {
            return null;
        }
        try {
            String dir = findListDir(channelService.treeVisible());
            return StringUtils.isBlank(dir) ? null : "/" + dir;
        } catch (Exception e) {
            logger.warn("[theme-probe] 推导列表页路径失败：{}", e.getMessage());
            return null;
        }
    }

    /** 深度优先找第一个「列表型 + 已显示 + 有目录名」的栏目 */
    private String findListDir(List<Channel> list) {
        if (list == null) {
            return null;
        }
        for (Channel c : list) {
            if (c == null) {
                continue;
            }
            // channelType: 0列表 1单页 2外链 3聚合
            if (c.getChannelType() == 0 && StringUtils.isNotBlank(c.getChannelDir())) {
                return c.getChannelDir();
            }
            String sub = findListDir(c.getChildren());
            if (sub != null) {
                return sub;
            }
        }
        return null;
    }
}
