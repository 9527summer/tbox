package org.tbox.distributedid.utils;

import org.tbox.distributedid.core.Snowflake;
import org.tbox.distributedid.manage.IdGenerator;
import org.tbox.distributedid.manage.IdGeneratorManage;

public class IdUtils {

    /**
     * 获取雪花算法实例
     * @return
     */
    public static Snowflake getInstance() {
        return IdGeneratorManage.getDefaultServiceIdGenerator().getSnowflake();
    }


    /**
     * 获取雪花算法下一个 ID
     * @return
     */

    public static long nextId() {
        return getInstance().nextId();
    }


    /**
     * 获取雪花算法下一个字符串类型 ID
     * @return
     */
   public static String nextIdStr(){
       return String.valueOf(getInstance().nextId());
   }

}
