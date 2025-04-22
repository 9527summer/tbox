package org.tbox.dapper.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 分布式追踪上下文
 * 
 * 基于ThreadLocal存储当前线程的跟踪上下文信息
 */
public class TraceContext {
    private static final Logger log = LoggerFactory.getLogger(TraceContext.class);
    
    // 线程本地变量，存储当前线程的追踪上下文
    private static final ThreadLocal<TraceContext> LOCAL = new ThreadLocal();
    
    // 追踪标识符
    private final String traceId;
    // 当前Span标识符
    private final String spanId;
    // 父Span标识符，根Span的父ID为null
    private final String parentSpanId;
    // 是否采样
    private final boolean sampled;
    // 调用开始时间
    private final long startTime;
    // 额外属性，用于存储自定义标签和传播上下文
    private final Map<String, String> attributes;
    // 服务名称
    private final String serviceName;
    
    /**
     * 创建追踪上下文
     */
    public TraceContext(String traceId, String spanId, String parentSpanId, boolean sampled, String serviceName) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.sampled = sampled;
        this.startTime = System.currentTimeMillis();
        this.attributes = new HashMap();
        this.serviceName = serviceName;
    }
    
    /**
     * 获取当前线程的追踪上下文，如果不存在则返回null
     */
    public static TraceContext current() {
        return LOCAL.get();
    }
    
    /**
     * 设置当前线程的追踪上下文
     */
    public static void setCurrent(TraceContext context) {
        if (context == null) {
            LOCAL.remove();
        } else {
            LOCAL.set(context);
        }
    }
    
    /**
     * 清除当前线程的追踪上下文
     */
    public static void clear() {
        LOCAL.remove();
    }
    
    // 各种 getters
    public String getTraceId() {
        return traceId;
    }
    
    public String getSpanId() {
        return spanId;
    }
    
    public String getParentSpanId() {
        return parentSpanId;
    }
    
    public boolean isSampled() {
        return sampled;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public Map<String, String> getAttributes() {
        return new HashMap(attributes);
    }
    
    public TraceContext setAttribute(String key, String value) {
        if (key != null && value != null) {
            this.attributes.put(key, value);
        }
        return this;
    }
    
    public String getAttribute(String key) {
        return this.attributes.get(key);
    }
    
    @Override
    public String toString() {
        return "TraceContext{" +
                "traceId='" + traceId + '\'' +
                ", spanId='" + spanId + '\'' +
                ", parentSpanId='" + parentSpanId + '\'' +
                ", sampled=" + sampled +
                ", serviceName='" + serviceName + '\'' +
                '}';
    }
} 