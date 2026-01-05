package org.tbox.dapper.mq;

import io.micrometer.tracing.Tracer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ContainerCustomizer;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.tbox.dapper.mq.kafka.KafkaTracingRecordInterceptor;
import org.tbox.dapper.mq.kafka.TracingProducer;
import org.tbox.dapper.mq.rocketmq.RocketMqTracingBeanPostProcessor;

/**
 * MQ Trace 串联自动配置（仅日志关联，W3C traceparent）。
 *
 * <p>结构参考调度模块：在 MQ 包下按不同 MQ 组件聚合配置，由总配置统一 import。</p>
 */
@AutoConfiguration
@ConditionalOnBean(Tracer.class)
@ConditionalOnProperty(prefix = "tbox.tracer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TracingMqAutoConfiguration {

    @ConditionalOnClass(name = "org.springframework.kafka.core.ProducerFactory")
    public static class KafkaAutoConfiguration {

        @Bean
        public DefaultKafkaProducerFactoryCustomizer kafkaTracingProducerFactoryCustomizer(Tracer tracer) {
            return factory -> factory.addPostProcessor(producer -> new TracingProducer<>(producer, tracer));
        }

        @Bean
        public ContainerCustomizer<Object, Object, ConcurrentMessageListenerContainer<Object, Object>> kafkaTracingContainerCustomizer(
                Tracer tracer) {
            return container -> container.setRecordInterceptor(new KafkaTracingRecordInterceptor<>(tracer));
        }
    }

    @ConditionalOnClass(name = "org.apache.rocketmq.client.producer.DefaultMQProducer")
    public static class RocketMqAutoConfiguration {

        @Bean
        public RocketMqTracingBeanPostProcessor rocketMqTracingBeanPostProcessor(Tracer tracer) {
            return new RocketMqTracingBeanPostProcessor(tracer);
        }
    }
}

