package com.flycms.web.front;

import com.flycms.core.base.BaseController;
import com.flycms.module.tag.service.TagService;
import com.flycms.module.template.model.TemplateContext;
import com.flycms.module.template.service.TemplateResolver;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 标签聚合页前台路由（规划 §5.1 标签页 / §9.4）。
 *
 * <p><b>为什么之前是"死路径"</b>：{@code PageType.TAG}、{@code TemplateContext.tag()}、
 * {@code TemplateResolver.resolveTag()} 三者早已存在，但没有任何前台路由与 {@code tag.html} 模板 ——
 * 声明存在、实际不可达（2026-09-28 补全）。
 *
 * <p><b>路由形态</b>：{@code /tag/{tag}} 与 {@code /tag/{tag}/p{n}}，
 * 同时注册带尾斜杠的变体（Spring 6 默认不匹配尾斜杠）。
 * 该前缀不与 {@code /{modelCode}/…}、{@code /{channelDir}/…} 冲突（前缀是字面量 {@code tag}）。
 *
 * <p><b>数据源</b>：{@link TagService}（跨模型关键词聚合）。不使用 {@code SearchService} ——
 * 那是 Solr 移除后的空壳，会让标签页永远 0 结果。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
public class TagController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(TagController.class);

    /** 标签名长度上限（同时作为 path 变量的合法性约束） */
    private static final int MAX_TAG_LEN = 50;

    /** 标签页每页条数 */
    private static final int ROWS = 10;

    @Autowired
    private TagService tagService;
    @Autowired
    private TemplateResolver templateResolver;

    @GetMapping(value = {
            "/tag/{tag}", "/tag/{tag}/",
            "/tag/{tag}/p{page:\\d+}", "/tag/{tag}/p{page:\\d+}/"
    })
    public String tag(@PathVariable String tag,
                      @PathVariable(value = "page", required = false) Integer page,
                      ModelMap modelMap) {
        String t = StringUtils.trimToEmpty(tag);
        if (StringUtils.isBlank(t) || t.length() > MAX_TAG_LEN || !safeTag(t)) {
            return "forward:/404";
        }
        int p = (page == null || page < 1) ? 1 : page;
        try {
            TagService.TagResult r = tagService.search(t, p, ROWS);
            if (getUser() != null) {
                modelMap.addAttribute("user", getUser());
            }
            modelMap.addAttribute("tag", r.getKeyword());
            modelMap.addAttribute("title", r.getKeyword());
            modelMap.addAttribute("keywords", r.getKeyword());
            modelMap.addAttribute("p", p);
            modelMap.addAttribute("pageRows", ROWS);
            modelMap.addAttribute("dataList", r.getRowsList());
            modelMap.addAttribute("tag_total", r.getTotal());
            modelMap.addAttribute("tag_pageCount", r.getPageCount());
            modelMap.addAttribute("relatedTags", r.getRelatedTags());
            modelMap.addAttribute("modelHits", r.getModelHits());
            // 复用 PageVo 风格的变量名，模板里 <@pager count=.. rows=.. cur=..> 与 info_page 两种写法都能用
            modelMap.addAttribute("tag_page", r.toPageVo());
            return templateResolver.resolveAndExpose(TemplateContext.tag(r.getKeyword()), modelMap);
        } catch (Exception e) {
            logger.error("标签页渲染失败：tag={}，原因={}", t, e.getMessage());
            return "forward:/404";
        }
    }

    /**
     * 标签名合法性：允许中文/字母/数字/下划线/中划线/空格/点，
     * 拒绝 {@code / \ % ? # & =} 等可改变路径语义或注入的字符。
     */
    private boolean safeTag(String tag) {
        return tag.matches("^[\\w\\u4e00-\\u9fa5 .\\-]{1,50}$");
    }
}
