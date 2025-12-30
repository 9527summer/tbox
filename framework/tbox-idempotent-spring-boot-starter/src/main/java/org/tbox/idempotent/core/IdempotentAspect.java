
package org.tbox.idempotent.core;



import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.tbox.idempotent.annotation.Idempotent;
import org.tbox.base.core.exception.RepeatConsumptionException;

import java.lang.reflect.Method;

/**
 * 幂等注解 AOP 拦截器
 */
@Aspect
public final class IdempotentAspect {


    @Around("@annotation(org.tbox.idempotent.annotation.Idempotent)")
    public Object idempotentHandler(ProceedingJoinPoint joinPoint) throws Throwable {
        Idempotent idempotent = getIdempotent(joinPoint);
        IdempotentExecuteHandler instance = IdempotentExecuteHandlerFactory.getInstance(idempotent.type());
        Object resultObj;
        try {
            instance.execute(joinPoint, idempotent);
            resultObj = joinPoint.proceed();
        } catch (RepeatConsumptionException ex) {
            throw ex;
        } catch (Throwable ex) {
            // 客户端消费存在异常，可丢到MQ重试或者直接忽略
            instance.exceptionProcessing();
            throw ex;
        } finally {
            IdempotentContext.clean();
            instance.postProcessing();
        }
        return resultObj;
    }

    public static Idempotent getIdempotent(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = joinPoint.getTarget().getClass().getDeclaredMethod(methodSignature.getName(), methodSignature.getMethod().getParameterTypes());
        return targetMethod.getAnnotation(Idempotent.class);
    }
}
