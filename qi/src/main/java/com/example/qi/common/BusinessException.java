package com.example.qi.common;

import lombok.Getter;

/**
 * 业务异常：校验失败、发布条件不满足等场景抛出，由全局异常处理器转为统一返回。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
