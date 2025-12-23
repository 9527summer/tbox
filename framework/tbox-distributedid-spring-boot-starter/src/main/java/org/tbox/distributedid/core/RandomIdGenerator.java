package org.tbox.distributedid.core;

import org.springframework.beans.factory.InitializingBean;

public class RandomIdGenerator extends SnowflakeIdGenerator implements InitializingBean {
    @Override
    public void afterPropertiesSet()  {
        init();
    }

    @Override
    WorkIdInfo getWordIdInfo() {
        return null;
    }
}
