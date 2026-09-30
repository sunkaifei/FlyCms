package com.flycms.web.front;

import com.flycms.module.ad.model.Ad;
import com.flycms.module.ad.model.AdPosition;
import com.flycms.module.ad.service.AdService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 广告点击统计（公开路由）：/ad/click/{id} 计数后 302 跳转到广告链接。
 * 标签缺省输出的图片/文字广告链接即指向这里；无链接的广告不走此入口。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
@RequestMapping("/ad")
public class AdController {

    @Autowired
    private AdService adService;

    @GetMapping("/click/{id}")
    public void click(@PathVariable("id") Long id, HttpServletResponse response) throws IOException {
        Ad ad = id == null ? null : adService.findAdById(id);
        String url = ad == null ? null : ad.getUrl();
        if (ad != null) {
            adService.incrementClick(ad);
        }
        if (StringUtils.isBlank(url)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.sendRedirect(url);
    }

    /**
     * 联盟式 JS 分发（公开）：/ad/js/{adKey} 输出 document.write(广告 HTML)。
     * 管理端「获取代码」生成 <script src=".../ad/js/{adKey}"></script>，
     * 粘贴到任意模板页/站外页面即可上线广告（同百度联盟/谷歌联盟取码模式）。
     * no-cache：后台改广告即时生效。
     */
    @GetMapping("/js/{adKey}")
    public void js(@PathVariable("adKey") String adKey, HttpServletResponse response) throws IOException {
        response.setContentType("application/javascript;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        AdPosition position = adService.findPositionByKey(adKey);
        if (position != null && position.getStatus() == 1) {
            java.util.List<Ad> ads = adService.listActiveAds(position.getId(), 20);
            java.util.List<Long> shownIds = new java.util.ArrayList<>();
            for (Ad ad : ads) {
                shownIds.add(ad.getId());
            }
            response.getWriter().write(adService.renderJs(ads));
            adService.incrementViews(position.getId(), shownIds);
        }
    }
}
