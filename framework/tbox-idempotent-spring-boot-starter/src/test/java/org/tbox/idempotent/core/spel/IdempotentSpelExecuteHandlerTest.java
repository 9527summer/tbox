package org.tbox.idempotent.core.spel;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tbox.idempotent.config.IdempotentProperties;
import org.tbox.idempotent.core.IdempotentContext;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * IdempotentSpELExecuteHandler 单元测试
 */
class IdempotentSpelExecuteHandlerTest {

    private IdempotentSpELExecuteHandler handler;
    private IdempotentProperties properties;

    @BeforeEach
    void setUp() {
        properties = new IdempotentProperties();
        properties.setTimeout(5L);
        handler = new IdempotentSpELExecuteHandler(properties);
        IdempotentContext.clean();
    }

    @AfterEach
    void tearDown() {
        IdempotentContext.clean();
    }

    @Test
    void testParseSpelKey_SimpleParam() throws Exception {
        // 准备测试数据
        String orderId = "ORDER_123";
        Object[] args = new Object[]{orderId};

        // Mock JoinPoint
        ProceedingJoinPoint joinPoint = createMockJoinPoint("testMethod", new Class[]{String.class}, args);

        // 通过反射调用 parseSpelKey
        Method parseMethod = IdempotentSpELExecuteHandler.class.getDeclaredMethod(
                "parseSpelKey", String.class, ProceedingJoinPoint.class);
        parseMethod.setAccessible(true);

        String result = (String) parseMethod.invoke(handler, "#orderId", joinPoint);

        assertEquals(orderId, result);
    }

    @Test
    void testParseSpelKey_ObjectProperty() throws Exception {
        // 准备测试数据 - 使用内部类模拟请求对象
        TestRequest request = new TestRequest();
        request.setOrderId("ORDER_456");
        request.setUserId("USER_789");
        Object[] args = new Object[]{request};

        // Mock JoinPoint
        ProceedingJoinPoint joinPoint = createMockJoinPoint("testMethod", new Class[]{TestRequest.class}, args);

        // 通过反射调用 parseSpelKey
        Method parseMethod = IdempotentSpELExecuteHandler.class.getDeclaredMethod(
                "parseSpelKey", String.class, ProceedingJoinPoint.class);
        parseMethod.setAccessible(true);

        String result = (String) parseMethod.invoke(handler, "#request.orderId", joinPoint);

        assertEquals("ORDER_456", result);
    }

    @Test
    void testParseSpelKey_ConcatExpression() throws Exception {
        // 准备测试数据
        String userId = "USER_001";
        String productId = "PROD_002";
        Object[] args = new Object[]{userId, productId};

        // Mock JoinPoint
        ProceedingJoinPoint joinPoint = createMockJoinPoint("testMethod",
                new Class[]{String.class, String.class}, args);

        // 通过反射调用 parseSpelKey
        Method parseMethod = IdempotentSpELExecuteHandler.class.getDeclaredMethod(
                "parseSpelKey", String.class, ProceedingJoinPoint.class);
        parseMethod.setAccessible(true);

        String result = (String) parseMethod.invoke(handler, "#userId + ':' + #productId", joinPoint);

        assertEquals("USER_001:PROD_002", result);
    }

    @Test
    void testExceptionProcessing_NoLock_NoError() {
        // 没有设置锁时调用异常处理，不应抛出异常（不会触发Redis调用）
        assertDoesNotThrow(() -> handler.exceptionProcessing());
    }

    @Test
    void testPostProcessing_NoLock_NoError() {
        // 没有设置锁时调用后置处理，不应抛出异常（不会触发Redis调用）
        assertDoesNotThrow(() -> handler.postProcessing());
    }

    // ============ 辅助方法 ============

    /**
     * 创建 Mock 的 JoinPoint
     */
    private ProceedingJoinPoint createMockJoinPoint(String methodName, Class<?>[] paramTypes, Object[] args) throws Exception {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        Method method = TestService.class.getMethod(methodName, paramTypes);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(args);
        when(signature.getMethod()).thenReturn(method);

        return joinPoint;
    }

    // ============ 测试用辅助类 ============

    /**
     * 测试用服务类，用于获取方法签名
     */
    public static class TestService {
        public void testMethod(String orderId) {}
        public void testMethod(TestRequest request) {}
        public void testMethod(String userId, String productId) {}
    }

    /**
     * 测试用请求类
     */
    public static class TestRequest {
        private String orderId;
        private String userId;

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }
    }
}