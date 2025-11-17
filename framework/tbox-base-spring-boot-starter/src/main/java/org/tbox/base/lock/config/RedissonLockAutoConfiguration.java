package org.tbox.base.lock.config;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tbox.base.lock.service.LockService;
import org.tbox.base.lock.service.RedissonLockService;

@Configuration
@ConditionalOnClass(RedissonClient.class)
@AutoConfigureBefore(RedisTemplateLockAutoConfiguration.class)
public class RedissonLockAutoConfiguration {

    /**
     * 配置Redisson锁服务
     * @param redissonClient
     * @return
     */
    @Bean
    @ConditionalOnBean(RedissonClient.class)
    public LockService redissonLockService(RedissonClient redissonClient) {
        return new RedissonLockService(redissonClient);
    }
}
