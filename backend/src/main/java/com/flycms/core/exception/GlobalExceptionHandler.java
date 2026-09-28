package com.flycms.core.exception;

import com.flycms.core.entity.ErrorVo;
import com.flycms.filter.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 全局异常处理器（阶段 K1 / G1，P0）。
 *
 * <p><b>为什么需要它</b>：改造前全仓 {@code @ControllerAdvice} 0 命中，且
 * {@code Application} 主动 {@code exclude} 了 {@code ErrorMvcAutoConfiguration}——
 * API 抛未捕获异常时退化为 Jetty 容器 HTML 错误页，前端拿不到结构化 body，且生产环境有泄漏堆栈风险。
 *
 * <p><b>边界</b>：{@code basePackages = "com.flycms.web.api"} —— **只接管 API 控制器**，
 * 前台 {@code web.front} 继续走自己的 {@code /404 /403 /500} 主题渲染，行为不变。
 *
 * <p><b>契约</b>：
 * <ul>
 *   <li>401/403 保持 HTTP 状态码原样（前端 {@code request.ts} 依赖 401 触发重新登录）；</li>
 *   <li>参数校验失败 → HTTP 400 + {@code errors[]}；</li>
 *   <li>业务异常（{@link BusinessException}/{@link FlycmsException}）→ HTTP 200 + {@code code != 0}
 *       （与 {@code DataVo} 的存量语义一致）；</li>
 *   <li>未知异常 → HTTP 500，**message 固定不外泄堆栈**，细节只进日志（带 traceId）。</li>
 * </ul>
 *
 * @author sun-kaifei
 * @version 1.0
 */
