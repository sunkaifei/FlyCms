package com.flycms.web.api;

import com.alibaba.fastjson2.JSON;
import com.flycms.core.entity.DataVo;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 稀疏字段集（阶段 L / G7，抄 WordPress {@code ?_fields=} 与 JSON:API {@code sparse fieldsets}）。
 *
 * <p>用法：{@code GET /api/system/article/list?fields=id,title,createTime}
 * —— 只回传指定字段，其余剥离。**不改 SQL**（与 107 处 {@code select *} 并存无冲突），
 * 在响应写出前对已序列化的 JSON 树做裁剪。对带宽敏感的前台/移动端尤其有用
 * （{@code fly_article} 的 mediumtext 正文往往比列表本身大一个数量级）。
 *
 * <p>裁剪语义：
 * <ul>
 *   <li>{@code data} 为数组 → 裁剪每个元素的<b>顶层键</b>；</li>
 *   <li>{@code data} 为对象 → 裁剪其<b>顶层键</b>；</li>
 *   <li>{@code data} 为分页信封（含 {@code list} 数组）→ 保留分页元信息，只裁剪 {@code list} 内每行；</li>
 *   <li>未传 {@code fields} → 原样返回（零开销、零行为变化）。</li>
 * </ul>
 *
 * <p>只作用于 {@code com.flycms.web.api}，前台 Freemarker 渲染不受影响。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@ControllerAdvice(basePackages = "com.flycms.web.api")
public class SparseFieldAdvice implements ResponseBodyAdvice<Object> {

    private static final String PARAM = "fields";
    /** 分页信封中的列表字段名 */
    private static final String PAGE_LIST_KEY = "list";

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        Set<String> keep = parseFields(request);
        if (keep.isEmpty() || !(body instanceof DataVo vo)) {
            return body;
        }
        Object data = vo.getData();
        if (data == null) {
            return body;
        }

        Object tree = JSON.toJSON(data);
        Object pruned = pruneEnvelope(tree, keep);

        // 保持 DataVo 信封结构不变，只替换 data
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("code", vo.getCode());
        out.put("message", vo.getMessage());
        out.put("url", vo.getUrl());
        out.put("data", pruned);
        return out;
    }

    /**
     * 分页信封与普通负载的分流
     */
    private Object pruneEnvelope(Object tree, Set<String> keep) {
        if (tree instanceof Map<?, ?> map && map.get(PAGE_LIST_KEY) instanceof Collection<?>) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                String k = String.valueOf(e.getKey());
                if (PAGE_LIST_KEY.equals(k)) {
                    out.put(k, pruneNode(e.getValue(), keep));
                } else {
                    out.put(k, e.getValue());
                }
            }
            return out;
        }
        return pruneNode(tree, keep);
    }

    private Object pruneNode(Object node, Set<String> keep) {
        if (node instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                String k = String.valueOf(e.getKey());
                if (keep.contains(k)) {
                    out.put(k, pruneNode(e.getValue(), keep));
                }
            }
            return out;
        }
        if (node instanceof Collection<?> col) {
            List<Object> out = new ArrayList<>(col.size());
            for (Object e : col) {
                out.add(pruneNode(e, keep));
            }
            return out;
        }
        return node;
    }

    /**
     * 从 query string 解析 {@code fields=a,b,c}（出现多次时并集）
     */
    private Set<String> parseFields(ServerHttpRequest request) {
        Set<String> keep = new LinkedHashSet<>();
        String rawQuery = request.getURI().getRawQuery();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return keep;
        }
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            if (!PARAM.equals(pair.substring(0, eq))) {
                continue;
            }
            String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            for (String f : value.split(",")) {
                String v = f.trim();
                if (!v.isEmpty()) {
                    keep.add(v);
                }
            }
        }
        return keep;
    }
}
