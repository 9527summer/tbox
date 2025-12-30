package org.tbox.distributedid.core;

import org.springframework.beans.factory.InitializingBean;

/**
 * 单机/开发模式的 Snowflake nodeId 分配方式：随机分配 0~1023。
 * <p>
 * 注意：随机分配不具备集群唯一性保障，仅适用于单机模式。
 */
public class RandomIdGenerator extends SnowflakeIdGenerator implements InitializingBean {

    @Override
    public void afterPropertiesSet()  {
        init();
    }

    @Override
    protected WorkIdInfo getWorkIdInfo() {
        return new WorkIdInfo(getRandom(NODE_ID_MIN, NODE_ID_MAX));
    }

    private long getRandom(int start, int end) {
        return (long) (Math.random() * (end - start + 1) + start);
    }
}
