package org.tbox.distributedid.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 使用 Redis ZSET 分配 TimeSnowflake 的 nodeId（0~99），并通过心跳续租。
 */
public class TimeRedisIdGenerator extends TimeSnowflakeIdGenerator implements InitializingBean, DisposableBean, EnvironmentAware {

    private static final Logger log = LoggerFactory.getLogger(TimeRedisIdGenerator.class);

    private static final String REGISTRY_KEY_PREFIX = "tbox:ids:registry-time:";

    // 24小时过期 (ms)
    private static final long EXPIRE_TIME = 24 * 60 * 60 * 1000L;
    // 心跳间隔 30秒
    private static final long HEARTBEAT_INTERVAL = 30;

    private final RedisTemplate<String, Object> redisTemplate;
    private Environment environment;
    private String registryKey;
    private ScheduledExecutorService heartbeatExecutor;
    private volatile Long currentNodeId;

    public TimeRedisIdGenerator(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    protected WorkIdInfo getWorkIdInfo() {
        String appName = environment.getProperty("spring.application.name", "default-app");
        this.registryKey = REGISTRY_KEY_PREFIX + appName;

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/chooseWorkIdLua.lua")));
        redisScript.setResultType(Long.class);

        Object[] args = new Object[]{
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(EXPIRE_TIME),
                NODE_ID_MAX
        };

        Long nodeId = redisTemplate.execute(redisScript, Collections.singletonList(registryKey), args);
        if (nodeId == null || nodeId < 0 || nodeId > NODE_ID_MAX) {
            log.error("Redis 分配 TimeSnowflake NodeId 失败, appName={}, nodeId={}", appName, nodeId);
            throw new IllegalStateException("Redis 分配 TimeSnowflake NodeId 失败");
        }

        this.currentNodeId = nodeId;
        startHeartbeat();
        return new WorkIdInfo(nodeId);
    }

    private void startHeartbeat() {
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "tbox-timeid-heartbeat");
            t.setDaemon(true);
            return t;
        });

        heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                if (currentNodeId == null) return;
                double newScore = System.currentTimeMillis() + EXPIRE_TIME;
                redisTemplate.opsForZSet().add(registryKey, currentNodeId, newScore);
            } catch (Exception e) {
                log.error("TimeSnowflake NodeId 心跳异常", e);
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
    }

    @Override
    public void afterPropertiesSet() {
        init();
    }

    @Override
    public void destroy() {
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdown();
        }
        try {
            if (registryKey != null && currentNodeId != null) {
                redisTemplate.opsForZSet().remove(registryKey, currentNodeId);
            }
        } catch (Exception e) {
            log.warn("TimeSnowflake NodeId 释放失败: {}", currentNodeId, e);
        }
    }
}
