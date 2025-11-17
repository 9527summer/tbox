package org.tbox.base.cache.utils;

import org.springframework.data.redis.core.RedisTemplate;
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
    
    // 私有构造函数，防止实例化
    private RedisUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    // 初始化方法，获取RedisTemplate
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
    
    //======================== 通用操作 ========================//
    
    /**
     * 删除一个或多个键
     */
    public static Long delete(String... keys) {
        return getRedisTemplate().delete(Arrays.asList(keys));
    }
    
    /**
     * 批量删除键
     */
    public static Long delete(Collection<String> keys) {
        return getRedisTemplate().delete(keys);
    }
    
    /**
     * 检查键是否存在
     */
    public static Boolean hasKey(String key) {
        return getRedisTemplate().hasKey(key);
    }
    
    /**
     * 设置过期时间（秒）
     */
    public static Boolean expire(String key, long timeout) {
        return expire(key, timeout, TimeUnit.SECONDS);
    }
    
    /**
     * 设置过期时间（指定时间单位）
     */
    public static Boolean expire(String key, long timeout, TimeUnit timeUnit) {
        return getRedisTemplate().expire(key, timeout, timeUnit);
    }
    
    /**
     * 获取过期时间
     */
    public static Long getExpire(String key, TimeUnit timeUnit) {
        return getRedisTemplate().getExpire(key, timeUnit);
    }
    
    //======================== String操作 ========================//
    
    /**
     * 设置键值对
     */
    public static void set(String key, Object value) {
        getRedisTemplate().opsForValue().set(key, value);
    }
    
    /**
     * 设置键值对及过期时间
     */
    public static void set(String key, Object value, long timeout, TimeUnit timeUnit) {
        getRedisTemplate().opsForValue().set(key, value, timeout, timeUnit);
    }
    
    /**
     * 获取值
     */
    public static Object get(String key) {
        return getRedisTemplate().opsForValue().get(key);
    }
    
    /**
     * 自增操作
     */
    public static Long increment(String key, long delta) {
        return getRedisTemplate().opsForValue().increment(key, delta);
    }
    
    /**
     * 自减操作
     */
    public static Long decrement(String key, long delta) {
        return getRedisTemplate().opsForValue().decrement(key, delta);
    }
    
    /**
     * 批量设置键值对
     */
    public static void multiSet(Map<String, Object> keyValueMap) {
        getRedisTemplate().opsForValue().multiSet(keyValueMap);
    }
    
    /**
     * 批量获取键值
     */
    public static List<Object> multiGet(Collection<String> keys) {
        return getRedisTemplate().opsForValue().multiGet(keys);
    }
    
    //======================== Hash操作 ========================//
    
    /**
     * 向Hash表中放入一个键值对
     */
    public static void hSet(String key, String hashKey, Object value) {
        getRedisTemplate().opsForHash().put(key, hashKey, value);
    }
    
    /**
     * 向Hash表中放入一个键值对，并设置过期时间
     */
    public static void hSet(String key, String hashKey, Object value, long timeout, TimeUnit timeUnit) {
        getRedisTemplate().opsForHash().put(key, hashKey, value);
        expire(key, timeout, timeUnit);
    }
    
    /**
     * 获取Hash表中的值
     */
    public static Object hGet(String key, String hashKey) {
        return getRedisTemplate().opsForHash().get(key, hashKey);
    }
    
    /**
     * 删除Hash表中的值
     */
    public static Long hDelete(String key, Object... hashKeys) {
        return getRedisTemplate().opsForHash().delete(key, hashKeys);
    }
    
    /**
     * 判断Hash表中是否有该键的值
     */
    public static Boolean hHasKey(String key, String hashKey) {
        return getRedisTemplate().opsForHash().hasKey(key, hashKey);
    }
    
    /**
     * Hash递增
     */
    public static Long hIncrement(String key, String hashKey, long delta) {
        return getRedisTemplate().opsForHash().increment(key, hashKey, delta);
    }
    
    /**
     * Hash递减
     */
    public static Long hDecrement(String key, String hashKey, long delta) {
        return getRedisTemplate().opsForHash().increment(key, hashKey, -delta);
    }
    
    /**
     * 获取Hash表中所有键值对
     */
    public static Map<Object, Object> hGetAll(String key) {
        return getRedisTemplate().opsForHash().entries(key);
    }
    
    /**
     * 批量设置Hash的键值对
     */
    public static void hMultiSet(String key, Map<String, Object> map) {
        getRedisTemplate().opsForHash().putAll(key, map);
    }
    
    /**
     * 获取Hash的所有键
     */
    public static Set<Object> hKeys(String key) {
        return getRedisTemplate().opsForHash().keys(key);
    }
    
    /**
     * 获取Hash的所有值
     */
    public static List<Object> hValues(String key) {
        return getRedisTemplate().opsForHash().values(key);
    }
    
    //======================== List操作 ========================//
    
    /**
     * 将元素放入列表左端
     */
    public static Long lLeftPush(String key, Object value) {
        return getRedisTemplate().opsForList().leftPush(key, value);
    }
    
    /**
     * 将多个元素放入列表左端
     */
    public static Long lLeftPushAll(String key, Object... values) {
        return getRedisTemplate().opsForList().leftPushAll(key, values);
    }
    
    /**
     * 将元素放入列表右端
     */
    public static Long lRightPush(String key, Object value) {
        return getRedisTemplate().opsForList().rightPush(key, value);
    }
    
    /**
     * 将多个元素放入列表右端
     */
    public static Long lRightPushAll(String key, Object... values) {
        return getRedisTemplate().opsForList().rightPushAll(key, values);
    }
    
    /**
     * 从列表左端弹出元素
     */
    public static Object lLeftPop(String key) {
        return getRedisTemplate().opsForList().leftPop(key);
    }
    
    /**
     * 从列表右端弹出元素
     */
    public static Object lRightPop(String key) {
        return getRedisTemplate().opsForList().rightPop(key);
    }
    
    /**
     * 获取列表长度
     */
    public static Long lSize(String key) {
        return getRedisTemplate().opsForList().size(key);
    }
    
    /**
     * 获取列表指定范围的元素
     */
    public static List<Object> lRange(String key, long start, long end) {
        return getRedisTemplate().opsForList().range(key, start, end);
    }
    
    /**
     * 获取列表中指定索引的元素
     */
    public static Object lIndex(String key, long index) {
        return getRedisTemplate().opsForList().index(key, index);
    }
    
    /**
     * 设置列表中指定索引的值
     */
    public static void lSet(String key, long index, Object value) {
        getRedisTemplate().opsForList().set(key, index, value);
    }
    
    /**
     * 从列表中删除值为value的元素
     */
    public static Long lRemove(String key, long count, Object value) {
        return getRedisTemplate().opsForList().remove(key, count, value);
    }
    
    //======================== Set操作 ========================//
    
    /**
     * 向集合添加元素
     */
    public static Long sAdd(String key, Object... values) {
        return getRedisTemplate().opsForSet().add(key, values);
    }
    
    /**
     * 获取集合中的所有元素
     */
    public static Set<Object> sMembers(String key) {
        return getRedisTemplate().opsForSet().members(key);
    }
    
    /**
     * 判断集合中是否存在元素
     */
    public static Boolean sIsMember(String key, Object value) {
        return getRedisTemplate().opsForSet().isMember(key, value);
    }
    
    /**
     * 获取集合的大小
     */
    public static Long sSize(String key) {
        return getRedisTemplate().opsForSet().size(key);
    }
    
    /**
     * 从集合中移除元素
     */
    public static Long sRemove(String key, Object... values) {
        return getRedisTemplate().opsForSet().remove(key, values);
    }
    
    /**
     * 随机获取集合中的元素
     */
    public static Object sRandomMember(String key) {
        return getRedisTemplate().opsForSet().randomMember(key);
    }
    
    /**
     * 随机获取集合中的多个元素
     */
    public static List<Object> sRandomMembers(String key, long count) {
        return getRedisTemplate().opsForSet().randomMembers(key, count);
    }
    
    /**
     * 获取两个集合的交集
     */
    public static Set<Object> sIntersect(String key1, String key2) {
        return getRedisTemplate().opsForSet().intersect(key1, key2);
    }
    
    /**
     * 获取两个集合的并集
     */
    public static Set<Object> sUnion(String key1, String key2) {
        return getRedisTemplate().opsForSet().union(key1, key2);
    }
    
    /**
     * 获取两个集合的差集
     */
    public static Set<Object> sDifference(String key1, String key2) {
        return getRedisTemplate().opsForSet().difference(key1, key2);
    }
    
    //======================== ZSet操作 ========================//
    
    /**
     * 向有序集合添加元素
     */
    public static Boolean zAdd(String key, Object value, double score) {
        return getRedisTemplate().opsForZSet().add(key, value, score);
    }
    
    /**
     * 获取有序集合中元素的分数
     */
    public static Double zScore(String key, Object value) {
        return getRedisTemplate().opsForZSet().score(key, value);
    }
    
    /**
     * 获取有序集合的大小
     */
    public static Long zSize(String key) {
        return getRedisTemplate().opsForZSet().size(key);
    }
    
    /**
     * 从有序集合中移除元素
     */
    public static Long zRemove(String key, Object... values) {
        return getRedisTemplate().opsForZSet().remove(key, values);
    }
    
    /**
     * 获取指定分数范围的元素
     */
    public static Set<Object> zRangeByScore(String key, double min, double max) {
        return getRedisTemplate().opsForZSet().rangeByScore(key, min, max);
    }
    
    /**
     * 为有序集合中的元素增加分数
     */
    public static Double zIncrementScore(String key, Object value, double delta) {
        return getRedisTemplate().opsForZSet().incrementScore(key, value, delta);
    }
    
    /**
     * 获取指定排名范围的元素
     */
    public static Set<Object> zRange(String key, long start, long end) {
        return getRedisTemplate().opsForZSet().range(key, start, end);
    }
    
    /**
     * 获取元素在有序集合中的排名（从0开始）
     */
    public static Long zRank(String key, Object value) {
        return getRedisTemplate().opsForZSet().rank(key, value);
    }
}
