package com.flycms.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ORDER BY 白名单校验测试（2026-10-08 加固）。
 *
 * <p>排序片段经 MyBatis {@code ${}} 直拼 SQL，是本站少数无法预编译的位置之一，
 * 因此把「经典注入」与「按敏感列排序的侧信道」两类风险都钉死在测试里。
 */
class OrderbyUtilsTest {

    private static final Set<String> USER_COLUMNS = OrderbyUtils.columns(
            "user_id", "user_name", "nick_name", "create_time", "status");

    @Test
    @DisplayName("结构合法且在白名单内：原样放行（方向归一化为小写）")
    void allowsValidFragment() {
        assertEquals("user_id desc", OrderbyUtils.check("user_id DESC", "user_id", USER_COLUMNS));
        assertEquals("create_time desc, user_id asc",
                OrderbyUtils.check("create_time desc, user_id asc", "user_id", USER_COLUMNS));
        assertEquals("user_id asc", OrderbyUtils.check("  user_id   asc  ", "user_id", USER_COLUMNS));
    }

    @Test
    @DisplayName("越权列（含别名前缀绕过）一律回落默认值")
    void rejectsSensitiveColumn() {
        // fly_user 有 password 列：只靠字符白名单就能 ORDER BY password，构成排序侧信道
        assertEquals("user_id", OrderbyUtils.check("password", "user_id", USER_COLUMNS));
        assertEquals("user_id", OrderbyUtils.check("u.password", "user_id", USER_COLUMNS));
        assertEquals("user_id", OrderbyUtils.check("password desc", "user_id", USER_COLUMNS));
        // 白名单里只放行裸列名，因此别名写法的合法列同样按裸名匹配
        assertEquals("a.score", OrderbyUtils.check("a.score", "id",
                OrderbyUtils.columns("id", "score")));
    }

    @Test
    @DisplayName("非法结构：引号/括号/分号/注释/子查询/函数调用一律回落")
    void rejectsInjectionShapes() {
        String fallback = "id";
        String[] injections = {
                "id; DROP TABLE fly_user",
                "id, (select 1)",
                "id`",
                "id'",
                "updatexml(1,concat(0x7e,version()),1)",
                "if(1=1,sleep(5),0)",
                "id desc -- comment",
                "id /* comment */",
                "id\nunion select 1",
                "1) OR (1=1",
        };
        for (String bad : injections) {
            assertEquals(fallback, OrderbyUtils.check(bad, fallback, USER_COLUMNS), "应拒绝：" + bad);
            // 无白名单的结构版同样要拒绝
            assertEquals(fallback, OrderbyUtils.check(bad, fallback), "应拒绝(结构版)：" + bad);
        }
    }

    @Test
    @DisplayName("空值/空串走默认值；无白名单时只做结构校验")
    void handlesNullAndStructureOnly() {
        assertEquals("id", OrderbyUtils.check(null, "id", USER_COLUMNS));
        assertEquals("id", OrderbyUtils.check("", "id", USER_COLUMNS));
        assertEquals("id", OrderbyUtils.check("   ", "id", USER_COLUMNS));
        // 无白名单：结构合法即放行（兼容无法枚举列的场景）
        assertEquals("other_col desc", OrderbyUtils.check("other_col desc", "id"));
    }

    @Test
    @DisplayName("方向校验只放行 asc/desc（不能拿列名校验来判方向）")
    void directionWhitelist() {
        assertEquals("asc", OrderbyUtils.direction("ASC", "desc"));
        assertEquals("desc", OrderbyUtils.direction(" desc ", "asc"));
        assertEquals("desc", OrderbyUtils.direction("user_id", "desc"));
        assertEquals("desc", OrderbyUtils.direction(null, "desc"));
        assertEquals("asc", OrderbyUtils.direction("; drop table x", "asc"));
    }
}
