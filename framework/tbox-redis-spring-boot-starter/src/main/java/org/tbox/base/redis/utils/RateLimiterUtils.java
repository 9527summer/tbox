package org.tbox.base.redis.utils;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.tbox.base.core.context.ApplicationContextHolder;

import java.util.concurrent.TimeUnit;


/**
 * 限流工具类
 * 支持两种限流算法：
 * 1. 滑动窗口 - 基于Redis ZSet实现，适合精确控制时间窗口内的请求数
 * 2. 令牌桶 - 基于Redisson RRateLimiter实现，适合平滑限流
 */
public class RateLimiterUtils {

    /** 滑动窗口限流key前缀 */
    private static final String SLIDING_WINDOW_PREFIX = "rate:sliding:";

    /** 令牌桶限流key前缀 */
    private static final String TOKEN_BUCKET_PREFIX = "rate:token:";

    private static volatile RedissonClient redissonClient;

    private RateLimiterUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    private static RedissonClient getRedissonClient() {
        if (redissonClient == null) {
            synchronized (RateLimiterUtils.class) {
                if (redissonClient == null) {
                    try {
                        redissonClient = ApplicationContextHolder.getBean(RedissonClient.class);
                    } catch (Exception e) {
                        return null;
                    }
                }
            }
        }
        return redissonClient;
    }

    // ==================== 滑动窗口算法 ====================

    /**
     * 滑动窗口限流（基于Redis ZSet）
     * <p>
     * 原理：使用ZSet存储每次请求的时间戳，统计时间窗口内的请求数
     * 优点：精确控制时间窗口，不会出现临界问题
     * 缺点：高并发下ZSet可能较大
     *
     * @param key           限流标识（如：接口名、用户ID、IP等）
     * @param maxRequests   时间窗口内最大请求数
     * @param windowSeconds 时间窗口大小（秒）
     * @return true-允许通过，false-被限流
     */
    public static boolean slidingWindowAllow(String key, int maxRequests, int windowSeconds) {
        String redisKey = SLIDING_WINDOW_PREFIX + key;
        long now = System.currentTimeMillis();
        long windowStart = now - windowSeconds * 1000L;

        // 使用Lua脚本保证原子性
        String script =
                "local key = KEYS[1] " +
                        "local now = tonumber(ARGV[1]) " +
                        "local windowStart = tonumber(ARGV[2]) " +
                        "local maxRequests = tonumber(ARGV[3]) " +
                        "local expireTime = tonumber(ARGV[4]) " +
                        // 移除窗口外的请求记录
                        "redis.call('ZREMRANGEBYSCORE', key, 0, windowStart) " +
                        // 统计当前窗口内的请求数
                        "local count = redis.call('ZCARD', key) " +
                        // 判断是否超过限制
                        "if count < maxRequests then " +
                        "    redis.call('ZADD', key, now, now .. '-' .. math.random()) " +
                        "    redis.call('EXPIRE', key, expireTime) " +
                        "    return 1 " +
                        "else " +
                        "    return 0 " +
                        "end";

        Long result = RedisUtils.execute(
                script,
                Long.class,
                java.util.Collections.singletonList(redisKey),
                String.valueOf(now),
                String.valueOf(windowStart),
                String.valueOf(maxRequests),
                String.valueOf(windowSeconds + 1)
        );

        return Long.valueOf(1).equals(result);
    }

    /**
     * 滑动窗口限流（简化版，带默认窗口1秒）
     *
     * @param key         限流标识
     * @param maxRequests 每秒最大请求数
     * @return true-允许通过，false-被限流
     */
    public static boolean slidingWindowAllow(String key, int maxRequests) {
        return slidingWindowAllow(key, maxRequests, 1);
    }

    /**
     * 获取滑动窗口当前请求数
     *
     * @param key           限流标识
     * @param windowSeconds 时间窗口大小（秒）
     * @return 当前窗口内的请求数
     */
    public static long slidingWindowCount(String key, int windowSeconds) {
        String redisKey = SLIDING_WINDOW_PREFIX + key;
        long now = System.currentTimeMillis();
        long windowStart = now - windowSeconds * 1000L;

        // 先清理过期数据
        RedisUtils.zRemoveRangeByScore(redisKey, 0, windowStart);
        // 统计当前数量
        Long count = RedisUtils.zSize(redisKey);
        return count != null ? count : 0;
    }

