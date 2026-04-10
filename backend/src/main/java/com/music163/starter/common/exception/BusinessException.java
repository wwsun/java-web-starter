package com.music163.starter.common.exception;

import com.music163.starter.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常基类
 * <p>
 * 业务逻辑中遇到不符合预期的情况直接抛出此异常，
 * 由 GlobalExceptionHandler 统一捕获并返回标准响应。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}
