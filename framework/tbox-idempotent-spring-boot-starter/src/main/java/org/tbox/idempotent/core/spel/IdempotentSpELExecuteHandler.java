package org.tbox.idempotent.core.spel;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.StringUtils;
import org.tbox.base.core.exception.SysException;
import org.tbox.idempotent.annotation.Idempotent;
import org.tbox.idempotent.config.IdempotentProperties;
import org.tbox.idempotent.core.AbstractIdempotentExecuteHandler;
import org.tbox.idempotent.core.IdempotentAspect;
import org.tbox.idempotent.core.IdempotentParamWrapper;

import java.lang.reflect.Method;

/**
 * 基于SpEL表达式验证请求幂等性
 * Key组成: path + SpEL表达式解析值
 */
public final class IdempotentSpELExecuteHandler extends AbstractIdempotentExecuteHandler implements IdempotentSpelService {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    public IdempotentSpELExecuteHandler(IdempotentProperties idempotentProperties) {
        super(idempotentProperties);
    }

    @Override
    protected IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        Idempotent idempotent = IdempotentAspect.getIdempotent(joinPoint);
        String spELKey = idempotent.key();
        if (!StringUtils.hasText(spELKey)) {
            throw new SysException("SPEL类型幂等必须指定key表达式");
        }

        String parsedKey = parseSpelKey(spELKey, joinPoint);
        if (!StringUtils.hasText(parsedKey)) {
            throw new SysException("SpEL表达式解析结果为空: " + spELKey);
        }

        String lockKey = String.format("idempotent:spel:path:%s:key:%s", getServletPath(), parsedKey);

        IdempotentParamWrapper wrapper = new IdempotentParamWrapper();
        wrapper.setJoinPoint(joinPoint);
        wrapper.setLockKey(lockKey);
        return wrapper;
    }

    /**
     * 解析SpEL表达式
     *
     * @param spelKey   SpEL表达式，如 "#request.orderId" 或 "#orderId"
     * @param joinPoint AOP连接点
     * @return 解析后的值
     */
    private String parseSpelKey(String spelKey, ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                null, method, args, NAME_DISCOVERER);

        Object value = PARSER.parseExpression(spelKey).getValue(context);
        return value != null ? value.toString() : null;
    }
}
