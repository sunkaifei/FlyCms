package com.flycms.core.exception;

import com.flycms.core.entity.DataVo;

/**
 * 业务异常（阶段 K1 / G1）。
 *
 * <p>语义与 {@link DataVo#failure(String)} 完全一致：**HTTP 200 + {@code code != 0}**。
 * 引入它的意义是把"业务失败"从"到处 return DataVo.failure(...)"里抽出一个可抛出的载体，
 * 让深层调用（Service 层）也能表达业务失败，而不必层层透传 DataVo。
 *
 * <p>注意：**不要**用它表达"未登录/无权限"——那两类是 HTTP 语义（401/403），
 * 请继续抛 {@code ResponseStatusException}。
 *
 * @author sun-kaifei
 * @version 1.0
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = DataVo.CODE_FAILURED;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
