package org.tbox.dapper.mq.rocketmq;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.apache.rocketmq.client.hook.ConsumeMessageContext;
import org.apache.rocketmq.client.hook.SendMessageContext;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.tbox.dapper.core.TracerConstants;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RocketMqTracingHooksTest {

    @AfterEach
    void cleanupMdc() {
        MDC.clear();
    }

    @Test
    void sendHook_putsTraceparentIntoUserProperties() {
        Tracer tracer = new FixedTracer("4bf92f3577b34da6a3ce929d0e0e4736", "00f067aa0ba902b7");

        RocketMqSendTraceHook hook = new RocketMqSendTraceHook(tracer);
        Message message = new Message("topic", "hello".getBytes());
        SendMessageContext sendCtx = new SendMessageContext();
        sendCtx.setMessage(message);

        hook.sendMessageBefore(sendCtx);

        assertThat(message.getUserProperty(TracerConstants.HEADER_TRACEPARENT))
                .isEqualTo("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
    }

    @Test
    void consumeHook_setsAndClearsMdc() {
        Tracer tracer = new FixedTracer(null, null);

        RocketMqConsumeTraceHook hook = new RocketMqConsumeTraceHook(tracer);
        MessageExt message = new MessageExt();
        message.setTopic("topic");
        message.putUserProperty(TracerConstants.HEADER_TRACEPARENT,
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");

        ConsumeMessageContext consumeCtx = new ConsumeMessageContext();
        consumeCtx.setMsgList(List.of(message));

        hook.consumeMessageBefore(consumeCtx);
        assertThat(MDC.get(TracerConstants.MDC_TRACE_ID)).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(MDC.get(TracerConstants.MDC_SPAN_ID)).isEqualTo("00f067aa0ba902b7");

        hook.consumeMessageAfter(consumeCtx);
        assertThat(MDC.get(TracerConstants.MDC_TRACE_ID)).isNull();
        assertThat(MDC.get(TracerConstants.MDC_SPAN_ID)).isNull();
    }

    private static final class FixedTracer implements Tracer {
        private final Span span;

        private FixedTracer(String traceId, String spanId) {
            if (traceId == null || spanId == null) {
                this.span = null;
            } else {
                this.span = new FixedSpan(traceId, spanId);
            }
        }

        @Override
        public Span currentSpan() {
            return span;
        }

        @Override
        public Map<String, String> getAllBaggage() {
            return Map.of();
        }

        @Override
        public io.micrometer.tracing.Baggage getBaggage(String name) {
            return null;
        }

        @Override
        public io.micrometer.tracing.Baggage getBaggage(TraceContext traceContext, String name) {
            return null;
        }

        @Override
        public io.micrometer.tracing.Baggage createBaggage(String name) {
            return null;
        }

        @Override
        public io.micrometer.tracing.Baggage createBaggage(String name, String value) {
            return null;
        }

        @Override
        public Span nextSpan() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Span nextSpan(Span span) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SpanInScope withSpan(Span span) {
            throw new UnsupportedOperationException();
        }

        @Override
        public io.micrometer.tracing.ScopedSpan startScopedSpan(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Span.Builder spanBuilder() {
            throw new UnsupportedOperationException();
        }

        @Override
        public TraceContext.Builder traceContextBuilder() {
            throw new UnsupportedOperationException();
        }

        @Override
        public io.micrometer.tracing.CurrentTraceContext currentTraceContext() {
            throw new UnsupportedOperationException();
        }

        @Override
        public io.micrometer.tracing.SpanCustomizer currentSpanCustomizer() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FixedSpan implements Span {
        private final TraceContext context;

        private FixedSpan(String traceId, String spanId) {
            this.context = new FixedTraceContext(traceId, spanId);
        }

        @Override
        public boolean isNoop() {
            return false;
        }

        @Override
        public TraceContext context() {
            return context;
        }

        @Override
        public Span start() {
            return this;
        }

        @Override
        public Span name(String name) {
            return this;
        }

        @Override
        public Span event(String value) {
            return this;
        }

        @Override
        public Span event(String value, long timestamp, TimeUnit timeUnit) {
            return this;
        }

        @Override
        public Span tag(String key, String value) {
            return this;
        }

        @Override
        public Span error(Throwable throwable) {
            return this;
        }

        @Override
        public void end() {
        }

        @Override
        public void end(long timestamp, TimeUnit timeUnit) {
        }

        @Override
        public void abandon() {
        }

        @Override
        public Span remoteServiceName(String remoteServiceName) {
            return this;
        }

        @Override
        public Span remoteIpAndPort(String ip, int port) {
            return this;
        }
    }

    private static final class FixedTraceContext implements TraceContext {
        private final String traceId;
        private final String spanId;

        private FixedTraceContext(String traceId, String spanId) {
            this.traceId = traceId;
            this.spanId = spanId;
        }

        @Override
        public String traceId() {
            return traceId;
        }

        @Override
        public String parentId() {
            return null;
        }

        @Override
        public String spanId() {
            return spanId;
        }

        @Override
        public Boolean sampled() {
            return null;
        }
    }
}
