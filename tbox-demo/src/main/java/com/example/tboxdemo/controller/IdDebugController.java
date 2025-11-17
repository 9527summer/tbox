package com.example.tboxdemo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tbox.distributedid.core.Snowflake;
import org.tbox.distributedid.manage.IdGeneratorManage;
import org.tbox.distributedid.utils.IdUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * ID生成器调试控制器
 * 用于检查和诊断分布式ID生成器相关问题
 */
@RestController
@RequestMapping("/id/debug")
public class IdDebugController {
    private static final Logger log = LoggerFactory.getLogger(IdDebugController.class);
    
    @Autowired
    private RedisTemplate redisTemplate;
    
    /**
     * 获取当前ID生成器状态和配置
     */
    @GetMapping("/status")
    public Map<String, Object> getIdGeneratorStatus() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取当前雪花算法实例
            Snowflake snowflake = IdUtils.getInstance();
            if (snowflake != null) {
                // 生成一个测试ID
                long testId = snowflake.nextId();
                
                result.put("status", "active");
                result.put("test_id", testId);
                result.put("workerId", snowflake.getWorkerId(testId));
                result.put("dataCenterId", snowflake.getDataCenterId(testId));
                result.put("timestamp", snowflake.getGenerateDateTime(testId));
                
                // 测试多个ID是否有序
                List<Long> testIds = new ArrayList<>();
                for (int i = 0; i < 5; i++) {
                    testIds.add(snowflake.nextId());
                }
                result.put("test_ids", testIds);
            } else {
                result.put("status", "not_initialized");
                result.put("error", "雪花算法实例未初始化");
            }
        } catch (Exception e) {
            log.error("获取ID生成器状态时出错", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 检查Redis连接状态
     */
    @GetMapping("/redis")
    public Map<String, Object> checkRedisConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                result.put("status", "error");
                result.put("error", "Redis连接工厂为空");
                return result;
            }
            
            try (RedisConnection connection = connectionFactory.getConnection()) {
                // 检查Redis连接
                String pong = connection.ping();
                result.put("status", "OK");
                result.put("ping", pong);
                
                // 获取Redis信息
                Properties info = connection.info();
                Map<String, String> redisInfo = new HashMap<>();
                
                if (info != null) {
                    // 提取关键信息
                    redisInfo.put("redis_version", info.getProperty("redis_version"));
                    redisInfo.put("redis_mode", info.getProperty("redis_mode"));
                    redisInfo.put("connected_clients", info.getProperty("connected_clients"));
                    redisInfo.put("used_memory_human", info.getProperty("used_memory_human"));
                    
                    // 检查是否是集群模式
                    boolean isCluster = "cluster".equals(info.getProperty("redis_mode"));
                    result.put("is_cluster", isCluster);
                    
                    if (isCluster) {
                        // 添加集群特定信息
                        redisInfo.put("cluster_enabled", info.getProperty("cluster_enabled"));
                        redisInfo.put("cluster_size", info.getProperty("cluster_known_nodes"));
                        
                        // 集群模式下，查询WorkId键是否存在
                        try {
                            Object workIdKey = redisTemplate.opsForHash().get("snowflake_work_id_key", "workId");
                            result.put("workId_exists", workIdKey != null);
                            result.put("current_workId", workIdKey);
                        } catch (Exception e) {
                            result.put("workId_error", e.getMessage());
                        }
                    }
                }
                
                result.put("redis_info", redisInfo);
            }
        } catch (Exception e) {
            log.error("检查Redis连接时出错", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 测试Redis Hash操作，确认与Lua脚本相同的操作直接执行可以成功
     */
    @GetMapping("/test-redis-hash")
    public Map<String, Object> testRedisHashOperations() {
        Map<String, Object> result = new HashMap<>();
        String hashKey = "snowflake_work_id_key";
        
        try {
            // 如果不存在，初始化
            if (!redisTemplate.hasKey(hashKey)) {
                redisTemplate.opsForHash().put(hashKey, "dataCenterId", "0");
                redisTemplate.opsForHash().put(hashKey, "workId", "0");
                result.put("init", "created new hash");
            }
            
            // 读取当前值
            Object dataCenterId = redisTemplate.opsForHash().get(hashKey, "dataCenterId");
            Object workId = redisTemplate.opsForHash().get(hashKey, "workId");
            
            result.put("current_dataCenterId", dataCenterId);
            result.put("current_workId", workId);
            
            // 增加workId
            Long newWorkId = redisTemplate.opsForHash().increment(hashKey, "workId", 1);
            result.put("new_workId", newWorkId);
            
            // 再次读取确认
            dataCenterId = redisTemplate.opsForHash().get(hashKey, "dataCenterId");
            workId = redisTemplate.opsForHash().get(hashKey, "workId");
            
            result.put("confirmed_dataCenterId", dataCenterId);
            result.put("confirmed_workId", workId);
            
            result.put("status", "OK");
        } catch (Exception e) {
            log.error("测试Redis Hash操作时出错", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        
        return result;
    }
} 