package org.tbox.base.lock.service;

/**
 * 分布式锁服务接口
 */
public interface LockService {

    /**
     * 获取分布式锁
     *
     * @param lockKey   锁的唯一标识键
     * @param leaseTime 锁的持有时间（秒，自动过期防止死锁）
     * @return true-获取锁成功，false-获取锁失败
     */
    boolean lock(String lockKey, long waitTime, long leaseTime);


    /**
     * 释放分布式锁
     *
     * @param lockKey 锁的唯一标识键
     */
    void unlock(String lockKey);
}
