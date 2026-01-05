package org.tbox.redis.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.tbox.base.redis.config.RedisSerializerConfig;

@AutoConfiguration
@ConditionalOnClass(RedisTemplate.class)
@ConditionalOnBean(RedisConnectionFactory.class)
@Import(RedisSerializerConfig.class)
public class TboxRedisAutoConfiguration {
}
