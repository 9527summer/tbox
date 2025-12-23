package org.tbox.idempotent.core.spel;

import org.aspectj.lang.ProceedingJoinPoint;
import org.tbox.base.lock.service.LockService;
import org.tbox.idempotent.config.IdempotentProperties;
import org.tbox.idempotent.core.AbstractIdempotentExecuteHandler;
import org.tbox.idempotent.core.IdempotentParamWrapper;
//todo 待实现
public final class IdempotentSpelExecuteHandler extends AbstractIdempotentExecuteHandler implements IdempotentSpelService {

    private final LockService lockService;

    private final IdempotentProperties idempotentProperties;

    public IdempotentSpelExecuteHandler(LockService lockService, IdempotentProperties idempotentProperties) {
        this.lockService = lockService;
        this.idempotentProperties = idempotentProperties;
    }

    @Override
    public void handler(IdempotentParamWrapper wrapper) {

    }

    @Override
    public void exceptionProcessing() {
        super.exceptionProcessing();
    }

    @Override
    public void postProcessing() {
        super.postProcessing();
    }

    @Override
    protected IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint) {

        return null;
    }
}
