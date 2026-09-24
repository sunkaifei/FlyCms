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
import java.util.List;
import java.util.Map;

/**
 * 相关内容标签（对应 DedeCMS likearticle，D12 收敛后）：同模型、可选同分类、排除指定 id。
 *
 * <p>模板用法：
 * <pre>
 * &lt;@fly_rel_model model="downloads" category="${info.categoryId}" notid="${info.id}" rows="5"&gt;
 *   &lt;#list dataList as item&gt;${item.title}&lt;/#list&gt;
 * &lt;/@fly_rel_model&gt;
 * </pre>
 *
 * <p>输出变量：{@code dataList}。
 * 参数：{@code model}(code) / {@code category}(可省) / {@code notid}(详情页传自身) / {@code rows}。
 */
@Service
public class RelModel extends AbstractModelTag {

    @Autowired
    private ModelDataService modelDataService;

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
                        TemplateDirectiveBody body) throws TemplateException, IOException {
        Map<String, String> p = parseParams(params);
        Model model = resolveModel(p);

        List<Map<String, Object>> dataList = null;
        if (model != null) {
            try {
                dataList = modelDataService.selectPage(
                        model.getId(), null, longVal(p, "category"), 1, null,
                        str(p, "orderby", null), str(p, "order", null),
                        1, intVal(p, "rows", 5), longVal(p, "notid"), true).getList();
                modelDataService.expandAttachments(model.getId(), dataList);
            } catch (Exception ignored) {
            }
        }

        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("dataList", dataList);
        doRender(env, body, vars);
    }

    private void doRender(Environment env, TemplateDirectiveBody body, Map<String, Object> vars)
            throws IOException, TemplateException, TemplateModelException {
        renderWith(env, body, vars);
    }
}
