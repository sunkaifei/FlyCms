package com.flycms.web.tags;

import com.flycms.core.entity.PageVo;
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
 * 自定义模型分页列表标签（D12 收敛后）。
 *
 * <p>模板用法：
 * <pre>
 * &lt;@fly_page_model model="articles" p="${p!1}" rows="10"&gt;
 *   &lt;#list dataList as item&gt;
 *     &lt;a href="/articles/${item.shortUrl}.html"&gt;${item.title}&lt;/a&gt;
 *   &lt;/#list&gt;
 *   ${pageHtml}
 * &lt;/@fly_page_model&gt;
 * </pre>
 *
 * <p>输出变量：{@code dataList}（当前页数据）、{@code model_page}（PageVo 分页对象）、
 * {@code pageHtml}（可直接输出的分页条 HTML）。
 *
 * <p>参数：{@code model}(code) / {@code category} / {@code title} / {@code orderby} /
 * {@code order} / {@code p} / {@code rows}，其余参数视为自定义字段筛选条件。
 */
@Service
public class PageModel extends AbstractModelTag {

    @Autowired
    private ModelDataService modelDataService;

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
                        TemplateDirectiveBody body) throws TemplateException, IOException {
        Map<String, String> p = parseParams(params);
        Model model = resolveModel(p);

        Map<String, Object> vars = new LinkedHashMap<>();
        if (model == null) {
            vars.put("dataList", null);
            vars.put("model_page", null);
            vars.put("pageHtml", "");
            doRender(env, body, vars);
            return;
        }

        int page = intVal(p, "p", 1);
        int rows = intVal(p, "rows", 10);
        try {
            PageVo<Map<String, Object>> pageVo = modelDataService.selectPage(
                    model.getId(), str(p, "title", null), longVal(p, "category"), 1,
                    extractFilters(p), str(p, "orderby", null), str(p, "order", null),
                    page, rows, longVal(p, "notid"), true);
            modelDataService.expandAttachments(model.getId(), pageVo.getList());
            vars.put("dataList", pageVo.getList());
            vars.put("model_page", pageVo);
            vars.put("pageHtml", buildPageBar(page, rows, pageVo.getCount()));
        } catch (Exception e) {
            vars.put("dataList", null);
            vars.put("model_page", null);
            vars.put("pageHtml", "");
        }
        doRender(env, body, vars);
    }

    private void doRender(Environment env, TemplateDirectiveBody body, Map<String, Object> vars)
            throws IOException, TemplateException, TemplateModelException {
        renderWith(env, body, vars);
    }
}
