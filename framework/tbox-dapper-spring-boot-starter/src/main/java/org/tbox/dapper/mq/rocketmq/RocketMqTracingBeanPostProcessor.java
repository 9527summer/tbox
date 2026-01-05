package org.tbox.dapper.mq.rocketmq;

import io.micrometer.tracing.Tracer;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * 自动为 RocketMQ Producer/Consumer 注册 hook：
 * <ul>
 *     <li>Producer：发送前把 traceparent 写入 Message user properties</li>
 *     <li>Consumer：消费前从 Message user properties 提取 traceId/spanId 写入 MDC</li>
 * </ul>
 */
public class RocketMqTracingBeanPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(RocketMqTracingBeanPostProcessor.class);

    private final RocketMqSendTraceHook sendHook;
    private final RocketMqConsumeTraceHook consumeHook;

    public RocketMqTracingBeanPostProcessor(Tracer tracer) {
        this.sendHook = new RocketMqSendTraceHook(tracer);
        this.consumeHook = new RocketMqConsumeTraceHook(tracer);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DefaultMQProducer producer) {
            try {
                producer.getDefaultMQProducerImpl().registerSendMessageHook(sendHook);
            } catch (Throwable e) {
                log.warn("Register RocketMQ send hook failed for bean: {}", beanName, e);
            }
            return bean;
        }

        if (bean instanceof DefaultMQPushConsumer consumer) {
            try {
                consumer.getDefaultMQPushConsumerImpl().registerConsumeMessageHook(consumeHook);
            } catch (Throwable e) {
                log.warn("Register RocketMQ consume hook failed for bean: {}", beanName, e);
            }
            return bean;
        }

        return bean;
    }
}
