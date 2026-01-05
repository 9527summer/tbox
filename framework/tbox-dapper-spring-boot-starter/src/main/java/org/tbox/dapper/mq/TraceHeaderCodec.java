package org.tbox.dapper.mq;

import org.tbox.dapper.core.TracerConstants;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/**
 * MQ 场景下的 Trace Header 编解码（用于日志串联）。
 *
 * <p>仅支持 W3C TraceContext（traceparent）。</p>
 */
public final class TraceHeaderCodec {

    public static final String HEADER_TRACEPARENT = TracerConstants.HEADER_TRACEPARENT;

    private TraceHeaderCodec() {
    }

    public record DecodedTrace(String traceId, String spanId) {
        public DecodedTrace {
            Objects.requireNonNull(traceId, "traceId");
            Objects.requireNonNull(spanId, "spanId");
        }
    }

    public static String encodeTraceparent(String traceId, String spanId) {
        if (traceId == null || spanId == null) {
            return null;
        }
        // version(00) - traceId(32 hex) - parentId(16 hex) - flags(01 sampled)
        return "00-" + traceId.toLowerCase(Locale.ROOT) + "-" + spanId.toLowerCase(Locale.ROOT) + "-01";
    }

    public static DecodedTrace decodeTraceparent(String traceparent) {
        if (traceparent == null) {
            return null;
        }
        // expected: 00-<32>-<16>-<2>
        String[] parts = traceparent.trim().split("-");
        if (parts.length < 4) {
            return null;
        }
        String traceId = parts[1];
        String spanId = parts[2];
        if (!isHex(traceId, 32) || !isHex(spanId, 16)) {
            return null;
        }
        return new DecodedTrace(traceId, spanId);
    }

    public static byte[] toBytes(String value) {
        if (value == null) {
            return null;
        }
        return value.getBytes(StandardCharsets.UTF_8);
    }

    public static String fromBytes(byte[] value) {
        if (value == null) {
            return null;
        }
        return new String(value, StandardCharsets.UTF_8);
    }

    private static boolean isHex(String value, int len) {
        if (value == null || value.length() != len) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!ok) {
                return false;
            }
        }
        return true;
    }
}
