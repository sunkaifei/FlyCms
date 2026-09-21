package com.flycms.web.front;

import com.flycms.module.config.service.ConfigService;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.service.ModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * SEO 基础设施（规划阶段 G2/G3）：/sitemap.xml 与 /robots.txt
 * 开关与条数走配置键 fly_sitemap_status / fly_sitemap_limit / fly_robots。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@RestController
public class SeoController {

    @Autowired
    private ConfigService configService;

    @Autowired
    private ModelService modelService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private boolean enabled(String key) {
        return !"0".equals(configService.getStringByKey(key));
    }

    private String siteUrl() {
        String url = configService.getStringByKey("fly_url");
        return url == null ? "" : url.replaceAll("/+$", "");
    }

    private String urlEntry(String loc, String lastmod) {
        return "<url><loc>" + loc + "</loc><lastmod>" + lastmod + "</lastmod></url>\n";
    }

    @GetMapping(value = "/sitemap.xml", produces = "application/xml;charset=UTF-8")
    @ResponseBody
    public String sitemap() {
        if (!enabled("fly_sitemap_status")) {
            return "";
        }
        int limit = 100;
        try {
            limit = Integer.parseInt(configService.getStringByKey("fly_sitemap_limit"));
        } catch (NumberFormatException ignored) {
        }
        String base = siteUrl();
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        sb.append(urlEntry(base + "/", now));
        for (Model model : modelService.getEnabledModels()) {
            sb.append(urlEntry(base + "/" + model.getCode() + "/", now));
            try {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "select short_url, update_time from `fly_cmodel_" + model.getId()
                                + "` where status = 1 order by id desc limit " + limit);
                for (Map<String, Object> row : rows) {
                    Object ut = row.get("update_time");
                    String lastmod = ut == null ? now
                            : ut.toString().replace('T', ' ').substring(0, 10);
                    sb.append(urlEntry(base + "/" + model.getCode() + "/" + row.get("short_url") + ".html", lastmod));
                }
            } catch (Exception ignored) {
                // 动态表不存在时跳过该模型
            }
        }
        sb.append("</urlset>");
        return sb.toString();
    }

    @GetMapping(value = "/robots.txt", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String robots() {
        String robots = configService.getStringByKey("fly_robots");
        return robots == null || robots.isEmpty()
                ? "User-agent: *\nAllow: /\n"
                : robots;
    }
}
