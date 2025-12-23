package org.tbox.base.redis.utils;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.tbox.base.core.context.ApplicationContextHolder;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class CacheUtils {

    private static RedissonClient redissonClient;

    private static RedissonClient getRedissonClient() {
        if (redissonClient == null) {
            synchronized (CacheUtils.class) {
                if (redissonClient == null) {
                    redissonClient = ApplicationContextHolder.getBean(RedissonClient.class);
                }
            }
        }
        return redissonClient;
    }

    /**
     * 带缓存的查询（防止缓存穿透）
     */
    public static <T> T getWithPassThrough(String key,
                                           Function<String, T> dbFallback,
                                           long timeout, TimeUnit timeUnit) {
        // 1. 查缓存
        T cached = RedisUtils.get(key);
        if (cached != null) {
            return cached;
        }

        // 2. 查数据库
        T data = dbFallback.apply(key);

        // 3. 空值也缓存（防止穿透），但时间短
        if (data == null) {
            RedisUtils.set(key, null, 2, TimeUnit.MINUTES);
            return null;
        }

        // 4. 写入缓存
        RedisUtils.set(key, data, timeout, timeUnit);
        return data;
    }

    /**
     * 带互斥锁的查询（防止缓存击穿）
     */
    public static <T> T getWithMutex(String key,
                                     Function<String, T> dbFallback,
                                     long timeout, TimeUnit timeUnit) {
        // 1. 查缓存
        T cached = RedisUtils.get(key);
        if (cached != null) {
            return cached;
        }


        if (getRedissonClient() == null) {
            return null;
        }

        // 2. 获取互斥锁
        String lockKey = "lock:cache:" + key;
        RLock lock = getRedissonClient().getLock(lockKey);
        lock.lock(5, TimeUnit.SECONDS);
        try {
            // 3. 双重检查
            cached = RedisUtils.get(key);
            if (cached != null) {
                return cached;
            }

            // 4. 查数据库并缓存
            T data = dbFallback.apply(key);
            if (data == null) {
                RedisUtils.set(key, null, 2, TimeUnit.MINUTES);
                return null;
            }
            RedisUtils.set(key, data, timeout, timeUnit);
            return data;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
