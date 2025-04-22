package org.tbox.dapper.client;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbox.dapper.context.TraceContext;
import org.tbox.dapper.core.TracerConstants;
import org.tbox.dapper.core.TracerMetricsCollector;
import org.tbox.dapper.config.TracerProperties;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Apache HttpClient追踪拦截器
 * 用于在HttpClient请求中添加追踪头信息，实现分布式追踪
 */
public class TracerHttpClientInterceptor {
    private static final Logger log = LoggerFactory.getLogger(TracerHttpClientInterceptor.class);
    
    // 用于存储请求开始时间的上下文属性名
    private static final String CONTEXT_START_TIME = "tbox.tracer.start_time";
    private static final String CONTEXT_URL = "tbox.tracer.url";
    private static final String CONTEXT_METHOD = "tbox.tracer.method";
    private static final String CONTEXT_TRACE_CONTEXT = "tbox.tracer.context";
    
    private final TracerProperties properties;
    private final TracerMetricsCollector metricsCollector;
    
    // 存储进行中的请求，用于异常情况下的清理（key为HttpContext hashCode）
    private final ConcurrentHashMap<Integer, TraceContext> activeRequests = new ConcurrentHashMap<>();
    
    public TracerHttpClientInterceptor(TracerProperties properties, TracerMetricsCollector metricsCollector) {
        this.properties = properties;
        this.metricsCollector = metricsCollector;
    }
    
    /**
     * 获取请求拦截器
     */
    public HttpRequestInterceptor getRequestInterceptor() {
        return new HttpRequestInterceptor() {
            @Override
            public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
                if (!properties.isEnabled()) {
                    return;
                }
                
                // 尝试从请求的URI中提取URL
                String url = "unknown";
                if (request.getRequestLine() != null && request.getRequestLine().getUri() != null) {
                    url = request.getRequestLine().getUri();
                }
                
                // 获取HTTP方法
                String method = "unknown";
                if (request.getRequestLine() != null && request.getRequestLine().getMethod() != null) {
                    method = request.getRequestLine().getMethod();
                }
                
                // 保存URL和方法到上下文中，以便在响应拦截器中使用
                context.setAttribute(CONTEXT_URL, url);
                context.setAttribute(CONTEXT_METHOD, method);
                
                // 获取当前追踪上下文
                TraceContext traceContext = TraceContext.getCurrentContext();
                
                if (traceContext != null) {
                    // 添加追踪头信息到请求中
                    request.addHeader(TracerConstants.HEADER_TRACE_ID, traceContext.getTraceId());
                    request.addHeader(TracerConstants.HEADER_SPAN_ID, traceContext.getSpanId());
                    if (traceContext.getParentSpanId() != null) {
                        request.addHeader(TracerConstants.HEADER_PARENT_SPAN_ID, traceContext.getParentSpanId());
                    }
                    request.addHeader(TracerConstants.HEADER_APP_NAME, properties.getApplicationName());
                    
                    if (log.isDebugEnabled()) {
                        log.debug("Added trace headers to HttpClient request: traceId={}, spanId={}, url={}",
                                traceContext.getTraceId(), traceContext.getSpanId(), url);
                    }
                    
                    // 记录请求开始时间和追踪上下文
                    context.setAttribute(CONTEXT_START_TIME, System.currentTimeMillis());
                    context.setAttribute(CONTEXT_TRACE_CONTEXT, traceContext);
                    activeRequests.put(context.hashCode(), traceContext);
                    
                    // 记录请求开始
                    metricsCollector.recordRequestStart(
                            method, 
                            url, 
                            TracerConstants.CLIENT_TYPE_HTTP_CLIENT, 
                            url
                    );
                } else {
                    log.debug("No active trace context found for HttpClient request to: {}", url);
                }
            }
        };
    }
    
    /**
     * 获取响应拦截器
     */
    public HttpResponseInterceptor getResponseInterceptor() {
        return new HttpResponseInterceptor() {
            @Override
            public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
                if (!properties.isEnabled() ) {
                    return;
                }
                
                // 从上下文中获取必要信息
                Long startTime = (Long) context.getAttribute(CONTEXT_START_TIME);
                String url = (String) context.getAttribute(CONTEXT_URL);
                String method = (String) context.getAttribute(CONTEXT_METHOD);
                TraceContext traceContext = (TraceContext) context.getAttribute(CONTEXT_TRACE_CONTEXT);
                
                if (startTime != null && traceContext != null) {
                    long duration = System.currentTimeMillis() - startTime;
                    int statusCode = response.getStatusLine() != null ? response.getStatusLine().getStatusCode() : -1;
                    boolean hasException = statusCode >= 400;
                    
                    // 记录请求结束
                    metricsCollector.recordRequestEnd(
                            method,
                            url,
                            TracerConstants.CLIENT_TYPE_HTTP_CLIENT,
                            url,
                            statusCode,
                            duration,
                            hasException
                    );
                    
                    if (log.isDebugEnabled()) {
                        log.debug("HttpClient request completed: url={}, status={}, duration={}ms, hasError={}",
                                url, statusCode, duration, hasException);
                    }
                    
                    // 清理活动请求
                    activeRequests.remove(context.hashCode());
                }
            }
        };
    }
    
    /**
     * 清理所有活动请求（通常在关闭应用时调用）
     */
    public void cleanup() {
        activeRequests.clear();
        log.debug("Cleaned up all active HttpClient requests");
    }
} 