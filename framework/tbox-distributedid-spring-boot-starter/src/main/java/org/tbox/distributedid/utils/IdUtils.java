package org.tbox.distributedid.utils;

import org.tbox.distributedid.core.AbstractSnowflake;
import org.tbox.distributedid.manage.IdGeneratorManage;

public class IdUtils {

    /**
     * 获取雪花算法实例
     * @return
     */
    public static AbstractSnowflake getInstance() {
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


    /**
     * 变种时间格式的算法ID
     * <p>
     * 注意：该算法生成的 ID“看起来可读”，但吞吐能力低于纯 Snowflake；
     * 适用于对 QPS 要求不高（破千已不错）的业务场景。
     * @return
     */
    public static AbstractSnowflake getTimeIdInstance() {
        return IdGeneratorManage.getTimeIdGenerator().getSnowflake();
    }

   /**
    * 获取变种算法 ID (yyMMddHHmmssSSS + NodeId(2) + Seq(2))
    * @return
    */
   public static long nextTimeId() {
       return getTimeIdInstance().nextId();
   }

    /**
     * 获取变种算法下一个字符串类型 ID
     * @return
     */
   public static String nextTimeIdStr() {
       return String.valueOf(nextTimeId());
   }

}
