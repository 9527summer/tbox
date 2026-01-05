package org.tbox.dapper.mq.kafka;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.tbox.dapper.core.TracerConstants;
import org.tbox.dapper.mq.TraceHeaderCodec;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * 包装 Kafka Producer，在 send 前写入 trace headers（用于日志串联）。
 */
public class TracingProducer<K, V> implements Producer<K, V> {

    private final Producer<K, V> delegate;
    private final Tracer tracer;

    public TracingProducer(Producer<K, V> delegate, Tracer tracer) {
        this.delegate = delegate;
        this.tracer = tracer;
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> record) {
        injectIfPossible(record);
        return delegate.send(record);
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback) {
        injectIfPossible(record);
        return delegate.send(record, callback);
    }

    private void injectIfPossible(ProducerRecord<K, V> record) {
        if (record == null) {
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
        Headers headers = record.headers();
        if (headers == null) {
            return;
        }
        headers.remove(TracerConstants.HEADER_TRACEPARENT);
        headers.add(TracerConstants.HEADER_TRACEPARENT, TraceHeaderCodec.toBytes(traceparent));
    }

    @Override
    public void flush() {
        delegate.flush();
    }

    @Override
    public List<PartitionInfo> partitionsFor(String topic) {
        return delegate.partitionsFor(topic);
    }

    @Override
    public Map<MetricName, ? extends Metric> metrics() {
        return delegate.metrics();
    }

    @Override
    public void initTransactions() {
        delegate.initTransactions();
    }

    @Override
    public void beginTransaction() {
        delegate.beginTransaction();
    }

    @Override
    public void sendOffsetsToTransaction(Map<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> offsets,
                                         String consumerGroupId) {
        delegate.sendOffsetsToTransaction(offsets, consumerGroupId);
    }

    @Override
    public void sendOffsetsToTransaction(Map<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> offsets,
                                         org.apache.kafka.clients.consumer.ConsumerGroupMetadata groupMetadata) {
        delegate.sendOffsetsToTransaction(offsets, groupMetadata);
    }

    @Override
    public void commitTransaction() {
        delegate.commitTransaction();
    }

    @Override
    public void abortTransaction() {
        delegate.abortTransaction();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public void close(Duration timeout) {
        delegate.close(timeout);
    }
}
