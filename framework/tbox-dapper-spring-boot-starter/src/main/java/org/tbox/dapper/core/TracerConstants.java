package org.tbox.dapper.core;

/**
 * 追踪相关常量
 */
public final class TracerConstants {
    
    // HTTP头部信息常量
    /**
     * 追踪ID的HTTP头，用于在请求间传递追踪标识符
     */
    public static final String HEADER_TRACE_ID = "X-Trace-ID";
    
    /**
     * 当前Span ID的HTTP头，标识当前操作的唯一ID
     */
    public static final String HEADER_SPAN_ID = "X-Span-ID";
    
    /**
     * 父Span ID的HTTP头，用于构建调用层级关系
     */
    public static final String HEADER_PARENT_SPAN_ID = "X-Parent-Span-ID";
    
    /**
     * 应用名称的HTTP头，标识发起请求的应用
     */
    public static final String HEADER_APP_NAME = "X-App-Name";
    
    // HTTP客户端类型常量
    /**
     * RestTemplate客户端类型标识
     */
    public static final String CLIENT_TYPE_REST_TEMPLATE = "RestTemplate";
    
    /**
     * OkHttp客户端类型标识
     */
    public static final String CLIENT_TYPE_OKHTTP = "OkHttp";
    
    /**
     * HttpClient客户端类型标识
     */
    public static final String CLIENT_TYPE_HTTP_CLIENT = "HttpClient";
    
    // 指标名称常量
    /**
     * 指标名称前缀，用于所有TBox-Tracer指标
     */
    public static final String METRIC_PREFIX = "tbox.tracer.";
    
    /**
     * 请求总数指标，记录所有收到的请求数量
     */
    public static final String METRIC_REQUESTS_TOTAL = METRIC_PREFIX + "requests.total";
    
    /**
     * 成功请求数指标，记录所有成功处理的请求数量
     */
    public static final String METRIC_REQUESTS_SUCCESS = METRIC_PREFIX + "requests.success";
    
    /**
     * 失败请求数指标，记录所有处理失败的请求数量
     */
    public static final String METRIC_REQUESTS_FAILED = METRIC_PREFIX + "requests.failed";
    
    /**
     * 活跃请求数指标，记录当前正在处理中的请求数量
     */
    public static final String METRIC_REQUESTS_ACTIVE = METRIC_PREFIX + "requests.active";
    
    /**
     * 请求持续时间指标，记录请求处理的耗时
     */
    public static final String METRIC_REQUEST_DURATION = METRIC_PREFIX + "request.duration";
    
    /**
     * 请求大小指标，记录请求体的大小（字节数）
     */
    public static final String METRIC_REQUEST_SIZE = METRIC_PREFIX + "request.size";
    
    /**
     * 响应大小指标，记录响应体的大小（字节数）
     */
    public static final String METRIC_RESPONSE_SIZE = METRIC_PREFIX + "response.size";
    
    /**
     * 请求详细耗时指标，记录请求处理各阶段的详细耗时
     */
    public static final String METRIC_REQUEST_DETAIL_DURATION = METRIC_PREFIX + "request.detail.duration";
    
    /**
     * 错误数指标，记录处理过程中发生的错误数量
     */
    public static final String METRIC_ERRORS = METRIC_PREFIX + "errors";
    
    /**
     * 最后请求时间戳指标，记录最近一次请求的时间戳
     */
    public static final String METRIC_LAST_REQUEST_TIMESTAMP = METRIC_PREFIX + "last_request.timestamp";
    
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
    
    /**
     * 错误标志属性，标识操作是否发生错误
     */
    public static final String ERROR = "error";
    
    /**
     * 错误信息属性，记录操作发生的错误详情
     */
    public static final String ERROR_MESSAGE = "error.message";
    
    // 禁止实例化
    private TracerConstants() {
        throw new IllegalStateException("Utility class");
    }
} 