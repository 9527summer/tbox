package org.tbox.dapper.mq;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TraceHeaderCodecTest {

    @Test
    void decodeTraceparent_parsesTraceIdAndSpanId() {
        String traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
        TraceHeaderCodec.DecodedTrace decoded = TraceHeaderCodec.decodeTraceparent(traceparent);
        assertThat(decoded).isNotNull();
        assertThat(decoded.traceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(decoded.spanId()).isEqualTo("00f067aa0ba902b7");
    }
}
