package org.tbox.dapper.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.tbox.dapper.context.TraceContext;
import org.tbox.dapper.config.TracerProperties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Web请求追踪拦截器
 * 负责创建和管理每个HTTP请求的追踪上下文
 */
public class TracerWebInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(TracerWebInterceptor.class);

    private final TracerProperties properties;

    // HTTP头部信息常量
    private static final String HEADER_TRACE_ID = "X-Trace-ID";
    private static final String HEADER_SPAN_ID = "X-Span-ID";
    private static final String HEADER_PARENT_SPAN_ID = "X-Parent-Span-ID";
    private static final String HEADER_APP_NAME = "X-App-Name";
    
    public TracerWebInterceptor(TracerProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){
        if (!properties.isEnabled() || shouldSkip(request)) {
            return true;
        }
        
        try {
            // 获取请求头中的追踪信息
            String traceId = request.getHeader(HEADER_TRACE_ID);
            String spanId = request.getHeader(HEADER_SPAN_ID);
            String parentSpanId = request.getHeader(HEADER_PARENT_SPAN_ID);
            String appName = request.getHeader(HEADER_APP_NAME);
            
            TraceContext context;
            // 如果有传入的traceId，则使用它，否则创建新的根上下文
            if (traceId != null) {
                context = TraceContext.createFromExternalContext(
                        traceId, 
                        spanId, 
                        parentSpanId, 
                        properties.getApplicationName()
                );
            } else {
                context = TraceContext.createRootContext(properties.getApplicationName());
            }
            
            // 设置请求属性
            context.setAttribute("http.method", request.getMethod());
            context.setAttribute("http.uri", request.getRequestURI());
            context.setAttribute("http.query", request.getQueryString());
            context.setAttribute("http.remote_addr", request.getRemoteAddr());
            context.setAttribute("http.user_agent", request.getHeader("User-Agent"));
            
            // 添加追踪ID到响应头
            response.addHeader(HEADER_TRACE_ID, context.getTraceId());
            response.addHeader(HEADER_SPAN_ID, context.getSpanId());
            
            // 发送请求开始事件到度量收集器
            if (handler instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                String controllerName = handlerMethod.getBeanType().getSimpleName();
                String methodName = handlerMethod.getMethod().getName();
                
                context.setAttribute("controller", controllerName);
                context.setAttribute("method", methodName);
            }
            TraceContext.setCurrentContext(context);
        } catch (Exception e) {
            log.error("Error in TracerWebInterceptor preHandle", e);
        }
        
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 不做任何处理
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (!properties.isEnabled() || shouldSkip(request)) {
            return;
        }
        
        try {
            TraceContext context = TraceContext.getCurrentContext();
            if (context != null) {
                // 设置响应属性
                context.setAttribute("http.status", String.valueOf(response.getStatus()));
                
                // 完成上下文并发送请求结束事件到度量收集器
                context.complete();

                // 最后清理上下文
                TraceContext.removeContext();
            }
        } catch (Exception e) {
            log.error("Error in TracerWebInterceptor afterCompletion", e);
        }
    }
    
    /**
     * 判断是否跳过此请求的追踪
     */
    private boolean shouldSkip(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String[] allExcludePaths = properties.getAllExcludePaths();
        if (allExcludePaths != null && allExcludePaths.length > 0) {
            return Arrays.stream(allExcludePaths)
                    .anyMatch(pattern -> {
                        if (pattern.endsWith("/**")) {
                            String prefix = pattern.substring(0, pattern.length() - 3);
                            return uri.startsWith(prefix);
                        } else {
                            return uri.equals(pattern);
                        }
                    });
        }
        return false;
    }
}