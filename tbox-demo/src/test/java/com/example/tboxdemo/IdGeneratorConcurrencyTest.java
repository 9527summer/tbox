package com.example.tboxdemo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.tbox.distributedid.utils.IdUtils;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分布式ID生成器并发安全性测试
 */
@SpringBootTest
public class IdGeneratorConcurrencyTest {

    /**
     * 测试多线程环境下是否会生成重复ID
     */
    @Test
    public void testConcurrentIdGeneration() throws InterruptedException {
        // 线程数
        int threadCount = 20;
        // 每个线程生成ID的数量
        int idsPerThread = 1000;
        // 总共要生成的ID数量
        int totalCount = threadCount * idsPerThread;
        
        // 存储所有生成的ID
        Set<Long> allIds = Collections.newSetFromMap(new ConcurrentHashMap<>(totalCount));
        // 是否有重复ID的标志
        AtomicBoolean hasDuplicate = new AtomicBoolean(false);
        
        // 线程池
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        // 用于等待所有线程完成
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        // 启动多个线程并发生成ID
        for (int t = 0; t < threadCount; t++) {
            executorService.execute(() -> {
                try {
                    for (int i = 0; i < idsPerThread; i++) {
                        long id = IdUtils.nextId();
                        if (!allIds.add(id)) {
                            hasDuplicate.set(true);
                            System.err.println("发现重复ID: " + id);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有线程完成
        latch.await();
        executorService.shutdown();
        
        // 断言没有重复ID
        assertFalse(hasDuplicate.get(), "存在重复ID");
        // 断言生成的ID数量是预期的总数
        assertEquals(totalCount, allIds.size(), "生成的ID数量与预期不符");
        
        System.out.println("成功生成 " + allIds.size() + " 个不重复ID");
    }
    
    /**
     * 测试ID的顺序性（生成的ID应该总是递增的）
     */
    @Test
    public void testIdSequentiality() {
        long[] ids = new long[10];
        for (int i = 0; i < 10; i++) {
            ids[i] = IdUtils.nextId();
        }
        
        for (int i = 1; i < 10; i++) {
            assertTrue(ids[i] > ids[i-1], "ID没有严格递增: " + ids[i-1] + " >= " + ids[i]);
        }
        
        System.out.println("ID严格递增测试通过");
    }
    
    /**
     * 测试ID在1毫秒内的生成性能
     */
    @Test
    public void testIdGenerationInOneMilli() {
        long startTime = System.currentTimeMillis();
        long endTime = startTime;
        int count = 0;
        
        // 计算1毫秒内能生成多少个ID
        while (endTime == startTime) {
            IdUtils.nextId();
            count++;
            endTime = System.currentTimeMillis();
        }
        
        System.out.println("1毫秒内生成的ID数量: " + count);
        // 理论上，序列号有12位，一毫秒内最多生成4096个ID
        assertTrue(count > 0, "无法在1毫秒内生成ID");
    }
    
    /**
     * 测试突发模式下的ID生成
     */
    @Test
    public void testBurstModeIdGeneration() throws InterruptedException {
        int threadCount = 50;
        int idsPerThread = 200;
        
        Set<Long> allIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        AtomicBoolean hasDuplicate = new AtomicBoolean(false);
        
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);
        
        // 创建并启动所有线程，但让它们等待统一开始信号
        for (int t = 0; t < threadCount; t++) {
            executorService.execute(() -> {
                try {
                    // 线程准备就绪
                    readyLatch.countDown();
                    // 等待开始信号
                    startLatch.await();
                    
                    for (int i = 0; i < idsPerThread; i++) {
                        long id = IdUtils.nextId();
                        if (!allIds.add(id)) {
                            hasDuplicate.set(true);
                            System.err.println("突发模式下发现重复ID: " + id);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }
        
        // 等待所有线程准备就绪
        readyLatch.await();
        // 发出开始信号，所有线程同时开始生成ID
        startLatch.countDown();
        // 等待所有线程完成
        completionLatch.await();
        executorService.shutdown();
        
        // 断言没有重复ID
        assertFalse(hasDuplicate.get(), "突发模式下存在重复ID");
        // 断言生成的ID数量是预期的总数
        assertEquals(threadCount * idsPerThread, allIds.size(), "突发模式下生成的ID数量与预期不符");
        
        System.out.println("突发模式下成功生成 " + allIds.size() + " 个不重复ID");
    }
} 