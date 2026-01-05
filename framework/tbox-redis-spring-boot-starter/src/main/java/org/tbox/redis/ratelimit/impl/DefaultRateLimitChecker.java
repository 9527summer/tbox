package org.tbox.redis.ratelimit.impl;

import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.tbox.base.redis.utils.RateLimiterUtils;
import org.tbox.redis.ratelimit.RateLimitChecker;
import org.tbox.redis.ratelimit.RateLimitMode;
import org.tbox.redis.ratelimit.RateLimitRequest;

public class DefaultRateLimitChecker implements RateLimitChecker {

    private final ApplicationContext applicationContext;

    public DefaultRateLimitChecker(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean allow(RateLimitRequest request) {
        if (request.getMode() == RateLimitMode.SLIDING_WINDOW) {
            if (!applicationContext.containsBean("redisConnectionFactory") && applicationContext.getBeanNamesForType(RedisConnectionFactory.class).length == 0) {
                return true;
            }
            return RateLimiterUtils.slidingWindowAllow(request.getKey(), request.getMaxRequests(), request.getWindowSeconds());
        }

        return RateLimiterUtils.tokenBucketTryAcquire(
                request.getKey(),
                request.getPermits(),
                request.getRate(),
                request.getInterval(),
                request.getIntervalUnit()
        );
    }
}

