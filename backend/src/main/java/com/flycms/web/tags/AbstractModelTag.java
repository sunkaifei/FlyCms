package com.flycms.web.tags;

import com.flycms.core.base.AbstractTagPlugin;
import com.flycms.module.model.model.Model;
import com.flycms.module.model.service.ModelService;
import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义模型标签族公共基类（D12）。
 *
 * <p><b>为什么需要它</b>：改造前 7 个模型标签（{@code InfoModel} / {@code ListModel} /
 * {@code PageModel} / {@code CategoryModel} / {@code RelModel} / {@code HotModel} /
 * {@code FieldsModel}）各自持有一份**逐字复制**的
 * {@code if ("model".equals(str)) ... } 参数解析（每份约 30 行），改一处要改七处，
 * 且新增参数极易漏改。表名/主键改造同时动到这些标签的模型解析链路，
 * 必须趁此收敛，否则改造面会翻七倍。
 *
 * <p><b>收敛边界</b>：本基类只承载"所有模型标签都需要"的部分——
 * 参数解析、模型解析、输出包装、分页条。各子类只保留差异化逻辑
 * （排序方式、过滤条件、聚合维度）。
 *
 * <p><b>兼容性</b>：标签名与参数名全部保持原样（{@code fly_info_model}、{@code fly_list_model}…），
 * 模板无需改动。这是内部实现重构，不是对外契约变更。
 *
 * <p><b>统一参数约定</b>（所有子类共享）：
 * <ul>
 *   <li>{@code model} — 模型 code，必填（如 {@code articles}）</li>
 *   <li>{@code category} — 分类 id，可选</li>
 *   <li>{@code rows} — 每页条数，默认 10</li>
 *   <li>{@code p} — 页码，默认 1</li>
 *   <li>{@code orderby} — 排序字段，白名单校验</li>
 *   <li>{@code order} — 排序方向 {@code asc}/{@code desc}</li>
 * </ul>
 *
 * @author sun-kaifei
 * @version 1.0
 */
public abstract class AbstractModelTag extends AbstractTagPlugin {

    /** 框架参数名：这些参数不参与"自定义字段筛选"（见 {@link #extractFilters}） */
    protected static final List<String> FRAMEWORK_PARAMS = Arrays.asList(
            "model", "category", "title", "orderby", "order", "p", "rows", "id", "shortUrl", "notid",
            "userId", "withContent");

    @Autowired
    protected ModelService modelService;

    /**
     * 统一的参数解析：把 FreeMarker 的 {@code Map<String, TemplateModel>} 转为
     * {@code Map<String, String>}，消除各处重复的 {@code paramWrap.get(str).toString()}。
     */
    protected Map<String, String> parseParams(Map params) {
        Map<String, String> result = new LinkedHashMap<>();
        if (params == null) {
            return result;
        }
        for (Object key : params.keySet()) {
            Object v = params.get(key);
            if (key != null && v != null) {
                result.put(String.valueOf(key), String.valueOf(v));
            }
        }
        return result;
    }

    protected String str(Map<String, String> p, String key, String def) {
        String v = p.get(key);
        return (v == null || v.isEmpty()) ? def : v;
    }

