package com.example.tboxdemo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tbox.base.core.exception.BizException;
import org.tbox.base.core.response.Result;
import org.tbox.base.core.response.Results;

/**
 * 用于验证 WebTraceAspect 是否会在一次请求内打印多次日志。
 *
 * 说明：
 * - /error-dispatch：抛出 Error（非 Exception），绕开 @ExceptionHandler(Exception.class)，触发 Spring Boot 的 /error 二次分发；
 *   因为 BasicErrorController 也是 @Controller，会再次被 WebTraceAspect 拦截，从而出现“同一 trace 打两次日志”。
 * - /biz-exception：抛出 BizException，会被全局异常处理器捕获并返回，不会触发 /error 分发，通常只打印一次。
 */
@RestController
@RequestMapping("/api/trace")
public class TraceVerifyController {

    @PostMapping("/error-dispatch")
    public Result<Void> errorDispatch(@RequestBody TraceVerifyRequest request) {
        throw new Error("force /error dispatch");
    }

    @PostMapping("/biz-exception")
    public Result<Void> bizException(@RequestBody TraceVerifyRequest request) {
        throw new BizException("force biz exception");
    }

    @PostMapping("/ok")
    public Result<TraceVerifyRequest> ok(@RequestBody TraceVerifyRequest request) {
        return Results.success(request);
    }
}

