package org.tbox.dapper.mq.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbox.dapper.context.TraceContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kafka追踪上下文管理器
 * 用于存储Kafka消息与追踪上下文之间的关系
 */
public class KafkaTracingContext {
    private static final Logger log = LoggerFactory.getLogger(KafkaTracingContext.class);
    
    // 生产者上下文映射：记录追踪上下文与消息的关联
    private static final Map<String, TraceContext> PRODUCER_CONTEXTS = new ConcurrentHashMap<>();
    
    // 消费者上下文映射：记录消费者组处理的消息上下文
    private static final Map<String, TraceContext> CONSUMER_CONTEXT_MAP = new ConcurrentHashMap<>();
    
    private KafkaTracingContext() {
        // 工具类禁止实例化
    }
    
    /**
     * 设置当前生产者消息的追踪上下文，用于在onAcknowledgement中获取
     * 
     * @param record 生产者记录
     * @param context 对应的追踪上下文
     */
    public static <K, V> void setCurrentContext(ProducerRecord<K, V> record, TraceContext context) {
        if (record == null || context == null) {
            return;
        }
        String key = generateProducerKey(record);
        PRODUCER_CONTEXTS.put(key, context);
    }
    
    /**
     * 获取生产者消息对应的追踪上下文
     * 
     * @param metadata 消息元数据
     * @return 对应的追踪上下文，如果不存在则返回null
     */
    public static TraceContext getCurrentContext(RecordMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        String key = generateProducerKey(metadata);
        return PRODUCER_CONTEXTS.get(key);
    }
    
    /**
     * 移除生产者消息对应的追踪上下文
     * 
     * @param metadata 消息元数据
     */
    public static void removeCurrentContext(RecordMetadata metadata) {
        if (metadata == null) {
            return;
        }
        String key = generateProducerKey(metadata);
        PRODUCER_CONTEXTS.remove(key);
    }
    
    /**
     * 设置消费者消息对应的追踪上下文
     */
    public static void setConsumerContext(ConsumerRecord<?, ?> record, String consumerGroup, TraceContext context) {
        if (record == null || context == null) {
            return;
        }
        
        String key = buildKey(record.topic(), record.partition(), record.offset(), consumerGroup);
        CONSUMER_CONTEXT_MAP.put(key, context);
        
        if (log.isDebugEnabled()) {
            log.debug("已设置Kafka消息追踪上下文: {}, traceId={}", key, context.getTraceId());
        }
    }
    
    /**
     * 获取消费者消息对应的追踪上下文
     */
    public static TraceContext getConsumerContext(ConsumerRecord<?, ?> record, String consumerGroup) {
        if (record == null) {
            return null;
        }
        
        String key = buildKey(record.topic(), record.partition(), record.offset(), consumerGroup);
        TraceContext context = CONSUMER_CONTEXT_MAP.get(key);
        
        if (log.isDebugEnabled()) {
            if (context != null) {
                log.debug("获取到Kafka消息追踪上下文: {}, traceId={}", key, context.getTraceId());
            } else {
                log.debug("未找到Kafka消息追踪上下文: {}", key);
            }
        }
        
        return context;
    }
    
    /**
     * 完成消费者消息的追踪上下文并移除
     */
    public static void completeConsumerContext(ConsumerRecord<?, ?> record, String consumerGroup, String status) {
        if (record == null) {
            return;
        }
        
        String key = buildKey(record.topic(), record.partition(), record.offset(), consumerGroup);
        TraceContext context = CONSUMER_CONTEXT_MAP.remove(key);
        
        if (context != null) {
            context.setAttribute("kafka.process.status", status);
            context.complete();
            
            if (log.isDebugEnabled()) {
                log.debug("完成Kafka消息追踪上下文: {}, traceId={}, status={}", 
                        key, context.getTraceId(), status);
            }
        }
    }
    
    /**
     * 清理所有上下文
     */
    public static void cleanup() {
        int size = PRODUCER_CONTEXTS.size() + CONSUMER_CONTEXT_MAP.size();
        PRODUCER_CONTEXTS.clear();
        CONSUMER_CONTEXT_MAP.clear();
        log.debug("清理Kafka追踪上下文，数量: {}", size);
    }
    
    /**
     * 生成生产者消息唯一键
     */
    private static <K, V> String generateProducerKey(ProducerRecord<K, V> record) {
        return record.topic() + "-" + 
               (record.partition() != null ? record.partition() : "null") + "-" + 
               System.identityHashCode(record);
    }
    
    /**
     * 生成生产者元数据唯一键
     */
    private static String generateProducerKey(RecordMetadata metadata) {
        return metadata.topic() + "-" + metadata.partition() + "-" + metadata.offset();
    }
    
    /**
     * 构建用于存储的key
     */
    private static String buildKey(String topic, int partition, long offset, String consumerGroup) {
        return String.format("%s-%d-%d-%s", topic, partition, offset, consumerGroup);
    }
} 