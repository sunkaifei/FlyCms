package com.flycms.web.tags;

import com.flycms.module.ai.service.EmbeddingService;
import com.flycms.module.tag.service.TagService;
import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 搜索结果标签（规划 §9.4 {@code <@fly_search_page/>}）。
 *
 * <p>类名 {@code SearchPage} → 注册名 {@code fly_search_page}。
 *
 * <p><b>为什么需要它</b>：{@code search.html} 长期缺失，搜索页实际回退到了 {@code index.html}
 * （解析链只剩 {@code search → index}）；而可用的 {@code fly_infopage} 数据源是 Solr 遗留空壳。
 * 本标签把"搜索"接到与标签页同一条<b>真实</b>数据链上（跨模型关键词聚合），
 * 让 §5.1 声明的 {@code search.html} 候选真正可用。
 *
 * <p>与标签页的差异：搜索用<b>查询参数式</b>分页（{@code /search?q=…&p=N}），且必须保留 {@code q}，
 * 因此本标签<b>不</b>输出 {@code pageHtml}（{@code AbstractModelTag.buildPageBar} 只拼 {@code ?p=N}，
 * 用在搜索页会丢掉关键词、翻页后变成"全站前 N 条"）。请基于 {@code search_page} 自行拼
 * {@code /search?q=…&p=N}，或直接用标签页那种路径式分页。
 *
 * <p>模板用法：
 * <pre>
 * &lt;@fly_search_page q="${q!''}" p="${(p)!1}" rows="10"&gt;
 *   &lt;#list dataList as row&gt;…&lt;/#list&gt;
 * &lt;/@fly_search_page&gt;
 * </pre>
 *
 * <p>输出变量：{@code dataList}、{@code search_total}、{@code search_page}（PageVo 风格分页对象）。
 * <p>G21：AI 向量模型就位时自动走语义通道（{@code semantic=true}，topK 单页，无分页），
 * 否则回退关键词聚合分页——模板无需感知差异。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class SearchPage extends AbstractModelTag {

    @Autowired
    private TagService tagService;
    @Autowired
    private EmbeddingService embeddingService;

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
                        TemplateDirectiveBody body) throws TemplateException, IOException {
        Map<String, String> p = parseParams(params);
        // q 优先；兼容用 title 传词的旧写法
        String q = str(p, "q", str(p, "title", str(p, "keyword", "")));
        int rows = intVal(p, "rows", 10);
        int page = intVal(p, "p", 1);

        Map<String, Object> vars = new LinkedHashMap<>();
        try {
            // G21：AI 已配置且向量模型就位时走语义通道（余弦排序，单页 topK），
            // 未配置/无向量/异常一律回退关键词聚合——搜索永不因 AI 缺席而空转
            if (embeddingService.semanticReady()) {
                List<Map<String, Object>> semanticRows =
                        embeddingService.searchAcrossModels(q, semanticTopK(page, rows));
                vars.put("dataList", semanticRows);
                vars.put("search_total", semanticRows.size());
                vars.put("search_page", null);
                vars.put("semantic", true);
                renderWith(env, body, vars);
                return;
            }
            TagService.TagResult r = tagService.search(q, page, rows);
            vars.put("dataList", r.getRowsList());
            vars.put("search_total", r.getTotal());
            vars.put("search_page", r.toPageVo());
        } catch (Exception e) {
            logTagFailure("fly_search_page", e);
            vars.put("dataList", null);
            vars.put("search_total", 0);
            vars.put("search_page", null);
        }
        renderWith(env, body, vars);
    }

    /** 语义单页容量 = rows * page（把 ?p=N 映射为 topK 偏移近似，避免翻页越界） */
    private int semanticTopK(int page, int rows) {
        return Math.max(page, 1) * Math.max(rows, 1);
    }
}
