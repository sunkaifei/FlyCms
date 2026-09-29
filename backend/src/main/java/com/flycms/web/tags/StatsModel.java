package com.flycms.web.tags;

import com.flycms.module.model.service.ModelDataService;
import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * V4 聚合统计标签：按固有维度（分类/作者）对已发布内容分组计数。
 *
 * <p>模板用法：
 * <pre>
 * &lt;@fly_stats_model model="articles" by="category" rows="10"&gt;
 *   &lt;#list statsList as s&gt;
 *     &lt;a href="/articles/c${s.key}.html"&gt;${s.name}（${s.count}）&lt;/a&gt;
 *   &lt;/#list&gt;
 * &lt;/@fly_stats_model&gt;
 * </pre>
 *
 * <p>输出变量：{@code statsList}（行含 key/name/count，按 count 降序）。
 * {@code by} ∈ category（按分类树节点，name=分类名）/ author（按发布者，name=昵称）。
 * 数值由 {@code ModelDataService.statsGroup} 白名单聚合（恒定过滤已发布）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class StatsModel extends AbstractModelTag {

    @Autowired
    private ModelDataService modelDataService;

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
                        TemplateDirectiveBody body) throws TemplateException, IOException {
        Map<String, String> p = parseParams(params);
        com.flycms.module.model.model.Model model = resolveModel(p);

        List<Map<String, Object>> statsList = new ArrayList<>();
        if (model != null && body != null) {
            try {
                String by = str(p, "by", "category");
                int status = intVal(p, "status", 1);
                int rows = intVal(p, "rows", 20);
                statsList = modelDataService.statsGroup(model, by, status,
                        Math.min(Math.max(rows, 1), 100));
            } catch (Exception e) {
                logTagFailure("fly_stats_model", e);
            }
        }

        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("statsList", statsList);
        renderWith(env, body, vars);
    }
}
