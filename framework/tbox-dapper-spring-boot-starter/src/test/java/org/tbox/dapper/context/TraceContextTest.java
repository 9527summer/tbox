package org.tbox.dapper.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TraceContext 单元测试
 */
class TraceContextTest {

    private static final String TEST_APP_NAME = "test-app";

    @BeforeEach
    void setUp() {
        TraceContext.removeContext();
    }

    @AfterEach
    void tearDown() {
        TraceContext.removeContext();
    }

    @Test
    void testCreateRootContext() {
        TraceContext context = TraceContext.createRootContext(TEST_APP_NAME);

        assertNotNull(context);
        assertNotNull(context.getTraceId());
        assertNotNull(context.getSpanId());
        assertNull(context.getParentSpanId());
        assertEquals(TEST_APP_NAME, context.getAppName());
        assertTrue(context.getStartTime() > 0);
        assertFalse(context.isCompleted());
    }

    @Test
    void testCreateRootContext_SetsCurrentContext() {
        TraceContext created = TraceContext.createRootContext(TEST_APP_NAME);
        TraceContext current = TraceContext.getCurrentContext();

        assertSame(created, current);
    }

    @Test
    void testCreateRootContext_SetsMDC() {
        TraceContext context = TraceContext.createRootContext(TEST_APP_NAME);

        assertEquals(context.getTraceId(), MDC.get(TraceContext.MDC_TRACE_ID));
        assertEquals(context.getSpanId(), MDC.get(TraceContext.MDC_SPAN_ID));
        assertEquals(TEST_APP_NAME, MDC.get(TraceContext.MDC_APP_NAME));
    }

    @Test
    void testCreateChildContext() {
        TraceContext parent = TraceContext.createRootContext(TEST_APP_NAME);
        TraceContext child = TraceContext.createChildContext();

        assertNotNull(child);
        assertEquals(parent.getTraceId(), child.getTraceId());
        assertEquals(parent.getSpanId(), child.getParentSpanId());
        assertTrue(child.getSpanId().startsWith(parent.getSpanId() + "."));
        assertEquals(TEST_APP_NAME, child.getAppName());
    }

    @Test
    void testCreateChildContext_WithoutParent_ReturnsNull() {
        TraceContext child = TraceContext.createChildContext();
        assertNull(child);
    }

    @Test
    void testCreateChildContext_MultipleChildren() {
        TraceContext parent = TraceContext.createRootContext(TEST_APP_NAME);
        String parentSpanId = parent.getSpanId();

        // 恢复父上下文以创建第一个子上下文
        TraceContext child1 = TraceContext.createChildContext();
        assertEquals(parentSpanId + ".1", child1.getSpanId());

        // 恢复父上下文以创建第二个子上下文
        TraceContext.setCurrentContext(parent);
        TraceContext child2 = TraceContext.createChildContext();
        assertEquals(parentSpanId + ".2", child2.getSpanId());
    }

    @Test
    void testCreateFromExternalContext() {
        String traceId = "external-trace-id";
        String spanId = "external-span-id";
        String parentSpanId = "external-parent-span-id";

        TraceContext context = TraceContext.createFromExternalContext(traceId, spanId, parentSpanId, TEST_APP_NAME);

        assertNotNull(context);
        assertEquals(traceId, context.getTraceId());
        assertEquals(spanId, context.getSpanId());
        assertEquals(parentSpanId, context.getParentSpanId());
        assertEquals(TEST_APP_NAME, context.getAppName());
    }

    @Test
    void testCreateFromExternalContext_WithNullTraceId_ReturnsNull() {
        TraceContext context = TraceContext.createFromExternalContext(null, "spanId", null, TEST_APP_NAME);
        assertNull(context);
    }

    @Test
    void testCreateFromExternalContext_WithNullSpanId_GeneratesNewSpanId() {
        String traceId = "trace-123";
        TraceContext context = TraceContext.createFromExternalContext(traceId, null, null, TEST_APP_NAME);

        assertNotNull(context);
        assertEquals(traceId, context.getTraceId());
        assertNotNull(context.getSpanId());
    }

    @Test
    void testGetCurrentContext_WhenNoContext_ReturnsNull() {
        assertNull(TraceContext.getCurrentContext());
    }

    @Test
    void testSetCurrentContext() {
        TraceContext context = TraceContext.createRootContext(TEST_APP_NAME);
        TraceContext.removeContext();

        TraceContext.setCurrentContext(context);

        assertSame(context, TraceContext.getCurrentContext());
    }

    @Test
    void testSetCurrentContext_WithNull_RemovesContext() {
        TraceContext.createRootContext(TEST_APP_NAME);
        TraceContext.setCurrentContext(null);

        assertNull(TraceContext.getCurrentContext());
    }

    @Test
    void testRemoveContext() {
        TraceContext.createRootContext(TEST_APP_NAME);
        assertNotNull(TraceContext.getCurrentContext());

        TraceContext.removeContext();

        assertNull(TraceContext.getCurrentContext());
    }

