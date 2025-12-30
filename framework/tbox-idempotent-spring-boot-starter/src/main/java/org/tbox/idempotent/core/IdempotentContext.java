
package org.tbox.idempotent.core;



import java.util.HashMap;
import java.util.Map;

/**
 * 幂等上下文
 */
public final class IdempotentContext {

    private static final ThreadLocal<Map<String, Object>> CONTEXT = new ThreadLocal<>();

    public static Map<String, Object> get() {
        return CONTEXT.get();
    }

    public static Object getKey(String key) {
        if (key == null) {
            return null;
        }
        Map<String, Object> context = get();
        if (context != null && !context.isEmpty()) {
            return context.get(key);
        }
        return null;
    }

    public static String getString(String key) {
        Object actual = getKey(key);
        if (actual != null) {
            return actual.toString();
        }
        return null;
    }

    public static void put(String key, Object val) {
        if (key == null) {
            return;
        }
        Map<String, Object> context = get();
        if (context == null) {
            context = new HashMap<>();
        }
        context.put(key, val);
        putContext(context);
    }

    public static void putContext(Map<String, Object> context) {
        if (context == null) {
            return;
        }
        Map<String, Object> threadContext = CONTEXT.get();
        if (threadContext != null && !threadContext.isEmpty()) {
            threadContext.putAll(context);
            return;
        }
        CONTEXT.set(context);
    }

    public static void clean() {
        CONTEXT.remove();
    }
}
