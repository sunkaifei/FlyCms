package com.flycms.module.model.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.flycms.core.entity.DataVo;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 字段控件配置（W 批次）白名单 schema：widget_conf 一列 JSON，键与取值域按 fieldType 收敛。
 *
 * <p>三道校验（保存前由 ModelFieldService 调 {@link #check}）：合法 JSON → 键在白名单 →
 * 值在取值域。任何越界即拒绝保存（防垃圾 JSON 进库，同 V1 options 裸串教训）。
 * 前端 field-modal 的「控件设置」面板与本表同源（改动两处同步）。
 *
 * <p>值域表示法：{@code r:正则} 逐值正则校验；数值键另设 min/max 精确边界
 * （rows 1–30 / maxCount 1–50 / rating.count 1–10 / editor.height 240–1200）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
public final class FieldWidgetConfUtil {

    /** fieldType -> (key -> 值域描述) */
    private static final Map<String, Map<String, KeyRule>> SCHEMA = new LinkedHashMap<>();

    private record KeyRule(java.util.regex.Pattern pattern, Integer min, Integer max) {
    }

    static {
        // 附件族
        schema("image", "shape", "square|circle", null, null);
        schema("image", "maxSize", "r:\\d{1,3}", 1, 999);
        schema("images", "maxCount", "r:\\d{1,2}", 1, 50);
        schema("images", "sortMode", "drag|fixed", null, null);
        schema("files", "maxCount", "r:\\d{1,2}", 1, 50);
        schema("files", "sortMode", "drag|fixed", null, null);
        // 时间族
        schema("date", "format", "YYYY-MM-DD|YYYY-MM|YYYY", null, null);
        schema("datetime", "format", "YYYY-MM-DD HH:mm|YYYY-MM-DD HH:mm:ss", null, null);
        // 文本族
        schema("textarea", "rows", "r:\\d{1,2}", 1, 30);
        schema("textarea", "autoSize", "true|false", null, null);
        schema("textarea", "showCount", "true|false", null, null);
        schema("input", "addonBefore", "r:[^<>\"']{0,20}", null, null);
        schema("input", "addonAfter", "r:[^<>\"']{0,20}", null, null);
        schema("number", "addonAfter", "r:[^<>\"']{0,12}", null, null);
        schema("decimal", "addonAfter", "r:[^<>\"']{0,12}", null, null);
        schema("decimal", "step", "r:\\d{1,6}(\\.\\d{1,4})?", null, null);
        schema("slug", "prefix", "r:[^<>\"']{0,30}", null, null);
        schema("editor", "height", "r:\\d{3,4}", 240, 1200);
        schema("editor", "mode", "full|simple", null, null);
        // 选项族
        schema("select", "layout", "select|row|column|button", null, null);
        schema("radio", "layout", "select|row|column|button", null, null);
        schema("checkbox", "layout", "checkbox|button", null, null);
        schema("switch", "checkedText", "r:[^<>\"']{0,10}", null, null);
        schema("switch", "unCheckedText", "r:[^<>\"']{0,10}", null, null);
        schema("rating", "count", "r:\\d{1,2}", 1, 10);
        schema("rating", "allowHalf", "true|false", null, null);
        schema("color", "palette", "r:\\[[^\\[\\]]{0,500}\\]", null, null);
    }

    private static void schema(String type, String key, String domain, Integer min, Integer max) {
        SCHEMA.computeIfAbsent(type, k -> new LinkedHashMap<>())
                .put(key, domain.startsWith("r:")
                        ? new KeyRule(java.util.regex.Pattern.compile(domain.substring(2)), min, max)
                        : new KeyRule(java.util.regex.Pattern.compile("^(" + domain + ")$"), min, max));
    }

    private FieldWidgetConfUtil() {
    }

    /** 该类型是否有控件配置项（无配置类型的字段传了即拒） */
    public static boolean hasSchema(String fieldType) {
        return SCHEMA.containsKey(fieldType);
    }

    /**
     * widget_conf 保存前校验：空值放行（NULL=全默认）；非空则整包三道校验。
     * 返回 DataVo：成功时 data 为规范化后的紧凑 JSON 字符串（键序稳定、去空白）。
     */
    public static DataVo check(String fieldType, String conf) {
        if (StringUtils.isBlank(conf)) {
            return DataVo.success("空配置", "");
        }
        JSONObject obj;
        try {
            obj = JSON.parseObject(conf);
        } catch (Exception e) {
            return DataVo.failure("控件配置不是合法 JSON");
        }
        if (obj == null || obj.isEmpty()) {
            return DataVo.success("空配置", "");
        }
        Map<String, KeyRule> rules = SCHEMA.get(fieldType);
        if (rules == null) {
            return DataVo.failure("该字段类型没有可配置的控件项");
        }
        JSONObject clean = new com.alibaba.fastjson2.JSONObject(new LinkedHashMap<>());
        for (String key : obj.keySet()) {
            KeyRule rule = rules.get(key);
            if (rule == null) {
                return DataVo.failure("控件配置键不合法：" + key + "（" + fieldType + " 允许键：" + rules.keySet() + "）");
            }
            Object v = obj.get(key);
            if (v == null) {
                continue;
            }
            String s = String.valueOf(v);
            if (!rule.pattern().matcher(s).matches()) {
                return DataVo.failure("控件配置值不合法：" + key + " = " + s);
            }
            if (rule.min() != null || rule.max() != null) {
                try {
                    int n = Integer.parseInt(s);
                    if (rule.min() != null && n < rule.min()) {
                        return DataVo.failure("控件配置超范围：" + key + " 最小 " + rule.min());
                    }
                    if (rule.max() != null && n > rule.max()) {
                        return DataVo.failure("控件配置超范围：" + key + " 最大 " + rule.max());
                    }
                } catch (NumberFormatException e) {
                    return DataVo.failure("控件配置值不合法：" + key + " = " + s);
                }
            }
            clean.put(key, v);
        }
        if (clean.isEmpty()) {
            return DataVo.success("空配置", "");
        }
        return DataVo.success("校验通过", clean.toJSONString());
    }
}
