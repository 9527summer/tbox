package org.tbox.distributedid.manage;


import org.tbox.distributedid.core.AbstractSnowflake;

public interface IdGenerator {

    AbstractSnowflake getSnowflake();


}
