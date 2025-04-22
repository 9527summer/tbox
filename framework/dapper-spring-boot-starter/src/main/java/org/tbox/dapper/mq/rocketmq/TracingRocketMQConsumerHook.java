package org.tbox.dapper.mq.rocketmq;

import org.apache.rocketmq.client.hook.ConsumeMessageContext;
import org.apache.rocketmq.client.hook.ConsumeMessageHook;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbox.dapper.context.TraceContext;
import org.tbox.dapper.core.TracerConstants;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * RocketMQ消费者追踪钩子
 * 负责处理消息消费过程中的追踪上下文
 */

public class TracingRocketMQConsumerHook implements ConsumeMessageHook {

    private static final Logger log = LoggerFactory.getLogger(TracingRocketMQConsumerHook.class);

    private final String applicationName;

    public TracingRocketMQConsumerHook(String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public String hookName() {
        return "TracingRocketMQConsumerHook";
    }

    @Override
    public void consumeMessageBefore(ConsumeMessageContext context) {
        try {
            List<MessageExt> msgs = context.getMsgList();
            if (msgs == null || msgs.isEmpty()) {
                return;
            }

            String consumerGroup = context.getConsumerGroup();
            for (MessageExt msg : msgs) {
                processMessage(msg, consumerGroup);
            }
        } catch (Exception e) {
            log.warn("RocketMQ消费前处理追踪信息时发生异常", e);
        }
    }

    @Override
    public void consumeMessageAfter(ConsumeMessageContext context) {
        try {
            List<MessageExt> msgs = context.getMsgList();
            if (msgs == null || msgs.isEmpty()) {
                return;
            }

            String consumerGroup = context.getConsumerGroup();
            boolean success = context.isSuccess();
            
            for (MessageExt msg : msgs) {
                TraceContext traceContext = RocketMQTracingContext.getConsumerContext(msg, consumerGroup);
                if (traceContext != null) {
                    if (!success) {
                        traceContext.setAttribute("rocketmq.consume.result", "failed");
                        log.debug("RocketMQ消息消费失败: topic={}, msgId={}, traceId={}", 
                                msg.getTopic(), msg.getMsgId(), traceContext.getTraceId());
                    } else {
                        traceContext.setAttribute("rocketmq.consume.result", "success");
                        log.debug("RocketMQ消息消费成功: topic={}, msgId={}, traceId={}", 
                                msg.getTopic(), msg.getMsgId(), traceContext.getTraceId());
                    }
                    
                    // 完成上下文处理
                    RocketMQTracingContext.completeConsumerContext(msg, consumerGroup);
                }
            }
        } catch (Exception e) {
            log.warn("RocketMQ消费后处理追踪信息时发生异常", e);
        }
    }

    /**
     * 处理单条消息的追踪信息
     */
    private void processMessage(MessageExt msg, String consumerGroup) {
        try {
            String traceId = getHeaderValue(msg, TracerConstants.HEADER_TRACE_ID);
            String spanId = getHeaderValue(msg, TracerConstants.HEADER_SPAN_ID);
            String parentSpanId = getHeaderValue(msg, TracerConstants.HEADER_PARENT_SPAN_ID);
            
            TraceContext context;
            
            if (traceId != null && spanId != null) {
                // 使用消息中的追踪信息创建子上下文
                context = TraceContext.createFromExternalContext(traceId, spanId, parentSpanId, applicationName);
                log.debug("从RocketMQ消息创建追踪上下文: topic={}, msgId={}, traceId={}", 
                        msg.getTopic(), msg.getMsgId(), traceId);
            } else {
                // 没有追踪信息，创建新的根上下文
                context = TraceContext.createRootContext(applicationName);
                log.debug("为RocketMQ消息创建新的根追踪上下文: topic={}, msgId={}, traceId={}", 
                        msg.getTopic(), msg.getMsgId(), context.getTraceId());
            }
            
            // 设置上下文属性
            context.setAttribute("rocketmq.topic", msg.getTopic());
            context.setAttribute("rocketmq.msgId", msg.getMsgId());
            context.setAttribute("rocketmq.tags", msg.getTags());
            context.setAttribute("rocketmq.keys", msg.getKeys());
            context.setAttribute("rocketmq.consumer.group", consumerGroup);
            context.setAttribute("app.name", applicationName);
            
            // 存储上下文，供后续处理使用
            RocketMQTracingContext.setConsumerContext(msg, consumerGroup, context);
            
        } catch (Exception e) {
            log.warn("处理RocketMQ消息追踪信息时发生异常: topic={}, msgId={}", 
                    msg.getTopic(), msg.getMsgId(), e);
        }
    }
    
    /**
     * 从消息属性中获取值
     */
    private String getHeaderValue(MessageExt msg, String key) {
        // 直接获取属性值，因为getUserProperty返回的就是String
        String value = msg.getProperty(key);
        if (value != null) {
            return value;
        }

        // 尝试使用getUserProperty获取
        value = msg.getUserProperty(key);
        if (value != null) {
            return value;
        }
        
        return null;
    }
} 