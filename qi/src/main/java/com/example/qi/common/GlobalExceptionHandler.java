package com.example.qi.common;

import cn.dev33.satoken.exception.NotLoginException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：所有异常统一转为 Result，不向前端暴露堆栈。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** Sa-Token 未登录（ADM-01：未登录访问管理接口返回未授权提示） */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLogin(NotLoginException e) {
        return Result.fail(401, "未登录或登录已过期");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe == null ? "参数校验失败" : fe.getField() + " " + fe.getDefaultMessage();
        return Result.fail(400, msg);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleUploadSize(MaxUploadSizeExceededException e) {
        return Result.fail(400, "文件大小超出限制");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNotFound(NoResourceFoundException e) {
        return Result.fail(404, "资源不存在");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("未处理异常", e);
        return Result.fail(500, "服务器繁忙，请稍后重试");
    }
}
