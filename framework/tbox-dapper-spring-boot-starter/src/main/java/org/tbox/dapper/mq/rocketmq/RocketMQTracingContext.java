package org.tbox.dapper.mq.rocketmq;

import org.apache.rocketmq.common.message.MessageExt;
import org.tbox.dapper.context.TraceContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RocketMQ追踪上下文管理器
 * 用于存储和管理消息消费过程中的追踪上下文
 */
public class RocketMQTracingContext {

    private static final Map<String, TraceContext> CONSUMER_CONTEXTS = new ConcurrentHashMap<>();

    /**
     * 生成消息的唯一键，用于标识消息上下文
     * 
     * @param msg 消息对象
     * @param consumerGroup 消费者组
     * @return 唯一键
     */
    private static String generateKey(MessageExt msg, String consumerGroup) {
        return msg.getMsgId() + ":" + consumerGroup;
    }

    /**
     * 设置消费者上下文
     * 
     * @param msg 消息对象
     * @param consumerGroup 消费者组
     * @param context 追踪上下文
     */
    public static void setConsumerContext(MessageExt msg, String consumerGroup, TraceContext context) {
        if (msg == null || context == null) {
            return;
        }
        
        String key = generateKey(msg, consumerGroup);
        CONSUMER_CONTEXTS.put(key, context);
    }

    /**
     * 获取消费者上下文
     * 
     * @param msg 消息对象
     * @param consumerGroup 消费者组
     * @return 追踪上下文，如果不存在则返回null
     */
    public static TraceContext getConsumerContext(MessageExt msg, String consumerGroup) {
        if (msg == null) {
            return null;
        }
        
        String key = generateKey(msg, consumerGroup);
        return CONSUMER_CONTEXTS.get(key);
    }

    /**
     * 完成消费者上下文处理
     * 该方法会从上下文存储中移除上下文并调用complete方法
     * 
     * @param msg 消息对象
     * @param consumerGroup 消费者组
     */
    public static void completeConsumerContext(MessageExt msg, String consumerGroup) {
        if (msg == null) {
            return;
        }
        
        String key = generateKey(msg, consumerGroup);
        TraceContext context = CONSUMER_CONTEXTS.remove(key);
        
        if (context != null) {
            context.complete();
        }
    }

    /**
     * 清理所有消费者上下文
     * 通常用于应用关闭时的资源清理
     */
    public static void clearAllConsumerContexts() {
        CONSUMER_CONTEXTS.clear();
    }
} 