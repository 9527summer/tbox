package org.tbox.base.lock.service;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

public class RedissonLockService implements LockService{
    private final RedissonClient redissonClient;

    public RedissonLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 尝试获取基于Redisson的分布式锁
     *
     * @param lockKey   锁的唯一标识键
     * @param waitTime  最大等待获取锁时间（秒）
     * @param leaseTime 锁的持有时间（秒，自动过期防止死锁）
     * @return true-获取锁成功，false-获取锁失败
     */
    @Override
    public boolean lock(String lockKey, long waitTime, long leaseTime) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }



    /**
     * 释放分布式锁
     *
     * @param lockKey 锁的唯一标识键
     */
    @Override
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isLocked() && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
