package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.template.model.Theme;
import com.flycms.module.template.service.ThemeRegistry;
import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * G19 设计令牌输出标签（对标 WordPress theme.json 的 CSS 变量消费方式）。
 *
 * <p>读取当前主题 theme.json 的 {@code settings}（ThemeRegistry 已解析），输出：
 * <pre>
 * &lt;style id="fly-theme-vars"&gt;
 *   :root{--fly-color-ink:#16324A; --fly-gold:#A97C2F; ...;
 *         --fly-font-small:13px; ...; --fly-layout-content:1200px;}
 * &lt;/style&gt;
 * </pre>
 * 模板与自定义 CSS 一律消费 {@code var(--fly-color-*)} 等令牌；改色只改 theme.json
 * （模板中心的文件编辑器可直接编辑），无需动样式文件。无 settings 时输出空串。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class ThemeVars extends AbstractTagPlugin {

    @Autowired
    private ThemeRegistry themeRegistry;

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
                        TemplateDirectiveBody body) throws TemplateException, IOException {
        StringBuilder css = new StringBuilder();
        try {
            Theme theme = themeRegistry.getTheme(themeRegistry.currentSkin());
            if (theme != null && theme.getSettings() != null) {
                Theme.ThemeSettings s = theme.getSettings();
                if (s.getPalette() != null) {
                    for (Theme.PaletteColor c : s.getPalette()) {
                        if (c.getSlug() != null && c.getColor() != null) {
                            css.append("--fly-color-").append(c.getSlug()).append(":")
                                    .append(c.getColor()).append(";");
                        }
                    }
                }
                if (s.getFontSizes() != null) {
                    for (Theme.FontSize f : s.getFontSizes()) {
                        if (f.getSlug() != null && f.getSize() != null) {
                            css.append("--fly-font-").append(f.getSlug()).append(":")
                                    .append(f.getSize()).append(";");
                        }
                    }
                }
                if (s.getContentSize() != null) {
                    css.append("--fly-layout-content:").append(s.getContentSize()).append(";");
                }
                if (s.getWideSize() != null) {
                    css.append("--fly-layout-wide:").append(s.getWideSize()).append(";");
                }
            }
        } catch (Exception e) {
            logTagFailure("fly_theme_vars", e);
        }
        if (!css.isEmpty()) {
            env.getOut().write("<style id=\"fly-theme-vars\">:root{" + css + "}</style>");
        }
    }
}
