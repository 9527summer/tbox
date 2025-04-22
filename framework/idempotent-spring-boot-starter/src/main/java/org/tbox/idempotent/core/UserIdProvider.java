package org.tbox.idempotent.core;


/**
 * 用户ID接口
 */
@FunctionalInterface
public interface UserIdProvider {


    /**
     * 获取当前用户ID
     *
     * @return 当前用户ID，如果未认证则返回默认值
     */
    String getCurrentUserId();


}
