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
 * RocketMQ生产者拦截器
 * 负责在发送消息前添加追踪上下文信息到消息属性
 */
public class TracingRocketMQProducerInterceptor implements SendMessageHook {
    private static final Logger log = LoggerFactory.getLogger(TracingRocketMQProducerInterceptor.class);

    private final TraceContext traceContext;
    private final String appName;

    /**
     * 创建追踪拦截器
     * 
     * @param applicationName 应用名称
     */
    public TracingRocketMQProducerInterceptor(String applicationName) {
        this(TraceContext.getCurrentContext(), applicationName);
        log.debug("创建RocketMQ生产者追踪拦截器: appName={}", applicationName);
    }

    /**
     * 创建追踪拦截器
     * 
     * @param traceContext 追踪上下文
     * @param appName 应用名称
     */
    public TracingRocketMQProducerInterceptor(TraceContext traceContext, String appName) {
        this.traceContext = traceContext;
        this.appName = appName;
        if (log.isDebugEnabled()) {
            log.debug("初始化RocketMQ生产者追踪拦截器: traceContext={}, appName={}", 
                    traceContext != null ? "已提供" : "未提供", 
                    appName != null ? appName : "未提供");
        }
    }

    @Override
    public String hookName() {
        return "TracingRocketMQProducerInterceptor";
    }

    @Override
    public void sendMessageBefore(SendMessageContext context) {
        if (traceContext == null || context == null || context.getMessage() == null) {
            return;
        }

        try {
            // 获取当前线程的追踪上下文
            TraceContext currentContext = TraceContext.getCurrentContext();
            if (currentContext == null) {
                // 如果没有上下文，创建一个新的根上下文
                currentContext = TraceContext.createRootContext(appName);
                if (log.isDebugEnabled()) {
                    log.debug("为RocketMQ消息创建新的根追踪上下文: traceId={}", currentContext.getTraceId());
                }
            }
            
            // 创建子上下文用于此次消息发送
            TraceContext childContext = TraceContext.createChildContext();
            if (childContext == null) {
                childContext = currentContext;
            }

            // 添加RocketMQ相关信息
            Message message = context.getMessage();
            String topic = message.getTopic();
            String tags = message.getTags();
            String keys = message.getKeys();
            
            childContext.setAttribute("mq.type", "rocketmq");
            childContext.setAttribute("mq.operation", "send");
            childContext.setAttribute("mq.topic", topic);
            
            if (tags != null && !tags.isEmpty()) {
                childContext.setAttribute("mq.tags", tags);
            }
            
            if (keys != null && !keys.isEmpty()) {
                childContext.setAttribute("mq.keys", keys);
            }

            // 将追踪信息添加到消息属性
            message.putUserProperty(TracerConstants.HEADER_TRACE_ID, childContext.getTraceId());
            message.putUserProperty(TracerConstants.HEADER_SPAN_ID, childContext.getSpanId());
            message.putUserProperty(TracerConstants.HEADER_PARENT_SPAN_ID, childContext.getParentSpanId());
            if (appName != null) {
                message.putUserProperty(TracerConstants.HEADER_APP_NAME, appName);
            }

            if (log.isDebugEnabled()) {
                log.debug("RocketMQ消息添加追踪信息: topic={}, tags={}, keys={}, traceId={}, spanId={}", 
                        topic, tags, keys, childContext.getTraceId(), childContext.getSpanId());
            }

            // 将上下文与当前发送操作关联，以便在发送后处理
            context.setMqTraceContext(childContext);

        } catch (Exception e) {
            log.warn("添加RocketMQ追踪信息时发生异常: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("异常详情:", e);
            }
        }
    }

    @Override
    public void sendMessageAfter(SendMessageContext context) {
        if (traceContext == null || context == null) {
            return;
        }

        try {
            // 获取之前关联的上下文
            Object contextObj = context.getMqTraceContext();
            if (!(contextObj instanceof TraceContext)) {
                return;
            }

            TraceContext msgContext = (TraceContext) contextObj;
            
            // 添加发送结果信息
            boolean sendSuccess = context.getSendResult() != null;
            msgContext.setAttribute("mq.send.success", String.valueOf(sendSuccess));
            
            if (context.getSendResult() != null) {
                msgContext.setAttribute("mq.msgId", context.getSendResult().getMsgId());
                if (context.getSendResult().getMessageQueue() != null) {
                    msgContext.setAttribute("mq.queue", context.getSendResult().getMessageQueue().toString());
                }
                
                // 记录发送状态
                if (context.getSendResult().getSendStatus() != null) {
                    msgContext.setAttribute("mq.sendStatus", context.getSendResult().getSendStatus().name());
                }
            }

            if (context.getException() != null) {
                msgContext.setAttribute("mq.error", context.getException().getMessage());
            }

            // 完成当前上下文
            msgContext.complete();

            if (log.isDebugEnabled()) {
                log.debug("RocketMQ消息发送完成: traceId={}, spanId={}, success={}", 
                        msgContext.getTraceId(), msgContext.getSpanId(), sendSuccess);
            }

        } catch (Exception e) {
            log.warn("处理RocketMQ消息发送后追踪信息时发生异常: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("异常详情:", e);
            }
        }
    }
} 