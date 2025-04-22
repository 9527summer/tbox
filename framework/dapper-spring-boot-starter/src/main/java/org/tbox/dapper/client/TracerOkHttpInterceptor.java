package org.tbox.dapper.client;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbox.dapper.context.TraceContext;
import org.tbox.dapper.core.TracerConstants;
import org.tbox.dapper.core.TracerMetricsCollector;
import org.tbox.dapper.config.TracerProperties;

import java.io.IOException;

/**
 * OkHttp追踪拦截器
 * 用于在OkHttp请求中添加追踪头信息，实现分布式追踪
 */
public class TracerOkHttpInterceptor implements Interceptor {
    private static final Logger log = LoggerFactory.getLogger(TracerOkHttpInterceptor.class);
    
    private final TracerProperties properties;
    private final TracerMetricsCollector metricsCollector;
    
    public TracerOkHttpInterceptor(TracerProperties properties, TracerMetricsCollector metricsCollector) {
        this.properties = properties;
        this.metricsCollector = metricsCollector;
    }
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        if (!properties.isEnabled()) {
            return chain.proceed(chain.request());
        }
        
        Request originalRequest = chain.request();
        String url = originalRequest.url().toString();
        String method = originalRequest.method();
        
        // 获取当前追踪上下文
        TraceContext context = TraceContext.getCurrentContext();
        
        Request.Builder requestBuilder = originalRequest.newBuilder();
        
        if (context != null) {
            // 添加追踪头信息到请求中
            requestBuilder.header(TracerConstants.HEADER_TRACE_ID, context.getTraceId())
                        .header(TracerConstants.HEADER_SPAN_ID, context.getSpanId());
            
            if (context.getParentSpanId() != null) {
                requestBuilder.header(TracerConstants.HEADER_PARENT_SPAN_ID, context.getParentSpanId());
            }
            
            requestBuilder.header(TracerConstants.HEADER_APP_NAME, properties.getApplicationName());
            
            if (log.isDebugEnabled()) {
                log.debug("Added trace headers to OkHttp request: traceId={}, spanId={}, url={}",
                        context.getTraceId(), context.getSpanId(), url);
            }
            
            // 记录请求开始
            metricsCollector.recordRequestStart(
                    method, 
                    url, 
                    TracerConstants.CLIENT_TYPE_OKHTTP, 
                    url
            );
        } else {
            log.debug("No active trace context found for OkHttp request to: {}", url);
        }
        
        Request newRequest = requestBuilder.build();
        long startTime = System.currentTimeMillis();
        Response response = null;
        boolean hasException = false;
        
        try {
            // 执行请求
            response = chain.proceed(newRequest);
            return response;
        } catch (Exception e) {
            hasException = true;
            log.debug("Exception during OkHttp request to {}: {}", url, e.getMessage());
            throw e;
        } finally {
            if (context != null) {
                long duration = System.currentTimeMillis() - startTime;
                int statusCode = response != null ? response.code() : -1;
                
                // 记录请求结束
                metricsCollector.recordRequestEnd(
                        method,
                        url,
                        TracerConstants.CLIENT_TYPE_OKHTTP,
                        url,
                        statusCode,
                        duration,
                        hasException
                );
                
                if (log.isDebugEnabled()) {
                    log.debug("OkHttp request completed: url={}, status={}, duration={}ms, hasError={}",
                            url, statusCode, duration, hasException);
                }
            }
        }
    }
} 