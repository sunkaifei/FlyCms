package com.flycms.core.utils;

import java.util.regex.Pattern;

/**
 * ORDER BY 排序片段白名单校验
 * <p>
 * 排序字段经 MyBatis ${} 直接拼入 SQL，无法使用 #{} 预编译。
 * 此工具只放行合法的排序片段：列名（允许表别名前缀 a.id）、asc/desc 方向、
 * 多字段逗号分隔和空白符；包含引号、括号、分号、注释等任意其他字符的
 * 片段一律拒绝并回落到默认值，杜绝 ORDER BY 注入。
 */
public abstract class OrderbyUtils {

    /**
     * 合法排序片段：列标识符（字母数字下划线点）、逗号、空白
     */
    private static final Pattern SAFE_PATTERN = Pattern.compile("[A-Za-z0-9_.\\s,]+");

    /**
     * 校验排序片段，非法或为空时返回默认值
     *
     * @param fragment 排序片段，如 "id desc" 或 "createTime desc, id asc"
     * @param fallback 校验失败时的默认排序片段
     * @return 校验通过的原始片段，或默认片段
     */
    public static String check(String fragment, String fallback) {
        if (fragment == null) {
            return fallback;
        }
        String trimmed = fragment.trim();
        if (trimmed.isEmpty() || !SAFE_PATTERN.matcher(trimmed).matches()) {
            return fallback;
        }
        return trimmed;
    }
}
