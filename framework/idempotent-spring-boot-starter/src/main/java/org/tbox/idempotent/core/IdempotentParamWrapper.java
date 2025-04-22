
package org.tbox.idempotent.core;


import org.aspectj.lang.ProceedingJoinPoint;
import org.tbox.idempotent.annotation.Idempotent;

/**
 * 幂等参数包装
 */

public final class IdempotentParamWrapper {

    /**
     * 幂等注解
     */
    private Idempotent idempotent;

    /**
     * AOP 处理连接点
     */
    private ProceedingJoinPoint joinPoint;

    /**
     * 锁标识
     */
    private String lockKey;


    public IdempotentParamWrapper() {
    }

    public IdempotentParamWrapper(Idempotent idempotent, ProceedingJoinPoint joinPoint, String lockKey) {
        this.idempotent = idempotent;
        this.joinPoint = joinPoint;
        this.lockKey = lockKey;
    }


    public Idempotent getIdempotent() {
        return idempotent;
    }

    public void setIdempotent(Idempotent idempotent) {
        this.idempotent = idempotent;
    }

    public ProceedingJoinPoint getJoinPoint() {
        return joinPoint;
    }

    public void setJoinPoint(ProceedingJoinPoint joinPoint) {
        this.joinPoint = joinPoint;
    }

    public String getLockKey() {
        return lockKey;
    }

    public void setLockKey(String lockKey) {
        this.lockKey = lockKey;
    }
}
