package com.flycms.core.entity;

import com.flycms.core.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 统一错误信封（阶段 K1 / G1）。
 *
 * <p>与 {@link DataVo} 的分工（**双轨但边界清晰**）：
 * <ul>
 *   <li><b>业务失败</b>走 {@code DataVo}（HTTP 200 + {@code code=-1}）—— 存量 17 个控制器零改动；</li>
 *   <li><b>异常路径</b>走 {@code ErrorVo}（HTTP 4xx/5xx + 结构化 body）—— 由
 *       {@code core/exception/GlobalExceptionHandler} 产出。</li>
 * </ul>
 *
 * <p>字段与 2026 REST 通行实践对齐：{@code code / message / status / path / traceId / errors / timestamp}。
 * {@code traceId} 来自 MDC（{@code filter/TraceIdFilter} 注入），与响应头 {@code X-Trace-Id} 同值，
 * 便于前端报错时直接抄给后端定位日志。
 *
 * @author sun-kaifei
 * @version 1.0
 */
public class ErrorVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 参数校验失败 */
    public static final int CODE_VALIDATION_FAILED = 40001;
    /** 未认证 / 登录态失效 */
    public static final int CODE_UNAUTHORIZED = 40101;
    /** 无权限 */
    public static final int CODE_FORBIDDEN = 40301;
    /** 资源不存在 */
    public static final int CODE_NOT_FOUND = 40401;
    /** 服务器内部错误 */
    public static final int CODE_INTERNAL_ERROR = 50000;

    /** 业务错误码（见上方常量；非 HTTP 状态码） */
    private int code;
    /** 供人阅读的错误信息（生产环境不含堆栈） */
    private String message;
    /** HTTP 状态码 */
    private int status;
    /** 请求路径 */
    private String path;
    /** 链路追踪 id */
    private String traceId;
    /** 字段级错误明细（参数校验失败时才有） */
    private List<FieldError> errors;
    /** ISO-8601 时间戳 */
    private String timestamp;

    public ErrorVo() {
        this.timestamp = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * 字段级错误明细
     */
    public static class FieldError implements Serializable {
        private static final long serialVersionUID = 1L;
        private String field;
        private String message;

        public FieldError() {
        }

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * 由 ResponseStatusException 构建（承接 requireAdmin/requirePermission 的 401/403）。
     * HTTP 状态码保持原样——前端 request.ts 依赖 401 触发重新登录。
     */
    public static ErrorVo of(ResponseStatusException ex, String path, String traceId) {
        int status = ex.getStatusCode().value();
        ErrorVo vo = new ErrorVo();
        vo.status = status;
        vo.code = switch (status) {
            case 401 -> CODE_UNAUTHORIZED;
            case 403 -> CODE_FORBIDDEN;
            case 404 -> CODE_NOT_FOUND;
            default -> status >= 500 ? CODE_INTERNAL_ERROR : 40000 + status;
        };
        vo.message = defaultMessage(status, ex.getReason());
        vo.path = path;
        vo.traceId = traceId;
        return vo;
    }

    /**
     * 由业务异常构建（HTTP 200 + 业务码，与 DataVo 语义一致）
     */
    public static ErrorVo of(BusinessException ex, String path, String traceId) {
        ErrorVo vo = new ErrorVo();
        vo.status = HttpStatus.OK.value();
        vo.code = ex.getCode();
        vo.message = ex.getMessage();
        vo.path = path;
        vo.traceId = traceId;
        return vo;
    }

    /**
     * 兜底错误（500，**不外泄堆栈**）
     */
    public static ErrorVo internal(String message, String path, String traceId) {
        ErrorVo vo = new ErrorVo();
        vo.status = HttpStatus.INTERNAL_SERVER_ERROR.value();
        vo.code = CODE_INTERNAL_ERROR;
        vo.message = message;
        vo.path = path;
        vo.traceId = traceId;
        return vo;
    }

    private static String defaultMessage(int status, String reason) {
        if (reason != null && !reason.isBlank()) {
            return reason;
        }
        return switch (status) {
            case 401 -> "未登录或登录态已失效";
            case 403 -> "无权访问该资源";
            case 404 -> "请求的资源不存在";
            case 405 -> "请求方法不被支持";
            default -> status >= 500 ? "服务器内部错误" : "请求未被接受";
        };
    }

    public void addError(String field, String message) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(new FieldError(field, message));
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public List<FieldError> getErrors() {
        return errors;
    }

    public void setErrors(List<FieldError> errors) {
        this.errors = errors;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
