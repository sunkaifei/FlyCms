package com.flycms.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 权限通配符匹配测试（阶段 K4）。
 *
 * <p>{@link CheckUrlUtils#match} 是 {@code ApiBaseController.requirePermission} 的判定核心——
 * 它错了就是越权。这里把"应放行"和"必须拒绝"两侧都钉死。
 */
class CheckUrlUtilsTest {

    @Test
    @DisplayName("精确匹配：完全相同才放行")
    void exactMatch() {
        assertTrue(CheckUrlUtils.match("article:edit", "article:edit"));
        assertFalse(CheckUrlUtils.match("article:edit", "article:delete"));
        assertFalse(CheckUrlUtils.match("article", "article:edit"));
    }

    @Test
    @DisplayName("星号通配：article:* 覆盖全部动作，但不越界到别的模块")
    void starWildcard() {
        assertTrue(CheckUrlUtils.match("article:*", "article:edit"));
        assertTrue(CheckUrlUtils.match("article:*", "article:delete"));
        assertFalse(CheckUrlUtils.match("article:*", "channel:edit"));
        assertFalse(CheckUrlUtils.match("article:*", "question:edit"));
    }

    @Test
    @DisplayName("超级通配：单独一个 * 放行一切")
    void matchAll() {
        assertTrue(CheckUrlUtils.match("*", "article:edit"));
        assertTrue(CheckUrlUtils.match("*", "anything:at:all"));
    }

    @Test
    @DisplayName("问号通配：单字符占位")
    void questionMark() {
        assertTrue(CheckUrlUtils.match("a?c", "abc"));
        assertFalse(CheckUrlUtils.match("a?c", "abcc"));
    }

    @Test
    @DisplayName("空值安全：null 一律拒绝，不得抛异常")
    void nullSafety() {
        assertFalse(CheckUrlUtils.match(null, "article:edit"));
        assertFalse(CheckUrlUtils.match("article:edit", null));
        assertFalse(CheckUrlUtils.match(null, null));
    }

    @Test
    @DisplayName("前缀相同的模块名不得互相放行（article vs article_log）")
    void noPrefixLeak() {
        assertFalse(CheckUrlUtils.match("article:edit", "article_log:edit"));
        assertFalse(CheckUrlUtils.match("article", "articles"));
    }
}
