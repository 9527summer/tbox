package org.tbox.base.core.utils;

import org.tbox.base.core.exception.BizException;

import java.util.Collection;
import java.util.Map;

public class AssertUtils {
    public static void isTrue(boolean expression, String errorCode, String errMessage) {
        if (!expression) {
            throw new BizException(errorCode, errMessage);
        }
    }

    public static void isFalse(boolean expression, String errorCode, String errMessage) {
        if (expression) {
            throw new BizException(errorCode, errMessage);
        }
    }

    public static void isTrue(boolean expression, String errMessage) {
        if (!expression) {
            throw new BizException(errMessage);
        }
    }

    public static void isFalse(boolean expression, String errMessage) {
        if (expression) {
            throw new BizException(errMessage);
        }
    }

    public static void isTrue(boolean expression) {
        isTrue(expression, "[Assertion failed] Must be true");
    }

    public static void isFalse(boolean expression) {
        isFalse(expression, "[Assertion failed] Must be false");
    }

    public static void notNull(Object object, String errorCode, String errMessage) {
        if (object == null) {
            throw new BizException(errorCode, errMessage);
        }
    }

    public static void notNull(Object object, String errMessage) {
        if (object == null) {
            throw new BizException(errMessage);
        }
    }

    public static void notNull(Object object) {
        notNull(object, "[Assertion failed] Must not null");
    }

    public static void notEmpty(CharSequence charSequence, String errorCode, String errMessage) {
        if (charSequence == null || charSequence.length() == 0) {
            throw new BizException(errorCode, errMessage);
        }
    }

    public static void notEmpty(CharSequence charSequence, String errMessage) {
        if (charSequence == null || charSequence.length() == 0) {
            throw new BizException(errMessage);
        }
    }

    public static void notEmpty(CharSequence charSequence) {
        notEmpty(charSequence, "[Assertion failed] CharSequence must not be empty");
    }

    public static void notEmpty(Object[] array, String errorCode, String errMessage) {
        if (array == null || array.length == 0) {
            throw new BizException(errorCode, errMessage);
        }
    }

    public static void notEmpty(Object[] array, String errMessage) {
        if (array == null || array.length == 0) {
            throw new BizException(errMessage);
        }
    }

    public static void notEmpty(Object[] array) {
        notEmpty(array, "[Assertion failed] CharSequence must not be empty");
    }

    public static void notEmpty(Collection<?> collection, String errorCode, String errMessage) {
        if (collection == null || collection.isEmpty()) {
            throw new BizException(errorCode, errMessage);
        }
    }

    public static void notEmpty(Collection<?> collection, String errMessage) {
        if (collection == null || collection.isEmpty()) {
            throw new BizException(errMessage);
        }
    }

    public static void notEmpty(Collection<?> collection) {
        notEmpty(collection, "[Assertion failed] Collection must not be empty: it must contain at least 1 element");
    }

    public static void notEmpty(Map<?, ?> map, String errorCode, String errMessage) {
        if (map == null || map.isEmpty()) {
            throw new BizException(errorCode, errMessage);
        }
    }

    public static void notEmpty(Map<?, ?> map, String errMessage) {
        if (map == null || map.isEmpty()) {
            throw new BizException(errMessage);
        }
    }

    public static void notEmpty(Map<?, ?> map) {
        notEmpty(map, "[Assertion failed] Map must not be empty: it must contain at least one entry");
    }
}
