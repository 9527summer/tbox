package org.tbox.base.redis.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.tbox.base.core.context.ApplicationContextHolder;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存工具类，提供各种数据类型的操作
 */
public final class RedisUtils {

    private static volatile RedisTemplate<String, Object> redisTemplate;
    private static volatile StringRedisTemplate stringRedisTemplate;

    private RedisUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static RedisTemplate<String, Object> getRedisTemplate() {
        if (redisTemplate == null) {
            synchronized (RedisUtils.class) {
                if (redisTemplate == null) {
                    redisTemplate = ApplicationContextHolder.getBean("redisTemplate", RedisTemplate.class);
                }
            }
        }
        return redisTemplate;
    }

    public static StringRedisTemplate getStringRedisTemplate() {
        if (stringRedisTemplate == null) {
            synchronized (RedisUtils.class) {
                if (stringRedisTemplate == null) {
                    stringRedisTemplate = ApplicationContextHolder.getBean(StringRedisTemplate.class);
                }
            }
        }
        return stringRedisTemplate;
    }

    // ==================== 通用操作 ====================

    /**
     * 删除键
     */
    public static Boolean delete(String key) {
        return getRedisTemplate().delete(key);
    }

    /**
     * 批量删除键
     */
    public static Long delete(Collection<String> keys) {
        return getRedisTemplate().delete(keys);
    }

    /**
     * 批量删除键
     */
    public static Long delete(String... keys) {
        return getRedisTemplate().delete(Arrays.asList(keys));
    }

    /**
     * 检查键是否存在
     */
    public static Boolean hasKey(String key) {
        return getRedisTemplate().hasKey(key);
    }

    /**
     * 设置过期时间
     */
    public static Boolean expire(String key, long timeout, TimeUnit timeUnit) {
        return getRedisTemplate().expire(key, timeout, timeUnit);
    }

    /**
     * 获取过期时间（秒）
     */
    public static Long getExpire(String key) {
        return getRedisTemplate().getExpire(key, TimeUnit.SECONDS);
    }

    // ==================== String操作 ====================

    /**
     * 设置值
     */
    public static void set(String key, Object value) {
        getRedisTemplate().opsForValue().set(key, value);
    }

    /**
     * 设置值并指定过期时间
     */
    public static void set(String key, Object value, long timeout, TimeUnit timeUnit) {
        getRedisTemplate().opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 获取值
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        return (T) getRedisTemplate().opsForValue().get(key);
    }

    /**
     * 自增（使用StringRedisTemplate）
     */
    public static Long increment(String key) {
        return getStringRedisTemplate().opsForValue().increment(key);
    }

    /**
     * 自增指定值（使用StringRedisTemplate）
     */
    public static Long increment(String key, long delta) {
        return getStringRedisTemplate().opsForValue().increment(key, delta);
    }

    /**
     * 自减（使用StringRedisTemplate）
     */
    public static Long decrement(String key) {
        return getStringRedisTemplate().opsForValue().decrement(key);
    }

    /**
     * 自减指定值（使用StringRedisTemplate）
     */
    public static Long decrement(String key, long delta) {
        return getStringRedisTemplate().opsForValue().decrement(key, delta);
    }

    // ==================== Hash操作 ====================

    /**
     * 设置Hash字段值
     */
    public static void hSet(String key, String field, Object value) {
        getRedisTemplate().opsForHash().put(key, field, value);
    }

    /**
     * 批量设置Hash字段值
     */
    public static void hSetAll(String key, Map<String, ?> map) {
        getRedisTemplate().opsForHash().putAll(key, map);
    }

    /**
     * 获取Hash字段值
     */
    @SuppressWarnings("unchecked")
    public static <T> T hGet(String key, String field) {
        return (T) getRedisTemplate().opsForHash().get(key, field);
    }

    /**
     * 获取Hash所有字段值
     */
    public static Map<Object, Object> hGetAll(String key) {
        return getRedisTemplate().opsForHash().entries(key);
    }

    /**
     * 删除Hash字段
     */
    public static Long hDelete(String key, Object... fields) {
        return getRedisTemplate().opsForHash().delete(key, fields);
    }

    /**
     * 判断Hash字段是否存在
     */
    public static Boolean hHasKey(String key, String field) {
        return getRedisTemplate().opsForHash().hasKey(key, field);
    }

    /**
     * Hash字段自增（使用StringRedisTemplate）
     */
    public static Long hIncrement(String key, String field, long delta) {
        return getStringRedisTemplate().opsForHash().increment(key, field, delta);
    }

    // ==================== List操作 ====================

    /**
     * 左侧入队
     */
    public static Long lLeftPush(String key, Object value) {
        return getRedisTemplate().opsForList().leftPush(key, value);
    }

    /**
     * 右侧入队
     */
    public static Long lRightPush(String key, Object value) {
        return getRedisTemplate().opsForList().rightPush(key, value);
    }

    /**
     * 左侧出队
     */
    @SuppressWarnings("unchecked")
    public static <T> T lLeftPop(String key) {
        return (T) getRedisTemplate().opsForList().leftPop(key);
    }

    /**
     * 右侧出队
     */
    @SuppressWarnings("unchecked")
    public static <T> T lRightPop(String key) {
        return (T) getRedisTemplate().opsForList().rightPop(key);
    }

    /**
     * 获取List指定范围
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> lRange(String key, long start, long end) {
        return (List<T>) getRedisTemplate().opsForList().range(key, start, end);
    }

    /**
     * 获取List长度
     */
    public static Long lSize(String key) {
        return getRedisTemplate().opsForList().size(key);
    }

    // ==================== Set操作 ====================

    /**
     * 添加Set元素
     */
    public static Long sAdd(String key, Object... values) {
        return getRedisTemplate().opsForSet().add(key, values);
    }

    /**
     * 获取Set所有元素
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> sMembers(String key) {
        return (Set<T>) getRedisTemplate().opsForSet().members(key);
    }

    /**
     * 删除Set元素
     */
    public static Long sRemove(String key, Object... values) {
        return getRedisTemplate().opsForSet().remove(key, values);
    }

    /**
     * Set是否包含元素
     */
    public static Boolean sIsMember(String key, Object value) {
        return getRedisTemplate().opsForSet().isMember(key, value);
    }

    // ==================== ZSet操作 ====================

    public static Boolean zAdd(String key, Object value, double score) {
        return getRedisTemplate().opsForZSet().add(key, value, score);
    }

    public static Long zRemove(String key, Object... values) {
        return getRedisTemplate().opsForZSet().remove(key, values);
    }

    public static Long zRemoveRangeByScore(String key, double min, double max) {
        return getRedisTemplate().opsForZSet().removeRangeByScore(key, min, max);
    }

    public static Long zSize(String key) {
        return getRedisTemplate().opsForZSet().size(key);
    }

    // ==================== Lua脚本执行 ====================

    public static <T> T execute(String script, Class<T> resultType, List<String> keys, Object... args) {
        RedisScript<T> redisScript = new DefaultRedisScript<>(script, resultType);
        return getRedisTemplate().execute(redisScript, keys, args);
    }
}

