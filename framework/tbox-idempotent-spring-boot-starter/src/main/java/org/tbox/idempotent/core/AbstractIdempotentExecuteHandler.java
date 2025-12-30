package org.tbox.idempotent.core;

import org.aspectj.lang.ProceedingJoinPoint;
import org.tbox.base.core.exception.RepeatConsumptionException;
import org.tbox.base.core.utils.WebUtils;
import org.tbox.base.redis.utils.LockUtils;
import org.tbox.idempotent.config.IdempotentProperties;

import java.util.concurrent.TimeUnit;

/**
 * 抽象幂等执行处理器
 */
public abstract class AbstractIdempotentExecuteHandler implements IdempotentExecuteHandler {

    private static final String LOCK_KEY = "idempotent:lock:key";

    protected final IdempotentProperties idempotentProperties;

    protected AbstractIdempotentExecuteHandler(IdempotentProperties idempotentProperties) {
        this.idempotentProperties = idempotentProperties;
    }

    /**
     * 构建幂等验证过程中所需要的参数包装器
     *
     * @param joinPoint AOP 方法处理
     * @return 幂等参数包装器
     */
    protected abstract IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint) throws NoSuchMethodException;

    /**
     * 执行幂等处理逻辑
     *
     * @param joinPoint  AOP 方法处理
     * @param idempotent 幂等注解
     */
    @Override
    public void execute(ProceedingJoinPoint joinPoint, org.tbox.idempotent.annotation.Idempotent idempotent) throws NoSuchMethodException {
        // 模板方法模式：构建幂等参数包装器
        IdempotentParamWrapper wrapper = buildWrapper(joinPoint);
        wrapper.setIdempotent(idempotent);
        handler(wrapper);
    }

    @Override
    public void handler(IdempotentParamWrapper wrapper) {
        String lockKey = wrapper.getLockKey();

        long leaseTime = wrapper.getIdempotent().keyTimeout();
        if (leaseTime <= 0) {
            leaseTime = idempotentProperties.getTimeout();
        }

        boolean locked = LockUtils.tryLock(lockKey, 0L, leaseTime, TimeUnit.SECONDS);
        if (!locked) {
            throw new RepeatConsumptionException(wrapper.getIdempotent().message());
        }
        IdempotentContext.put(LOCK_KEY, lockKey);
    }

    @Override
    public void exceptionProcessing() {
        // 异常时释放锁，允许用户重试
        String lockKey = (String) IdempotentContext.getKey(LOCK_KEY);
        if (lockKey != null) {
            LockUtils.unlock(lockKey);
        }
    }

    @Override
    public void postProcessing() {
        // 正常执行完成后释放锁
        String lockKey = (String) IdempotentContext.getKey(LOCK_KEY);
        if (lockKey != null) {
            LockUtils.unlock(lockKey);
        }
    }

    /**
     * 获取当前请求路径
     */
    protected String getServletPath() {
        return WebUtils.getServletPath();
    }
}