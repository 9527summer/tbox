package org.tbox.distributedid.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.tbox.distributedid.core.Snowflake;

import java.util.HashSet;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RedeemCodeUtilsTest {

    @Test
    void redeemCodeIsShortAndCharsetIsValid() {
        Snowflake snowflake = new Snowflake(1);
        String code = RedeemCodeUtils.nextRedeemCode(snowflake);

        assertNotNull(code);
        assertTrue(code.matches("^[0-9a-zA-Z]+$"), "invalid chars: " + code);
        assertTrue(code.length() <= 11, "too long: " + code.length());
    }

    @Test
    void batchGeneration_WorksAndIsUnique() {
        Snowflake snowflake = new Snowflake(1);
        List<String> codes = RedeemCodeUtils.nextRedeemCodes(snowflake, 10_000);

        assertEquals(10_000, codes.size());
        assertEquals(10_000, new HashSet<>(codes).size(), "duplicates detected");
        assertTrue(codes.stream().allMatch(s -> s.matches("^[0-9a-zA-Z]+$")));
    }

    @Test
    void batchGeneration_ValidatesArgs() {
        Snowflake snowflake = new Snowflake(1);
        assertThrows(IllegalArgumentException.class, () -> RedeemCodeUtils.nextRedeemCodes(0));
        assertThrows(IllegalArgumentException.class, () -> RedeemCodeUtils.nextRedeemCodes(-1));
        assertThrows(IllegalArgumentException.class, () -> RedeemCodeUtils.nextRedeemCodes(null, 1));
        assertThrows(IllegalArgumentException.class, () -> RedeemCodeUtils.nextRedeemCodes(snowflake, 0));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void redeemCodeIsUnique_SingleThread() {
        Snowflake snowflake = new Snowflake(1);
        int count = 200_000;
        Set<String> codes = Collections.newSetFromMap(new ConcurrentHashMap<>());

        for (int i = 0; i < count; i++) {
            String code = RedeemCodeUtils.nextRedeemCode(snowflake);
            assertTrue(codes.add(code), "duplicate: " + code);
        }

        assertEquals(count, codes.size());
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void redeemCodeIsUnique_MultiThread() throws InterruptedException {
        Snowflake snowflake = new Snowflake(1);
        int threadCount = 20;
        int perThread = 20_000;
        int total = threadCount * perThread;

        Set<String> codes = Collections.newSetFromMap(new ConcurrentHashMap<>());
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < perThread; i++) {
                        codes.add(RedeemCodeUtils.nextRedeemCode(snowflake));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(total, codes.size(), "duplicates detected");
    }
}