    @Test
    void testRemoveContext_ClearsMDC() {
        TraceContext.createRootContext(TEST_APP_NAME);
        TraceContext.removeContext();

        assertNull(MDC.get(TraceContext.MDC_TRACE_ID));
        assertNull(MDC.get(TraceContext.MDC_SPAN_ID));
        assertNull(MDC.get(TraceContext.MDC_APP_NAME));
    }

    @Test
    void testComplete() {
        TraceContext context = TraceContext.createRootContext(TEST_APP_NAME);
        assertFalse(context.isCompleted());

        context.complete();

        assertTrue(context.isCompleted());
    }

    @Test
    void testSetAttribute_AndGetAttribute() {
        TraceContext context = TraceContext.createRootContext(TEST_APP_NAME);

        context.setAttribute("key1", "value1");
        context.setAttribute("key2", "value2");

        assertEquals("value1", context.getAttribute("key1"));
        assertEquals("value2", context.getAttribute("key2"));
    }

    @Test
    void testSetAttribute_WithNullKey_IsIgnored() {
        TraceContext context = TraceContext.createRootContext(TEST_APP_NAME);
        context.setAttribute(null, "value");

        assertNull(context.getAttribute(null));
    }

    @Test
    void testSetAttribute_WithNullValue_IsIgnored() {
        TraceContext context = TraceContext.createRootContext(TEST_APP_NAME);
        context.setAttribute("key", null);

        assertNull(context.getAttribute("key"));
    }

    @Test
    void testGetAttributes_ReturnsCopy() {
        TraceContext context = TraceContext.createRootContext(TEST_APP_NAME);
        context.setAttribute("key", "value");

        Map<String, String> attributes = context.getAttributes();
        attributes.put("newKey", "newValue");

        // 原始属性不应被修改
        assertNull(context.getAttribute("newKey"));
    }

    @Test
    void testGetDuration() throws InterruptedException {
        TraceContext context = TraceContext.createRootContext(TEST_APP_NAME);
        Thread.sleep(50);

        long duration = context.getDuration();
        assertTrue(duration >= 50);
    }

    @Test
    void testGetContextForAsync() {
        TraceContext context = TraceContext.createRootContext(TEST_APP_NAME);

        Map<String, String> asyncContext = TraceContext.getContextForAsync();

        assertNotNull(asyncContext);
        assertEquals(context.getTraceId(), asyncContext.get(TraceContext.MDC_TRACE_ID));
        assertEquals(context.getSpanId(), asyncContext.get(TraceContext.MDC_SPAN_ID));
        assertEquals(TEST_APP_NAME, asyncContext.get(TraceContext.MDC_APP_NAME));
    }

    @Test
    void testGetContextForAsync_WhenNoContext_ReturnsNull() {
        Map<String, String> asyncContext = TraceContext.getContextForAsync();
        assertNull(asyncContext);
    }

    @Test
    void testRestoreFromAsync() {
        TraceContext original = TraceContext.createRootContext(TEST_APP_NAME);
        Map<String, String> asyncContext = TraceContext.getContextForAsync();
        TraceContext.removeContext();

        TraceContext restored = TraceContext.restoreFromAsync(asyncContext);

        assertNotNull(restored);
        assertEquals(original.getTraceId(), restored.getTraceId());
        assertEquals(original.getSpanId(), restored.getSpanId());
    }

    @Test
    void testRestoreFromAsync_WithNullMap_ReturnsNull() {
        TraceContext restored = TraceContext.restoreFromAsync(null);
        assertNull(restored);
    }

    @Test
    void testRestoreFromAsync_WithEmptyMap_ReturnsNull() {
        TraceContext restored = TraceContext.restoreFromAsync(new HashMap<>());
        assertNull(restored);
    }

    @Test
    void testThreadIsolation() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicBoolean failed = new AtomicBoolean(false);

        for (int i = 0; i < threadCount; i++) {
            final String appName = "app-" + i;
            executor.submit(() -> {
                try {
                    TraceContext context = TraceContext.createRootContext(appName);
                    Thread.sleep(10);

                    TraceContext current = TraceContext.getCurrentContext();
                    if (!appName.equals(current.getAppName())) {
                        failed.set(true);
                    }
                } catch (Exception e) {
                    failed.set(true);
                } finally {
                    TraceContext.removeContext();
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertFalse(failed.get(), "线程隔离测试失败");
    }

    @Test
    void testAsyncContextPropagation() throws InterruptedException {
        TraceContext parent = TraceContext.createRootContext(TEST_APP_NAME);
        Map<String, String> asyncContext = TraceContext.getContextForAsync();

        AtomicReference<String> childTraceId = new AtomicReference<>();

        Thread childThread = new Thread(() -> {
            TraceContext restored = TraceContext.restoreFromAsync(asyncContext);
            if (restored != null) {
                childTraceId.set(restored.getTraceId());
            }
            TraceContext.removeContext();
        });

        childThread.start();
        childThread.join();

        assertEquals(parent.getTraceId(), childTraceId.get());
    }
}