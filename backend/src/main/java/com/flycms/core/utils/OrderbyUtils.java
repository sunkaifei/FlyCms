package com.flycms.core.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ORDER BY 排序片段白名单校验。
 *
 * <p>排序字段经 MyBatis {@code ${}} 直接拼入 SQL，无法使用 {@code #{}} 预编译，
 * 因此必须在本工具内完成全部安全校验。校验分两层：
 *
 * <ol>
 *   <li><b>结构校验（必过）</b>：片段必须整体形如
 *       {@code (alias.)?column [asc|desc](, (alias.)?column [asc|desc])*}。
 *       只允许标识符、单个别名前缀、逗号、空白与 asc/desc 关键字——
 *       引号、括号、分号、注释符、函数体、子查询一律拒绝。这一层单独就能杜绝
 *       "把整段表达式塞进 ORDER BY" 的经典注入。</li>
 *   <li><b>列名白名单（可选）</b>：调用方传入目标查询允许排序的列集合后，
 *       片段里出现的列（去掉别名前缀后）必须在该集合内。
 *       仅靠结构校验仍允许 {@code ORDER BY password} 这类"按敏感列排序"的
 *       侧信道 oracle（观察返回顺序即可盲取该列内容），
 *       因此但凡能枚举列的场景都应传白名单。</li>
 * </ol>
 *
 * <p><b>失败语义</b>：任一校验不通过都返回 {@code fallback}，不抛异常——
 * 排序参数来自模板/URL，非法值降级为默认排序比 500 更合理。
 *
 * @author sun-kaifei
 * @version 2.0
 */
public final class OrderbyUtils {

    /**
     * 单个排序项：{@code column} 或 {@code alias.column}，后可跟 asc/desc。
     * 故意不匹配任何含引号、括号、函数调用的片段。
     */
    private static final Pattern SORT_ITEM =
            Pattern.compile("^([A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)?)(?:\\s+(asc|desc))?$",
                    Pattern.CASE_INSENSITIVE);

    private OrderbyUtils() {
    }

    /**
     * 校验排序片段（结构校验 + 列名白名单）。
     *
     * @param fragment       排序片段，如 {@code "id desc"} 或 {@code "create_time desc, id asc"}
     * @param fallback       校验失败时的默认排序片段
     * @param allowedColumns 允许排序的列名集合（不带别名）；传 {@code null} 或空集时只做结构校验
     * @return 校验通过的片段（已归一化方向关键字为小写），或 {@code fallback}
     */
    public static String check(String fragment, String fallback, Collection<String> allowedColumns) {
        if (fragment == null) {
            return fallback;
        }
        String trimmed = fragment.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }
        boolean hasWhitelist = allowedColumns != null && !allowedColumns.isEmpty();
        StringBuilder out = new StringBuilder();
        for (String raw : trimmed.split(",")) {
            String part = raw.trim();
            if (part.isEmpty()) {
                return fallback;
            }
            Matcher m = SORT_ITEM.matcher(part);
            if (!m.matches()) {
                return fallback;
            }
            String column = m.group(1);
            String direction = m.group(2);
            if (hasWhitelist && !allowedColumns.contains(bareColumn(column))) {
                return fallback;
            }
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(column);
            if (direction != null) {
                out.append(' ').append(direction.toLowerCase(java.util.Locale.ROOT));
            }
        }
        return out.toString();
    }

    /**
     * 结构校验版（无列名白名单）。保留此重载以兼容无法枚举列的场景，
     * 新代码优先使用带白名单的三参版本。
     */
    public static String check(String fragment, String fallback) {
        return check(fragment, fallback, null);
    }

    /**
     * 排序方向校验：只放行 asc / desc，其余一律回落默认值。
     *
     * <p>方向与列名是两种不同粒度的校验（方向是枚举、列名是白名单），
     * 必须分开命名、分开演进——把 {@code order=asc} 交给列名校验会因
     * "asc 不是列名"而被误判为非法。
     *
     * @param direction 请求方向，如 {@code "asc"} / {@code "DESC "}
     * @param fallback  非法时的默认方向
     */
    public static String direction(String direction, String fallback) {
        if (direction == null) {
            return fallback;
        }
        String v = direction.trim().toLowerCase(java.util.Locale.ROOT);
        return ("asc".equals(v) || "desc".equals(v)) ? v : fallback;
    }

    /**
     * 便捷构造列名白名单：{@code OrderbyUtils.columns("id", "create_time", ...)}
     */
    public static Set<String> columns(String... names) {
        if (names == null || names.length == 0) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(names)));
    }

    /** 去掉形如 {@code a.} 的别名前缀，取裸列名 */
    private static String bareColumn(String column) {
        int i = column.lastIndexOf('.');
        return i < 0 ? column : column.substring(i + 1);
    }
}
