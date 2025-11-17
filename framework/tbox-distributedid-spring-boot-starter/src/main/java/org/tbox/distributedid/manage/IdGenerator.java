package org.tbox.distributedid.manage;


import org.tbox.distributedid.core.Snowflake;

public interface IdGenerator {

    Snowflake getSnowflake();


}
