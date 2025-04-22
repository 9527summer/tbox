package org.tbox.dapper.mq.kafka;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbox.dapper.context.TraceContext;
import org.tbox.dapper.core.TracerConstants;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Kafka生产者追踪拦截器
 * 用于在Kafka消息中注入追踪上下文
 */
public class TracingKafkaProducerInterceptor implements ProducerInterceptor<String, Object> {

    private static final Logger log = LoggerFactory.getLogger(TracingKafkaProducerInterceptor.class);
    
    private final String applicationName;
    
    public TracingKafkaProducerInterceptor(String applicationName) {
        this.applicationName = applicationName;
    }
    
    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {
        // 获取当前追踪上下文
        TraceContext currentContext = TraceContext.getCurrentContext();
        if (currentContext == null) {
            // 如果当前没有活跃的上下文，创建一个新的根上下文
            currentContext = TraceContext.createRootContext(applicationName);
            if (log.isDebugEnabled()) {
                log.debug("为Kafka消息创建新的根追踪上下文: traceId={}", currentContext.getTraceId());
            }
        }
        
        try {
            // 添加Kafka相关属性
            currentContext.setAttribute("kafka.topic", record.topic());
            if (record.key() != null) {
                currentContext.setAttribute("kafka.key", record.key().toString());
            }
            if (record.partition() != null) {
                currentContext.setAttribute("kafka.partition", String.valueOf(record.partition()));
            }
            
            // 创建一个新的ProducerRecord，包含所有原始数据和追踪头
            ProducerRecord<String, Object> newRecord = new ProducerRecord<>(
                    record.topic(),
                    record.partition(),
                    record.timestamp(),
                    record.key(),
                    record.value(),
                    record.headers()
            );
            
            // 添加追踪头
            newRecord.headers().add(TracerConstants.HEADER_TRACE_ID, 
                    currentContext.getTraceId().getBytes(StandardCharsets.UTF_8));
            newRecord.headers().add(TracerConstants.HEADER_SPAN_ID, 
                    currentContext.getSpanId().getBytes(StandardCharsets.UTF_8));
            
            if (currentContext.getParentSpanId() != null) {
                newRecord.headers().add(TracerConstants.HEADER_PARENT_SPAN_ID, 
                        currentContext.getParentSpanId().getBytes(StandardCharsets.UTF_8));
            } else {
                newRecord.headers().add(TracerConstants.HEADER_PARENT_SPAN_ID, 
                        "".getBytes(StandardCharsets.UTF_8));
            }
            
            newRecord.headers().add(TracerConstants.HEADER_APP_NAME, 
                    applicationName.getBytes(StandardCharsets.UTF_8));
            
            // 存储当前上下文用于后续处理
            KafkaTracingContext.setCurrentContext(record, currentContext);
            
            if (log.isDebugEnabled()) {
                log.debug("向Kafka消息添加追踪上下文: topic={}, key={}, traceId={}, spanId={}",
                        record.topic(), record.key(), currentContext.getTraceId(), currentContext.getSpanId());
            }
            
            return newRecord;
        } catch (Exception e) {
            log.warn("向Kafka消息添加追踪上下文时发生异常", e);
            return record;
        }
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        try {
            if (metadata != null) {
                // 根据元数据查找对应的上下文
                TraceContext context = KafkaTracingContext.getCurrentContext(metadata);
                if (context != null) {
                    // 添加发送结果信息
                    context.setAttribute("kafka.sent", "true");
                    context.setAttribute("kafka.offset", String.valueOf(metadata.offset()));
                    
                    if (exception == null) {
                        context.setAttribute("kafka.status", "success");
                    } else {
                        context.setAttribute("kafka.status", "error");
                        context.setAttribute("kafka.error", exception.getMessage());
                    }
                    
                    // 结束上下文
                    context.complete();
                    
                    // 从上下文中移除
                    KafkaTracingContext.removeCurrentContext(metadata);
                    
                    if (log.isDebugEnabled()) {
                        log.debug("Kafka消息发送完成: topic={}, partition={}, offset={}, traceId={}, status={}",
                                metadata.topic(), metadata.partition(), metadata.offset(), 
                                context.getTraceId(), exception == null ? "success" : "error");
                    }
                }
            } else if (exception != null) {
                log.warn("Kafka消息发送失败: {}", exception.getMessage());
            }
        } catch (Exception e) {
            log.warn("处理Kafka发送确认时发生异常", e);
        }
    }
    
    @Override
    public void close() {
        // 清理资源
        log.debug("Kafka生产者拦截器关闭");
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // 配置拦截器
        log.debug("Kafka追踪生产者拦截器已配置，应用名称: {}", applicationName);
    }
} 