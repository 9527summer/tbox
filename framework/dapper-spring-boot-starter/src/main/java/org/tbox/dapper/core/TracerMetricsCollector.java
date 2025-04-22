package org.tbox.dapper.core;

import io.micrometer.core.instrument.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.tbox.dapper.config.TracerProperties;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 追踪指标收集器
 * 负责收集并暴露追踪指标
 */
public class TracerMetricsCollector {
    private static final Logger log = LoggerFactory.getLogger(TracerMetricsCollector.class);
    
    private final MeterRegistry meterRegistry;
    private final TracerProperties properties;
    
    // 活跃请求计数器
    private final ConcurrentMap<String, AtomicInteger> activeRequestsCounters = new ConcurrentHashMap<>();
    
    // 请求总数计数器
    private final Counter totalRequests;
    
    // 成功请求计数器
    private final Counter successRequests;
    
    // 失败请求计数器
    private final Counter failedRequests;
    
    // 请求执行时间
    private final Timer requestTimer;
    
    // 请求大小直方图
    private final DistributionSummary requestSizeSummary;
    
    // 响应大小直方图
    private final DistributionSummary responseSizeSummary;
    
    // 最近一次请求时间
    private final AtomicLong lastRequestTimestamp = new AtomicLong(0);
    
    /**
     * 构造函数
     */
    public TracerMetricsCollector(MeterRegistry meterRegistry, TracerProperties properties) {
        this.meterRegistry = meterRegistry;
        this.properties = properties;
        
        // 初始化所有默认的指标
        totalRequests = Counter.builder("tbox.tracer.requests.total")
                .description("Total number of requests processed")
                .register(meterRegistry);
        
        successRequests = Counter.builder("tbox.tracer.requests.success")
                .description("Number of successful requests")
                .register(meterRegistry);
        
        failedRequests = Counter.builder("tbox.tracer.requests.failed")
                .description("Number of failed requests")
                .register(meterRegistry);
        
        requestTimer = Timer.builder("tbox.tracer.request.duration")
                .description("Request processing time")
                .register(meterRegistry);
        
        requestSizeSummary = DistributionSummary.builder("tbox.tracer.request.size")
                .description("HTTP request size in bytes")
                .baseUnit("bytes")
                .register(meterRegistry);
        
        responseSizeSummary = DistributionSummary.builder("tbox.tracer.response.size")
                .description("HTTP response size in bytes")
                .baseUnit("bytes")
                .register(meterRegistry);
        
        // 注册最近请求时间的仪表
        Gauge.builder("tbox.tracer.last_request.timestamp", lastRequestTimestamp, AtomicLong::get)
                .description("Timestamp of the last request")
                .register(meterRegistry);
    }
    
    /**
     * 记录请求开始
     */
    public void recordRequestStart(String method, String uri, String controller, String action) {
        if (!properties.isExposeMetrics()) {
            return;
        }
        
        try {
            totalRequests.increment();
            lastRequestTimestamp.set(System.currentTimeMillis());
            
            // 更新活跃请求数
            String requestKey = getRequestKey(method, uri, controller, action);
            getOrCreateActiveCounter(requestKey).incrementAndGet();
            
        } catch (Exception e) {
            log.warn("Error recording request start metrics", e);
        }
    }
    
    /**
     * 记录请求结束
     */
    public void recordRequestEnd(String method, String uri, String controller, String action, 
            int statusCode, long duration, boolean hasException) {
        if (!properties.isExposeMetrics()) {
            return;
        }
        
        try {
            // 更新活跃请求数
            String requestKey = getRequestKey(method, uri, controller, action);
            getOrCreateActiveCounter(requestKey).decrementAndGet();
            
            // 记录执行时间
            requestTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
            
            // 根据状态码和异常状态记录成功/失败
            if (statusCode >= 200 && statusCode < 400 && !hasException) {
                successRequests.increment();
            } else {
                failedRequests.increment();
                
                // 记录错误明细
                Counter errorCounter = Counter.builder("tbox.tracer.errors")
                        .tag("uri", uri)
                        .tag("status", String.valueOf(statusCode))
                        .tag("controller", controller)
                        .tag("action", action)
                        .description("Error count by URI and status code")
                        .register(meterRegistry);
                errorCounter.increment();
            }
            
            // 记录请求明细时间
            Timer requestDetailTimer = Timer.builder("tbox.tracer.request.detail.duration")
                    .tag("uri", uri)
                    .tag("method", method)
                    .tag("controller", controller)
                    .tag("action", action)
                    .description("Request processing time by URI")
                    .register(meterRegistry);
            requestDetailTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
            
        } catch (Exception e) {
            log.warn("Error recording request end metrics", e);
        }
    }
    
    /**
     * 记录请求大小
     */
    public void recordRequestSize(long bytes) {
        if (properties.isExposeMetrics()) {
            requestSizeSummary.record(bytes);
        }
    }
    
    /**
     * 记录响应大小
     */
    public void recordResponseSize(long bytes) {
        if (properties.isExposeMetrics()) {
            responseSizeSummary.record(bytes);
        }
    }
    
    /**
     * 获取或创建请求计数器
     */
    private AtomicInteger getOrCreateActiveCounter(String key) {
        return activeRequestsCounters.computeIfAbsent(key, k -> {
            AtomicInteger counter = new AtomicInteger(0);
            // 注册活跃请求的仪表
            Gauge.builder("tbox.tracer.requests.active", counter, AtomicInteger::get)
                    .tag("request", key)
                    .description("Number of active requests")
                    .register(meterRegistry);
            return counter;
        });
    }
    
    /**
     * 生成请求唯一键
     */
    private String getRequestKey(String method, String uri, String controller, String action) {
        // 为了避免过多的标签导致内存占用过大，这里进行了简化
        if (StringUtils.hasText(controller) && StringUtils.hasText(action)) {
            return controller + "#" + action;
        } else {
            return method + "#" + uri;
        }
    }
} 