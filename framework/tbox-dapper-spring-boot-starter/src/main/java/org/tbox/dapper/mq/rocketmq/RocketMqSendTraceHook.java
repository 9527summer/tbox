package org.tbox.dapper.mq.rocketmq;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.apache.rocketmq.client.hook.SendMessageContext;
import org.apache.rocketmq.client.hook.SendMessageHook;
import org.apache.rocketmq.common.message.Message;
import org.tbox.dapper.core.TracerConstants;
import org.tbox.dapper.mq.TraceHeaderCodec;

/**
 * RocketMQ Producer hook：发送前把 traceparent 写入 Message user properties。
 */
public class RocketMqSendTraceHook implements SendMessageHook {

    private final Tracer tracer;

    public RocketMqSendTraceHook(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public String hookName() {
        return "tbox-tracer-send-traceparent";
    }

    @Override
    public void sendMessageBefore(SendMessageContext context) {
        if (context == null) {
            return;
        }
        Message message = context.getMessage();
        if (message == null) {
            return;
        }
        Span span = tracer.currentSpan();
        if (span == null || span.context() == null) {
            return;
        }
        String traceparent = TraceHeaderCodec.encodeTraceparent(span.context().traceId(), span.context().spanId());
        if (traceparent == null) {
            return;
        }
        message.putUserProperty(TracerConstants.HEADER_TRACEPARENT, traceparent);
    }

    @Override
    public void sendMessageAfter(SendMessageContext context) {
        // no-op (仅日志串联：只负责注入 header)
    }
}

