package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.template.service.AreaBlockService;
import com.flycms.module.template.service.TemplateResolver;
import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * 区域占位标签（规划 §7.3 / §8.4 V2 / §9.4 / P10）。
 *
 * <p>类名 {@code Area} → 注册名 {@code fly_area}。
 *
 * <p>模板只声明"这里可以放东西"，放什么由后台「布局管理」决定：
 * <pre>
 *   &lt;@fly_area name="content_top"/&gt;
 * </pre>
 * 默认行为是直接把编排好的区块 HTML 写进页面（最常见用法）。
 * 需要自己控制外层容器时，可给标签体，用 {@code area}（结构化）与 {@code area_html}（拼好的 HTML）自渲染：
 * <pre>
 *   &lt;@fly_area name="content_top"&gt;
 *     &lt;section class="area"&gt;
 *       &lt;#if (area.blocks)?size gt 0&gt;
 *         ${area_html}
 *       &lt;/#if&gt;
 *     &lt;/section&gt;
 *   &lt;/@fly_area&gt;
 * </pre>
 *
 * <p>区域名下无需担心"没配就 500"：没有区块时渲染为空，标签体缺失时零输出。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class Area extends AbstractTagPlugin {

    @Autowired
    private AreaBlockService areaBlockService;
    @Autowired
    private TemplateResolver templateResolver;

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
                        TemplateDirectiveBody body) throws TemplateException, IOException {
        String name = null;
        Map<String, TemplateModel> paramWrap = new HashMap<>(params);
        for (Object o : paramWrap.keySet()) {
            String key = String.valueOf(o);
            if ("name".equals(key)) {
                name = paramWrap.get(key).toString();
            }
        }
        if (StringUtils.isBlank(name)) {
            if (body != null) {
                body.render(env.getOut());
            }
            return;
        }

        // 预览感知：管理员带 ?__skin=xxx 时区域也按预览主题取，否则预览会看到旧主题的编排
        AreaBlockService.AreaView view = areaBlockService.renderArea(templateResolver.activeSkin(), name);

        DefaultObjectWrapperBuilder builder =
                new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
        env.setVariable("area", builder.build().wrap(view));
        env.setVariable("area_html", builder.build().wrap(view.getHtml()));

        Writer out = env.getOut();
        if (body != null) {
            // 给了标签体：交给模板决定怎么包壳
            body.render(out);
        } else {
            // 没给标签体：直接输出编排结果
            out.write(view.getHtml());
        }
    }
}
