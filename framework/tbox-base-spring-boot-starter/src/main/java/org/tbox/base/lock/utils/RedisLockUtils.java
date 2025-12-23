package org.tbox.base.lock.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.StringUtils;
import org.tbox.base.redis.utils.RedisUtils;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁工具类
 * 提供两种锁实现：
 * 1. 带requestId的锁 - 可以验证锁的持有者
 * 2. 简单锁 - 不验证持有者，适用于简单场景
 * 
 * 每种锁都支持设置过期时间或永久锁
 */
public class RedisLockUtils {

    private static final Logger log = LoggerFactory.getLogger(RedisLockUtils.class);

    // 锁相关脚本 - 改用更安全的实现
    private static final String LOCK_WITH_ID_SCRIPT =
            "local expireTime = tonumber(ARGV[2]) or 0\n" +
            "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then\n" +
            "   if expireTime > 0 then\n" +
            "       redis.call('pexpire', KEYS[1], expireTime)\n" +
            "   end\n" +
            "   return 1\n" +
            "else\n" +
            "   return 0\n" +
            "end";

    private static final String UNLOCK_WITH_ID_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then\n" +
            "   return redis.call('del', KEYS[1])\n" +
            "else\n" +
            "   return 0\n" +
            "end";

    private static final String SIMPLE_LOCK_SCRIPT =
            "local expireTime = tonumber(ARGV[1]) or 0\n" +
            "if redis.call('setnx', KEYS[1], 1) == 1 then\n" +
            "   if expireTime > 0 then\n" +
            "       redis.call('pexpire', KEYS[1], expireTime)\n" +
            "   end\n" +
            "   return 1\n" +
            "else\n" +
            "   return 0\n" +
            "end";

    //======================== 分布式锁 - 基本方法 ========================//

    /**
     * 获取简单分布式锁（永久锁，不自动过期）
     * 
     * @param lockKey 锁的键名
     * @return 是否成功获取锁
     */
    public static boolean lock(String lockKey) {
        return lock(lockKey, 0, TimeUnit.SECONDS);
    }

    /**
     * 获取简单分布式锁（过期时间单位为秒）
     * 
     * @param lockKey 锁的键名
     * @param expireTime 过期时间（秒）
     * @return 是否成功获取锁
     */
    public static boolean lock(String lockKey, long expireTime) {
        return lock(lockKey, expireTime, TimeUnit.SECONDS);
    }

