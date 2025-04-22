package org.tbox.dapper.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.tbox.dapper.config.TracerProperties;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

/**
 * 追踪过滤器
 * 包装请求和响应，用于记录内容大小
 */
public class TracerFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(TracerFilter.class);
    
    private final TracerProperties properties;
    
    public TracerFilter(TracerProperties properties) {
        this.properties = properties;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isEnabled() || shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        if (isRequestValid(request)) {
            // 包装请求和响应
            ContentCachingRequestWrapper requestWrapper = wrapRequest(request);
            ContentCachingResponseWrapper responseWrapper = wrapResponse(response);
            
            try {
                filterChain.doFilter(requestWrapper, responseWrapper);
            } finally {
                // 确保内容能够被读取
                responseWrapper.copyBodyToResponse();
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }
    
    /**
     * 包装请求
     */
    private ContentCachingRequestWrapper wrapRequest(HttpServletRequest request) throws IOException {
        if (request instanceof ContentCachingRequestWrapper) {
            return (ContentCachingRequestWrapper) request;
        }
        // 使用默认值1024，因为之前的maxPayloadLength已被移除
        return new ContentCachingRequestWrapper(request, 1024);
    }

    /**
     * 包装响应
     */
    private ContentCachingResponseWrapper wrapResponse(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper) {
            return (ContentCachingResponseWrapper) response;
        }
        return new ContentCachingResponseWrapper(response);
    }

    /**
     * 判断是否跳过此请求的追踪
     */
    private boolean shouldSkip(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // 使用合并后的排除路径
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
    
    /**
     * 判断请求是否有效
     */
    private boolean isRequestValid(HttpServletRequest request) {
        return request != null && request.getContentType() != null;
    }
} 