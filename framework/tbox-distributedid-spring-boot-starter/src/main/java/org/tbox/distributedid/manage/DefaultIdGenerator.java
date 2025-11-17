package org.tbox.distributedid.manage;

import org.tbox.distributedid.core.Snowflake;

public class DefaultIdGenerator implements IdGenerator{

    private static Snowflake SNOWFLAKE;

    public static void setSnowflake(Snowflake snowflake) {
        if(DefaultIdGenerator.SNOWFLAKE==null){
            DefaultIdGenerator.SNOWFLAKE = snowflake;
        }
    }

    @Override
    public Snowflake getSnowflake() {
        return SNOWFLAKE;
    }
}
