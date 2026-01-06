package org.tbox.redis.ratelimit.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.tbox.base.core.enums.StandardErrorCodeEnum;
import org.tbox.base.core.exception.BizException;
import org.tbox.redis.ratelimit.RateLimitChecker;
import org.tbox.redis.ratelimit.RateLimitMode;
import org.tbox.redis.ratelimit.RateLimitRequest;
import org.tbox.redis.ratelimit.annotation.RateLimit;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Aspect
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private final RateLimitChecker rateLimitChecker;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public RateLimitAspect(RateLimitChecker rateLimitChecker) {
        this.rateLimitChecker = rateLimitChecker;
    }

    @Around("@annotation(org.tbox.redis.ratelimit.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Method targetMethod = AopUtils.getMostSpecificMethod(method, joinPoint.getTarget().getClass());
        RateLimit rateLimit = AnnotationUtils.findAnnotation(targetMethod, RateLimit.class);
        if (rateLimit == null) {
            return joinPoint.proceed();
        }

        String resolvedKey = resolveKey(rateLimit, targetMethod, joinPoint.getTarget(), joinPoint.getArgs());
        RateLimitRequest request = new RateLimitRequest(
                rateLimit.mode(),
                resolvedKey,
                rateLimit.maxRequests(),
                rateLimit.windowSeconds(),
                rateLimit.permits(),
                rateLimit.rate(),
                rateLimit.interval(),
                rateLimit.intervalUnit()
        );

        if (!rateLimitChecker.allow(request)) {
            RequestLogInfo requestLogInfo = currentRequestLogInfo();
            if (log.isWarnEnabled()) {
                log.warn(
                        "Rate limit blocked: httpMethod={}, uri={}, key={}, mode={}, rule={}, method={}",
                        requestLogInfo.httpMethod,
                        requestLogInfo.uri,
                        request.getKey(),
                        request.getMode(),
                        formatRule(request, rateLimit),
                        targetMethod.toGenericString()
                );
            }

            String message = rateLimit.message();
            if (message == null || message.trim().isEmpty()) {
                message = StandardErrorCodeEnum.RATE_LIMIT_ERROR.getMessage();
            }
            throw new BizException(StandardErrorCodeEnum.RATE_LIMIT_ERROR.getCode(), message);
        }

        return joinPoint.proceed();
    }

    private String resolveKey(RateLimit rateLimit, Method method, Object target, Object[] args) {
        String key = rateLimit.key();
        if (key == null) {
            key = "";
        }

        String computed;
        if (key.trim().isEmpty()) {
            computed = currentRequestUrlOrFallback(method);
        } else if (key.contains("#")) {
            computed = evalSpel(key, method, target, args);
        } else {
            computed = key;
        }

        String prefix = rateLimit.prefix();
        if (prefix == null) {
            prefix = "";
        }
        return prefix + computed;
    }

    private String currentRequestUrlOrFallback(Method method) {
        try {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes) {
                HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
                if (request != null) {
                    String uri = request.getRequestURI();
                    if (uri != null && !uri.trim().isEmpty()) {
                        return uri;
                    }
                    StringBuffer urlBuf = request.getRequestURL();
                    String url = urlBuf == null ? null : urlBuf.toString();
                    if (url != null && !url.trim().isEmpty()) {
                        return url;
                    }
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        return method.getDeclaringClass().getName() + "#" + method.getName();
    }

    private String evalSpel(String expressionText, Method method, Object target, Object[] args) {
        StandardEvaluationContext context = new StandardEvaluationContext(target);
        context.setVariable("method", method);
        context.setVariable("target", target);

        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                context.setVariable("p" + i, args[i]);
                context.setVariable("a" + i, args[i]);
            }
        }

        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        if (parameterNames != null && args != null) {
            for (int i = 0; i < Math.min(parameterNames.length, args.length); i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        Expression expression = expressionParser.parseExpression(expressionText);
        Object value = expression.getValue(context);
        return String.valueOf(value);
    }

    private static String formatRule(RateLimitRequest request, RateLimit rateLimit) {
        if (request.getMode() == RateLimitMode.SLIDING_WINDOW) {
            return "slidingWindow(maxRequests=" + rateLimit.maxRequests() + ", windowSeconds=" + rateLimit.windowSeconds() + ")";
        }
        return "tokenBucket(permits=" + rateLimit.permits()
                + ", rate=" + rateLimit.rate()
                + ", interval=" + rateLimit.interval()
                + ", unit=" + rateLimit.intervalUnit()
                + ")";
    }

    private static RequestLogInfo currentRequestLogInfo() {
        try {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes) {
                HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
                if (request != null) {
                    return new RequestLogInfo(request.getMethod(), request.getRequestURI());
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        return new RequestLogInfo("-", "-");
    }

    private static final class RequestLogInfo {
        private final String httpMethod;
        private final String uri;

        private RequestLogInfo(String httpMethod, String uri) {
            this.httpMethod = httpMethod == null ? "-" : httpMethod;
            this.uri = uri == null ? "-" : uri;
        }
    }
}

