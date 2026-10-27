package com.gourmet.common.exception;

import com.gourmet.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常：由全局异常处理器统一转换为标准返回体。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.PARAM_ERROR.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public static BusinessException of(String message) {
        return new BusinessException(message);
    }
}
