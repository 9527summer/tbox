package org.tbox.dapper.mq.rocketmq;

import io.micrometer.tracing.Tracer;
import org.apache.rocketmq.client.hook.ConsumeMessageContext;
import org.apache.rocketmq.client.hook.ConsumeMessageHook;
import org.apache.rocketmq.common.message.MessageExt;
import org.tbox.dapper.core.TracerConstants;
import org.tbox.dapper.mq.MdcScope;
import org.tbox.dapper.mq.TraceHeaderCodec;

import java.util.List;

/**
 * RocketMQ Consumer hook：消费前从 Message user properties 提取 traceparent 并写入 MDC。
 */
public class RocketMqConsumeTraceHook implements ConsumeMessageHook {

    private final Tracer tracer;
    private final ThreadLocal<MdcScope> mdcScope = new ThreadLocal<>();

    public RocketMqConsumeTraceHook(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public String hookName() {
        return "tbox-tracer-consume-traceparent";
    }

    @Override
    public void consumeMessageBefore(ConsumeMessageContext context) {
        if (context == null) {
            return;
        }
        if (tracer.currentSpan() != null) {
            return;
        }
        List<MessageExt> messages = context.getMsgList();
        if (messages == null || messages.isEmpty()) {
            return;
        }
        MessageExt first = messages.get(0);
        String traceparent = first.getUserProperty(TracerConstants.HEADER_TRACEPARENT);
        TraceHeaderCodec.DecodedTrace decoded = TraceHeaderCodec.decodeTraceparent(traceparent);
        if (decoded == null) {
            return;
        }
        mdcScope.set(MdcScope.openIfAbsent(decoded.traceId(), decoded.spanId()));
    }

    @Override
    public void consumeMessageAfter(ConsumeMessageContext context) {
        MdcScope scope = mdcScope.get();
        if (scope != null) {
            try {
                scope.close();
            } finally {
                mdcScope.remove();
            }
        }
    }
}

