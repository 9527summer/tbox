package org.tbox.dapper.web.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.tbox.base.core.exception.SysException;
import org.tbox.base.core.utils.JsonUtils;
import org.tbox.dapper.config.TracerProperties;
import org.tbox.dapper.context.TraceContext;
import org.tbox.dapper.utils.PathMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Web接口入参和出参日志记录切面
 * 记录Controller层接口的请求参数和响应结果
 */
@Aspect
@Order(10)
public class WebTraceAspect {
    private static final Logger log = LoggerFactory.getLogger(WebTraceAspect.class);
    private static final String REQ_PREFIX = "请求参数";
    private static final String RESP_PREFIX = "响应结果";

    @Autowired
    private TracerProperties tracerProperties;

    /**
     * 定义切点，匹配所有Controller方法
     */
    @Pointcut("@within(org.springframework.stereotype.Controller) || " +
            "@within(org.springframework.web.bind.annotation.RestController)")
    public void controllerPointcut() {
    }

    /**
     * 环绕通知，记录请求参数和响应结果
     *
     * @param joinPoint 切点
     * @return 方法执行结果
     * @throws Throwable 可能抛出的异常
     */
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 检查追踪功能是否开启
        if (!tracerProperties.isEnabled() || !tracerProperties.isPrintPayload()) {
            return joinPoint.proceed();
        }
        
        HttpServletRequest request = getCurrentRequest();
        if (request == null || isExcludedPath(request.getRequestURI())) {
            return joinPoint.proceed();
        }

        Object[] args = joinPoint.getArgs();
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String methodName = joinPoint.getSignature().getDeclaringType().getSimpleName() + "." + method.getName();
        
        String clientIp = getClientIp(request);
        String requestMethod = request.getMethod();
        String uri = request.getRequestURI();
        Object[] filteredArgs = filterArgs(args);
        
        // 记录请求参数
        logRequest(methodName, clientIp, requestMethod, uri, filteredArgs);
        
        try {
            // 执行目标方法
            Object result = joinPoint.proceed();
            
            // 从TraceContext获取耗时信息
            long executionTime = getExecutionTime();
            
            // 记录响应结果和执行时间
            logResponse(methodName, result, executionTime);
            return result;
        } catch (Exception e) {
            // 从TraceContext获取耗时信息
            long executionTime = getExecutionTime();
            
            // 记录异常
            logError(methodName, e, executionTime);
            throw e;
        }
    }
    
    /**
     * 从TraceContext获取当前执行时间
     * 如果无法获取TraceContext，则返回0
     */
    private long getExecutionTime() {
        TraceContext context = TraceContext.getCurrentContext();
        return context != null ? context.getDuration() : 0;
    }

    /**
     * 记录请求参数
     */
    private void logRequest(String methodName, String clientIp, String requestMethod, String uri, Object[] args) {
        try {
            String argsJson = JsonUtils.toJson(args);
            String message = String.format("[%s] 接口:%s %s | IP:%s | %s: %s", 
                    methodName, requestMethod, uri, clientIp, REQ_PREFIX, argsJson);
            logWithLevel(message);
        } catch (Exception e) {
            log.warn("[{}] 序列化请求参数失败: {}", methodName, e.getMessage());
        }
    }

    /**
     * 记录响应结果
     */
    private void logResponse(String methodName, Object result, long executionTime) {
        try {
            String resultJson = formatResult(result);
            String message = String.format("[%s] %s | 耗时: %dms | %s: %s", 
                    methodName, RESP_PREFIX, executionTime, RESP_PREFIX, resultJson);
            logWithLevel(message);
        } catch (Exception e) {
            log.warn("[{}] 序列化响应结果失败: {}", methodName, e.getMessage());
        }
    }

    /**
     * 记录异常
     */
    private void logError(String methodName, Exception e, long executionTime) {
        log.error("[{}] 接口异常 | 耗时: {}ms | 异常: {}", methodName, executionTime, e.getMessage(), e);
    }

    /**
     * 按照DEBUG级别记录日志
     */
    private void logWithLevel(String message) {
        log.info(message);
    }

    /**
     * 格式化响应结果，并根据配置截断长文本
     */
    private String formatResult(Object result) {
        if (result == null) {
            return "null";
        }
        
        try {
            String resultJson = JsonUtils.toJson(result);
            int maxLength = tracerProperties.getMaxResponseLength();
            
            if (maxLength > 0 && resultJson.length() > maxLength) {
                return resultJson.substring(0, maxLength) + "... (省略" + (resultJson.length() - maxLength) + "字符)";
            }
            
            return resultJson;
        } catch (Exception e) {
            throw new SysException("序列化响应结果失败", e);
        }
    }

    /**
     * 获取当前HTTP请求
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 检查是否为排除的路径
     */
    private boolean isExcludedPath(String path) {
        String[] allExcludePaths = tracerProperties.getAllExcludePaths();
        if (allExcludePaths == null || allExcludePaths.length == 0) {
            return false;
        }
        
        for (String pattern : allExcludePaths) {
            if (PathMatcher.match(pattern, path)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 过滤参数，排除HttpServletRequest、HttpServletResponse和MultipartFile等类型
     */
    private Object[] filterArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return new Object[0];
        }
        
        List<Object> filteredArgs = new ArrayList<>();
        for (Object arg : args) {
            if (arg instanceof HttpServletRequest || 
                arg instanceof HttpServletResponse || 
                arg instanceof MultipartFile ||
                arg instanceof MultipartFile[]) {
                continue;
            }
            filteredArgs.add(arg);
        }
        
        return filteredArgs.toArray();
    }
} 