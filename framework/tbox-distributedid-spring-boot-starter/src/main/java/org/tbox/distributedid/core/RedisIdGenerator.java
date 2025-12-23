package org.tbox.distributedid.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RedisIdGenerator extends SnowflakeIdGenerator implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(RedisIdGenerator.class);

    private static final String HASH_KEY = "snowflake_work_id_key";

    private RedisTemplate redisTemplate;

    public RedisIdGenerator(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    WorkIdInfo getWordIdInfo() {
        List<Long> luaResultList = null;
        try {
            DefaultRedisScript redisScript = new DefaultRedisScript();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/chooseWorkIdLua.lua")));
            redisScript.setResultType(List.class);
            luaResultList = (ArrayList) this.redisTemplate.execute(redisScript, Collections.singletonList(HASH_KEY));
        } catch (Exception ex) {
            log.error("Redis Lua 脚本获取 WorkId 失败", ex);
        }
        return !CollectionUtils.isEmpty(luaResultList) ? new WorkIdInfo(luaResultList.get(0), luaResultList.get(1)) : null;
    }

    @Override
    public void afterPropertiesSet() {
        init();
    }
}
