package org.tbox.idempotent.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IdempotentContext 单元测试
 */
class IdempotentContextTest {

    @BeforeEach
    void setUp() {
        // 确保每个测试开始时上下文是干净的
        IdempotentContext.clean();
    }

    @AfterEach
    void tearDown() {
        // 清理上下文
        IdempotentContext.clean();
    }

    @Test
    void testPut_AndGet() {
        String key = "testKey";
        String value = "testValue";

        IdempotentContext.put(key, value);

        assertEquals(value, IdempotentContext.getKey(key));
        assertEquals(value, IdempotentContext.getString(key));
    }

    @Test
    void testGet_WhenEmpty_ReturnsNull() {
        assertNull(IdempotentContext.get());
        assertNull(IdempotentContext.getKey("anyKey"));
        assertNull(IdempotentContext.getString("anyKey"));
    }

    @Test
    void testPut_MultipleValues() {
        IdempotentContext.put("key1", "value1");
        IdempotentContext.put("key2", "value2");
        IdempotentContext.put("key3", 123);

        assertEquals("value1", IdempotentContext.getString("key1"));
        assertEquals("value2", IdempotentContext.getString("key2"));
        assertEquals("123", IdempotentContext.getString("key3"));
    }

    @Test
    void testPut_OverwriteValue() {
        String key = "key";
        IdempotentContext.put(key, "oldValue");
        IdempotentContext.put(key, "newValue");

        assertEquals("newValue", IdempotentContext.getString(key));
    }

    @Test
    void testPutContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("key1", "value1");
        context.put("key2", "value2");

        IdempotentContext.putContext(context);

        assertEquals("value1", IdempotentContext.getString("key1"));
        assertEquals("value2", IdempotentContext.getString("key2"));
    }

    @Test
    void testPutContext_MergesWithExisting() {
        // 先放入一些值
        IdempotentContext.put("existingKey", "existingValue");

        // 再放入上下文
        Map<String, Object> newContext = new HashMap<>();
        newContext.put("newKey", "newValue");
        IdempotentContext.putContext(newContext);

        // 两个值都应该存在
        assertEquals("existingValue", IdempotentContext.getString("existingKey"));
        assertEquals("newValue", IdempotentContext.getString("newKey"));
    }

    @Test
    void testClean() {
        IdempotentContext.put("key", "value");
        assertNotNull(IdempotentContext.get());

        IdempotentContext.clean();

        assertNull(IdempotentContext.get());
        assertNull(IdempotentContext.getKey("key"));
    }

    @Test
    void testGetString_WithNonStringValue() {
        IdempotentContext.put("intKey", 12345);
        IdempotentContext.put("boolKey", true);
        IdempotentContext.put("objectKey", new Object() {
            @Override
            public String toString() {
                return "customObject";
            }
        });

        assertEquals("12345", IdempotentContext.getString("intKey"));
        assertEquals("true", IdempotentContext.getString("boolKey"));
        assertEquals("customObject", IdempotentContext.getString("objectKey"));
    }

    @Test
    void testGetString_WhenKeyNotExists_ReturnsNull() {
        IdempotentContext.put("existingKey", "value");
        assertNull(IdempotentContext.getString("nonExistingKey"));
    }

    @Test
    void testThreadIsolation() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    // 每个线程设置自己的值
                    String key = "threadKey";
                    String expectedValue = "thread-" + threadIndex;
                    IdempotentContext.put(key, expectedValue);

                    // 短暂等待，让其他线程也有机会设置值
                    Thread.sleep(10);

                    // 验证获取到的是自己线程设置的值
                    String actualValue = IdempotentContext.getString(key);
                    if (expectedValue.equals(actualValue)) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    IdempotentContext.clean();
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // 所有线程都应该获取到自己设置的值
        assertEquals(threadCount, successCount.get(), "线程隔离测试失败");
    }

    @Test
    void testGetKey_ReturnsOriginalType() {
        Integer intValue = 100;
        IdempotentContext.put("intKey", intValue);

        Object result = IdempotentContext.getKey("intKey");
        assertTrue(result instanceof Integer);
        assertEquals(intValue, result);
    }

    @Test
    void testPutContext_WithEmptyMap() {
        Map<String, Object> emptyMap = new HashMap<>();
        IdempotentContext.putContext(emptyMap);

        // 应该设置一个空的上下文
        Map<String, Object> context = IdempotentContext.get();
        assertNotNull(context);
        assertTrue(context.isEmpty());
    }

    @Test
    void testMultipleClean() {
        IdempotentContext.put("key", "value");
        IdempotentContext.clean();
        IdempotentContext.clean(); // 多次清理不应报错

        assertNull(IdempotentContext.get());
    }
}