    protected int intVal(Map<String, String> p, String key, int def) {
        try {
            String v = p.get(key);
            return (v == null || v.isEmpty()) ? def : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    protected Long longVal(Map<String, String> p, String key) {
        try {
            String v = p.get(key);
            return (v == null || v.isEmpty()) ? null : Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 模型解析：code → Model。找不到时返回 null，由子类决定降级行为。
     */
    protected Model resolveModel(Map<String, String> p) {
        String code = str(p, "model", null);
        return code == null ? null : modelService.findModelByCode(code);
    }

    /**
     * 提取"自定义字段筛选"参数：即所有非框架参数。
     * 例：{@code <@fly_page_model model="downloads" language="java">} 中 language 即筛选条件。
     */
    protected Map<String, String> extractFilters(Map<String, String> p) {
        Map<String, String> filters = new HashMap<>();
        for (Map.Entry<String, String> e : p.entrySet()) {
            if (!FRAMEWORK_PARAMS.contains(e.getKey())) {
                filters.put(e.getKey(), e.getValue());
            }
        }
        return filters;
    }

    /**
     * 输出变量包装（统一 DefaultObjectWrapper 版本，避免各处 new 出不一致的 wrapper）。
     */
    protected TemplateModel wrap(Object value) throws TemplateModelException {
        DefaultObjectWrapperBuilder builder =
                new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_25);
        return builder.build().wrap(value);
    }

    /**
     * 批量设置输出变量并渲染 body。
     *
     * @param vars 变量名 → 值（null 值会被安全包装为 null model）
     */
    protected void renderWith(Environment env, freemarker.template.TemplateDirectiveBody body,
                              Map<String, Object> vars)
            throws IOException, TemplateException, TemplateModelException {
        for (Map.Entry<String, Object> e : vars.entrySet()) {
            env.setVariable(e.getKey(), wrap(e.getValue()));
        }
        if (body != null) {
            body.render(env.getOut());
        }
    }

    // /////////////////// V3 标签微缓存 ///////////////////

    private static final java.util.concurrent.ConcurrentHashMap<String, long[]> CACHE_TICK =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ConcurrentHashMap<String, Object> CACHE_DATA =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * V3 标签微缓存：模板参数 {@code cache="秒"} 开启（对标帝国标签缓存时间）。
     * key = 标签名 : 模型 : 全参数指纹；内容变更事件（G18）按模型精准失效；
     * 超过 512 条整体清空（微缓存宁可有价再算一次）。
     *
     * @param tagName   标签注册名（如 fly_page_model）
     * @param modelCode 模型 code（失效粒度）
     * @param params    全参数（参与指纹）
     * @param ttlSeconds 缓存秒数，≤0 直接执行不缓存
     * @param loader    实际查询逻辑
     */
    protected Object tagCache(String tagName, String modelCode, Map<String, String> params,
                              int ttlSeconds, java.util.function.Supplier<Object> loader) {
        if (ttlSeconds <= 0) {
            return loader.get();
        }
        String key = tagName + ":" + (modelCode == null ? "" : modelCode) + ":" + params.hashCode();
        long now = System.currentTimeMillis();
        long[] tick = CACHE_TICK.get(key);
        if (tick != null && now - tick[0] < ttlSeconds * 1000L) {
            Object cached = CACHE_DATA.get(key);
            if (cached != null) {
                return cached;
            }
        }
        Object value = loader.get();
        CACHE_TICK.put(key, new long[]{now});
        CACHE_DATA.put(key, value);
        if (CACHE_DATA.size() > 512) {
            CACHE_TICK.clear();
            CACHE_DATA.clear();
        }
        return value;
    }

    /** G18 事件消费：内容变更 → 失效该模型的全部标签微缓存 */
    @org.springframework.context.event.EventListener(
            com.flycms.core.event.ContentChangedEvent.class)
    public void onContentChanged(com.flycms.core.event.ContentChangedEvent event) {
        String marker = ":" + (event.getModelCode() == null ? "" : event.getModelCode()) + ":";
        CACHE_TICK.keySet().removeIf(k -> k.contains(marker));
        CACHE_DATA.keySet().removeIf(k -> k.contains(marker));
    }

    /**
     * 帝国/Dede 式分页条：首页/上一页/页码窗口(±5)/下一页/末页，get 方式 {@code ?p=N}。
     * 原实现内联在 PageModel 中，收敛到基类供所有分页类标签复用（D12）。
     */
    protected String buildPageBar(int page, int rows, int count) {
        if (count <= 0 || rows <= 0) {
            return "";
        }
        int totalPage = (int) Math.ceil((double) count / rows);
        if (totalPage <= 1) {
            return "";
        }
        if (page < 1) {
            page = 1;
        }
        if (page > totalPage) {
            page = totalPage;
        }
        StringBuilder sb = new StringBuilder("<div class=\"fly-page\">");
        sb.append("<span>共").append(count).append("条/").append(totalPage).append("页</span>");
        sb.append(page > 1 ? "<a href='?p=" + (page - 1) + "'>上一页</a>" : "<span>上一页</span>");
        int begin = Math.max(1, page - 5);
        int end = Math.min(totalPage, page + 5);
        for (int i = begin; i <= end; i++) {
            if (i == page) {
                sb.append("<span class='current'>").append(i).append("</span>");
            } else {
                sb.append("<a href='?p=").append(i).append("'>").append(i).append("</a>");
            }
        }
        sb.append(page < totalPage ? "<a href='?p=" + (page + 1) + "'>下一页</a>" : "<span>下一页</span>");
        sb.append("</div>");
        return sb.toString();
    }
}
