package org.tbox.redis.ratelimit;

import java.util.concurrent.TimeUnit;

public final class RateLimitRequest {

    private final RateLimitMode mode;
    private final String key;

    private final int maxRequests;
    private final int windowSeconds;

    private final long permits;
    private final long rate;
    private final long interval;
    private final TimeUnit intervalUnit;

    public RateLimitRequest(
            RateLimitMode mode,
            String key,
            int maxRequests,
            int windowSeconds,
            long permits,
            long rate,
            long interval,
            TimeUnit intervalUnit
    ) {
        this.mode = mode;
        this.key = key;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
        this.permits = permits;
        this.rate = rate;
        this.interval = interval;
        this.intervalUnit = intervalUnit;
    }

    public RateLimitMode getMode() {
        return mode;
    }

    public String getKey() {
        return key;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public long getPermits() {
        return permits;
    }

    public long getRate() {
        return rate;
    }

    public long getInterval() {
        return interval;
    }

    public TimeUnit getIntervalUnit() {
        return intervalUnit;
    }
}

