package org.tbox.distributedid.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ReadableSnowflakeTest {

    private TimeSnowflake readableSnowflake;

    @BeforeEach
    void setUp() {
        readableSnowflake = new TimeSnowflake(1); // NodeId = 1
    }

    @Test
    void testNextId_LengthAndFormat() {
        long id = readableSnowflake.nextId();
        String idStr = String.valueOf(id);
        
        System.out.println("Generated Readable ID: " + idStr);
        
        // 验证长度应为 19 位
        assertEquals(19, idStr.length());
        
        // 验证前缀是否为当前年份 (yy)
        String currentYearShort = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yy"));
        assertTrue(idStr.startsWith(currentYearShort));
    }

    @Test
    void testNextId_Uniqueness() {
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            long id = readableSnowflake.nextId();
            assertTrue(ids.add(id), "ID Duplicated: " + id);
        }
    }

    @Test
    void testNodeIdAndSequenceParsing() {
        long nodeId = 88;
        TimeSnowflake generator = new TimeSnowflake(nodeId);
        
        long id = generator.nextId();
        
        long extractedNodeId = TimeSnowflake.parseNodeId(id);
        long extractedSeq = TimeSnowflake.parseSequence(id);
        
        assertEquals(nodeId, extractedNodeId);
        assertTrue(extractedSeq >= 0 && extractedSeq <= 99);
    }
    
    @Test
    void testMaxSequenceHandling() {
        // 测试毫秒内序列溢出是否能自动等待下一毫秒
        // 由于单测环境很难精确控制毫秒，我们通过生成大量 ID 来间接验证
        // 单毫秒最多 100 个，生成 200 个肯定会跨越至少 2ms
        
        long start = System.currentTimeMillis();
        for (int i = 0; i < 200; i++) {
            readableSnowflake.nextId();
        }
        long end = System.currentTimeMillis();
        
        assertTrue((end - start) >= 1, "Should take at least some time to generate more than max sequence");
    }

    @Test
    void testLongOverflowGuard_WillThrowAfter2092() {
        ZoneId zoneId = ZoneId.systemDefault();

        long ts2093 = LocalDateTime.of(2093, 1, 1, 0, 0, 0, 0)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli();

        TimeSnowflake generator = new TimeSnowflake(1, () -> ts2093);
        assertThrows(IllegalStateException.class, generator::nextId);
    }

    @Test
    void testLongOverflowGuard_Allows2092End() {
        ZoneId zoneId = ZoneId.systemDefault();

        long ts2092 = LocalDateTime.of(2092, 12, 31, 23, 59, 59, 999_000_000)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli();

        TimeSnowflake generator = new TimeSnowflake(1, () -> ts2092);
        assertDoesNotThrow(generator::nextId);
    }

    @Test
    @Timeout(10)
    void testConcurrency() throws InterruptedException {
        int threadCount = 20;
        int idsPerThread = 500;
        Set<Long> allIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        AtomicBoolean hasDuplicate = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < idsPerThread; i++) {
                        long id = readableSnowflake.nextId();
                        if (!allIds.add(id)) {
                            hasDuplicate.set(true);
                            System.err.println("Duplicate found: " + id);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertFalse(hasDuplicate.get(), "Concurrent generation produced duplicates");
        assertEquals(threadCount * idsPerThread, allIds.size());
    }
}
