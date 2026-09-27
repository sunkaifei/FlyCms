package com.flycms.web.tags;

import com.flycms.module.tag.service.TagService;
import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 标签聚合标签（规划 §9.4 {@code <@fly_tag_list/>}）。
 *
 * <p>类名 {@code TagList} → 注册名 {@code fly_tag_list}（{@code AbstractTagPlugin.init()} 按
 * {@code fly_ + bean 名下划线化} 注册）。
 *
 * <p><b>与 {@code /tag/{tag}/} 页面的关系</b>：页面路由由 {@code TagController} 承担并直接注入
 * {@code dataList}；本标签是同一能力的<b>嵌入式</b>入口，让任意模板（侧栏、单页、首页区块）
 * 都能就地展示某个标签下的内容，无需新增路由。两者共用 {@link TagService}，口径完全一致。
 *
 * <p>模板用法：
 * <pre>
 * &lt;@fly_tag_list tag="Spring" rows="8"&gt;
 *   &lt;#if dataList?size gt 0&gt;
 *     &lt;#list dataList as row&gt;
 *       &lt;a href="/${row['__modelCode']}/${row.shortUrl}.html"&gt;${row.title}&lt;/a&gt;
 *     &lt;/#list&gt;
 *   &lt;/#if&gt;
 * &lt;/@fly_tag_list&gt;
 * </pre>
 *
 * <p>参数：{@code tag}（或 {@code keyword}）必填、{@code rows}（默认 8，上限 100）、{@code p}（默认 1）。
 * <p>输出变量：{@code dataList}（当前页行，含 {@code __modelCode}/{@code __modelName}）、
 * {@code tag_total}、{@code tag_page}（PageVo 风格分页对象）、{@code relatedTags}（相关标签 Top20）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class TagList extends AbstractModelTag {

    @Autowired
    private TagService tagService;

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
                        TemplateDirectiveBody body) throws TemplateException, IOException {
        Map<String, String> p = parseParams(params);
        String tag = str(p, "tag", str(p, "keyword", null));
        int rows = intVal(p, "rows", 8);
        int page = intVal(p, "p", 1);

        Map<String, Object> vars = new LinkedHashMap<>();
        try {
            TagService.TagResult r = tagService.search(tag == null ? "" : tag, page, rows);
            vars.put("dataList", r.getRowsList());
            vars.put("tag_total", r.getTotal());
            vars.put("tag_page", r.toPageVo());
            vars.put("relatedTags", r.getRelatedTags());
        } catch (Exception e) {
            logTagFailure("fly_tag_list", e);
            vars.put("dataList", null);
            vars.put("tag_total", 0);
            vars.put("tag_page", null);
            vars.put("relatedTags", null);
        }
        renderWith(env, body, vars);
    }
}
