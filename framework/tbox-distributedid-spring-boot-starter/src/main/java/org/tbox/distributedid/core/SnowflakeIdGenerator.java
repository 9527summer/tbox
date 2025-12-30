package org.tbox.distributedid.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbox.distributedid.manage.DefaultIdGenerator;


public abstract class SnowflakeIdGenerator {

    protected static final Logger log = LoggerFactory.getLogger(SnowflakeIdGenerator.class);

    /**
     * NodeId 最大值 (10位二进制，最大1023)
     */
    protected static final int NODE_ID_MAX = 1023;

    protected static final int NODE_ID_MIN = 0;


    protected abstract WorkIdInfo getWorkIdInfo();
    protected void init() {
        WorkIdInfo workIdInfo = getWorkIdInfo();
        if (workIdInfo == null || workIdInfo.getNodeId() == null) {
            throw new IllegalStateException("Failed to allocate Snowflake nodeId");
        }
        long nodeId = workIdInfo.getNodeId();
        if (nodeId < NODE_ID_MIN || nodeId > NODE_ID_MAX) {
            throw new IllegalStateException("Snowflake nodeId out of range: " + nodeId);
        }

        Snowflake snowflake = new Snowflake(nodeId);
        DefaultIdGenerator.setSnowflake(snowflake);
        if (log.isDebugEnabled()) {
            log.debug("初始化 Snowflake 成功，nodeId:{}", nodeId);
        }
    }

}
