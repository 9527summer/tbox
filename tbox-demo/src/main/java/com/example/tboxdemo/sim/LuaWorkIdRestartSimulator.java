package com.example.tboxdemo.sim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 用于模拟“频繁重启/实例身份变化（如 IP 变化）”下的 Lua 分配行为。
 *
 * <p>开启方式：</p>
 * <pre>
 * tbox.lua.sim.enabled=true
 * tbox.lua.sim.iterations=50
 * tbox.lua.sim.expireMs=3000
 * tbox.lua.sim.maxId=16
 * tbox.lua.sim.keyMode=ip     # ip|app
 * tbox.lua.sim.release=true   # true 表示模拟优雅停机会释放（ZREM），false 表示模拟崩溃不释放
 * tbox.lua.sim.reclaim=true   # true 表示增加“等待过期后再分配”的回收验证
 * tbox.lua.sim.reclaimWaitMs=0 # 0 表示默认等待 expireMs+200ms
 * </pre>
 */
@Component
@ConditionalOnProperty(prefix = "tbox.lua.sim", name = "enabled", havingValue = "true")
public class LuaWorkIdRestartSimulator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LuaWorkIdRestartSimulator.class);

    private static final DefaultRedisScript<Long> CHOOSE_WORK_ID_SCRIPT;

    static {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/chooseWorkIdLua.lua")));
        script.setResultType(Long.class);
        CHOOSE_WORK_ID_SCRIPT = script;
    }

    private final StringRedisTemplate redisTemplate;
    private final Environment environment;

    public LuaWorkIdRestartSimulator(StringRedisTemplate redisTemplate, Environment environment) {
        this.redisTemplate = redisTemplate;
        this.environment = environment;
    }

    @Override
    public void run(String... args) throws Exception {
        int iterations = envInt("tbox.lua.sim.iterations", 20);
        long expireMs = envLong("tbox.lua.sim.expireMs", 3_000L);
        int maxId = envInt("tbox.lua.sim.maxId", 32);
        String keyMode = environment.getProperty("tbox.lua.sim.keyMode", "app");
        boolean release = Boolean.parseBoolean(environment.getProperty("tbox.lua.sim.release", "false"));
        boolean reclaim = Boolean.parseBoolean(environment.getProperty("tbox.lua.sim.reclaim", "false"));
        long reclaimWaitMs = envLong("tbox.lua.sim.reclaimWaitMs", 0L);

        String appName = environment.getProperty("spring.application.name", "default-app");

        log.info("Lua restart sim start: iterations={}, expireMs={}, maxId={}, keyMode={}, release={}, reclaim={}, reclaimWaitMs={}",
                iterations, expireMs, maxId, keyMode, release, reclaim, reclaimWaitMs);

        Long lastAllocated = null;
        String lastKey = null;

        for (int i = 0; i < iterations; i++) {
            String key = buildRegistryKey(appName, keyMode, i);
            long now = System.currentTimeMillis();
            Object[] luaArgs = new Object[]{String.valueOf(now), String.valueOf(expireMs), String.valueOf(maxId)};

            Long nodeId = redisTemplate.execute(CHOOSE_WORK_ID_SCRIPT, Collections.singletonList(key), luaArgs);
            log.info("iter={} key={} nodeId={} expireAt={}", i, key, nodeId, now + expireMs);

            if (release && lastAllocated != null && lastKey != null) {
                // 模拟“优雅停机释放占用”
                redisTemplate.opsForZSet().remove(lastKey, String.valueOf(lastAllocated));
                log.info("released nodeId={} from key={}", lastAllocated, lastKey);
            }

            lastAllocated = nodeId;
            lastKey = key;

            // 模拟重启间隔（很短）
            Thread.sleep(100L);
        }

        if (reclaim) {
            String key = buildRegistryKey(appName, "app", 0);
            long waitMs = reclaimWaitMs > 0 ? reclaimWaitMs : (expireMs + 200L);

            log.info("Lua reclaim verify start: key={}, maxId={}, expireMs={}, waitMs={}", key, maxId, expireMs, waitMs);

            // Phase 1: fill pool (expect the last one to return -1)
            int fillCount = maxId + 2;
            Long last = null;
            for (int i = 0; i < fillCount; i++) {
                last = allocateOnce(key, expireMs, maxId);
                log.info("[fill] i={} nodeId={}", i, last);
            }

            if (last == null || last >= 0) {
                log.warn("pool not exhausted as expected (last={}), consider using smaller maxId", last);
            } else {
                log.info("pool exhausted ok (last={})", last);
            }

            log.info("waiting for expire... {}ms", waitMs);
            Thread.sleep(waitMs);

            // Phase 2: allocate again (should reuse from 0..maxId)
            for (int i = 0; i <= maxId; i++) {
                Long nodeId = allocateOnce(key, expireMs, maxId);
                log.info("[reclaim] i={} nodeId={}", i, nodeId);
            }

            log.info("Lua reclaim verify done");
        }

        log.info("Lua restart sim done");
    }

    private Long allocateOnce(String key, long expireMs, int maxId) {
        long now = System.currentTimeMillis();
        Object[] luaArgs = new Object[]{String.valueOf(now), String.valueOf(expireMs), String.valueOf(maxId)};
        return redisTemplate.execute(CHOOSE_WORK_ID_SCRIPT, Collections.singletonList(key), luaArgs);
    }

    private String buildRegistryKey(String appName, String keyMode, int iter) {
        String prefix = "tbox:ids:registry:";
        if ("ip".equalsIgnoreCase(keyMode)) {
            // 注意：真实实现的 key 默认只按 appName，这里用“ip-xx”后缀模拟实例身份变化
            return prefix + appName + ":ip-" + iter;
        }
        return prefix + appName;
    }

    private int envInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(environment.getProperty(key, String.valueOf(defaultValue)));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private long envLong(String key, long defaultValue) {
        try {
            return Long.parseLong(environment.getProperty(key, String.valueOf(defaultValue)));
        } catch (Exception e) {
            return defaultValue;
        }
    }
}

