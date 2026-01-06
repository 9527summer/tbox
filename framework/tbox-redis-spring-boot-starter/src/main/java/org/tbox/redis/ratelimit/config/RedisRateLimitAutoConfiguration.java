package org.tbox.redis.ratelimit.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tbox.redis.ratelimit.RateLimitChecker;
import org.tbox.redis.ratelimit.aspect.RateLimitAspect;
import org.tbox.redis.ratelimit.impl.DefaultRateLimitChecker;

@Configuration
@ConditionalOnClass(RateLimitAspect.class)
public class RedisRateLimitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RateLimitChecker rateLimitChecker(ApplicationContext applicationContext) {
        return new DefaultRateLimitChecker(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitAspect rateLimitAspect(RateLimitChecker rateLimitChecker) {
        return new RateLimitAspect(rateLimitChecker);
    }
}

