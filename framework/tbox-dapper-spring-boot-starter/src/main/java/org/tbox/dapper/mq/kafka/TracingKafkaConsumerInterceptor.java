package org.tbox.dapper.mq.kafka;

import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbox.dapper.context.TraceContext;
import org.tbox.dapper.core.TracerConstants;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Kafka消费者追踪拦截器
 * 为消费的消息创建追踪上下文
 */
public class TracingKafkaConsumerInterceptor<K, V> implements ConsumerInterceptor<K, V> {
    private static final Logger log = LoggerFactory.getLogger(TracingKafkaConsumerInterceptor.class);
    
    private String consumerGroup;
    private String applicationName;
    
    /**
     * 默认构造函数
     */
    public TracingKafkaConsumerInterceptor() {
        // 默认无参构造函数，通过configure方法设置属性
    }
    
    /**
     * 带应用名称的构造函数
     * 
     * @param applicationName 应用名称
     */
    public TracingKafkaConsumerInterceptor(String applicationName) {
        this.applicationName = applicationName;
        log.debug("创建Kafka消费者追踪拦截器: applicationName={}", applicationName);
    }
    
    @Override
    public ConsumerRecords<K, V> onConsume(ConsumerRecords<K, V> records) {
        if (records.isEmpty()) {
            return records;
        }
        
        for (ConsumerRecord<K, V> record : records) {
            try {
                String traceId = getHeaderValue(record, TracerConstants.HEADER_TRACE_ID);
                String spanId = getHeaderValue(record, TracerConstants.HEADER_SPAN_ID);
                String parentSpanId = getHeaderValue(record, TracerConstants.HEADER_PARENT_SPAN_ID);
                
                // 创建新的追踪上下文，继承消息中的追踪信息
                TraceContext context;
                
                if (traceId != null && spanId != null) {
                    // 使用消息中的追踪信息创建子上下文
                    context = TraceContext.createFromExternalContext(traceId, spanId, parentSpanId, applicationName);
                    log.debug("从Kafka消息创建追踪上下文: topic={}, partition={}, offset={}, traceId={}", 
                            record.topic(), record.partition(), record.offset(), traceId);
                } else {
                    // 没有追踪信息，创建新的根上下文
                    context = TraceContext.createRootContext(applicationName);
                    log.debug("为Kafka消息创建新的根追踪上下文: topic={}, partition={}, offset={}, traceId={}", 
                            record.topic(), record.partition(), record.offset(), context.getTraceId());
                }
                
                // 设置上下文属性
                context.setAttribute("kafka.topic", record.topic());
                context.setAttribute("kafka.partition", Integer.toString(record.partition()));
                context.setAttribute("kafka.offset", Long.toString(record.offset()));
                context.setAttribute("kafka.consumer.group", consumerGroup);
                context.setAttribute("app.name", applicationName);
                
                // 存储上下文，供后续处理使用
                KafkaTracingContext.setConsumerContext(record, consumerGroup, context);
                
            } catch (Exception e) {
                log.warn("处理Kafka消息追踪信息时发生异常", e);
            }
        }
        
        return records;
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        // 在提交偏移量时处理，可选择在此完成追踪上下文
        if (log.isDebugEnabled() && offsets != null && !offsets.isEmpty()) {
            log.debug("Kafka消费者提交偏移量: {}, 消费者组: {}", offsets, consumerGroup);
        }
    }

    @Override
    public void close() {
        // 清理资源
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // 获取消费者组和应用名称配置
        if (configs.containsKey("group.id")) {
            this.consumerGroup = String.valueOf(configs.get("group.id"));
        }
        
        if (this.applicationName == null) {
            if (configs.containsKey("application.name")) {
                this.applicationName = String.valueOf(configs.get("application.name"));
            } else {
                this.applicationName = "unknown-app";
            }
        }
        
        log.debug("初始化Kafka消费者追踪拦截器: consumerGroup={}, applicationName={}", 
                consumerGroup, applicationName);
    }
    
    /**
     * 获取消息头部值
     */
    private String getHeaderValue(ConsumerRecord<K, V> record, String headerKey) {
        Header header = record.headers().lastHeader(headerKey);
        if (header != null && header.value() != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        return null;
    }
}