package org.tbox.distributedid.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Snowflake 算法单元测试
 */
class SnowflakeTest {

    private Snowflake snowflake;

    @BeforeEach
    void setUp() {
        // 使用 NodeId = 1
        snowflake = new Snowflake(1);
    }

    @Test
    void testConstructor_ValidParameters() {
        assertDoesNotThrow(() -> new Snowflake(0));
        assertDoesNotThrow(() -> new Snowflake(1023));
        assertDoesNotThrow(() -> new Snowflake(512));
    }

    @Test
    void testConstructor_InvalidNodeId() {
        assertThrows(IllegalArgumentException.class, () -> new Snowflake(-1));
        assertThrows(IllegalArgumentException.class, () -> new Snowflake(1024));
    }

    @Test
    void testConstructor_WithEpochDate() {
        Date epochDate = new Date(System.currentTimeMillis() - 1000000);
        Snowflake sf = new Snowflake(epochDate, 1);

        long id = sf.nextId();
        assertTrue(id > 0);
    }

    @Test
    void testNextId_GeneratesPositiveNumber() {
        long id = snowflake.nextId();
        assertTrue(id > 0);
    }

    @Test
    void testNextId_GeneratesUniqueIds() {
        Set<Long> ids = new HashSet<>();
        int count = 10000;

        for (int i = 0; i < count; i++) {
            long id = snowflake.nextId();
            assertTrue(ids.add(id), "发现重复ID: " + id);
        }

        assertEquals(count, ids.size());
    }

    @Test
    void testNextId_IdsAreIncreasing() {
        long previousId = 0;
        for (int i = 0; i < 1000; i++) {
            long id = snowflake.nextId();
            assertTrue(id > previousId, "ID没有递增: " + previousId + " >= " + id);
            previousId = id;
        }
    }

    @Test
    void testGetNodeId() {
        long expectedNodeId = 555;
        Snowflake sf = new Snowflake(expectedNodeId);
        long id = sf.nextId();

        long extractedNodeId = sf.getNodeId(id);
        assertEquals(expectedNodeId, extractedNodeId);
    }

    @Test
    void testGetGenerateDateTime() {
        long beforeGenerate = System.currentTimeMillis();
        long id = snowflake.nextId();
        long afterGenerate = System.currentTimeMillis();

        long generateTime = snowflake.getGenerateDateTime(id);

        assertTrue(generateTime >= beforeGenerate);
        assertTrue(generateTime <= afterGenerate);
    }

    @Test
    void testConcurrentIdGeneration() throws InterruptedException {
        int threadCount = 20;
        int idsPerThread = 1000;
        Set<Long> allIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        AtomicBoolean hasDuplicate = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < idsPerThread; i++) {
                        long id = snowflake.nextId();
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
        executor.shutdown();

        assertFalse(hasDuplicate.get(), "并发生成存在重复ID");
        assertEquals(threadCount * idsPerThread, allIds.size());
    }

    @Test
    void testBurstModeIdGeneration() throws InterruptedException {
        int threadCount = 50;
        int idsPerThread = 200;
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
        startLatch.countDown();
        completionLatch.await();
        executor.shutdown();

        assertFalse(hasDuplicate.get(), "突发模式存在重复ID");
        assertEquals(threadCount * idsPerThread, allIds.size());
    }

    @Test
    void testDifferentNodesGenerateDifferentIds() {
        Snowflake sf1 = new Snowflake(1);
        Snowflake sf2 = new Snowflake(2);
        Snowflake sf3 = new Snowflake(3);

        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            assertTrue(ids.add(sf1.nextId()));
            assertTrue(ids.add(sf2.nextId()));
            assertTrue(ids.add(sf3.nextId()));
        }

        assertEquals(300, ids.size());
    }

    @RepeatedTest(5)
    void testIdUniquenessRepeated() {
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            assertTrue(ids.add(snowflake.nextId()), "第" + i + "次生成发现重复ID");
        }
    }
}