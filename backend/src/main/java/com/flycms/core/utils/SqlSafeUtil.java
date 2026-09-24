package com.flycms.core.utils;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 动态 DDL/DML 的标识符与数字安全工具（自定义模型系统专用）。
 *
 * 进入动态 SQL 的标识符只有四条来源：
 * 1. 字段名：safeColumnName（正则白名单 + 保留字黑名单，调用侧再加反引号包裹）；
 * 2. 模型表名后缀（= 模型 code）：safeTableSuffix（标识符白名单 + 独立保留字黑名单，D7/D10）；
 * 3. modelId：safeNumber（纯数字校验，历史兼容）；
 * 4. 列类型：仅 FieldTypeEnum.resolveColumnType() 产物，绝不接收前端类型串。
 *
 * @author sun-kaifei
 * @version 1.1
 */
public final class SqlSafeUtil {

    private static final Pattern SAFE_NAME = Pattern.compile("^[a-z][a-z0-9_]{0,63}$");
    private static final Pattern SAFE_CODE = Pattern.compile("^[a-z][a-z0-9_]{1,31}$");
    private static final Pattern SAFE_NUMBER = Pattern.compile("^\\d{1,19}$");

    /**
     * 模型物理表名后缀白名单（D7）：小写字母开头，总长 2~32。
     * 与 SAFE_CODE 当前规则相同，但**安全边界不同**，故意独立命名（D10）：
     * safeModelCode 出错 → 路由 404；safeTableSuffix 出错 → SQL 注入 / DROP TABLE。
     */
    private static final Pattern SAFE_TABLE_SUFFIX = Pattern.compile("^[a-z][a-z0-9_]{1,31}$");

    /** MySQL 保留字与关键字黑名单（字段名黑名单） */
    private static final Set<String> RESERVED = Set.of(
            "order", "group", "desc", "asc", "select", "insert", "update", "delete", "where",
            "index", "key", "primary", "table", "values", "limit", "by", "as", "on", "join",
            "left", "right", "union", "having", "distinct", "default", "column", "database",
            "schema", "create", "drop", "alter");

    /**
     * 模型表名后缀保留字（D7/D10）。两部分：
     * 1. SQL 保留字（防语义歧义，与 RESERVED 同源但独立维护）；
     * 2. 项目既有表名后缀（防人为混淆：fly_cmodel_images 与 fly_images 并存时运维易误判）。
     */
    private static final Set<String> RESERVED_TABLE_SUFFIX = Set.of(
            // ---- SQL 保留字 ----
            "order", "group", "select", "insert", "update", "delete", "where", "index", "key",
            "table", "values", "limit", "union", "having", "distinct", "column", "database",
            "schema", "create", "drop", "alter", "join", "left", "right", "desc", "asc",
            // ---- 项目既有表名后缀（去 fly_ / fly_cmodel_ 前缀后的部分）----
            "admin", "admin_log", "admin_permission", "admin_group_permission_merge",
            "article", "article_category", "article_comment",
            "images", "model", "model_field", "model_category",
            "user", "config", "config_web", "channel", "block", "block_item",
            "template", "template_version", "form", "form_field", "form_data",
            "guide", "links", "job", "score", "score_detail", "score_rule", "message",
            "announcement", "share", "share_category", "share_comment",
            "question", "answer", "topic", "useraccount", "usergroup",
            "attachment", "captcha", "sms", "email", "log", "search");

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
     * 模型物理表名后缀白名单（D7/D10）。
     *
     * <p>生成物理表名 {@code fly_cmodel_{code}}，本方法校验 {@code code} 部分。
     * 与 {@link #safeModelCode(String)} 分离的理由：安全边界不同——前者出错影响面是
     * "路由 404 / 模板找不到"，本方法出错影响面是"SQL 注入 + DROP TABLE"。
     * 安全边界不同的校验必须独立命名、独立演进、独立测试。
     *
     * @param code 模型 code（同时是路由目录名、模板目录名、物理表名后缀）
     * @return 通过校验的 code
     * @throws IllegalArgumentException code 为 null、格式非法或命中保留字
     */
    public static String safeTableSuffix(String code) {
        if (code == null || !SAFE_TABLE_SUFFIX.matcher(code).matches()
                || RESERVED_TABLE_SUFFIX.contains(code)) {
            throw new IllegalArgumentException("非法模型标识（不能用作表名）：" + code);
        }
        return code;
    }

    /**
     * 表名后缀是否为保留字（不抛异常版本，供创建表单做友好提示）
     */
    public static boolean isReservedTableSuffix(String code) {
        return code != null && RESERVED_TABLE_SUFFIX.contains(code);
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
