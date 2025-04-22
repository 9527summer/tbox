package org.tbox.dapper.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tbox.dapper.mq.kafka.TracingKafkaConsumerInterceptor;
import org.tbox.dapper.mq.kafka.TracingKafkaProducerInterceptor;
import org.tbox.dapper.mq.rocketmq.TracingRocketMQConsumerHook;
import org.tbox.dapper.mq.rocketmq.TracingRocketMQConsumerInterceptor;
import org.tbox.dapper.mq.rocketmq.TracingRocketMQProducerHook;
import org.tbox.dapper.mq.rocketmq.TracingRocketMQProducerInterceptor;

/**
 * 消息队列追踪自动配置类
 * 只要检测到相关类存在，就自动注册追踪拦截器，无需用户配置
 */
@Configuration
@ConditionalOnProperty(prefix = "tbox.tracer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TracingMQAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TracingMQAutoConfiguration.class);
    
    /**
     * RocketMQ追踪配置
     * 检测到RocketMQ类存在时自动启用
     */
    @Configuration
    @ConditionalOnClass(name = "org.apache.rocketmq.client.producer.DefaultMQProducer")
    public static class RocketMQTracingConfiguration {
        
        @Value("${spring.application.name:unknown-service}")
        private String applicationName;
        
        @Bean
        @ConditionalOnMissingBean
        public TracingRocketMQProducerInterceptor tracingRocketMQProducerInterceptor() {
            log.debug("自动注册RocketMQ生产者追踪拦截器");
            return new TracingRocketMQProducerInterceptor(applicationName);
        }
        
        @Bean
        @ConditionalOnMissingBean
        public TracingRocketMQConsumerInterceptor tracingRocketMQConsumerInterceptor() {
            log.debug("自动注册RocketMQ消费者追踪拦截器");
            return new TracingRocketMQConsumerInterceptor(applicationName);
        }
        
        @Bean
        @ConditionalOnMissingBean
        public TracingRocketMQProducerHook tracingRocketMQProducerHook() {
            log.debug("自动注册RocketMQ生产者追踪钩子");
            return new TracingRocketMQProducerHook(applicationName);
        }
        
        @Bean
        @ConditionalOnMissingBean
        public TracingRocketMQConsumerHook tracingRocketMQConsumerHook() {
            log.debug("自动注册RocketMQ消费者追踪钩子");
            return new TracingRocketMQConsumerHook(applicationName);
        }
    }
    
    /**
     * Kafka追踪配置
     * 检测到Kafka类存在时自动启用
     */
    @Configuration
    @ConditionalOnClass(name = "org.apache.kafka.clients.producer.KafkaProducer")
    public static class KafkaTracingConfiguration {
        
        @Value("${spring.application.name:unknown-service}")
        private String applicationName;
        
        @Bean
        public TracingKafkaProducerInterceptor tracingKafkaProducerInterceptor() {
            log.debug("自动注册Kafka生产者追踪拦截器");
            return new TracingKafkaProducerInterceptor(applicationName);
        }
        
        @Bean
        @SuppressWarnings({"unchecked", "rawtypes"})
        public TracingKafkaConsumerInterceptor tracingKafkaConsumerInterceptor() {
            log.debug("自动注册Kafka消费者追踪拦截器: applicationName={}", applicationName);
            return new TracingKafkaConsumerInterceptor(applicationName);
        }
    }
} 