    /**
     * 获取简单分布式锁（指定时间单位）
     * 当expireTime大于0时，锁会在指定时间后自动过期
     * 当expireTime等于0时，锁不会自动过期，需要手动释放
     * 
     * @param lockKey 锁的键名
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     * @return 是否成功获取锁
     */
    public static boolean lock(String lockKey, long expireTime, TimeUnit timeUnit) {
        if (StringUtils.isEmpty(lockKey)) {
            return false;
        }
        
        try {
            RedisScript<Long> script = new DefaultRedisScript<>(SIMPLE_LOCK_SCRIPT, Long.class);
            Long expireTimeMillis = expireTime > 0 ? timeUnit.toMillis(expireTime) : 0;
            log.debug("正在获取锁 key:{}, expireTime:{}ms", lockKey, expireTimeMillis);
            
            Long result = RedisUtils.getRedisTemplate().execute(
                    script,
                    Collections.singletonList(lockKey),
                    expireTimeMillis
            );
            log.debug("获取锁结果 key:{}, result:{}", lockKey, result);
            return result != null && result == 1;
        } catch (Exception e) {
            log.error("获取锁时出错: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取带requestId的分布式锁（永久锁，不自动过期）
     * 
     * @param lockKey 锁的键名
     * @param requestId 请求标识
     * @return 是否成功获取锁
     */
    public static boolean lock(String lockKey, String requestId) {
        return lock(lockKey, requestId, 0, TimeUnit.SECONDS);
    }

    /**
     * 获取带requestId的分布式锁（过期时间单位为秒）
     * 
     * @param lockKey 锁的键名
     * @param requestId 请求标识
     * @param expireTime 过期时间（秒）
     * @return 是否成功获取锁
     */
    public static boolean lock(String lockKey, String requestId, long expireTime) {
        return lock(lockKey, requestId, expireTime, TimeUnit.SECONDS);
    }

    /**
     * 获取带requestId的分布式锁（指定时间单位）
     * 当expireTime大于0时，锁会在指定时间后自动过期
     * 当expireTime等于0时，锁不会自动过期，需要手动释放
     * 
     * @param lockKey 锁的键名
     * @param requestId 请求标识
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     * @return 是否成功获取锁
     */
    public static boolean lock(String lockKey, String requestId, long expireTime, TimeUnit timeUnit) {
        try {
            RedisScript<Long> script = new DefaultRedisScript<>(LOCK_WITH_ID_SCRIPT, Long.class);
            Long expireTimeMillis = expireTime > 0 ? timeUnit.toMillis(expireTime) : 0;
            log.debug("正在获取锁(带ID) key:{}, id:{}, expireTime:{}ms", lockKey, requestId, expireTimeMillis);
            
            Long result = RedisUtils.getRedisTemplate().execute(
                    script,
                    Collections.singletonList(lockKey),
                    requestId,
                    expireTimeMillis
            );
            log.debug("获取锁(带ID)结果 key:{}, id:{}, result:{}", lockKey, requestId, result);
            return result != null && result == 1;
        } catch (Exception e) {
            log.error("获取锁时出错: {}", e.getMessage(), e);
            return false;
        }
    }

    //======================== 分布式锁 - 释放锁 ========================//

    /**
     * 释放简单分布式锁
     * 
     * @param lockKey 锁的键名
     * @return 是否成功释放锁
     */
    public static boolean unlock(String lockKey) {
        if (StringUtils.isEmpty(lockKey)) {
            return false;
        }
        try {
            log.debug("正在释放锁 key:{}", lockKey);
            Boolean result = RedisUtils.getRedisTemplate().delete(lockKey);
            log.debug("释放锁结果 key:{}, result:{}", lockKey, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("释放锁时出错: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 释放带requestId的分布式锁（验证锁的持有者）
     * 
     * @param lockKey 锁的键名
     * @param requestId 请求标识
     * @return 是否成功释放锁
     */
    public static boolean unlock(String lockKey, String requestId) {
        if (StringUtils.isEmpty(lockKey) || StringUtils.isEmpty(requestId)) {
            return false;
        }
        try {
            log.debug("正在释放锁(带ID) key:{}, id:{}", lockKey, requestId);
            RedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_WITH_ID_SCRIPT, Long.class);
            Long result = RedisUtils.getRedisTemplate().execute(
                    script,
                    Collections.singletonList(lockKey),
                    requestId
            );
            log.debug("释放锁(带ID)结果 key:{}, id:{}, result:{}", lockKey, requestId, result);
            return result != null && result == 1;
        } catch (Exception e) {
            log.error("释放锁时出错: {}", e.getMessage(), e);
            return false;
        }
    }

    //======================== 分布式锁 - 尝试获取锁 ========================//

    /**
     * 尝试获取简单分布式锁（超时和过期时间单位为秒）
     * 
     * @param lockKey 锁的键名
     * @param expireTime 过期时间（秒）
     * @param waitTime 最大等待时间（秒）
     * @return 是否成功获取锁
     */
    public static boolean tryLock(String lockKey, long expireTime, long waitTime) throws InterruptedException {
        return tryLock(lockKey, expireTime, waitTime, TimeUnit.SECONDS);
    }

    /**
     * 尝试获取简单分布式锁（指定时间单位）
     * 
     * @param lockKey 锁的键名
     * @param expireTime 过期时间
     * @param waitTime 最大等待时间
     * @param timeUnit 时间单位
     * @return 是否成功获取锁
     */
    public static boolean tryLock(String lockKey, long expireTime, long waitTime, TimeUnit timeUnit) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long waitTimeMillis = timeUnit.toMillis(waitTime);

        do {
            if (lock(lockKey, expireTime, timeUnit)) {
                return true;
            }
            Thread.sleep(100);
        } while ((System.currentTimeMillis() - startTime) < waitTimeMillis);

        return false;
    }

    /**
     * 尝试获取带requestId的分布式锁（超时和过期时间单位为秒）
     * 
     * @param lockKey 锁的键名
     * @param requestId 请求标识
     * @param expireTime 过期时间（秒）
     * @param waitTime 最大等待时间（秒）
     * @return 是否成功获取锁
     */
    public static boolean tryLock(String lockKey, String requestId, long expireTime, long waitTime) throws InterruptedException {
        return tryLock(lockKey, requestId, expireTime, waitTime, TimeUnit.SECONDS);
    }

    /**
     * 尝试获取带requestId的分布式锁（指定时间单位）
     * 
     * @param lockKey 锁的键名
     * @param requestId 请求标识
     * @param expireTime 过期时间
     * @param waitTime 最大等待时间
     * @param timeUnit 时间单位
     * @return 是否成功获取锁
     */
    public static boolean tryLock(String lockKey, String requestId, long expireTime, long waitTime, TimeUnit timeUnit) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long waitTimeMillis = timeUnit.toMillis(waitTime);

        do {
            if (lock(lockKey, requestId, expireTime, timeUnit)) {
                return true;
            }
            Thread.sleep(100);
        } while ((System.currentTimeMillis() - startTime) < waitTimeMillis);

        return false;
    }
}


