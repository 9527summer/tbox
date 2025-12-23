package com.example.tboxdemo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
//import org.tbox.distributedid.utils.IdUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 分布式ID生成器测试控制器
 */
@RestController
@RequestMapping("/id")
public class IdTestController {


    /**
     * 生成一个ID
     */
    @GetMapping("/next")
    public Map<String, Object> nextId() {
        Map<String, Object> result = new HashMap<>();
//
//        long id = IdUtils.nextId();
//        result.put("id", id);
//        result.put("time", System.currentTimeMillis());

        return result;
    }

    /**
     * 生成字符串形式的ID
     */
    @GetMapping("/next/str")
    public Map<String, Object> nextIdStr() {
        Map<String, Object> result = new HashMap<>();

//        String id = IdUtils.nextIdStr();
//        result.put("id", id);
//        result.put("time", System.currentTimeMillis());

        return result;
    }

    /**
     * 批量生成ID
     */
    @GetMapping("/batch")
    public Map<String, Object> batchIds() {
        Map<String, Object> result = new HashMap<>();

//        List<Long> ids = new ArrayList<>();
//        for (int i = 0; i < 10; i++) {
//            ids.add(IdUtils.nextId());
//        }
//
//        result.put("ids", ids);
//        result.put("count", ids.size());
//        result.put("time", System.currentTimeMillis());

        return result;
    }

    /**
     * 性能测试：生成10000个ID并统计耗时
     */
    @GetMapping("/performance")
    public Map<String, Object> performanceTest() {
        Map<String, Object> result = new HashMap<>();

        int count = 10000;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
//            IdUtils.nextId();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        result.put("count", count);
        result.put("duration_ms", duration);
        result.put("ids_per_second", count * 1000.0 / duration);

        return result;
    }
    
    /**
     * 多线程并发测试：检测是否会生成重复ID
     */
    @GetMapping("/concurrent")
    public Map<String, Object> concurrentTest() {
        Map<String, Object> result = new HashMap<>();
        
        // 线程数
        int threadCount = 20;
        // 每个线程生成ID的数量
        int idsPerThread = 5000;
        // 总共要生成的ID数量
        int totalCount = threadCount * idsPerThread;
        
        // 用于存储生成的所有ID，使用ConcurrentHashMap保证线程安全
        Set<Long> allIds = ConcurrentHashMap.newKeySet(totalCount);
        // 用于存储重复的ID
        Set<Long> duplicateIds = ConcurrentHashMap.newKeySet();
        
        // 线程池
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        // 倒计时锁存器，用于等待所有线程完成
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        long startTime = System.currentTimeMillis();
        
        // 启动多个线程并发生成ID
        for (int t = 0; t < threadCount; t++) {
            executorService.execute(() -> {
                try {
                    for (int i = 0; i < idsPerThread; i++) {
//                        long id = IdUtils.nextId();
                        long id = 0L;
                        // 如果添加失败，说明ID已经存在，是重复的
                        if (!allIds.add(id)) {
                            duplicateIds.add(id);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            // 等待所有线程完成
            latch.await();
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
        result.put("total_expected", totalCount);
        result.put("total_generated", allIds.size());
        result.put("unique_ids", allIds.size());
        result.put("duplicate_count", duplicateIds.size());
        result.put("has_duplicates", !duplicateIds.isEmpty());
        result.put("duration_ms", duration);
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
}
