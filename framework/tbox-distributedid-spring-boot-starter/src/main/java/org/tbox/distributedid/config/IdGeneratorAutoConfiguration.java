package org.tbox.distributedid.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.tbox.distributedid.core.RandomIdGenerator;
import org.tbox.distributedid.core.RedisIdGenerator;

@Configuration
public class IdGeneratorAutoConfiguration {

    @Configuration
    public static class RandomIdGeneratorConfiguration {
        @Bean
        @ConditionalOnMissingBean(RedisIdGenerator.class)
        public RandomIdGenerator defaultIdGenerator() {
            return new RandomIdGenerator();
        }
    }


    @Configuration
    @ConditionalOnClass(name = "org.springframework.data.redis.core.RedisTemplate")
    public static class RedisIdGeneratorConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public RedisIdGenerator redisIdGenerator(RedisTemplate redisTemplate) {
            return new RedisIdGenerator(redisTemplate);
        }
    }
}
