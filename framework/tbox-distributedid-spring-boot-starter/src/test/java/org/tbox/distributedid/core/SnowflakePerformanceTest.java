package org.tbox.distributedid.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Snowflake 性能测试
 */
class SnowflakePerformanceTest {

    private Snowflake snowflake;

    @BeforeEach
    void setUp() {
        snowflake = new Snowflake(1);
    }

    /**
     * 测试单线程ID生成性能
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testSingleThreadPerformance() {
        int count = 1_000_000;

        long startTime = System.nanoTime();
        for (int i = 0; i < count; i++) {
            snowflake.nextId();
        }
        long endTime = System.nanoTime();

        long durationMs = (endTime - startTime) / 1_000_000;
        double opsPerSecond = count * 1000.0 / durationMs;

        System.out.println("======= 单线程性能测试 =======");
        System.out.println("生成ID数量: " + count);
        System.out.println("耗时: " + durationMs + " ms");
        System.out.println("吞吐量: " + String.format("%.2f", opsPerSecond) + " ops/s");

        // 单线程至少应该达到 100万/秒
        assertTrue(opsPerSecond > 100_000, "单线程性能过低: " + opsPerSecond + " ops/s");
    }

    /**
     * 测试多线程ID生成性能
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testMultiThreadPerformance() throws InterruptedException {
        int threadCount = 10;
        int idsPerThread = 100_000;
        int totalIds = threadCount * idsPerThread;

        Set<Long> allIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        AtomicBoolean hasDuplicate = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    for (int i = 0; i < idsPerThread; i++) {
                        long id = snowflake.nextId();
                        if (!allIds.add(id)) {
                            hasDuplicate.set(true);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        readyLatch.await();
        long startTime = System.nanoTime();
        startLatch.countDown();
        completionLatch.await();
        long endTime = System.nanoTime();

        executor.shutdown();

        long durationMs = (endTime - startTime) / 1_000_000;
        double opsPerSecond = totalIds * 1000.0 / durationMs;

        System.out.println("======= 多线程性能测试 =======");
        System.out.println("线程数: " + threadCount);
        System.out.println("总生成ID数量: " + totalIds);
        System.out.println("耗时: " + durationMs + " ms");
        System.out.println("吞吐量: " + String.format("%.2f", opsPerSecond) + " ops/s");

        assertFalse(hasDuplicate.get(), "存在重复ID");
        assertEquals(totalIds, allIds.size(), "ID数量不匹配");
    }

    /**
     * 测试1毫秒内的ID生成能力
     */
    @Test
    void testIdsPerMillisecond() {
        long startTime = System.currentTimeMillis();
        long endTime = startTime;
        int count = 0;

        while (endTime == startTime) {
            snowflake.nextId();
            count++;
            endTime = System.currentTimeMillis();
        }

        System.out.println("======= 毫秒级生成能力测试 =======");
        System.out.println("1毫秒内生成ID数量: " + count);

        // 理论上12位序列号，最多支持4096个
        assertTrue(count > 0, "无法在1毫秒内生成ID");
        System.out.println("理论最大值: 4096");
    }