@RestControllerAdvice(basePackages = "com.flycms.web.api")
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 承接 ApiBaseController.requireAdmin()/requirePermission() 抛出的 401/403
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorVo> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        ErrorVo vo = ErrorVo.of(ex, request.getRequestURI(), traceId());
        // 5xx 才需要完整堆栈，4xx 属预期分支
        if (vo.getStatus() >= 500) {
            logger.warn("ResponseStatusException [traceId={}] {} {}", vo.getTraceId(), vo.getPath(), ex.getMessage(), ex);
        } else {
            logger.debug("认证/授权失败 [traceId={}] {} -> {}", vo.getTraceId(), vo.getPath(), vo.getStatus());
        }
        return ResponseEntity.status(vo.getStatus()).body(vo);
    }

    /**
     * {@code @Valid} 校验失败（请求体）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorVo> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                HttpServletRequest request) {
        ErrorVo vo = new ErrorVo();
        vo.setStatus(HttpStatus.BAD_REQUEST.value());
        vo.setCode(ErrorVo.CODE_VALIDATION_FAILED);
        vo.setMessage("参数校验失败");
        vo.setPath(request.getRequestURI());
        vo.setTraceId(traceId());
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            vo.addError(fieldError.getField(), fieldError.getDefaultMessage());
        }
        logger.debug("参数校验失败 [traceId={}] {} -> {}", vo.getTraceId(), vo.getPath(), vo.getErrors());
        return ResponseEntity.badRequest().body(vo);
    }

    /**
     * 表单绑定校验失败（{@code @Valid} + ModelAttribute / 非请求体）
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorVo> handleBind(BindException ex, HttpServletRequest request) {
        ErrorVo vo = new ErrorVo();
        vo.setStatus(HttpStatus.BAD_REQUEST.value());
        vo.setCode(ErrorVo.CODE_VALIDATION_FAILED);
        vo.setMessage("参数校验失败");
        vo.setPath(request.getRequestURI());
        vo.setTraceId(traceId());
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            vo.addError(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(vo);
    }

    /**
     * 方法参数上的约束校验失败（{@code @Validated} + 方法级约束）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorVo> handleConstraintViolation(ConstraintViolationException ex,
                                                             HttpServletRequest request) {
        ErrorVo vo = new ErrorVo();
        vo.setStatus(HttpStatus.BAD_REQUEST.value());
        vo.setCode(ErrorVo.CODE_VALIDATION_FAILED);
        vo.setMessage("参数校验失败");
        vo.setPath(request.getRequestURI());
        vo.setTraceId(traceId());
        ex.getConstraintViolations().forEach(cv ->
                vo.addError(String.valueOf(cv.getPropertyPath()), cv.getMessage()));
        return ResponseEntity.badRequest().body(vo);
    }

    /**
     * 方法参数校验（Spring 6.1+ 对 @RequestParam/@PathVariable 的直接约束）
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorVo> handleHandlerMethodValidation(HandlerMethodValidationException ex,
                                                                 HttpServletRequest request) {
        ErrorVo vo = new ErrorVo();
        vo.setStatus(HttpStatus.BAD_REQUEST.value());
        vo.setCode(ErrorVo.CODE_VALIDATION_FAILED);
        vo.setMessage("参数校验失败");
        vo.setPath(request.getRequestURI());
        vo.setTraceId(traceId());
        for (var err : ex.getAllErrors()) {
            String[] codes = err.getCodes();
            String field = (codes != null && codes.length > 0) ? codes[codes.length - 1] : "param";
            vo.addError(field, err.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(vo);
    }

    /**
     * 请求体不可读 / 缺少必需参数 → 400
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorVo> handleBadRequest(Exception ex, HttpServletRequest request) {
        ErrorVo vo = new ErrorVo();
        vo.setStatus(HttpStatus.BAD_REQUEST.value());
        vo.setCode(ErrorVo.CODE_VALIDATION_FAILED);
        vo.setMessage(ex instanceof MissingServletRequestParameterException m
                ? "缺少必需参数：" + m.getParameterName()
                : "请求体格式不正确");
        vo.setPath(request.getRequestURI());
        vo.setTraceId(traceId());
        return ResponseEntity.badRequest().body(vo);
    }

    /**
     * 静态资源/路由未命中 → 404（结构化，而非容器 HTML 页）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorVo> handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        ErrorVo vo = new ErrorVo();
        vo.setStatus(HttpStatus.NOT_FOUND.value());
        vo.setCode(ErrorVo.CODE_NOT_FOUND);
        vo.setMessage("请求的资源不存在");
        vo.setPath(request.getRequestURI());
        vo.setTraceId(traceId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(vo);
    }

    /**
     * 业务异常 → HTTP 200 + 业务码（与 DataVo 语义一致，存量前端无需改动）
     */
    @ExceptionHandler({BusinessException.class, FlycmsException.class})
    public ResponseEntity<ErrorVo> handleBusiness(RuntimeException ex, HttpServletRequest request) {
        ErrorVo vo;
        if (ex instanceof BusinessException be) {
            vo = ErrorVo.of(be, request.getRequestURI(), traceId());
        } else {
            FlycmsException fe = (FlycmsException) ex;
            vo = ErrorVo.of(new BusinessException(fe.getId(), fe.getMessage()), request.getRequestURI(), traceId());
        }
        logger.debug("业务异常 [traceId={}] {} -> code={} msg={}", vo.getTraceId(), vo.getPath(), vo.getCode(),
                vo.getMessage());
        return ResponseEntity.ok(vo);
    }

    /**
     * 兜底：未预期异常 → 500，**不外泄堆栈**（细节只进日志）
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorVo> handleAll(Exception ex, HttpServletRequest request) {
        String traceId = traceId();
        logger.error("未捕获异常 [traceId={}] {}", traceId, request.getRequestURI(), ex);
        ErrorVo vo = ErrorVo.internal("服务器内部错误", request.getRequestURI(), traceId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(vo);
    }

    private String traceId() {
        String traceId = TraceIdFilter.currentTraceId();
        return traceId == null ? "-" : traceId;
    }
}
