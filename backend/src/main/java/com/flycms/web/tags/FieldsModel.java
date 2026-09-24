package com.flycms.web.tags;

import com.flycms.module.model.model.Model;
import com.flycms.module.model.service.ModelFieldService;
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
 * 自定义模型字段元数据标签（详情页动态展示用，D12 收敛后）。
 *
 * <p>模板用法：
 * <pre>
 * &lt;@fly_fields_model model="downloads"&gt;
 *   &lt;#list fieldsList as f&gt;&lt;dt&gt;${f.fieldLabel}&lt;/dt&gt;&lt;dd&gt;${info[f.fieldName]!''}&lt;/dd&gt;&lt;/#list&gt;
 * &lt;/@fly_fields_model&gt;
 * </pre>
 *
 * <p>输出变量：{@code fieldsList}（启用中的字段定义列表）。
 */
@Service
public class FieldsModel extends AbstractModelTag {

    @Autowired
    private ModelFieldService modelFieldService;

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
                        TemplateDirectiveBody body) throws TemplateException, IOException {
        Map<String, String> p = parseParams(params);
        Model model = resolveModel(p);

        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("fieldsList",
                model == null ? null : modelFieldService.findFieldsByModelId(model.getId(), 1));
        doRender(env, body, vars);
    }

    private void doRender(Environment env, TemplateDirectiveBody body, Map<String, Object> vars)
            throws IOException, TemplateException, TemplateModelException {
        renderWith(env, body, vars);
    }
}
