package org.tbox.dapper.mq.rocketmq;

import org.apache.rocketmq.client.hook.SendMessageContext;
import org.apache.rocketmq.client.hook.SendMessageHook;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbox.dapper.context.TraceContext;
import org.tbox.dapper.core.TracerConstants;

import java.nio.charset.StandardCharsets;

/**
 * RocketMQ生产者钩子
 * 负责添加追踪上下文信息到RocketMQ消息
 */
public class TracingRocketMQProducerHook implements SendMessageHook {
    private static final Logger log = LoggerFactory.getLogger(TracingRocketMQProducerHook.class);
    
    private final String appName;
    
    public TracingRocketMQProducerHook(String appName) {
        this.appName = appName;
    }
    
    @Override
    public String hookName() {
        return "TracingRocketMQProducerHook";
    }
    
    @Override
    public void sendMessageBefore(SendMessageContext context) {
        if (context == null) {
            return;
        }
        
        try {
            TraceContext currentContext = TraceContext.getCurrentContext();
            if (currentContext == null) {
                // 如果当前没有有效的追踪上下文，创建一个新的
                currentContext = TraceContext.createRootContext(appName);
                if (log.isDebugEnabled()) {
                    log.debug("为RocketMQ消息创建新的根追踪上下文: traceId={}, spanId={}",
                            currentContext.getTraceId(), currentContext.getSpanId());
                }
            }
            
            // 创建子上下文用于此次消息发送
            TraceContext childContext = TraceContext.createChildContext();
            if (childContext == null) {
                childContext = currentContext;
            }
            
            // 获取当前上下文的信息
            String traceId = childContext.getTraceId();
            String spanId = childContext.getSpanId();
            String parentSpanId = childContext.getParentSpanId();
            
            // 获取消息对象
            Message msg = context.getMessage();
            if (msg != null) {
                // 添加追踪信息到消息属性
                msg.putUserProperty(TracerConstants.HEADER_TRACE_ID, traceId);
                msg.putUserProperty(TracerConstants.HEADER_SPAN_ID, spanId);
                if (parentSpanId != null) {
                    msg.putUserProperty(TracerConstants.HEADER_PARENT_SPAN_ID, parentSpanId);
                }
                msg.putUserProperty(TracerConstants.HEADER_APP_NAME, appName);
                
                // 记录消息信息
                childContext.setAttribute("rocketmq.topic", msg.getTopic());
                if (msg.getTags() != null) {
                    childContext.setAttribute("rocketmq.tags", msg.getTags());
                }
                if (msg.getKeys() != null) {
                    childContext.setAttribute("rocketmq.keys", msg.getKeys());
                }
                
                if (log.isDebugEnabled()) {
                    log.debug("RocketMQ消息添加追踪信息: topic={}, tags={}, keys={}, traceId={}, spanId={}",
                            msg.getTopic(), msg.getTags(), msg.getKeys(), traceId, spanId);
                }
            }
            
            // 保存上下文供后续处理
            context.setMqTraceContext(childContext);
            
        } catch (Exception e) {
            log.warn("处理RocketMQ消息发送前追踪信息时发生异常", e);
        }
    }
    
    @Override
    public void sendMessageAfter(SendMessageContext context) {
        if (context == null) {
            return;
        }
        
        try {
            Object contextObj = context.getMqTraceContext();
            if (!(contextObj instanceof TraceContext)) {
                return;
            }
            
            TraceContext msgContext = (TraceContext) contextObj;
            
            // 处理发送结果
            boolean success = context.getSendResult() != null;
            msgContext.setAttribute("rocketmq.send.success", String.valueOf(success));
            
            if (context.getSendResult() != null) {
                String msgId = context.getSendResult().getMsgId();
                if (msgId != null) {
                    msgContext.setAttribute("rocketmq.msgId", msgId);
                }
                
                if (context.getSendResult().getMessageQueue() != null) {
                    msgContext.setAttribute("rocketmq.queue", context.getSendResult().getMessageQueue().toString());
                }
                
                // 记录发送状态
                if (context.getSendResult().getSendStatus() != null) {
                    msgContext.setAttribute("rocketmq.sendStatus", context.getSendResult().getSendStatus().name());
                }
            }
            
            if (context.getException() != null) {
                msgContext.setAttribute("rocketmq.error", context.getException().getMessage());
            }
            
            // 完成当前上下文
            msgContext.complete();
            
            if (log.isDebugEnabled()) {
                log.debug("RocketMQ消息发送完成: traceId={}, spanId={}, success={}",
                        msgContext.getTraceId(), msgContext.getSpanId(), success);
            }
            
        } catch (Exception e) {
            log.warn("处理RocketMQ消息发送后追踪信息时发生异常", e);
        }
    }
} 