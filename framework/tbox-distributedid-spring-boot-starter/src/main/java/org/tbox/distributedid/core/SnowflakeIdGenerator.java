package org.tbox.distributedid.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbox.distributedid.manage.DefaultIdGenerator;


public abstract class SnowflakeIdGenerator {

    private static final Logger log = LoggerFactory.getLogger(SnowflakeIdGenerator.class);

    abstract WorkIdInfo getWordIdInfo();

    void init() {
        WorkIdInfo wordIdInfo = getWordIdInfo();
        if (wordIdInfo == null) {
            wordIdInfo = getDefaultWorkIdInfo();
        }
        Snowflake snowflake = new Snowflake(wordIdInfo.getDataCenterId(), wordIdInfo.getWorkId());
        DefaultIdGenerator.setSnowflake(snowflake);
        if (log.isDebugEnabled()) {
            log.debug("初始化分布式ID成功，workerId:{},dataCenterId:{}", wordIdInfo.getWorkId(), wordIdInfo.getDataCenterId());
        }
    }

    private WorkIdInfo getDefaultWorkIdInfo() {
        int start = 0, end = 31;
        return new WorkIdInfo(getRandom(start, end), getRandom(start, end));
    }

    private long getRandom(int start, int end) {
        long random = (long) (Math.random() * (end - start + 1) + start);
        return random;
    }
}
