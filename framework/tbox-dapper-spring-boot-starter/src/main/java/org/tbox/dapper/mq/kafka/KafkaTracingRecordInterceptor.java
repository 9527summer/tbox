package org.tbox.dapper.mq.kafka;

import io.micrometer.tracing.Tracer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.listener.RecordInterceptor;
import org.tbox.dapper.core.TracerConstants;
import org.tbox.dapper.mq.MdcScope;
import org.tbox.dapper.mq.TraceHeaderCodec;

/**
 * 消费端：从 Kafka headers 提取 trace 信息并写入 MDC（用于日志串联）。
 *
 * <p>仅在当前线程 MDC 尚未有 traceId 时才会写入，避免覆盖已有 tracing/agent 行为。</p>
 */
public class KafkaTracingRecordInterceptor<K, V> implements RecordInterceptor<K, V> {

    private final Tracer tracer;
    private final ThreadLocal<MdcScope> mdcScope = new ThreadLocal<>();

    public KafkaTracingRecordInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public ConsumerRecord<K, V> intercept(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
        if (record == null) {
            return null;
        }
        if (tracer.currentSpan() != null) {
            return record;
        }
        TraceHeaderCodec.DecodedTrace decoded = decode(record.headers());
        if (decoded != null) {
            mdcScope.set(MdcScope.openIfAbsent(decoded.traceId(), decoded.spanId()));
        }
        return record;
    }

    @Override
    public void afterRecord(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
        MdcScope scope = mdcScope.get();
        if (scope != null) {
            try {
                scope.close();
            } finally {
                mdcScope.remove();
            }
        }
    }

    private TraceHeaderCodec.DecodedTrace decode(Headers headers) {
        if (headers == null) {
            return null;
        }
        String traceparent = headerValue(headers, TracerConstants.HEADER_TRACEPARENT);
        return TraceHeaderCodec.decodeTraceparent(traceparent);
    }

    private String headerValue(Headers headers, String key) {
        org.apache.kafka.common.header.Header header = headers.lastHeader(key);
        if (header == null) {
            // 某些场景会把 header key 做大小写变化，这里再兜底一次
            header = headers.lastHeader(key.toLowerCase());
        }
        if (header == null) {
            header = headers.lastHeader(key.toUpperCase());
        }
        return header == null ? null : TraceHeaderCodec.fromBytes(header.value());
    }
}
