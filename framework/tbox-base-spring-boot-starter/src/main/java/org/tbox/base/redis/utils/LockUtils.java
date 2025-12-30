package org.tbox.base.redis.utils;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.tbox.base.core.context.ApplicationContextHolder;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class LockUtils {
    private static final String LOCK_PREFIX = "lock:";
    private static volatile RedissonClient redissonClient;

    private LockUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }


    private static RedissonClient getClient() {
        if (redissonClient == null) {
            synchronized (LockUtils.class) {
                if (redissonClient == null) {
                    redissonClient = ApplicationContextHolder.getBean(RedissonClient.class);
                }
            }
        }
        return redissonClient;
    }

    /**
     * 尝试获取锁
     *
     * @param key      锁标识
     * @param waitTime 最大等待时间
     * @param unit     时间单位
     * @return 是否获取成功
     */
    public static boolean tryLock(String key, long waitTime, TimeUnit unit) {
        try {
            return getClient().getLock(LOCK_PREFIX + key).tryLock(waitTime, -1, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 尝试获取锁（指定锁持有时间）
     *
     * @param key       锁标识
     * @param waitTime  最大等待时间
     * @param leaseTime 锁持有时间（-1表示启用看门狗自动续期）
     * @param unit      时间单位
     * @return 是否获取成功
     */
    public static boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        try {
            return getClient().getLock(LOCK_PREFIX + key).tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 尝试获取锁（不等待）
     */
    public static boolean tryLock(String key) {
        return tryLock(key, 0, TimeUnit.SECONDS);
    }

    /**
     * 释放锁
     */
    public static void unlock(String key) {
        RLock lock = getClient().getLock(LOCK_PREFIX + key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 加锁执行（推荐使用）
     *
     * @param key      锁标识
     * @param waitTime 最大等待时间
     * @param unit     时间单位
     * @param supplier 业务逻辑
     * @return 业务返回值
     * @throws RuntimeException 获取锁失败时抛出
     */
    public static <T> T executeWithLock(String key, long waitTime, TimeUnit unit, Supplier<T> supplier) {
        if (!tryLock(key, waitTime, unit)) {
            throw new RuntimeException("获取锁失败: " + key);
        }
        try {
            return supplier.get();
        } finally {
            unlock(key);
        }
    }

    /**
     * 加锁执行（默认等待3秒）
     */
    public static <T> T executeWithLock(String key, Supplier<T> supplier) {
        return executeWithLock(key, 3, TimeUnit.SECONDS, supplier);
    }

    /**
     * 加锁执行（无返回值）
     */
    public static void executeWithLock(String key, long waitTime, TimeUnit unit, Runnable runnable) {
        executeWithLock(key, waitTime, unit, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 加锁执行（无返回值，默认等待3秒）
     */
    public static void executeWithLock(String key, Runnable runnable) {
        executeWithLock(key, 3, TimeUnit.SECONDS, runnable);
    }

    /**
     * 尝试加锁执行（获取锁失败不抛异常）
     *
     * @param key      锁标识
     * @param waitTime 最大等待时间
     * @param unit     时间单位
     * @param supplier 业务逻辑
     * @param fallback 获取锁失败时的降级逻辑
     * @return 业务返回值
     */
    public static <T> T tryExecuteWithLock(String key, long waitTime, TimeUnit unit,
                                           Supplier<T> supplier, Supplier<T> fallback) {
        if (!tryLock(key, waitTime, unit)) {
            return fallback.get();
        }
        try {
            return supplier.get();
        } finally {
            unlock(key);
        }
    }
}
