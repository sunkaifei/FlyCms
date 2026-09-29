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
            // G16 草稿预览：request 参数 __preview=1 + __pid + __ptoken → 按 id 取任意状态行
            if ("1".equals(request.getParameter("__preview"))) {
                info = previewById(model);
            } else if (id != null) {
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

    /**
     * G16 预览取数：预览参数走 HTTP request（__pid + __ptoken），任何详情模板零改动生效。
     * 令牌绑定 model/admin/id 的短时效 HMAC；校验失败返回 null（模板空态承接 404 语义）。
     */
    private Long parseLong(String v) {
        try {
            return (v == null || v.isEmpty()) ? null : Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Map<String, Object> previewById(Model model) {
        try {
            Long pid = parseLong(request.getParameter("__pid"));
            String token = request.getParameter("__ptoken");
            if (pid == null || token == null) {
                return null;
            }
            if (!com.flycms.core.utils.PreviewTokenUtils.verify(model.getCode(), pid, token)) {
                return null;
            }
            Map<String, Object> row = modelDataService.findDataById(model.getId(), pid);
            if (row != null) {
                java.util.ArrayList<Map<String, Object>> one = new java.util.ArrayList<>();
                one.add(row);
                modelDataService.expandAttachments(model.getId(), one);
            }
            return row;
        } catch (Exception e) {
            logTagFailure("fly_info_model#preview", e);
            return null;
        }
    }

    private void doRender(Environment env, TemplateDirectiveBody body, Map<String, Object> vars)
            throws IOException, TemplateException, TemplateModelException {
        renderWith(env, body, vars);
    }
}
