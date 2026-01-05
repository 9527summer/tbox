package com.example.tboxdemo.controller;

import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用于测试 tbox-distributedid 的 Lua 脚本分配 nodeId。
 *
 * <p>脚本：classpath:lua/chooseWorkIdLua.lua</p>
 * <p>Redis Key（默认）：tbox:ids:registry:${spring.application.name}</p>
 */
@RestController
@RequestMapping("/debug/lua")
public class LuaWorkIdController {

    private static final String REGISTRY_KEY_PREFIX = "tbox:ids:registry:";
    private static final long DEFAULT_EXPIRE_MS = 24 * 60 * 60 * 1000L;
    private static final int DEFAULT_MAX_ID = 1023;

    private static final DefaultRedisScript<Long> CHOOSE_WORK_ID_SCRIPT;

    static {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/chooseWorkIdLua.lua")));
        script.setResultType(Long.class);
        CHOOSE_WORK_ID_SCRIPT = script;
    }

    private final StringRedisTemplate redisTemplate;
    private final Environment environment;

    public LuaWorkIdController(StringRedisTemplate redisTemplate, Environment environment) {
        this.redisTemplate = redisTemplate;
        this.environment = environment;
    }

    /**
     * 执行一次 Lua，申请一个 nodeId。
     */
    @GetMapping("/chooseWorkId")
    public Map<String, Object> chooseWorkId(
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "expireMs", required = false) Long expireMs,
            @RequestParam(value = "maxId", required = false) Integer maxId) {

        String registryKey = (key != null && !key.isBlank()) ? key : defaultRegistryKey();
        long expire = (expireMs != null && expireMs > 0) ? expireMs : DEFAULT_EXPIRE_MS;
        int max = (maxId != null && maxId > 0) ? maxId : DEFAULT_MAX_ID;

        long now = System.currentTimeMillis();
        Object[] args = new Object[]{String.valueOf(now), String.valueOf(expire), String.valueOf(max)};

        Long nodeId = redisTemplate.execute(CHOOSE_WORK_ID_SCRIPT, Collections.singletonList(registryKey), args);

        Map<String, Object> resp = new HashMap<>();
        resp.put("registryKey", registryKey);
        resp.put("now", now);
        resp.put("expireMs", expire);
        resp.put("expireAt", now + expire);
        resp.put("maxId", max);
        resp.put("nodeId", nodeId);
        return resp;
    }

    /**
     * 连续执行多次 Lua，用于观察是否按序分配、是否会返回 -1 等。
     */
    @GetMapping("/chooseWorkId/batch")
    public Map<String, Object> chooseWorkIdBatch(
            @RequestParam(value = "count", defaultValue = "10") int count,
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "expireMs", required = false) Long expireMs,
            @RequestParam(value = "maxId", required = false) Integer maxId) {

        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }

        String registryKey = (key != null && !key.isBlank()) ? key : defaultRegistryKey();
        long expire = (expireMs != null && expireMs > 0) ? expireMs : DEFAULT_EXPIRE_MS;
        int max = (maxId != null && maxId > 0) ? maxId : DEFAULT_MAX_ID;

        List<Long> allocated = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long now = System.currentTimeMillis();
            Object[] args = new Object[]{String.valueOf(now), String.valueOf(expire), String.valueOf(max)};
            allocated.add(redisTemplate.execute(CHOOSE_WORK_ID_SCRIPT, Collections.singletonList(registryKey), args));
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("registryKey", registryKey);
        resp.put("count", count);
        resp.put("allocated", allocated);
        resp.put("uniqueCount", allocated.stream().filter(v -> v != null).collect(Collectors.toSet()).size());
        return resp;
    }

    /**
     * 查看当前 registry ZSET 的占用情况（返回前 N 条，按 score 从小到大）。
     */
    @GetMapping("/registry")
    public Map<String, Object> registry(
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {

        String registryKey = (key != null && !key.isBlank()) ? key : defaultRegistryKey();
        int n = Math.max(1, limit);

        ZSetOperations<String, String> ops = redisTemplate.opsForZSet();
        Set<ZSetOperations.TypedTuple<String>> tuples = ops.rangeWithScores(registryKey, 0, n - 1);

        List<Map<String, Object>> items = new ArrayList<>();
        if (tuples != null) {
            for (ZSetOperations.TypedTuple<String> t : tuples) {
                Map<String, Object> row = new HashMap<>();
                row.put("member", t.getValue());
                row.put("score", t.getScore());
                items.add(row);
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("registryKey", registryKey);
        resp.put("zCard", ops.zCard(registryKey));
        resp.put("items", items);
        return resp;
    }

    /**
     * 手动释放一个 nodeId（ZREM），便于重复测试。
     */
    @DeleteMapping("/registry")
    public Map<String, Object> release(
            @RequestParam(value = "key", required = false) String key,
            @RequestParam("nodeId") long nodeId) {

        String registryKey = (key != null && !key.isBlank()) ? key : defaultRegistryKey();
        Long removed = redisTemplate.opsForZSet().remove(registryKey, String.valueOf(nodeId));

        Map<String, Object> resp = new HashMap<>();
        resp.put("registryKey", registryKey);
        resp.put("nodeId", nodeId);
        resp.put("removed", removed);
        return resp;
    }

    private String defaultRegistryKey() {
        String appName = environment.getProperty("spring.application.name", "default-app");
        return REGISTRY_KEY_PREFIX + appName;
    }
}

