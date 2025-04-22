package org.tbox.base.lock.config;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.tbox.base.lock.service.LockService;
import org.tbox.base.lock.service.RedisTemplateLockService;
import org.tbox.base.lock.service.RedissonLockService;

@Configuration
public class LockAutoConfiguration {


    /**
     * 配置Redisson锁服务（优先使用）
     */
//    @Bean
//    @Primary
//    @ConditionalOnClass(RedissonClient.class)
//    @ConditionalOnBean(RedissonClient.class)
//    public LockService redissonLockService(RedissonClient redissonClient) {
//        return new RedissonLockService(redissonClient);
//    }

    /**
     * 配置RedisTemplate锁服务（当无Redisson时使用）
     */
    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    @ConditionalOnMissingBean(LockService.class)
    public LockService redisTemplateLockService() {
        return new RedisTemplateLockService();
    }

}
