package org.tbox.distributedid.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.tbox.distributedid.core.RandomIdGenerator;
import org.tbox.distributedid.core.RedisIdGenerator;
import org.tbox.distributedid.core.TimeRandomIdGenerator;
import org.tbox.distributedid.core.TimeRedisIdGenerator;

@AutoConfiguration
public class IdGeneratorAutoConfiguration {

    @Configuration
    @ConditionalOnMissingBean(type = "org.springframework.data.redis.core.StringRedisTemplate")
    public static class RandomIdGeneratorConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public RandomIdGenerator defaultIdGenerator() {
            return new RandomIdGenerator();
        }

        @Bean
        @ConditionalOnMissingBean
        public TimeRandomIdGenerator timeRandomIdGenerator() {
            return new TimeRandomIdGenerator();
        }
    }


    @Configuration
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnBean(StringRedisTemplate.class)
    public static class RedisIdGeneratorConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public RedisIdGenerator redisIdGenerator(StringRedisTemplate stringRedisTemplate) {
            return new RedisIdGenerator(stringRedisTemplate);
        }

        @Bean
        @ConditionalOnMissingBean
        public TimeRedisIdGenerator timeRedisIdGenerator(StringRedisTemplate stringRedisTemplate) {
            return new TimeRedisIdGenerator(stringRedisTemplate);
        }
        
//        @Bean
//        @ConditionalOnMissingBean(ReadableRedisIdGenerator.class)
//        public ReadableRedisIdGenerator readableRedisIdGenerator(RedisTemplate<String, Object> redisTemplate) {
//            return new ReadableRedisIdGenerator(redisTemplate);
//        }
    }
}
