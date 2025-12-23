package org.tbox.dapper.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.tbox.dapper.context.TraceContext;
import org.tbox.dapper.core.TracerConstants;
import org.tbox.dapper.config.TracerProperties;

import java.io.IOException;

/**
 * RestTemplate追踪拦截器
 * 用于在RestTemplate请求中添加追踪头信息，实现分布式追踪
 */
public class TracerRestTemplateInterceptor implements ClientHttpRequestInterceptor {
    private static final Logger log = LoggerFactory.getLogger(TracerRestTemplateInterceptor.class);
    
    private final TracerProperties properties;

    public TracerRestTemplateInterceptor(TracerProperties properties) {
        this.properties = properties;
    }
    
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        if (!properties.isEnabled()) {
            return execution.execute(request, body);
        }
        
        String url = request.getURI().toString();
        String method = request.getMethod().toString();
        
        // 获取当前追踪上下文
        TraceContext context = TraceContext.getCurrentContext();
        
        if (context != null) {
            // 添加追踪头信息到请求中
            request.getHeaders().add(TracerConstants.HEADER_TRACE_ID, context.getTraceId());
            request.getHeaders().add(TracerConstants.HEADER_SPAN_ID, context.getSpanId());
            
            if (context.getParentSpanId() != null) {
                request.getHeaders().add(TracerConstants.HEADER_PARENT_SPAN_ID, context.getParentSpanId());
            }
            
            request.getHeaders().add(TracerConstants.HEADER_APP_NAME, properties.getApplicationName());
            
            if (log.isDebugEnabled()) {
                log.debug("Added trace headers to RestTemplate request: traceId={}, spanId={}, url={}",
                        context.getTraceId(), context.getSpanId(), url);
            }

        } else {
            log.debug("No active trace context found for RestTemplate request to: {}", url);
        }
        
        long startTime = System.currentTimeMillis();
        ClientHttpResponse response = null;
        boolean hasException = false;
        
        try {
            // 执行请求
            response = execution.execute(request, body);
            return response;
        } catch (Exception e) {
            hasException = true;
            log.debug("Exception during RestTemplate request to {}: {}", url, e.getMessage());
            throw e;
        } finally {
            if (context != null) {
                long duration = System.currentTimeMillis() - startTime;
                int statusCode = response != null ? response.getStatusCode().value() : -1;
                

                
                if (log.isDebugEnabled()) {
                    log.debug("RestTemplate request completed: url={}, status={}, duration={}ms, hasError={}",
                            url, statusCode, duration, hasException);
                }
            }
        }
    }
} 