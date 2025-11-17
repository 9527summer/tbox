package org.tbox.distributedid.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.tbox.distributedid.core.RedisIdGenerator;

/**
 * 分布式ID生成器自动配置
 */
@Configuration
public class DistributedIdAutoConfiguration {


    /**
     * 配置Redis ID生成器
     * @param redisTemplate Redis 模板
     * @return RedisIdGenerator 实例
     */
    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    @ConditionalOnMissingBean(RedisIdGenerator.class)
    public RedisIdGenerator redisIdGenerator(RedisTemplate redisTemplate) {
        return new RedisIdGenerator(redisTemplate);
    }
} 