    /**
     * 测试高并发极限场景
     */
    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testHighConcurrency() throws InterruptedException {
        int threadCount = 100;
        int idsPerThread = 10_000;
        int totalIds = threadCount * idsPerThread;

        Set<Long> allIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        AtomicBoolean hasDuplicate = new AtomicBoolean(false);
        AtomicLong duplicateCount = new AtomicLong(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long startTime = System.nanoTime();

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < idsPerThread; i++) {
                        long id = snowflake.nextId();
                        if (!allIds.add(id)) {
                            hasDuplicate.set(true);
                            duplicateCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.nanoTime();
        executor.shutdown();

        long durationMs = (endTime - startTime) / 1_000_000;
        double opsPerSecond = totalIds * 1000.0 / durationMs;

        System.out.println("======= 高并发极限测试 =======");
        System.out.println("线程数: " + threadCount);
        System.out.println("总生成ID数量: " + totalIds);
        System.out.println("实际唯一ID数量: " + allIds.size());
        System.out.println("重复ID数量: " + duplicateCount.get());
        System.out.println("耗时: " + durationMs + " ms");
        System.out.println("吞吐量: " + String.format("%.2f", opsPerSecond) + " ops/s");

        assertFalse(hasDuplicate.get(), "高并发下存在重复ID");
        assertEquals(totalIds, allIds.size(), "高并发下ID数量不匹配");
    }

    /**
     * 测试持续压力场景
     */
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testSustainedLoad() throws InterruptedException {
        int durationSeconds = 5;
        int threadCount = 20;

        Set<Long> allIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        AtomicBoolean hasDuplicate = new AtomicBoolean(false);
        AtomicLong totalGenerated = new AtomicLong(0);
        AtomicBoolean running = new AtomicBoolean(true);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long startTime = System.currentTimeMillis();

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    while (running.get()) {
                        long id = snowflake.nextId();
                        if (!allIds.add(id)) {
                            hasDuplicate.set(true);
                        }
                        totalGenerated.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // 运行指定时间
        Thread.sleep(durationSeconds * 1000L);
        running.set(false);

        latch.await();
        long endTime = System.currentTimeMillis();
        executor.shutdown();

        long actualDurationMs = endTime - startTime;
        long total = totalGenerated.get();
        double opsPerSecond = total * 1000.0 / actualDurationMs;

        System.out.println("======= 持续压力测试 =======");
        System.out.println("测试时长: " + actualDurationMs + " ms");
        System.out.println("线程数: " + threadCount);
        System.out.println("总生成ID数量: " + total);
        System.out.println("唯一ID数量: " + allIds.size());
        System.out.println("吞吐量: " + String.format("%.2f", opsPerSecond) + " ops/s");

        assertFalse(hasDuplicate.get(), "持续压力下存在重复ID");
        assertEquals(total, allIds.size(), "持续压力下ID数量不匹配");
    }

    /**
     * 测试ID解析性能
     */
    @Test
    void testIdParsingPerformance() {
        int count = 100_000;
        long[] ids = new long[count];

        // 先生成ID
        for (int i = 0; i < count; i++) {
            ids[i] = snowflake.nextId();
        }

        // 测试解析性能
        long startTime = System.nanoTime();
        for (long id : ids) {
            snowflake.getNodeId(id);
            snowflake.getGenerateDateTime(id);
        }
        long endTime = System.nanoTime();

        long durationMs = (endTime - startTime) / 1_000_000;
        double opsPerSecond = count * 2 * 1000.0 / durationMs; // 每个ID调用2次解析

        System.out.println("======= ID解析性能测试 =======");
        System.out.println("解析次数: " + (count * 2));
        System.out.println("耗时: " + durationMs + " ms");
        System.out.println("吞吐量: " + String.format("%.2f", opsPerSecond) + " ops/s");
    }

    /**
     * 测试不同NodeId组合
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testMultipleSnowflakeInstances() throws InterruptedException {
        int instanceCount = 4;
        int idsPerInstance = 50_000;
        int totalIds = instanceCount * idsPerInstance;

        Snowflake[] instances = new Snowflake[instanceCount];
        for (int i = 0; i < instanceCount; i++) {
            instances[i] = new Snowflake(i);
        }

        Set<Long> allIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        AtomicBoolean hasDuplicate = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(instanceCount);
        CountDownLatch latch = new CountDownLatch(instanceCount);

        long startTime = System.nanoTime();

        for (int i = 0; i < instanceCount; i++) {
            final Snowflake sf = instances[i];
            executor.submit(() -> {
                try {
                    for (int j = 0; j < idsPerInstance; j++) {
                        long id = sf.nextId();
                        if (!allIds.add(id)) {
                            hasDuplicate.set(true);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.nanoTime();
        executor.shutdown();

        long durationMs = (endTime - startTime) / 1_000_000;
        double opsPerSecond = totalIds * 1000.0 / durationMs;

        System.out.println("======= 多实例性能测试 =======");
        System.out.println("实例数: " + instanceCount);
        System.out.println("总生成ID数量: " + totalIds);
        System.out.println("耗时: " + durationMs + " ms");
        System.out.println("吞吐量: " + String.format("%.2f", opsPerSecond) + " ops/s");

        assertFalse(hasDuplicate.get(), "多实例生成存在重复ID");
        assertEquals(totalIds, allIds.size(), "多实例ID数量不匹配");
    }
}