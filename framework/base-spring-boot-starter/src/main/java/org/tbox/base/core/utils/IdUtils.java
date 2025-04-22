package org.tbox.base.core.utils;

import org.tbox.base.core.id.DistributedIdGenerator;

/**
 * ID生成工具类
 * 
 * 封装了基于雪花算法的分布式ID生成器，提供全局唯一ID生成功能
 */
public final class IdUtils {
    
    /**
     * 私有构造函数，防止实例化
     */
    private IdUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    /**
     * 生成全局唯一Long型ID
     * 
     * @return Long型ID
     */
    public static Long generateLongId() {
        return DistributedIdGenerator.nextId();
    }
    
    /**
     * 生成全局唯一字符串ID
     * 
     * @return 16位字符串格式的ID
     */
    public static String generateId() {
        return DistributedIdGenerator.nextIdString();
    }
    
    /**
     * 从ID中解析出生成时间
     *
     * @param id Long型ID
     * @return 生成ID时的时间戳(毫秒)
     */
    public static long getTimestampFromId(long id) {
        return DistributedIdGenerator.getTimestampFromId(id);
    }
} 