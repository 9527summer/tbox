package org.tbox.dapper.utils;


import java.util.UUID;

/**
 * 分布式追踪ID生成工具类
 */
public class DapperIdUtils {
    
    /**
     * 生成TraceId，使用分布式ID生成器
     * 
     * @return 全局唯一的TraceId
     */
    public static String generateTraceId() {
        return String.valueOf(Math.abs(UUID.randomUUID().getLeastSignificantBits()));
    }
    
    /**
     * 生成SpanId，使用分布式ID生成器
     * 
     * @return 全局唯一的SpanId
     */
    public static String generateSpanId() {
        return String.valueOf(Math.abs(UUID.randomUUID().getLeastSignificantBits()));
    }
    

    
    /**
     * 私有构造函数，防止实例化
     */
    private DapperIdUtils() {
        throw new IllegalStateException("Utility class");
    }
} 