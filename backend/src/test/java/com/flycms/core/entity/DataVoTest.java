package com.flycms.core.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 统一响应实体测试（阶段 K4）。
 *
 * <p>钉死 {@link DataVo} 的字段语义——历史上这里出过两次事故：
 * 构造器实参顺序写反（message 与 url 互换）、以及 data 为 null 时 toString NPE。
 * 存量 17 个控制器都依赖该语义，改动前必须被测试拦住。
 */
class DataVoTest {

    @Test
    @DisplayName("success(Object)：code=0，message 落在 message 字段（不是 url）")
    void successWithData() {
        DataVo vo = DataVo.success(List.of(1, 2, 3));
        assertEquals(DataVo.CODE_SUCCESS, vo.getCode());
        assertEquals("操作成功", vo.getMessage());
        assertNull(vo.getUrl(), "历史 Bug：成功消息曾被塞进 url 字段");
        assertNotNull(vo.getData());
    }

    @Test
    @DisplayName("success(String)：仅消息，data 为空数组占位")
    void successWithMessage() {
        DataVo vo = DataVo.success("保存成功");
        assertEquals(DataVo.CODE_SUCCESS, vo.getCode());
        assertEquals("保存成功", vo.getMessage());
        assertNull(vo.getUrl());
    }

    @Test
    @DisplayName("failure(String)：code=-1")
    void failure() {
        DataVo vo = DataVo.failure("参数错误");
        assertEquals(DataVo.CODE_FAILURED, vo.getCode());
        assertEquals("参数错误", vo.getMessage());
    }

    @Test
    @DisplayName("failure(int,String)：自定义业务码")
    void failureWithCode() {
        DataVo vo = DataVo.failure(40001, "查重不通过");
        assertEquals(40001, vo.getCode());
        assertEquals("查重不通过", vo.getMessage());
    }

    @Test
    @DisplayName("jump：message 与 url 各就各位")
    void jump() {
        DataVo vo = DataVo.jump("请登录", "/ucenter/login");
        assertEquals(DataVo.CODE_SUCCESS, vo.getCode());
        assertEquals("请登录", vo.getMessage());
        assertEquals("/ucenter/login", vo.getUrl());
    }

    @Test
    @DisplayName("toString 在 data=null 时不抛异常")
    void toStringSafeWithNullData() {
        DataVo vo = DataVo.failure("失败");
        assertNotNull(vo.toString());
    }
}
