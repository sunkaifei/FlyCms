package com.flycms.web.front;

import com.flycms.core.base.BaseController;
import com.flycms.core.utils.SqlSafeUtil;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.service.ModelDataService;
import com.flycms.module.model.service.ModelService;
import com.flycms.module.template.service.TemplateService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.File;

/**
 * 自定义模型前台路由。
 *
 * /{modelCode}/                     模型列表首页
 * /{modelCode}/p{page}/             模型列表分页
 * /{modelCode}/c{categoryId}        模型分类列表
 * /{modelCode}/c{categoryId}/p{page} 分类分页
 * /{modelCode}/{shortUrl}.html      详情页（数据由模板内 <@InfoModel> 标签取，Controller 只传参）
 *
 * 与现有前台路由共存：Spring 对字面量段优先于变量段匹配，/article/**、/a/**、/question/** 等
 * 不受影响；modelCode 过 SqlSafeUtil.safeModelCode 白名单，查不到或禁用一律回 404 模板。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Controller
public class ModelController extends BaseController {

    @Autowired
    private ModelService modelService;
    @Autowired
    private ModelDataService modelDataService;
    @Autowired
    private TemplateService theme;
    @Autowired
    private com.flycms.module.config.service.ConfigService config;
    @Autowired
    private com.flycms.module.channel.service.ChannelRenderService channelRenderService;
    @Autowired
    private com.flycms.module.template.service.TemplateResolver templateResolver;

    /**
     * /{x}/ 与 /{x}/p{n} 两种 URL 形态在阶段 C 后由模型与栏目共用：
     * 先按栏目命中，命中则交给栏目渲染（含外链 redirect），未命中才走原有模型逻辑。
     * 之所以不新增 pattern，是因为完全相同的 pattern 会触发 Spring Ambiguous mapping 启动失败。
     */
    @GetMapping(value = {"/{modelCode}/", "/{modelCode}/index", "/{modelCode}/p{page:\\d+}"})
    public String list(@PathVariable String modelCode,
                       @PathVariable(value = "page", required = false) Integer page,
                       ModelMap modelMap) {
        int p = page == null ? 1 : page;
        String channelView = channelRenderService.render(modelCode, p, modelMap);
        if (channelView != null) {
            return channelView;
        }
        Model model = resolveModel(modelCode);
        if (model == null) {
            return theme.getPcTemplate("404");
        }
        modelMap.addAttribute("model", model);
        modelMap.addAttribute("p", p);
        return templateResolver.resolveAndExpose(
                com.flycms.module.template.model.TemplateContext.list(model.getCode(), null), modelMap);
    }

    @GetMapping(value = {"/{modelCode}/c{categoryId}", "/{modelCode}/c{categoryId}/p{page:\\d+}"})
    public String categoryList(@PathVariable String modelCode,
                               @PathVariable Long categoryId,
                               @PathVariable(value = "page", required = false) Integer page,
                               ModelMap modelMap) {
        Model model = resolveModel(modelCode);
        if (model == null) {
            return theme.getPcTemplate("404");
        }
        modelMap.addAttribute("model", model);
        modelMap.addAttribute("p", page == null ? 1 : page);
        modelMap.addAttribute("categoryId", categoryId);
        return templateResolver.resolveAndExpose(
                com.flycms.module.template.model.TemplateContext.list(model.getCode(), null), modelMap);
    }

    @GetMapping(value = "/{modelCode}/{shortUrl}.html")
    public String detail(@PathVariable String modelCode,
                         @PathVariable String shortUrl,
                         ModelMap modelMap) {
        Model model = resolveModel(modelCode);
        if (model == null || StringUtils.length(shortUrl) > 10) {
            return theme.getPcTemplate("404");
        }
        modelMap.addAttribute("model", model);
        modelMap.addAttribute("shortUrl", shortUrl);
        // 浏览计数在模板 InfoModel 取数时由 ModelDataService 处理（略，前台查询强制 status=1）
        return templateResolver.resolveAndExpose(
                com.flycms.module.template.model.TemplateContext.detail(model.getCode(), shortUrl, null, null), modelMap);
    }

    private Model resolveModel(String modelCode) {
        try {
            SqlSafeUtil.safeModelCode(modelCode);
        } catch (IllegalArgumentException e) {
            return null;
        }
        Model model = modelService.findModelByCode(modelCode);
        return (model == null || model.getStatus() != 1) ? null : model;
    }
}
