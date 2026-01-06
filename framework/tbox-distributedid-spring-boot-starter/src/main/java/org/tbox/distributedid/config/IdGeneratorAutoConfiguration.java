package org.tbox.distributedid.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.tbox.distributedid.core.RandomIdGenerator;
import org.tbox.distributedid.core.RedisIdGenerator;
import org.tbox.distributedid.core.TimeRandomIdGenerator;
import org.tbox.distributedid.core.TimeRedisIdGenerator;

@Configuration
public class IdGeneratorAutoConfiguration {

    @Configuration
    public static class RandomIdGeneratorConfiguration {
        @Bean
        @ConditionalOnMissingBean(RedisIdGenerator.class)
        public RandomIdGenerator defaultIdGenerator() {
            return new RandomIdGenerator();
        }

        @Bean
        @ConditionalOnMissingBean(TimeRedisIdGenerator.class)
        public TimeRandomIdGenerator timeRandomIdGenerator() {
            return new TimeRandomIdGenerator();
        }
    }


    @Configuration
    @ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
    public static class RedisIdGeneratorConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public RedisIdGenerator redisIdGenerator(StringRedisTemplate redisTemplate) {
            return new RedisIdGenerator(redisTemplate);
        }

        @Bean
        @ConditionalOnMissingBean
        public TimeRedisIdGenerator timeRedisIdGenerator(StringRedisTemplate redisTemplate) {
            return new TimeRedisIdGenerator(redisTemplate);
        }
        
//        @Bean
//        @ConditionalOnMissingBean(ReadableRedisIdGenerator.class)
//        public ReadableRedisIdGenerator readableRedisIdGenerator(RedisTemplate<String, Object> redisTemplate) {
//            return new ReadableRedisIdGenerator(redisTemplate);
//        }
    }
}
