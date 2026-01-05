package org.tbox.redis.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.tbox.base.core.exception.BizException;
import org.tbox.redis.ratelimit.annotation.RateLimit;
import org.tbox.redis.ratelimit.aspect.RateLimitAspect;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitAspectTest {

    @Test
    void shouldBlockWhenCheckerDenies() {
        CapturingChecker checker = new CapturingChecker();
        TestService proxy = createProxy(checker);
        BizException ex = assertThrows(BizException.class, proxy::blocked);
        assertTrue(ex.getMessage().contains("too many"));
    }

    @Test
    void shouldResolveSpelKey() {
        CapturingChecker checker = new CapturingChecker();
        TestService proxy = createProxy(checker);
        proxy.ok("u1");
        assertEquals("rate:u1", checker.lastKey);
    }

    @Test
    void shouldDefaultToCurrentRequestUrlWhenKeyBlank() {
        CapturingChecker checker = new CapturingChecker();
        TestService proxy = createProxy(checker);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/demo");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            proxy.defaultKey();
            assertEquals("rate:/api/demo", checker.lastKey);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    private static TestService createProxy(CapturingChecker checker) {
        TestService target = new TestService();
        RateLimitAspect aspect = new RateLimitAspect(checker);
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        return factory.getProxy();
    }

    static class CapturingChecker implements RateLimitChecker {
        String lastKey;
        @Override
        public boolean allow(RateLimitRequest request) {
            this.lastKey = request.getKey();
            if (request.getKey().contains("block")) {
                return false;
            }
            return true;
        }
    }

    static class TestService {
        @RateLimit(key = "block", message = "too many")
        public void blocked() {}

        @RateLimit(key = "#p0", prefix = "rate:")
        public void ok(String userId) {}

        @RateLimit(prefix = "rate:")
        public void defaultKey() {}
    }
}
