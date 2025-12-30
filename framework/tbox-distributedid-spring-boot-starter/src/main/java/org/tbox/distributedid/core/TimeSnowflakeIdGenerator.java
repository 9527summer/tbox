package org.tbox.distributedid.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbox.distributedid.manage.TimeIdGenerator;

public abstract class TimeSnowflakeIdGenerator {

    private static final Logger log = LoggerFactory.getLogger(TimeSnowflakeIdGenerator.class);

    /**
     * NodeId 最大值 (最大99)
     */
    protected static final int NODE_ID_MAX = 99;

    protected static final int NODE_ID_MIN = 0;

    protected abstract WorkIdInfo getWorkIdInfo();

    protected void init() {
        WorkIdInfo workIdInfo = getWorkIdInfo();
        if (workIdInfo == null || workIdInfo.getNodeId() == null) {
            throw new IllegalStateException("Failed to allocate TimeSnowflake nodeId");
        }
        long nodeId = workIdInfo.getNodeId();
        if (nodeId < NODE_ID_MIN || nodeId > NODE_ID_MAX) {
            throw new IllegalStateException("TimeSnowflake nodeId out of range: " + nodeId);
        }

        TimeSnowflake snowflake = new TimeSnowflake(nodeId);
        TimeIdGenerator.setSnowflake(snowflake);

        if (log.isDebugEnabled()) {
            log.debug("初始化 TimeSnowflake 成功，nodeId:{}", nodeId);
        }
    }
}
