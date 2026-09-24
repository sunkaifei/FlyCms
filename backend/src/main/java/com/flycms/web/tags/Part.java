package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.config.service.ConfigService;
import com.flycms.module.template.service.ThemeRegistry;
import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * 模板部件标签（规划 §7 / P6）：<@fly_part name="header"/>。
 *
 * <p>自动按<b>子主题→父主题</b>查找 {@code parts/{name}.html} 并渲染（与 WP template parts 一致）。
 * 部件路径不再写死在模板里，换父主题/子主题覆盖自动生效；后台"模板部件"页可直接编辑页头页脚。
 *
 * <p>部件文件作为 FreeMarker 模板渲染（可内含 {@code <@fly_block>} 等标签）。
 * 找不到部件时渲染标签体（若有），否则静默跳过，避免因缺部件导致整页 500。
 *
 * <p>用法：
 * <pre>
 *   &lt;@fly_part name="header"/&gt;
 *   &lt;@fly_part name="footer"&gt;默认页脚&lt;/@fly_part&gt;
 * </pre>
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class Part extends AbstractTagPlugin {

    @Autowired
    private ThemeRegistry registry;
    @Autowired
    private ConfigService configService;

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
                        TemplateDirectiveBody body) throws TemplateException, IOException {
        String name = null;
        Map<String, TemplateModel> paramWrap = new HashMap<>(params);
        for (String str : paramWrap.keySet()) {
            if ("name".equals(str)) {
                name = paramWrap.get(str).toString();
            }
        }
        if (StringUtils.isBlank(name)) {
            if (body != null) {
                body.render(env.getOut());
            }
            return;
        }
        String skin = activeSkin();
        ThemeRegistry.Resolved r = registry.locate(skin, "parts/" + name);
        if (r == null) {
            // 部件不存在：渲染标签体作为兜底，保证页面不崩
            if (body != null) {
                body.render(env.getOut());
            }
            return;
        }
        String includeName = "/pc_theme/" + r.skin + "/parts/" + name + ".html";
        try {
            env.include(includeName, null, true);
        } catch (TemplateException | IOException e) {
            // 部件渲染失败（语法错误等）：不向上抛，渲染标签体兜底，避免整页 500
            Writer out = env.getOut();
            out.write("<!-- fly_part 渲染失败：" + name + " -->");
            if (body != null) {
                body.render(out);
            }
        }
    }

    /** 与 TemplateResolver 一致的预览逻辑：管理员带 ?__skin= 时返回预览主题 */
    private String activeSkin() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String preview = req.getParameter("__skin");
                if (StringUtils.isNotBlank(preview)) {
                    if (registry.getTheme(preview) == null) {
                        registry.refresh();
                    }
                    if (registry.getTheme(preview) != null) {
                        return preview;
                    }
                }
            }
        } catch (Exception ignored) {
            // 非请求上下文忽略
        }
        return StringUtils.defaultIfBlank(configService.getStringByKey("pc_theme"), "defalut");
    }
}
