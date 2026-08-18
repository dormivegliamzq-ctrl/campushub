package com.campushub.common;

import lombok.Getter;

/**
 * 业务异常：service 层主动抛它，由全局异常处理器统一转成 Result 返回
 */
@Getter
public class BizException extends RuntimeException {

    private final ResultCode resultCode;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}
