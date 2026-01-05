package org.tbox.redis.ratelimit.annotation;

import org.tbox.redis.ratelimit.RateLimitMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    RateLimitMode mode() default RateLimitMode.TOKEN_BUCKET;

    /**
     * Key 支持两种写法：
     * 1) 普通字符串：直接作为 key
     * 2) SpEL：包含 '#' 的表达式（如 "#userId"、"#p0"、"#req.id"）
     */
    String key() default "";

    /**
     * key 前缀，避免与业务 key 冲突
     */
    String prefix() default "rate:";

    /**
     * 滑动窗口：窗口内最大请求数
     */
    int maxRequests() default 10;

    /**
     * 滑动窗口：窗口大小（秒）
     */
    int windowSeconds() default 1;

    /**
     * 令牌桶：本次申请令牌数
     */
    long permits() default 1;

    /**
     * 令牌桶：生成速率（每 interval 个时间单位生成 rate 个令牌）
     */
    long rate() default 10;

    long interval() default 1;

    TimeUnit intervalUnit() default TimeUnit.SECONDS;

    /**
     * 被限流时的提示文案
     */
    String message() default "请求过于频繁，请稍后再试";
}

