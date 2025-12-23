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

    private static RedisTemplate<String, Object> redisTemplate;
    private static StringRedisTemplate stringRedisTemplate;

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
     * 获取列表指定范围元素
     */
    public static List<Object> lRange(String key, long start, long end) {
        return getRedisTemplate().opsForList().range(key, start, end);
    }

    /**
     * 获取列表长度
     */
    public static Long lSize(String key) {
        return getRedisTemplate().opsForList().size(key);
    }

    // ==================== Set操作 ====================

    /**
     * 添加元素
     */
    public static Long sAdd(String key, Object... values) {
        return getRedisTemplate().opsForSet().add(key, values);
    }

    /**
     * 移除元素
     */
    public static Long sRemove(String key, Object... values) {
        return getRedisTemplate().opsForSet().remove(key, values);
    }

    /**
     * 获取所有元素
     */
    public static Set<Object> sMembers(String key) {
        return getRedisTemplate().opsForSet().members(key);
    }

    /**
     * 判断元素是否存在
     */
    public static Boolean sIsMember(String key, Object value) {
        return getRedisTemplate().opsForSet().isMember(key, value);
    }

    /**
     * 获取集合大小
     */
    public static Long sSize(String key) {
        return getRedisTemplate().opsForSet().size(key);
    }

    // ==================== ZSet操作 ====================

    /**
     * 添加元素
     */
    public static Boolean zAdd(String key, Object value, double score) {
        return getRedisTemplate().opsForZSet().add(key, value, score);
    }

    /**
     * 移除元素
     */
    public static Long zRemove(String key, Object... values) {
        return getRedisTemplate().opsForZSet().remove(key, values);
    }

    /**
     * 获取元素分数
     */
    public static Double zScore(String key, Object value) {
        return getRedisTemplate().opsForZSet().score(key, value);
    }

    /**
     * 增加元素分数
     */
    public static Double zIncrementScore(String key, Object value, double delta) {
        return getRedisTemplate().opsForZSet().incrementScore(key, value, delta);
    }

    /**
     * 获取元素排名（从0开始，分数从小到大）
     */
    public static Long zRank(String key, Object value) {
        return getRedisTemplate().opsForZSet().rank(key, value);
    }

    /**
     * 获取元素排名（从0开始，分数从大到小）
     */
    public static Long zReverseRank(String key, Object value) {
        return getRedisTemplate().opsForZSet().reverseRank(key, value);
    }

    /**
     * 获取指定排名范围的元素（分数从小到大）
     */
    public static Set<Object> zRange(String key, long start, long end) {
        return getRedisTemplate().opsForZSet().range(key, start, end);
    }

    /**
     * 获取指定排名范围的元素（分数从大到小）
     */
    public static Set<Object> zReverseRange(String key, long start, long end) {
        return getRedisTemplate().opsForZSet().reverseRange(key, start, end);
    }

    /**
     * 获取集合大小
     */
    public static Long zSize(String key) {
        return getRedisTemplate().opsForZSet().zCard(key);
    }

    /**
     * 删除指定排名范围的元素
     */
    public static void zRemoveRangeByScore(String key, double min, double max) {
        getRedisTemplate().opsForZSet().removeRangeByScore(key, min, max);
    }

    // ==================== Lua脚本执行 ====================

    /**
     * 执行Lua脚本
     *
     * @param script Lua脚本
     * @param keys 键列表
     * @param args 参数列表
     * @return 脚本执行结果
     */
    public static <T> T execute(String script, List<String> keys, Object... args) {
        return (T) getRedisTemplate().execute(
                new DefaultRedisScript<>(script, Object.class), keys, args);
    }

    /**
     * 执行Lua脚本
     *
     * @param script Lua脚本
     * @param returnType 返回值类型
     * @param keys 键列表
     * @param args 参数列表
     * @return 脚本执行结果
     */
    public static <T> T execute(String script, Class<T> returnType, List<String> keys, Object... args) {
        return getRedisTemplate().execute(
                new DefaultRedisScript<>(script, returnType), keys, args);
    }

    /**
     * 执行Lua脚本
     *
     * @param redisScript RedisScript对象
     * @param keys 键列表
     * @param args 参数列表
     * @return 脚本执行结果
     */
    public static <T> T execute(RedisScript<T> redisScript, List<String> keys, Object... args) {
        return getRedisTemplate().execute(redisScript, keys, args);
    }


}
