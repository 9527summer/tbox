package org.tbox.dapper.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.tbox.dapper.utils.DapperIdUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 追踪上下文，用于存储和传递追踪信息
 */
public class TraceContext {
    private static final Logger log = LoggerFactory.getLogger(TraceContext.class);
    
    private static final ThreadLocal<TraceContext> CONTEXT_HOLDER = new ThreadLocal<TraceContext>();
    
    // MDC常量键值
    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_SPAN_ID = "spanId";
    public static final String MDC_PARENT_SPAN_ID = "parentSpanId";
    public static final String MDC_APP_NAME = "appName";
    
    // 追踪ID
    private String traceId;
    // 当前Span ID
    private String spanId;
    // 父Span ID
    private String parentSpanId;
    // 应用名称
    private String appName;
    // 请求开始时间
    private long startTime;
    // 是否已完成
    private boolean completed = false;
    // 额外属性
    private Map<String, String> attributes = new HashMap();
    // 子Span计数器
    private final AtomicInteger childCounter = new AtomicInteger(0);
    
    /**
     * 创建一个根追踪上下文
     */
    public static TraceContext createRootContext(String appName) {
        TraceContext context = new TraceContext();
        context.traceId = DapperIdUtils.generateTraceId();
        context.spanId = DapperIdUtils.generateSpanId();
        context.parentSpanId = null;
        context.appName = appName;
        context.startTime = System.currentTimeMillis();
        
        CONTEXT_HOLDER.set(context);
        updateMDC(context);

        return context;
    }
    
    /**
     * 从现有上下文创建子Span
     */
    public static TraceContext createChildContext() {
        TraceContext parent = getCurrentContext();
        if (parent == null) {
            log.warn("Creating child context without parent context, creating root context instead");
            return null;
        }
        
        TraceContext child = new TraceContext();
        child.traceId = parent.traceId;
        child.parentSpanId = parent.spanId;
        
        // 生成子Span ID（格式: 父SpanId.计数）
        int childIndex = parent.childCounter.incrementAndGet();
        child.spanId = parent.spanId + "." + childIndex;
        
        child.appName = parent.appName;
        child.startTime = System.currentTimeMillis();
        
        CONTEXT_HOLDER.set(child);
        updateMDC(child);
        
        if (log.isDebugEnabled()) {
            log.debug("Created child trace context: traceId={}, spanId={}, parentSpanId={}", 
                    child.traceId, child.spanId, child.parentSpanId);
        }
        
        return child;
    }
    
    /**
     * 从外部传入的trace信息恢复上下文
     */
    public static TraceContext createFromExternalContext(String traceId, String spanId, String parentSpanId, String appName) {
        if (traceId == null) {
            log.warn("Cannot create context from external context without traceId");
            return null;
        }
        
        TraceContext context = new TraceContext();
        context.traceId = traceId;
        context.spanId = spanId != null ? spanId : DapperIdUtils.generateSpanId();
        context.parentSpanId = parentSpanId;
        context.appName = appName;
        context.startTime = System.currentTimeMillis();
        
        CONTEXT_HOLDER.set(context);
        updateMDC(context);
        
        if (log.isDebugEnabled()) {
            log.debug("Created context from external: traceId={}, spanId={}, parentSpanId={}", 
                    context.traceId, context.spanId, context.parentSpanId);
        }
        
        return context;
    }
    
    /**
     * 获取当前线程上下文
     */
    public static TraceContext getCurrentContext() {
        return CONTEXT_HOLDER.get();
    }
    
    /**
     * 设置当前线程上下文
     * 
     * @param context 要设置的上下文，可以为null
     */
    public static void setCurrentContext(TraceContext context) {
        if (context == null) {
            removeContext();
            return;
        }
        
        CONTEXT_HOLDER.set(context);
        updateMDC(context);
        
        if (log.isDebugEnabled()) {
            log.debug("Set current thread trace context: traceId={}, spanId={}", 
                    context.traceId, context.spanId);
        }
    }
    
    /**
     * 移除当前线程上下文
     */
    public static void removeContext() {
        TraceContext context = CONTEXT_HOLDER.get();
        if (context != null && !context.completed) {
            log.warn("Removing incomplete trace context: traceId={}, spanId={}", 
                    context.traceId, context.spanId);
        }
        
        CONTEXT_HOLDER.remove();
        clearMDC();
    }
    
    /**
     * 标记当前Span已完成
     */
    public void complete() {
        this.completed = true;
//
//        if (log.isDebugEnabled()) {
//            log.debug("Completed trace span: traceId={}, spanId={}, duration={}ms",
//                    this.traceId, this.spanId, System.currentTimeMillis() - this.startTime);
//        }
    }
    
    /**
     * 设置属性
     */
    public void setAttribute(String key, String value) {
        if (key != null && value != null) {
            this.attributes.put(key, value);
        }
    }
    
    /**
     * 获取属性
     */
    public String getAttribute(String key) {
        return this.attributes.get(key);
    }
    
    /**
     * 获取所有属性
     */
    public Map<String, String> getAttributes() {
        return new HashMap(this.attributes);
    }
    
    /**
     * 更新MDC值
     */
    private static void updateMDC(TraceContext context) {
        if (context != null) {
            MDC.put(MDC_TRACE_ID, context.traceId);
            MDC.put(MDC_SPAN_ID, context.spanId);
            if (context.parentSpanId != null) {
                MDC.put(MDC_PARENT_SPAN_ID, context.parentSpanId);
            }
            if (context.appName != null) {
                MDC.put(MDC_APP_NAME, context.appName);
            }
        }
    }
    
    /**
     * 清除MDC值
     */
    private static void clearMDC() {
        MDC.remove(MDC_TRACE_ID);
        MDC.remove(MDC_SPAN_ID);
        MDC.remove(MDC_PARENT_SPAN_ID);
        MDC.remove(MDC_APP_NAME);
    }
    
    /**
     * 拷贝当前上下文到新线程
     */
    public static Map<String, String> getContextForAsync() {
        TraceContext current = getCurrentContext();
        if (current == null) {
            return null;
        }
        
        Map<String, String> contextMap = new HashMap();
        contextMap.put(MDC_TRACE_ID, current.traceId);
        contextMap.put(MDC_SPAN_ID, current.spanId);
        if (current.parentSpanId != null) {
            contextMap.put(MDC_PARENT_SPAN_ID, current.parentSpanId);
        }
        if (current.appName != null) {
            contextMap.put(MDC_APP_NAME, current.appName);
        }
        
        return contextMap;
    }
    
    /**
     * 从异步上下文映射中还原上下文
     */
    public static TraceContext restoreFromAsync(Map<String, String> contextMap) {
        if (contextMap == null || contextMap.isEmpty()) {
            return null;
        }
        
        String traceId = contextMap.get(MDC_TRACE_ID);
        String spanId = contextMap.get(MDC_SPAN_ID);
        String parentSpanId = contextMap.get(MDC_PARENT_SPAN_ID);
        String appName = contextMap.get(MDC_APP_NAME);
        
        return createFromExternalContext(traceId, spanId, parentSpanId, appName);
    }
    
    // getter方法
    public String getTraceId() {
        return traceId;
    }
    
    public String getSpanId() {
        return spanId;
    }
    
    public String getParentSpanId() {
        return parentSpanId;
    }
    
    public String getAppName() {
        return appName;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    /**
     * 获取当前追踪的运行时间（毫秒）
     */
    public long getDuration() {
        return System.currentTimeMillis() - startTime;
    }
} 