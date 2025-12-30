package org.tbox.dapper.utils;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DapperIdUtils 单元测试
 */
class DapperIdUtilsTest {

    @Test
    void testGenerateTraceId_NotNull() {
        String traceId = DapperIdUtils.generateTraceId();
        assertNotNull(traceId);
        assertFalse(traceId.isEmpty());
    }

    @Test
    void testGenerateSpanId_NotNull() {
        String spanId = DapperIdUtils.generateSpanId();
        assertNotNull(spanId);
        assertFalse(spanId.isEmpty());
    }

    @Test
    void testGenerateTraceId_IsNumeric() {
        String traceId = DapperIdUtils.generateTraceId();
        assertTrue(traceId.matches("\\d+"), "TraceId应该是数字: " + traceId);
    }

    @Test
    void testGenerateSpanId_IsNumeric() {
        String spanId = DapperIdUtils.generateSpanId();
        assertTrue(spanId.matches("\\d+"), "SpanId应该是数字: " + spanId);
    }

    @Test
    void testGenerateTraceId_IsPositive() {
        for (int i = 0; i < 100; i++) {
            String traceId = DapperIdUtils.generateTraceId();
            long value = Long.parseLong(traceId);
            assertTrue(value >= 0, "TraceId应该是非负数: " + value);
        }
    }

    @Test
    void testGenerateSpanId_IsPositive() {
        for (int i = 0; i < 100; i++) {
            String spanId = DapperIdUtils.generateSpanId();
            long value = Long.parseLong(spanId);
            assertTrue(value >= 0, "SpanId应该是非负数: " + value);
        }
    }

    @Test
    void testGenerateTraceId_Uniqueness() {
        Set<String> traceIds = new HashSet<>();
        int count = 10000;

        for (int i = 0; i < count; i++) {
            String traceId = DapperIdUtils.generateTraceId();
            assertTrue(traceIds.add(traceId), "发现重复TraceId: " + traceId);
        }

        assertEquals(count, traceIds.size());
    }

    @Test
    void testGenerateSpanId_Uniqueness() {
        Set<String> spanIds = new HashSet<>();
        int count = 10000;

        for (int i = 0; i < count; i++) {
            String spanId = DapperIdUtils.generateSpanId();
            assertTrue(spanIds.add(spanId), "发现重复SpanId: " + spanId);
        }

        assertEquals(count, spanIds.size());
    }

    @Test
    void testConcurrentTraceIdGeneration() throws InterruptedException {
        int threadCount = 10;
        int idsPerThread = 1000;
        Set<String> allIds = ConcurrentHashMap.newKeySet();
        AtomicBoolean hasDuplicate = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < idsPerThread; i++) {
                        String traceId = DapperIdUtils.generateTraceId();
                        if (!allIds.add(traceId)) {
                            hasDuplicate.set(true);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertFalse(hasDuplicate.get(), "并发生成存在重复TraceId");
    }

    @Test
    void testConcurrentSpanIdGeneration() throws InterruptedException {
        int threadCount = 10;
        int idsPerThread = 1000;
        Set<String> allIds = ConcurrentHashMap.newKeySet();
        AtomicBoolean hasDuplicate = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < idsPerThread; i++) {
                        String spanId = DapperIdUtils.generateSpanId();
                        if (!allIds.add(spanId)) {
                            hasDuplicate.set(true);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertFalse(hasDuplicate.get(), "并发生成存在重复SpanId");
    }

    @RepeatedTest(5)
    void testRepeatedUniqueness() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            assertTrue(ids.add(DapperIdUtils.generateTraceId()));
            assertTrue(ids.add(DapperIdUtils.generateSpanId()));
        }
    }

    @Test
    void testCannotInstantiate() {
        // 使用反射测试私有构造函数
        try {
            java.lang.reflect.Constructor<DapperIdUtils> constructor =
                DapperIdUtils.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertThrows(java.lang.reflect.InvocationTargetException.class, constructor::newInstance);
        } catch (NoSuchMethodException e) {
            fail("应该存在私有构造函数");
        }
    }
}