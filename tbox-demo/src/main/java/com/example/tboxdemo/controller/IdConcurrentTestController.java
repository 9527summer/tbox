package com.example.tboxdemo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tbox.distributedid.utils.IdUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 分布式ID生成器并发测试控制器
 * 专门用于测试在各种并发场景下ID生成器的性能和可靠性
 */
@RestController
@RequestMapping("/id/test")
public class IdConcurrentTestController {

    /**
     * 可配置参数的并发测试
     * 
     * @param threadCount 线程数量
     * @param idsPerThread 每个线程生成ID的数量
     * @param burst 是否使用突发模式（所有线程同时启动）
     * @return 测试结果
     */
    @GetMapping("/concurrent")
    public Map<String, Object> concurrentTest(
            @RequestParam(defaultValue = "20") int threadCount,
            @RequestParam(defaultValue = "5000") int idsPerThread,
            @RequestParam(defaultValue = "true") boolean burst) {
        
        Map<String, Object> result = new HashMap<>();
        
        // 总共要生成的ID数量
        int totalCount = threadCount * idsPerThread;
        
        // 用于存储生成的所有ID，使用ConcurrentHashMap保证线程安全
        Set<Long> allIds = ConcurrentHashMap.newKeySet(totalCount);
        // 用于存储重复的ID
        Set<Long> duplicateIds = ConcurrentHashMap.newKeySet();
        
        // 线程池
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        // 倒计时锁存器，用于等待所有线程完成
        CountDownLatch completionLatch = new CountDownLatch(threadCount);
        
        // 用于突发模式，所有线程同时开始
        CountDownLatch startLatch = burst ? new CountDownLatch(1) : null;
        
        // 记录ID生成情况的计数器
        AtomicInteger generatedCount = new AtomicInteger(0);
        
        // 性能指标
        long startTime = System.currentTimeMillis();
        // 使用原子变量解决Lambda表达式引用非final变量的问题
        AtomicLong maxThreadTime = new AtomicLong(0);
        
        // 启动多个线程并发生成ID
        for (int t = 0; t < threadCount; t++) {
            executorService.execute(() -> {
                try {
                    // 如果是突发模式，等待所有线程就绪
                    if (burst && startLatch != null) {
                        try {
                            startLatch.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    
                    long threadStartTime = System.currentTimeMillis();
                    
                    for (int i = 0; i < idsPerThread; i++) {
                        long id = IdUtils.nextId();
                        generatedCount.incrementAndGet();
                        
                        // 如果添加失败，说明ID已经存在，是重复的
                        if (!allIds.add(id)) {
                            duplicateIds.add(id);
                        }
                    }
                    
                    long threadTime = System.currentTimeMillis() - threadStartTime;
                    // 更新最长线程执行时间，使用原子变量的方法
                    maxThreadTime.updateAndGet(current -> Math.max(current, threadTime));
                } finally {
                    completionLatch.countDown();
                }
            });
        }
        
        // 如果是突发模式，现在释放所有线程同时开始
        if (burst && startLatch != null) {
            startLatch.countDown();
        }
        
        try {
            // 等待所有线程完成
            completionLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.put("error", "测试被中断");
            return result;
        } finally {
            executorService.shutdown();
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 统计结果
        result.put("thread_count", threadCount);
        result.put("ids_per_thread", idsPerThread);
        result.put("burst_mode", burst);
        result.put("total_expected", totalCount);
        result.put("total_generated", generatedCount.get());
        result.put("unique_ids", allIds.size());
        result.put("duplicate_count", duplicateIds.size());
        result.put("has_duplicates", !duplicateIds.isEmpty());
        result.put("duration_ms", duration);
        result.put("max_thread_time_ms", maxThreadTime.get());
        result.put("ids_per_second", totalCount * 1000.0 / duration);
        
        if (!duplicateIds.isEmpty()) {
            // 如果有重复ID，只返回前10个，避免响应过大
            List<Long> sampleDuplicates = new ArrayList<>(duplicateIds);
            if (sampleDuplicates.size() > 10) {
                sampleDuplicates = sampleDuplicates.subList(0, 10);
            }
            result.put("sample_duplicates", sampleDuplicates);
        }
        
        return result;
    }
    
    /**
     * 不同场景下的高压力测试
     * 
     * @param scenario 测试场景:
     *                 extreme - 极端场景，最大线程数和最大生成量
     *                 burst - 突发场景，所有线程同时启动
     *                 sustained - 持续场景，较长时间持续生成
     * @return 测试结果
     */
    @GetMapping("/stress")
    public Map<String, Object> stressTest(@RequestParam(defaultValue = "burst") String scenario) {
        Map<String, Object> result = new HashMap<>();
        
        int threadCount;
        int idsPerThread;
        boolean burst;
        
        // 根据场景选择不同的参数
        switch (scenario.toLowerCase()) {
            case "extreme":
                threadCount = 100;  // 极端高并发
                idsPerThread = 10000;
                burst = true;
                break;
            case "sustained":
                threadCount = 10;
                idsPerThread = 100000; // 每个线程生成大量ID
                burst = false;
                break;
            case "burst":
            default:
                threadCount = 50;
                idsPerThread = 5000;
                burst = true;
                break;
        }
        
        return concurrentTest(threadCount, idsPerThread, burst);
    }
    
    /**
     * 检测时钟回拨情况下的ID生成
     * 注意：这个测试不会真的回拨时钟，只是通过等待模拟时间流逝后再生成ID
     */
    @GetMapping("/clock-backwards")
    public Map<String, Object> clockBackwardTest() {
        Map<String, Object> result = new HashMap<>();
        List<Long> generatedIds = new ArrayList<>();
        
        try {
            // 第一阶段：生成一些ID
            for (int i = 0; i < 5; i++) {
                generatedIds.add(IdUtils.nextId());
            }
            
            // 等待一段时间，模拟时间流逝
            Thread.sleep(1000);
            
            // 第二阶段：再生成一些ID
            for (int i = 0; i < 5; i++) {
                generatedIds.add(IdUtils.nextId());
            }
            
            // 检查生成的ID是否严格递增
            boolean strictlyIncreasing = true;
            for (int i = 1; i < generatedIds.size(); i++) {
                if (generatedIds.get(i) <= generatedIds.get(i - 1)) {
                    strictlyIncreasing = false;
                    break;
                }
            }
            
            result.put("ids", generatedIds);
            result.put("strictly_increasing", strictlyIncreasing);
            result.put("success", true);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 紧急测试：立即生成大量ID并检查唯一性
     */
    @GetMapping("/emergency")
    public Map<String, Object> emergencyTest() {
        Map<String, Object> result = new HashMap<>();
        Set<Long> idSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
        int count = 100000;
        
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            idSet.add(IdUtils.nextId());
        }
        long endTime = System.currentTimeMillis();
        
        result.put("requested", count);
        result.put("unique", idSet.size());
        result.put("duplicates", count - idSet.size());
        result.put("time_ms", endTime - startTime);
        result.put("ids_per_second", count * 1000.0 / (endTime - startTime));
        
        return result;
    }
} 