package com.flycms.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 动态 SQL 标识符安全测试（阶段 K4）。
 *
 * <p>覆盖自定义模型系统唯一的四个"进 SQL"入口：字段名、表名后缀、数字、排序列。
 * 这些校验一旦被绕过就是 SQL 注入 + DROP TABLE 级别的事故，因此逐个钉死。
 */
class SqlSafeUtilTest {

    @Test
    @DisplayName("合法字段名放行，非法/保留字/撞固有列一律抛异常")
    void safeColumnName() {
        assertEquals("price", SqlSafeUtil.safeColumnName("price"));
        assertEquals("price_2", SqlSafeUtil.safeColumnName("price_2"));

        assertThrows(IllegalArgumentException.class, () -> SqlSafeUtil.safeColumnName("Price"));
        assertThrows(IllegalArgumentException.class, () -> SqlSafeUtil.safeColumnName("2price"));
        assertThrows(IllegalArgumentException.class, () -> SqlSafeUtil.safeColumnName("price-price"));
        assertThrows(IllegalArgumentException.class, () -> SqlSafeUtil.safeColumnName("select"));
        assertThrows(IllegalArgumentException.class, () -> SqlSafeUtil.safeColumnName("id"));
        assertThrows(IllegalArgumentException.class, () -> SqlSafeUtil.safeColumnName(null));
        // 注入尝试
        assertThrows(IllegalArgumentException.class, () -> SqlSafeUtil.safeColumnName("a; DROP TABLE fly_admin"));
        assertThrows(IllegalArgumentException.class, () -> SqlSafeUtil.safeColumnName("a`b"));
    }

    @Test
    @DisplayName("表名后缀：新建模型走全量黑名单，项目表名也被拦")
    void safeTableSuffix() {
        assertEquals("product", SqlSafeUtil.safeTableSuffix("product"));
        assertThrows(IllegalArgumentException.class, () -> SqlSafeUtil.safeTableSuffix("article"));
        assertThrows(IllegalArgumentException.class, () -> SqlSafeUtil.safeTableSuffix("select"));
        assertThrows(IllegalArgumentException.class, () -> SqlSafeUtil.safeTableSuffix("a"));
        assertThrows(IllegalArgumentException.class, () -> SqlSafeUtil.safeTableSuffix("../../etc"));
    }

    @Test
    @DisplayName("存量模型：只拦 SQL 保留字，项目表名豁免（images 种子模型可用）")
    void safeTableSuffixForExisting() {
        assertEquals("images", SqlSafeUtil.safeTableSuffixForExisting("images"));
        assertThrows(IllegalArgumentException.class, () -> SqlSafeUtil.safeTableSuffixForExisting("select"));
        assertThrows(IllegalArgumentException.class, () -> SqlSafeUtil.safeTableSuffixForExisting("drop"));
        // 新建路径仍然拦 images
        assertTrue(SqlSafeUtil.isReservedTableSuffix("images"));
    }

    @Test
    @DisplayName("数字参数：纯数字放行，其余抛异常")
    void safeNumber() {
        assertEquals("123", SqlSafeUtil.safeNumber(123));
        assertEquals("123", SqlSafeUtil.safeNumber("123"));
        assertThrows(IllegalArgumentException.class, () -> SqlSafeUtil.safeNumber("1 OR 1=1"));
        assertThrows(IllegalArgumentException.class, () -> SqlSafeUtil.safeNumber("-1"));
        assertThrows(IllegalArgumentException.class, () -> SqlSafeUtil.safeNumber(null));
    }

    @Test
    @DisplayName("排序列：固有列可排，注入串被拒")
    void safeOrderColumn() {
        assertEquals("create_time", SqlSafeUtil.safeOrderColumn("create_time"));
        assertThrows(IllegalArgumentException.class,
                () -> SqlSafeUtil.safeOrderColumn("create_time; DROP TABLE fly_admin"));
        assertThrows(IllegalArgumentException.class, () -> SqlSafeUtil.safeOrderColumn("order"));
    }

    @Test
    @DisplayName("模型 code：格式白名单 + 保留字")
    void safeModelCode() {
        assertEquals("product", SqlSafeUtil.safeModelCode("product"));
        assertThrows(IllegalArgumentException.class, () -> SqlSafeUtil.safeModelCode("Product"));
        assertThrows(IllegalArgumentException.class, () -> SqlSafeUtil.safeModelCode("a"));
        assertFalse(SqlSafeUtil.isReservedTableSuffix("product"));
    }
}
