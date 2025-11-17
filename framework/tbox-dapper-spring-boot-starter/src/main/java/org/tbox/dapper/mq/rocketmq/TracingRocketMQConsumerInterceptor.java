package org.tbox.dapper.mq.rocketmq;

import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.hook.ConsumeMessageContext;
import org.apache.rocketmq.client.hook.ConsumeMessageHook;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbox.dapper.context.TraceContext;
import org.tbox.dapper.core.TracerConstants;

import java.util.List;
import java.util.Map;

/**
 * RocketMQ消费者拦截器
 * 负责从接收的消息中提取追踪上下文信息并恢复到当前线程
 */
public class TracingRocketMQConsumerInterceptor implements ConsumeMessageHook {
    private static final Logger log = LoggerFactory.getLogger(TracingRocketMQConsumerInterceptor.class);

    private final TraceContext traceContext;
    private final String appName;

    /**
     * 创建追踪拦截器
     * 
     * @param applicationName 应用名称
     */
    public TracingRocketMQConsumerInterceptor(String applicationName) {
        this(TraceContext.getCurrentContext(), applicationName);
        log.debug("创建RocketMQ消费者追踪拦截器: appName={}", applicationName);
    }

    /**
     * 创建追踪拦截器
     * 
     * @param traceContext 追踪上下文
     * @param appName 应用名称
     */
    public TracingRocketMQConsumerInterceptor(TraceContext traceContext, String appName) {
        this.traceContext = traceContext;
        this.appName = appName;
        if (log.isDebugEnabled()) {
            log.debug("初始化RocketMQ消费者追踪拦截器: traceContext={}, appName={}", 
                    traceContext != null ? "已提供" : "未提供", 
                    appName != null ? appName : "未提供");
        }
    }

    @Override
    public String hookName() {
        return "TracingRocketMQConsumerInterceptor";
    }

    @Override
    public void consumeMessageBefore(ConsumeMessageContext context) {
        if (traceContext == null || context == null) {
            return;
        }

        try {
            List<MessageExt> msgs = context.getMsgList();
            if (msgs == null || msgs.isEmpty()) {
                return;
            }

            if (log.isDebugEnabled()) {
                log.debug("开始消费RocketMQ消息: 消息数量={}", msgs.size());
            }

            // 为批量消息创建一个共享的父上下文
            TraceContext batchContext = null;
            
            // 从第一条消息中提取traceId，并为整个批次创建一个上下文
            MessageExt firstMsg = msgs.get(0);
            String traceId = firstMsg.getUserProperty(TracerConstants.HEADER_TRACE_ID);
            String spanId = firstMsg.getUserProperty(TracerConstants.HEADER_SPAN_ID); // 作为父SpanId
            String parentSpanId = firstMsg.getUserProperty(TracerConstants.HEADER_PARENT_SPAN_ID);
            
            if (traceId != null && !traceId.isEmpty()) {
                // 继续已有的追踪上下文
                batchContext = TraceContext.createFromExternalContext(traceId, null, spanId, appName);
                if (log.isDebugEnabled()) {
                    log.debug("从RocketMQ消息中恢复追踪上下文: traceId={}, parentSpanId={}", 
                            traceId, spanId);
                }
            } else {
                // 创建新的根上下文
                batchContext = TraceContext.createRootContext(appName);
                if (log.isDebugEnabled()) {
                    log.debug("为RocketMQ消息批次创建新的根追踪上下文: traceId={}", batchContext.getTraceId());
                }
            }
            
            // 设置当前线程上下文
            TraceContext.setCurrentContext(batchContext);
            
            // 添加批处理相关信息
            batchContext.setAttribute("mq.type", "rocketmq");
            batchContext.setAttribute("mq.operation", "consume");
            batchContext.setAttribute("mq.batch.size", String.valueOf(msgs.size()));
            
            // 从上下文属性获取消费信息
            Map<String, String> props = context.getProps();
            
            // 提取队列相关信息
            if (props != null) {
                // 从消息中提取主题信息
                String topic = firstMsg.getTopic();
                if (topic != null && !topic.isEmpty()) {
                    batchContext.setAttribute("mq.topic", topic);
                }
                
                // 从消息属性中获取队列信息
                String queueId = String.valueOf(firstMsg.getQueueId());
                batchContext.setAttribute("mq.queueId", queueId);
                
                // 记录消息ID
                String msgId = firstMsg.getMsgId();
                if (msgId != null) {
                    batchContext.setAttribute("mq.msgId", msgId);
                }
                
                // 判断是否是有序消费
                if (props.containsKey("ConsumeOrderlyContext")) {
                    batchContext.setAttribute("mq.consume.ordered", "true");
                } else {
                    batchContext.setAttribute("mq.consume.ordered", "false");
                }
            }
            
            // 记录消费者组信息
            String consumerGroup = context.getConsumerGroup();
            if (consumerGroup != null && !consumerGroup.isEmpty()) {
                batchContext.setAttribute("mq.consumer.group", consumerGroup);
            }
            
            // 将上下文与当前消费操作关联
            context.setMqTraceContext(batchContext);
            
        } catch (Exception e) {
            log.warn("RocketMQ消费前处理追踪信息时发生异常: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("异常详情:", e);
            }
        }
    }

    @Override
    public void consumeMessageAfter(ConsumeMessageContext context) {
        if (traceContext == null || context == null) {
            return;
        }

        try {
            // 获取之前关联的上下文
            Object contextObj = context.getMqTraceContext();
            if (!(contextObj instanceof TraceContext)) {
                return;
            }
            
            TraceContext batchContext = (TraceContext) contextObj;
            boolean success = context.isSuccess();
            
            // 记录消费结果
            batchContext.setAttribute("mq.consume.result", success ? "success" : "failed");
            
            // 完成当前上下文
            batchContext.complete();
            
            // 清理当前线程上下文
            TraceContext.removeContext();
            
            if (log.isDebugEnabled()) {
                log.debug("RocketMQ消息批次消费完成: traceId={}, success={}", 
                        batchContext.getTraceId(), success);
            }
            
        } catch (Exception e) {
            log.warn("RocketMQ消费后处理追踪信息时发生异常: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("异常详情:", e);
            }
        }
    }
} 