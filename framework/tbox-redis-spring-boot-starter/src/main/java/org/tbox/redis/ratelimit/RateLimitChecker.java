package org.tbox.redis.ratelimit;

public interface RateLimitChecker {
    boolean allow(RateLimitRequest request);
}

