package org.tbox.dapper.mq;

import org.slf4j.MDC;
import org.tbox.dapper.core.TracerConstants;

/**
 * 简单的 MDC 作用域：设置 traceId/spanId 并在 close 时恢复。
 */
public final class MdcScope implements AutoCloseable {

    private final String previousTraceId;
    private final String previousSpanId;
    private final boolean changed;

    private MdcScope(String previousTraceId, String previousSpanId, boolean changed) {
        this.previousTraceId = previousTraceId;
        this.previousSpanId = previousSpanId;
        this.changed = changed;
    }

    public static MdcScope openIfAbsent(String traceId, String spanId) {
        String currentTraceId = MDC.get(TracerConstants.MDC_TRACE_ID);
        String currentSpanId = MDC.get(TracerConstants.MDC_SPAN_ID);
        if (currentTraceId != null && !currentTraceId.isBlank()) {
            return new MdcScope(currentTraceId, currentSpanId, false);
        }
        MDC.put(TracerConstants.MDC_TRACE_ID, traceId);
        MDC.put(TracerConstants.MDC_SPAN_ID, spanId);
        return new MdcScope(currentTraceId, currentSpanId, true);
    }

    @Override
    public void close() {
        if (!changed) {
            return;
        }
        if (previousTraceId == null) {
            MDC.remove(TracerConstants.MDC_TRACE_ID);
        } else {
            MDC.put(TracerConstants.MDC_TRACE_ID, previousTraceId);
        }
        if (previousSpanId == null) {
            MDC.remove(TracerConstants.MDC_SPAN_ID);
        } else {
            MDC.put(TracerConstants.MDC_SPAN_ID, previousSpanId);
        }
    }
}
