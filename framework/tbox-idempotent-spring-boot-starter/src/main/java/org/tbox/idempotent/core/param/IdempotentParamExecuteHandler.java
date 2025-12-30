package org.tbox.idempotent.core.param;

import net.openhft.hashing.LongHashFunction;
import org.aspectj.lang.ProceedingJoinPoint;
import org.tbox.base.core.utils.JsonUtils;
import org.tbox.idempotent.config.IdempotentProperties;
import org.tbox.idempotent.core.AbstractIdempotentExecuteHandler;
import org.tbox.idempotent.core.IdempotentParamWrapper;
import org.tbox.idempotent.core.UserIdProvider;

/**
 * 基于方法参数验证请求幂等性
 * Key组成: path + userId + 参数hash
 */
public final class IdempotentParamExecuteHandler extends AbstractIdempotentExecuteHandler implements IdempotentParamService {

    private final UserIdProvider userIdProvider;

    public IdempotentParamExecuteHandler(IdempotentProperties idempotentProperties, UserIdProvider userIdProvider) {
        super(idempotentProperties);
        this.userIdProvider = userIdProvider;
    }

    @Override
    protected IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint) {
        String lockKey = String.format("idempotent:param:path:%s:userId:%s:hash:%s",
                getServletPath(), getCurrentUserId(), calcArgsHash(joinPoint));

        IdempotentParamWrapper wrapper = new IdempotentParamWrapper();
        wrapper.setJoinPoint(joinPoint);
        wrapper.setLockKey(lockKey);
        return wrapper;
    }

    /**
     * @return 当前操作用户 ID
     */
    private String getCurrentUserId() {
        return userIdProvider.getCurrentUserId();
    }

    /**
     * @return 参数哈希值
     */
    private String calcArgsHash(ProceedingJoinPoint joinPoint) {
        long hash = LongHashFunction.murmur_3().hashBytes(JsonUtils.toBytes(joinPoint.getArgs()));
        return Long.toHexString(hash);
    }
}