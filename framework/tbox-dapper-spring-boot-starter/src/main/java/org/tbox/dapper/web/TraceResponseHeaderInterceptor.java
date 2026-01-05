package org.tbox.dapper.web;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 在响应头写入 traceId/spanId，便于排查。
 *
 * <p>说明：链路传播仍由 Spring Boot Tracing（W3C TraceContext）负责，这里只做“回显”。</p>
 */
public class TraceResponseHeaderInterceptor implements HandlerInterceptor {

    private final Tracer tracer;

    public TraceResponseHeaderInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        writeHeadersIfPossible(response);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        writeHeadersIfPossible(response);
    }

    private void writeHeadersIfPossible(HttpServletResponse response) {
        if (response == null || response.isCommitted()) {
            return;
        }

        Span span = tracer.currentSpan();
        if (span == null) {
            return;
        }

        String traceId = span.context().traceId();
        String spanId = span.context().spanId();
        if (traceId == null || spanId == null) {
            return;
        }

        response.setHeader("traceId", traceId);
        response.setHeader("spanId", spanId);
    }
}

