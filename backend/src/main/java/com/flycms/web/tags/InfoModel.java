package com.flycms.web.tags;

import com.flycms.module.model.model.Model;
import com.flycms.module.model.service.ModelDataService;
import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 自定义模型详情标签（D12 收敛后）。
 *
 * <p>模板用法：
 * <pre>
 * &lt;@fly_info_model model="articles" shortUrl="${shortUrl!}"&gt;
 *   &lt;h1&gt;${info.title}&lt;/h1&gt;
 *   &lt;div&gt;${info.content!''}&lt;/div&gt;
 * &lt;/@fly_info_model&gt;
 * </pre>
 *
 * <p>输出变量：{@code info}（单条内容 Map，附件字段已展开为 {@code xxxUrl}/{@code xxxUrls}）。
 *
 * <p>参数：{@code model}(code) + {@code id} 或 {@code shortUrl} 二选一。
 */
@Service
public class InfoModel extends AbstractModelTag {

    @Autowired
    private ModelDataService modelDataService;

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
                        TemplateDirectiveBody body) throws TemplateException, IOException {
        Map<String, String> p = parseParams(params);
        Model model = resolveModel(p);

        Map<String, Object> info = null;
        if (model != null) {
            Long id = longVal(p, "id");
            String shortUrl = str(p, "shortUrl", null);
            if (id != null) {
                info = modelDataService.findDataById(model.getId(), id);
            } else if (shortUrl != null) {
                info = modelDataService.findByShortUrl(model.getId(), shortUrl);
            }
            if (info != null) {
                java.util.ArrayList<Map<String, Object>> one = new java.util.ArrayList<>();
                one.add(info);
                modelDataService.expandAttachments(model.getId(), one);
            }
        }

        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("info", info);
        doRender(env, body, vars);
    }

    private void doRender(Environment env, TemplateDirectiveBody body, Map<String, Object> vars)
            throws IOException, TemplateException, TemplateModelException {
        renderWith(env, body, vars);
    }
}
