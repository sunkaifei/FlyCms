package com.flycms.core.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 统一错误信封测试（阶段 K1 / K4）。
 *
 * <p>K1 的核心契约：业务失败走 200+业务码，认证/授权失败保持 401/403（前端依赖它触发重新登录），
 * 未知异常走 500 且不外泄堆栈。任何一条被改动都会破坏前端行为，故用测试固定。
 */
class ErrorVoTest {

    @Test
    @DisplayName("401 → status=401，code=40101")
    void unauthorized() {
        ErrorVo vo = ErrorVo.of(new ResponseStatusException(HttpStatus.UNAUTHORIZED), "/api/x", "t1");
        assertEquals(401, vo.getStatus());
        assertEquals(ErrorVo.CODE_UNAUTHORIZED, vo.getCode());
        assertEquals("t1", vo.getTraceId());
        assertEquals("/api/x", vo.getPath());
    }

    @Test
    @DisplayName("403 → status=403，code=40301")
    void forbidden() {
        ErrorVo vo = ErrorVo.of(new ResponseStatusException(HttpStatus.FORBIDDEN), "/api/x", "t2");
        assertEquals(403, vo.getStatus());
        assertEquals(ErrorVo.CODE_FORBIDDEN, vo.getCode());
    }

    @Test
    @DisplayName("业务异常 → HTTP 200 + 业务码（与 DataVo 语义一致）")
    void business() {
        ErrorVo vo = ErrorVo.of(new com.flycms.core.exception.BusinessException("标题重复"), "/api/x", "t3");
        assertEquals(200, vo.getStatus());
        assertEquals(DataVo.CODE_FAILURED, vo.getCode());
        assertEquals("标题重复", vo.getMessage());
    }

    @Test
    @DisplayName("兜底 500：message 固定，不含堆栈关键字")
    void internal() {
        ErrorVo vo = ErrorVo.internal("服务器内部错误", "/api/x", "t4");
        assertEquals(500, vo.getStatus());
        assertEquals(ErrorVo.CODE_INTERNAL_ERROR, vo.getCode());
        assertTrue(vo.getMessage().indexOf("Exception") < 0);
    }

    @Test
    @DisplayName("字段级错误可追加，timestamp 自动生成")
    void fieldErrors() {
        ErrorVo vo = ErrorVo.internal("x", "/api/x", "t5");
        vo.addError("title", "不能为空");
        vo.addError("price", "必须为正数");
        assertEquals(2, vo.getErrors().size());
        assertEquals("title", vo.getErrors().get(0).getField());
        assertNotNull(vo.getTimestamp());
    }
}
