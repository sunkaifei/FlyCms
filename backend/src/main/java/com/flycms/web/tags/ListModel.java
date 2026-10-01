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
 * 自定义模型列表标签（D12 收敛后）。
 *
 * <p><b>与 {@link PageModel} 的关系</b>：本标签是 PageModel 的轻量版，只输出 {@code dataList}，
 * 不产出分页条与 PageVo。收敛决策（D12）保留了二者的**标签名**以兼容既有模板，
 * 但内部实现同源——需要分页条时用 {@code fly_page_model}，只需数据时用 {@code fly_list_model}。
 *
 * <p>模板用法：
 * <pre>
 * &lt;@fly_list_model model="articles" rows="5" orderby="count_view" order="desc"&gt;
 *   &lt;#list dataList as item&gt;&lt;li&gt;${item.title}&lt;/li&gt;&lt;/#list&gt;
 * &lt;/@fly_list_model&gt;
 * </pre>
 *
 * <p>输出变量：{@code dataList}（列表数据）。
 */
@Service
public class ListModel extends AbstractModelTag {

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
                // V3 微缓存：cache="秒" 开启（缓存 List 查询结果；内容变更事件按模型失效）
                dataList = (java.util.List<Map<String, Object>>) tagCache(
                        "fly_list_model", model.getCode(), p, intVal(p, "cache", 0),
                        () -> modelDataService.selectPage(
                                model.getId(), str(p, "title", null), longVal(p, "category"), 1,
                                extractFilters(p), str(p, "orderby", null), str(p, "order", null),
                                intVal(p, "p", 1), intVal(p, "rows", 10), longVal(p, "notid"), true,
                                longVal(p, "userId"), null, null, null,
                                "1".equals(str(p, "withContent", null))).getList());
                modelDataService.expandAttachments(model.getId(), dataList);
            } catch (Exception e) {
            logTagFailure("fly_list_model", e);
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
