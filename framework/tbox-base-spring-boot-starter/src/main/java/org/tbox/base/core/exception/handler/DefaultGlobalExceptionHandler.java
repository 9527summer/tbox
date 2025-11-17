package org.tbox.base.core.exception.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.tbox.base.core.enums.StandardErrorCodeEnum;
import org.tbox.base.core.exception.BizException;
import org.tbox.base.core.exception.RepeatConsumptionException;
import org.tbox.base.core.exception.SysException;
import org.tbox.base.core.response.Result;

import javax.servlet.http.HttpServletRequest;
import java.util.StringJoiner;

//@RestControllerAdvice
public class DefaultGlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(DefaultGlobalExceptionHandler.class);


    /**
     * 处理自定义业务异常
     */
    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBizException(BizException e, HttpServletRequest request) {
        log.warn("业务异常: {}, URL: {}", e.getMessage(), request.getRequestURI(), e);
        return buildErrorResponse(
                e.getErrCode() != null ? e.getErrCode() : StandardErrorCodeEnum.BIZ_ERROR.getCode(),
                e.getMessage(),
                request
        );
    }

    /**
     * 处理系统异常
     */
    @ExceptionHandler(SysException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleSysException(SysException e, HttpServletRequest request) {
        log.error("系统异常: {}, URL: {}", e.getMessage(), request.getRequestURI(), e);
        return buildErrorResponse(
                e.getErrCode() != null ? e.getErrCode() : StandardErrorCodeEnum.SYSTEM_ERROR.getCode(),
                e.getMessage(),
                request
        );
    }

    /**
     * 处理重复消费异常
     */
    @ExceptionHandler(RepeatConsumptionException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleRepeatConsumptionException(RepeatConsumptionException e, HttpServletRequest request) {
        log.warn("重复消费异常: {}, URL: {}", e.getMessage(), request.getRequestURI(), e);
        return buildErrorResponse(
                e.getErrCode() != null ? e.getErrCode() : StandardErrorCodeEnum.REPEAT_CONSUMER_ERROR.getCode(),
                e.getMessage(),
                request
        );
    }

    /**
     * 处理绑定异常（form表单提交）
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("参数绑定失败");
        log.warn("参数绑定异常: {}, URL: {}", message, request.getRequestURI());
        return buildErrorResponse(
                StandardErrorCodeEnum.PARAMETER_ERROR.getCode(),
                message,
                request
        );
    }

    /**
     * 处理JSON解析异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("JSON解析异常: {}, URL: {}", e.getMessage(), request.getRequestURI());
        return buildErrorResponse(
                StandardErrorCodeEnum.PARAMETER_ERROR.getCode(),
                "请求体格式不正确",
                request
        );
    }

    /**
     * 处理请求参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.warn("参数类型不匹配异常: {}, URL: {}", e.getMessage(), request.getRequestURI());
        return buildErrorResponse(
                StandardErrorCodeEnum.PARAMETER_ERROR.getCode(),
                "参数 '" + e.getName() + "' 类型不匹配，应为: " + (e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知"),
                request
        );
    }

    /**
     * 处理缺少必要参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("缺少必要参数异常: {}, URL: {}", e.getMessage(), request.getRequestURI());
        return buildErrorResponse(
                StandardErrorCodeEnum.PARAMETER_ERROR.getCode(),
                "缺少必要参数: " + e.getParameterName(),
                request
        );
    }

    /**
     * 处理请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        StringJoiner joiner = new StringJoiner(", ");
        if (e.getSupportedMethods() != null) {
            for (String method : e.getSupportedMethods()) {
                joiner.add(method);
            }
        }
        log.warn("请求方法不支持异常: {}, URL: {}, 支持的方法: {}", e.getMessage(), request.getRequestURI(), joiner);
        return buildErrorResponse(
                StandardErrorCodeEnum.NOT_FOUND.getCode(),
                "不支持 " + e.getMethod() + " 请求方法，支持的方法: " + joiner,
                request
        );
    }

    /**
     * 处理媒体类型不支持异常
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<Void> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        log.warn("媒体类型不支持异常: {}, URL: {}", e.getMessage(), request.getRequestURI());
        return buildErrorResponse(
                StandardErrorCodeEnum.PARAMETER_ERROR.getCode(),
                "不支持的媒体类型: " + e.getContentType(),
                request
        );
    }

    /**
     * 处理资源未找到异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("资源未找到异常: {}, URL: {}, 方法: {}", e.getMessage(), e.getRequestURL(), e.getHttpMethod());
        return buildErrorResponse(
                StandardErrorCodeEnum.NOT_FOUND.getCode(),
                "找不到资源: " + e.getRequestURL(),
                request
        );
    }

    /**
     * 处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("未知异常: {}, URL: {}", e.getMessage(), request.getRequestURI(), e);
        return buildErrorResponse(
                StandardErrorCodeEnum.SYSTEM_ERROR.getCode(),
                "系统繁忙，请稍后再试",
                request
        );
    }

    /**
     * 构建错误响应
     */
    private Result<Void> buildErrorResponse(String code, String message, HttpServletRequest request) {
        Result<Void> result = new Result<>();
        result.setCode(code);
        result.setText(message);
        // 如果有日志追踪系统，可以设置requestId
        // result.setRequestId(MDC.get("traceId"));
        return result;
    }
}
