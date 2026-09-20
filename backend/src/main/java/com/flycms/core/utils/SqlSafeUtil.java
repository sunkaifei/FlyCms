package com.flycms.core.utils;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 动态 DDL/DML 的标识符与数字安全工具（自定义模型系统专用）。
 *
 * 进入动态 SQL 的标识符只有三条来源：
 * 1. 字段名：safeColumnName（正则白名单 + 保留字黑名单，调用侧再加反引号包裹）；
 * 2. modelId：safeNumber（纯数字校验）；
 * 3. 列类型：仅 FieldTypeEnum.resolveColumnType() 产物，绝不接收前端类型串。
 *
 * @author sun-kaifei
 * @version 1.0
 */
public final class SqlSafeUtil {

    private static final Pattern SAFE_NAME = Pattern.compile("^[a-z][a-z0-9_]{0,63}$");
    private static final Pattern SAFE_CODE = Pattern.compile("^[a-z][a-z0-9_]{1,31}$");
    private static final Pattern SAFE_NUMBER = Pattern.compile("^\\d{1,19}$");

    /** MySQL 保留字与关键字黑名单（字段名黑名单） */
    private static final Set<String> RESERVED = Set.of(
            "order", "group", "desc", "asc", "select", "insert", "update", "delete", "where",
            "index", "key", "primary", "table", "values", "limit", "by", "as", "on", "join",
            "left", "right", "union", "having", "distinct", "default", "column", "database",
            "schema", "create", "drop", "alter");

    /** 模型数据表的固有列名，自定义字段不允许撞名 */
    private static final Set<String> RESERVED_COLUMNS = Set.of(
            "id", "short_url", "user_id", "category_id", "title", "content", "keywords",
            "description", "thumbnail", "recommend", "count_view", "count_comment",
            "status", "create_time", "update_time");

    private SqlSafeUtil() {
    }

    /**
     * 自定义字段名（= 列名）白名单校验，非法直接抛 IllegalArgumentException
     */
    public static String safeColumnName(String name) {
        if (name == null || !SAFE_NAME.matcher(name).matches() || RESERVED.contains(name)
                || RESERVED_COLUMNS.contains(name)) {
            throw new IllegalArgumentException("非法字段名：" + name);
        }
        return name;
    }

    /**
     * 雪花 ID / modelId 等拼接进 SQL 的数字强校验
     */
    public static String safeNumber(Object v) {
        if (v == null || !SAFE_NUMBER.matcher(String.valueOf(v)).matches()) {
            throw new IllegalArgumentException("非法数字参数：" + v);
        }
        return String.valueOf(v);
    }

    /**
     * 模型标识校验（用于前台路由 /{code}/ 与模板目录）
     */
    public static String safeModelCode(String code) {
        if (code == null || !SAFE_CODE.matcher(code).matches() || RESERVED.contains(code)) {
            throw new IllegalArgumentException("非法模型标识：" + code);
        }
        return code;
    }

    /**
     * 排序列名白名单：仅允许固有列或已注册的自定义字段名（复用 safeColumnName 规则，
     * 但固有列允许参与排序）
     */
    public static String safeOrderColumn(String name) {
        if (name == null || !SAFE_NAME.matcher(name).matches() || RESERVED.contains(name)) {
            throw new IllegalArgumentException("非法排序字段：" + name);
        }
        return name;
    }
}
