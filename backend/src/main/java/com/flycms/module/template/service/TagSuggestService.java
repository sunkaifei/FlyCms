package com.flycms.module.template.service;

import com.flycms.module.model.model.Model;
import com.flycms.module.model.model.ModelField;
import com.flycms.module.model.service.ModelFieldService;
import com.flycms.module.model.service.ModelService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能标签建议（规划 §8.2「智能标签建议」，P8 剩余项）。
 *
 * <b>要解决的问题</b>：编辑 {@code list-news.html} 时，用户得先知道模型 {@code news}
 * 有哪些字段、哪些字段可筛选，再去翻手册拼标签。这里把「当前文件 → 它服务哪个模型 →
 * 该模型有哪些字段/筛选字段」串成一步，直接给出<b>可复制粘贴的标签骨架</b>。
 *
 * <b>模型推定规则</b>（按可靠度从高到低）：
 * <ol>
 *   <li>文件名规整形态：{@code list-带出模型} / {@code detail-带出模型} / {@code {code}/list} / {@code {code}/detail}</li>
 *   <li>文件名含模型 code 片段（如 {@code page-news} 命中 news）</li>
 *   <li>页面类型兜底：列表/详情默认推荐通用骨架（不含具体模型字段）</li>
 * </ol>
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Service
public class TagSuggestService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TagSuggestService.class);

    @Autowired
    private ModelService modelService;
    @Autowired
    private ModelFieldService modelFieldService;

    /**
     * 生成建议。
     *
     * @param file      相对模板文件，如 list-news.html 或 articles/detail.html
     * @param pageType  页面类型（LIST/DETAIL/INDEX/...），可空，空时按文件名推断
     * @return {file, pageType, model, modelName, fields[], filters[], suggestions[{title,code,note}]}
     */
    public Map<String, Object> suggest(String file, String pageType) {
        String base = StringUtils.defaultString(file).replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        base = StringUtils.removeEnd(base, ".html");

        String type = StringUtils.isNotBlank(pageType) ? pageType.toUpperCase() : inferType(base, file);
        Model model = inferModel(base, file);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("file", file);
        data.put("pageType", type);
        data.put("model", model == null ? "" : model.getCode());
        data.put("modelName", model == null ? "" : StringUtils.defaultString(model.getName()));
        data.put("detected", model != null);

        List<Map<String, Object>> fields = new ArrayList<>();
        List<Map<String, Object>> filters = new ArrayList<>();
        if (model != null) {
            List<ModelField> list = modelFieldService.findFieldsByModelId(model.getId(), 1);
            if (list != null) {
                for (ModelField f : list) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", f.getFieldName());
                    item.put("label", f.getFieldLabel());
                    item.put("type", f.getFieldType());
                    item.put("isList", f.getIsList() == 1);
                    item.put("isFilter", f.getIsFilter() == 1);
                    item.put("isSearch", f.getIsSearch() == 1);
                    fields.add(item);
                    if (f.getIsFilter() == 1) {
                        filters.add(item);
                    }
                }
            }
        }
        data.put("fields", fields);
        data.put("filters", filters);
        data.put("suggestions", buildSuggestions(type, model, filters));
        return data;
    }

    /** 按页面类型给出骨架建议；有模型时把模型 code 与筛选字段直接填进去 */
    private List<Map<String, Object>> buildSuggestions(String type, Model model, List<Map<String, Object>> filters) {
        String code = model == null ? "articles" : model.getCode();
        List<Map<String, Object>> list = new ArrayList<>();
        if ("LIST".equals(type) || "TAG".equals(type) || "SEARCH".equals(type)) {
            StringBuilder extra = new StringBuilder();
            for (Map<String, Object> f : filters) {
                extra.append(" ").append(f.get("name")).append("=\"${").append(f.get("name")).append("!}\"");
            }
            list.add(suggestion("模型分页列表（首选）",
                    "<@fly_page_model model=\"" + code + "\" rows=\"10\" p=\"${p!1}\"" + extra + ">\n"
                            + "  <#list dataList as row>\n"
                            + "    <a href=\"/" + code + "/${row.shortUrl}.html\">${row.title}</a>\n"
                            + "  </#list>\n"
                            + "  ${pageHtml!''}\n"
                            + "</@fly_page_model>",
                    filters.isEmpty()
                            ? "该模型未标记筛选字段；如需要可按字段加参数。"
                            : "已带上该模型全部可筛选字段（isFilter=1），不需要的可删。"));
            list.add(suggestion("纯列表（无分页条，适合侧栏/首页区块）",
                    "<@fly_list_model model=\"" + code + "\" rows=\"8\">\n"
                            + "  <#list dataList as row>\n"
                            + "    <a href=\"/" + code + "/${row.shortUrl}.html\">${row.title}</a>\n"
                            + "  </#list>\n"
                            + "</@fly_list_model>",
                    "同数据源，不输出分页条。"));
        } else if ("DETAIL".equals(type)) {
            list.add(suggestion("内容详情（包裹式，title 可取到真实标题）",
                    "<@fly_info_model model=\"" + code + "\" shortUrl=\"${shortUrl!}\">\n"
                            + "  <#if info??>\n"
                            + "    <h1>${info.title}</h1>\n"
                            + "    <div>${(info.content)!''}</div>\n"
                            + "  </#if>\n"
                            + "</@fly_info_model>",
                    "详情数据必须由标签取；直接引用 info 会恒为空。"));
            if (model != null) {
                list.add(suggestion("动态字段渲染（自定义模型通用）",
                        "<@fly_fields_model model=\"" + code + "\">\n"
                                + "  <#list fieldsList as f>\n"
                                + "    <#if (info[f.fieldName])??>\n"
                                + "      <#assign v = info[f.fieldName]>\n"
                                + "      <#if !(v?is_string) || v != ''>\n"
                                + "        <dt>${f.fieldLabel}</dt><dd>${v}</dd>\n"
                                + "      </#if>\n"
                                + "    </#if>\n"
                                + "  </#list>\n"
                                + "</@fly_fields_model>",
                        "数字/日期列判空不能与 '' 比较，必须先 ?is_string（map 类型陷阱）。"));
            }
        } else if ("INDEX".equals(type)) {
            list.add(suggestion("跨栏目聚合",
                    "<@fly_list_channel rows=\"6\">\n"
                            + "  <#list dataList as row>\n"
                            + "    <a href=\"/${row.__modelCode!'articles'}/${row.shortUrl}.html\">${row.title}</a>\n"
                            + "  </#list>\n"
                            + "</@fly_list_channel>",
                    "聚合栏目/首页流使用；行内带 __modelCode 可直接拼详情链。"));
        } else if ("CHANNEL_PAGE".equals(type)) {
            list.add(suggestion("栏目单页正文",
                    "<h1>${(channel.channelName)!''}</h1>\n"
                            + "<div>${(channel.pageContent)!''}</div>",
                    "单页栏目的正文来自 channel.pageContent。"));
        }
        if (list.isEmpty()) {
            list.add(suggestion("热点排行（通用侧栏）",
                    "<@fly_hot_model model=\"" + code + "\" rows=\"8\">\n"
                            + "  <#list dataList as it><a href=\"/" + code + "/${it.shortUrl}.html\">${it.title}</a></#list>\n"
                            + "</@fly_hot_model>",
                    "按浏览量取热点内容。"));
        }
        return list;
    }

    private Map<String, Object> suggestion(String title, String code, String note) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", title);
        m.put("code", code);
        m.put("note", note);
        return m;
    }

    /** 文件名约定 → 页面类型 */
    private String inferType(String base, String file) {
        String f = StringUtils.defaultString(file);
        if (base.startsWith("list")) {
            return "LIST";
        }
        if (base.startsWith("detail")) {
            return "DETAIL";
        }
        if (base.startsWith("page-")) {
            return "CHANNEL_PAGE";
        }
        if ("index".equals(base)) {
            return "INDEX";
        }
        if (f.contains("/list")) {
            return "LIST";
        }
        if (f.contains("/detail")) {
            return "DETAIL";
        }
        return "INDEX";
    }

    /**
     * 文件名 → 模型：把候选片段按「列表 → 模型 code」全部试一遍，
     * 命中库中真实模型才返回（避免把 page 这种通用词误判成模型）。
     */
    private Model inferModel(String base, String file) {
        List<String> candidates = new ArrayList<>();
        String f = StringUtils.defaultString(file).replace('\\', '/');
        // a) 目录形态：articles/list、articles/detail
        int slash = f.lastIndexOf('/');
        if (slash > 0) {
            candidates.add(f.substring(0, slash));
        }
        // b) list-xxx / detail-xxx（xxx 可能是 模型 或 栏目-模型，逐个后缀试）
        String[] parts = base.split("-");
        for (int i = 1; i < parts.length; i++) {
            candidates.add(parts[i]);
        }
        for (int i = 2; i <= parts.length - 1; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = i; j < parts.length; j++) {
                if (sb.length() > 0) {
                    sb.append('-');
                }
                sb.append(parts[j]);
            }
            candidates.add(sb.toString());
        }
        for (String c : candidates) {
            if (StringUtils.isBlank(c) || c.length() > 32) {
                continue;
            }
            try {
                Model m = modelService.findModelByCode(c);
                if (m != null) {
                    return m;
                }
            } catch (Exception e) {
                logger.debug("模型 code 查询失败，智能建议跳过（{}）：{}", c, e.getMessage());
            }
        }
        return null;
    }
}
