package org.tbox.distributedid.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.tbox.distributedid.core.Snowflake;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 兑换码生成性能测试（基于 Snowflake long -> Base62）。
 */
class RedeemCodePerformanceTest {

    private Snowflake snowflake;

    @BeforeEach
    void setUp() {
        snowflake = new Snowflake(1);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testSingleThreadPerformance() {
        int count = 1_000_000;

        long start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            RedeemCodeUtils.nextRedeemCode(snowflake);
        }
        long end = System.nanoTime();

        long durationMs = (end - start) / 1_000_000;
        double opsPerSecond = count * 1000.0 / Math.max(durationMs, 1);

        System.out.println("======= 兑换码单线程性能测试 =======");
        System.out.println("生成数量: " + count);
        System.out.println("耗时: " + durationMs + " ms");
        System.out.println("吞吐量: " + String.format("%.2f", opsPerSecond) + " ops/s");

        assertTrue(opsPerSecond > 50_000, "单线程性能过低: " + opsPerSecond + " ops/s");
    }
}

