package org.tbox.base.lock.service;

import org.tbox.base.lock.utils.RedisLockUtils;

public class RedisTemplateLockService implements LockService {

    /**
     * 尝试获取基于Redis的分布式锁
     *
     * @param lockKey   锁的唯一标识键
     * @param waitTime  最大等待获取锁时间（秒）
     * @param leaseTime 锁的持有时间（秒，自动过期防止死锁）
     * @return true-获取锁成功，false-获取锁失败
     */
    @Override
    public boolean lock(String lockKey, long waitTime, long leaseTime) {
        return RedisLockUtils.lock(lockKey, leaseTime);

    }


    /**
     * 释放分布式锁
     *
     * @param lockKey 锁
     *                的唯一标识键
     */
    @Override
    public void unlock(String lockKey) {
        RedisLockUtils.unlock(lockKey);
    }
}
