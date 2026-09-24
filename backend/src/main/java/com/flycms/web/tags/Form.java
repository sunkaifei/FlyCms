package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.form.model.FormField;
import com.flycms.module.form.service.FormService;
import freemarker.core.Environment;
import freemarker.template.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表单标签（规划阶段 F）。类名 Form → 注册名 fly_form，与规划文档用法一致。
 *
 * 模板用法：
 * <@fly_form code="liuyan">
 *   <#if form??>
 *     <form action="/api/form/submit/${form.formCode}" method="post">
 *       <#list form.fields as f>
 *         <label>${f.fieldName}</label>
 *         <input name="${f.fieldCode}" placeholder="${f.placeholder!''}" />
 *       </#list>
 *       <#if form.needCaptcha == 1><img src="/captcha"><input name="captcha"></#if>
 *       <button type="submit">提交</button>
 *     </form>
 *   </#if>
 * </@fly_form>
 *
 * params: code(表单编码)；输出变量 form（含 fields 字段列表）
 */
@Service
public class Form extends AbstractTagPlugin {

    @Autowired
    private FormService formService;

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
                        TemplateDirectiveBody body) throws TemplateException, IOException {
        DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
        String code = null;
        Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
        for (String str : paramWrap.keySet()) {
            if ("code".equals(str)) {
                code = paramWrap.get(str).toString();
            }
        }
        com.flycms.module.form.model.Form form = null;
        if (code != null && !code.trim().isEmpty()) {
            form = formService.findFormByCode(code.trim());
            // 只输出启用中的表单；停用表单在模板侧等同不存在
            if (form != null && (form.getStatus() == null || form.getStatus() != 1)) {
                form = null;
            }
            if (form != null) {
                List<FormField> fields = formService.getFieldList(form.getId());
                form.setFields(fields);
            }
        }
        env.setVariable("form", builder.build().wrap(form));
        body.render(env.getOut());
    }
}
