package org.tbox.distributedid.manage;

import org.tbox.distributedid.core.AbstractSnowflake;
import org.tbox.distributedid.core.TimeSnowflake;

public class TimeIdGenerator implements IdGenerator {

    private static TimeSnowflake SNOWFLAKE;

    public static void setSnowflake(TimeSnowflake snowflake) {
        if(TimeIdGenerator.SNOWFLAKE==null){
            TimeIdGenerator.SNOWFLAKE = snowflake;
        }
    }

    @Override
    public AbstractSnowflake getSnowflake() {
        return SNOWFLAKE;
    }
}
