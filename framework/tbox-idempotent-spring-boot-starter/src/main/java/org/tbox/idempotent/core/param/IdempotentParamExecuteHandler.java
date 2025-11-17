package org.tbox.idempotent.core.param;


import net.openhft.hashing.LongHashFunction;
import org.aspectj.lang.ProceedingJoinPoint;
import org.tbox.base.core.exception.RepeatConsumptionException;
import org.tbox.base.lock.service.LockService;
import org.tbox.base.core.utils.AssertUtils;
import org.tbox.base.core.utils.JsonUtils;
import org.tbox.base.core.utils.WebUtils;
import org.tbox.idempotent.config.IdempotentProperties;
import org.tbox.idempotent.core.AbstractIdempotentExecuteHandler;
import org.tbox.idempotent.core.IdempotentContext;
import org.tbox.idempotent.core.IdempotentParamWrapper;
import org.tbox.idempotent.core.UserIdProvider;

/**
 * 基于方法参数验证请求幂等性
 */
public final class IdempotentParamExecuteHandler extends AbstractIdempotentExecuteHandler implements IdempotentParamService {

    private final LockService lockService;

    private final IdempotentProperties idempotentProperties;

    private final UserIdProvider userIdProvider;


    public IdempotentParamExecuteHandler(LockService lockService, IdempotentProperties idempotentProperties,UserIdProvider userIdProvider) {
        this.lockService = lockService;
        this.idempotentProperties = idempotentProperties;
        this.userIdProvider = userIdProvider;
    }

    private final static String LOCK = "lock:param:restAPI";

    @Override
    protected IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint) {
        String lockKey = String.format("idempotent:path:%s:userId:%s:hash:%s", getServletPath(), getCurrentUserId(), calcArgsHash(joinPoint));
        IdempotentParamWrapper idempotentParamWrapper = new IdempotentParamWrapper();
        idempotentParamWrapper.setJoinPoint(joinPoint);
        idempotentParamWrapper.setLockKey(lockKey);
        return idempotentParamWrapper;
    }

    /**
     * @return 获取当前线程上下文 ServletPath
     */
    private String getServletPath() {
        return WebUtils.getServletPath();
    }

    /**
     * @return 当前操作用户 ID
     */
    private String getCurrentUserId() {
        return userIdProvider.getCurrentUserId();
    }

    /**
     * @return joinPoint hash
     */
    private String calcArgsHash(ProceedingJoinPoint joinPoint) {
        long hash = LongHashFunction.murmur_3().hashBytes(JsonUtils.toBytes(joinPoint.getArgs()));
        return Long.toHexString(hash);
    }

    @Override
    public void handler(IdempotentParamWrapper wrapper) {
        String lockKey = wrapper.getLockKey();

        Long leaseTime = wrapper.getIdempotent().keyTimeout();
        if (leaseTime==null) {
            leaseTime = idempotentProperties.getTimeout();
        }

        AssertUtils.notNull(lockService, "lockService is null");
        boolean lock = lockService.lock(lockKey, 0L, leaseTime);
        if (!lock) {
            throw new RepeatConsumptionException(wrapper.getIdempotent().message());
        }
        IdempotentContext.put(LOCK, lockKey);
    }

    @Override
    public void postProcessing() {
        String lockKey= (String) IdempotentContext.getKey(LOCK);
        lockService.unlock(lockKey);
    }
}
