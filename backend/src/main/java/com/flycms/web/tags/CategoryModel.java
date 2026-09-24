package com.flycms.web.tags;

import com.flycms.module.model.model.Model;
import com.flycms.module.model.model.ModelCategory;
import com.flycms.module.model.service.ModelCategoryService;
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
 * 自定义模型分类列表标签（对应 DedeCMS channel/type，D12 收敛后）。
 *
 * <p>模板用法：
 * <pre>
 * &lt;@fly_category_model model="downloads"&gt;
 *   &lt;#list categoryList as c&gt;&lt;a href="/downloads/c${c.id}"&gt;${c.name}&lt;/a&gt;&lt;/#list&gt;
 * &lt;/@fly_category_model&gt;
 * </pre>
 *
 * <p>输出变量：{@code categoryList}（启用中的分类列表）。
 */
@Service
public class CategoryModel extends AbstractModelTag {

    @Autowired
    private ModelCategoryService modelCategoryService;

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
                        TemplateDirectiveBody body) throws TemplateException, IOException {
        Map<String, String> p = parseParams(params);
        Model model = resolveModel(p);

        List<ModelCategory> categoryList = model == null
                ? null
                : modelCategoryService.findCategoriesByModelId(model.getId(), 1);

        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("categoryList", categoryList);
        doRender(env, body, vars);
    }

    private void doRender(Environment env, TemplateDirectiveBody body, Map<String, Object> vars)
            throws IOException, TemplateException, TemplateModelException {
        renderWith(env, body, vars);
    }
}