    /**
     * 重置滑动窗口
     *
     * @param key 限流标识
     */
    public static void slidingWindowReset(String key) {
        RedisUtils.delete(SLIDING_WINDOW_PREFIX + key);
    }

    // ==================== 令牌桶算法 ====================

    /**
     * 令牌桶限流（基于Redisson RRateLimiter）
     * <p>
     * 原理：以固定速率向桶中添加令牌，请求需要获取令牌才能通过
     * 优点：平滑限流，允许一定程度的突发流量
     * 缺点：需要Redisson支持
     *
     * @param key      限流标识
     * @param rate     令牌生成速率（每个时间单位生成的令牌数）
     * @param interval 时间间隔
     * @param unit     时间单位
     * @return true-允许通过，false-被限流
     */
    public static boolean tokenBucketAllow(String key, long rate, long interval, TimeUnit unit) {
        return tokenBucketTryAcquire(key, 1, rate, interval, unit);
    }

    /**
     * 令牌桶限流（每秒速率）
     *
     * @param key         限流标识
     * @param ratePerSec  每秒令牌生成数
     * @return true-允许通过，false-被限流
     */
    public static boolean tokenBucketAllow(String key, long ratePerSec) {
        return tokenBucketAllow(key, ratePerSec, 1, TimeUnit.SECONDS);
    }

    /**
     * 令牌桶尝试获取指定数量的令牌
     *
     * @param key      限流标识
     * @param permits  需要获取的令牌数
     * @param rate     令牌生成速率
     * @param interval 时间间隔
     * @param unit     时间单位
     * @return true-获取成功，false-获取失败
     */
    public static boolean tokenBucketTryAcquire(String key, long permits, long rate, long interval, TimeUnit unit) {
        RedissonClient client = getRedissonClient();
        if (client == null) {
            // Redisson未配置，默认放行
            return true;
        }

        String redisKey = TOKEN_BUCKET_PREFIX + key;
        RRateLimiter rateLimiter = client.getRateLimiter(redisKey);

        // 初始化令牌桶（如果已存在则不会重复初始化）
        rateLimiter.trySetRate(
                RateType.OVERALL,
                rate,
                interval,
                convertToRateIntervalUnit(unit)
        );

        return rateLimiter.tryAcquire(permits);
    }

    /**
     * 令牌桶阻塞获取令牌
     *
     * @param key      限流标识
     * @param permits  需要获取的令牌数
     * @param timeout  最大等待时间
     * @param unit     时间单位
     * @param rate     令牌生成速率（每秒）
     * @return true-获取成功，false-超时
     */
    public static boolean tokenBucketAcquire(String key, long permits, long timeout, TimeUnit unit, long rate) {
        RedissonClient client = getRedissonClient();
        if (client == null) {
            return true;
        }

        String redisKey = TOKEN_BUCKET_PREFIX + key;
        RRateLimiter rateLimiter = client.getRateLimiter(redisKey);

        // 初始化令牌桶
        rateLimiter.trySetRate(RateType.OVERALL, rate, 1, RateIntervalUnit.SECONDS);

        return rateLimiter.tryAcquire(permits, timeout, unit);
    }

    private static RateIntervalUnit convertToRateIntervalUnit(TimeUnit unit) {
        if (unit == null) {
            return RateIntervalUnit.SECONDS;
        }
        switch (unit) {
            case MILLISECONDS:
                return RateIntervalUnit.MILLISECONDS;
            case SECONDS:
                return RateIntervalUnit.SECONDS;
            case MINUTES:
                return RateIntervalUnit.MINUTES;
            case HOURS:
                return RateIntervalUnit.HOURS;
            case DAYS:
                return RateIntervalUnit.DAYS;
            default:
                return RateIntervalUnit.SECONDS;
        }
    }
}
