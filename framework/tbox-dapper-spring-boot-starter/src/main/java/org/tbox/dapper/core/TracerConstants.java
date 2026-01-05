package org.tbox.dapper.core;

/**
 * 追踪相关常量
 */
public final class TracerConstants {

    /**
     * W3C TraceContext header：traceparent
     */
    public static final String HEADER_TRACEPARENT = "traceparent";

    // MDC key 常量
    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_SPAN_ID = "spanId";
    
    // 组件和资源类型常量
    /**
     * 组件类型属性，用于标识不同类型的组件（如HTTP、数据库、消息队列等）
     */
    public static final String COMPONENT_TYPE = "component.type";
    
    /**
     * 资源类型属性，用于标识被访问资源的类型（如REST、RPC、MQ等）
     */
    public static final String RESOURCE_TYPE = "resource.type";
    
    /**
     * 资源名称属性，用于标识具体的资源名称（如API路径、方法名等）
     */
    public static final String RESOURCE_NAME = "resource.name";

    /**
     * 持续时间属性，记录操作执行的时长（毫秒）
     */
    public static final String DURATION = "duration";
    
    // 禁止实例化
    private TracerConstants() {
        throw new IllegalStateException("Utility class");
    }
}
