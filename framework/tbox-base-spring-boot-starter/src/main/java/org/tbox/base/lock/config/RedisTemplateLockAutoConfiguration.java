package org.tbox.base.lock.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.tbox.base.lock.service.LockService;
import org.tbox.base.lock.service.RedisTemplateLockService;

@Configuration
@ConditionalOnClass(RedisTemplate.class)
@AutoConfigureAfter(RedissonLockAutoConfiguration.class)
public class RedisTemplateLockAutoConfiguration {

    /**
     * 配置redisTemplate锁
     * @return
     */
    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    @ConditionalOnMissingBean(LockService.class)
    public LockService redisTemplateLockService() {
        return new RedisTemplateLockService();
    }
}
