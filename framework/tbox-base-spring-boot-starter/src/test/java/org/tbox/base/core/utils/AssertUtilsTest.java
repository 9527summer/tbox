package org.tbox.base.core.utils;

import org.junit.jupiter.api.Test;
import org.tbox.base.core.exception.BizException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AssertUtils 单元测试
 */
class AssertUtilsTest {

    // ============ isTrue 测试 ============

    @Test
    void testIsTrue_WhenTrue_NoException() {
        assertDoesNotThrow(() -> AssertUtils.isTrue(true));
        assertDoesNotThrow(() -> AssertUtils.isTrue(true, "error message"));
        assertDoesNotThrow(() -> AssertUtils.isTrue(true, "ERROR_CODE", "error message"));
    }

    @Test
    void testIsTrue_WhenFalse_ThrowsException() {
        BizException ex = assertThrows(BizException.class, () -> AssertUtils.isTrue(false));
        assertTrue(ex.getMessage().contains("Must be true"));
    }

    @Test
    void testIsTrue_WhenFalse_WithMessage() {
        String message = "自定义错误信息";
        BizException ex = assertThrows(BizException.class, () -> AssertUtils.isTrue(false, message));
        assertEquals(message, ex.getMessage());
    }

    @Test
    void testIsTrue_WhenFalse_WithCodeAndMessage() {
        String code = "CUSTOM_ERROR";
        String message = "带错误码的信息";
        BizException ex = assertThrows(BizException.class, () -> AssertUtils.isTrue(false, code, message));
        assertEquals(code, ex.getErrCode());
        assertEquals(message, ex.getMessage());
    }

    // ============ isFalse 测试 ============

    @Test
    void testIsFalse_WhenFalse_NoException() {
        assertDoesNotThrow(() -> AssertUtils.isFalse(false));
        assertDoesNotThrow(() -> AssertUtils.isFalse(false, "error message"));
        assertDoesNotThrow(() -> AssertUtils.isFalse(false, "ERROR_CODE", "error message"));
    }

    @Test
    void testIsFalse_WhenTrue_ThrowsException() {
        BizException ex = assertThrows(BizException.class, () -> AssertUtils.isFalse(true));
        assertTrue(ex.getMessage().contains("Must be false"));
    }

    @Test
    void testIsFalse_WhenTrue_WithMessage() {
        String message = "不应该为真";
        BizException ex = assertThrows(BizException.class, () -> AssertUtils.isFalse(true, message));
        assertEquals(message, ex.getMessage());
    }

    // ============ notNull 测试 ============

    @Test
    void testNotNull_WhenNotNull_NoException() {
        assertDoesNotThrow(() -> AssertUtils.notNull("string"));
        assertDoesNotThrow(() -> AssertUtils.notNull(new Object()));
        assertDoesNotThrow(() -> AssertUtils.notNull(123));
        assertDoesNotThrow(() -> AssertUtils.notNull("value", "error message"));
        assertDoesNotThrow(() -> AssertUtils.notNull("value", "ERROR_CODE", "error message"));
    }

    @Test
    void testNotNull_WhenNull_ThrowsException() {
        BizException ex = assertThrows(BizException.class, () -> AssertUtils.notNull(null));
        assertTrue(ex.getMessage().contains("Must not null"));
    }

    @Test
    void testNotNull_WhenNull_WithMessage() {
        String message = "对象不能为空";
        BizException ex = assertThrows(BizException.class, () -> AssertUtils.notNull(null, message));
        assertEquals(message, ex.getMessage());
    }

    @Test
    void testNotNull_WhenNull_WithCodeAndMessage() {
        String code = "NULL_ERROR";
        String message = "参数为空";
        BizException ex = assertThrows(BizException.class, () -> AssertUtils.notNull(null, code, message));
        assertEquals(code, ex.getErrCode());
        assertEquals(message, ex.getMessage());
    }

    // ============ notEmpty (CharSequence) 测试 ============

    @Test
    void testNotEmpty_CharSequence_WhenNotEmpty_NoException() {
        assertDoesNotThrow(() -> AssertUtils.notEmpty("hello"));
        assertDoesNotThrow(() -> AssertUtils.notEmpty("  ")); // 空格不算空
        assertDoesNotThrow(() -> AssertUtils.notEmpty("test", "error message"));
        assertDoesNotThrow(() -> AssertUtils.notEmpty("test", "ERROR_CODE", "error message"));
    }

    @Test
    void testNotEmpty_CharSequence_WhenNull_ThrowsException() {
        String nullString = null;
        assertThrows(BizException.class, () -> AssertUtils.notEmpty(nullString));
    }

    @Test
    void testNotEmpty_CharSequence_WhenEmpty_ThrowsException() {
        assertThrows(BizException.class, () -> AssertUtils.notEmpty(""));
    }

    @Test
    void testNotEmpty_CharSequence_WithMessage() {
        String message = "字符串不能为空";
        BizException ex = assertThrows(BizException.class, () -> AssertUtils.notEmpty("", message));
        assertEquals(message, ex.getMessage());
    }

    // ============ notEmpty (Array) 测试 ============

    @Test
    void testNotEmpty_Array_WhenNotEmpty_NoException() {
        assertDoesNotThrow(() -> AssertUtils.notEmpty(new Object[]{"a", "b"}));
        assertDoesNotThrow(() -> AssertUtils.notEmpty(new Integer[]{1, 2, 3}));
    }

    @Test
    void testNotEmpty_Array_WhenNull_ThrowsException() {
        Object[] nullArray = null;
        assertThrows(BizException.class, () -> AssertUtils.notEmpty(nullArray));
    }

    @Test
    void testNotEmpty_Array_WhenEmpty_ThrowsException() {
        assertThrows(BizException.class, () -> AssertUtils.notEmpty(new Object[]{}));
    }

    @Test
    void testNotEmpty_Array_WithMessage() {
        String message = "数组不能为空";
        BizException ex = assertThrows(BizException.class, () -> AssertUtils.notEmpty(new Object[]{}, message));
        assertEquals(message, ex.getMessage());
    }

    // ============ notEmpty (Collection) 测试 ============

    @Test
    void testNotEmpty_Collection_WhenNotEmpty_NoException() {
        assertDoesNotThrow(() -> AssertUtils.notEmpty(Arrays.asList("a", "b")));
        assertDoesNotThrow(() -> AssertUtils.notEmpty(Collections.singleton("single")));
    }

    @Test
    void testNotEmpty_Collection_WhenNull_ThrowsException() {
        Collection<?> nullCollection = null;
        assertThrows(BizException.class, () -> AssertUtils.notEmpty(nullCollection));
    }

    @Test
    void testNotEmpty_Collection_WhenEmpty_ThrowsException() {
        assertThrows(BizException.class, () -> AssertUtils.notEmpty(Collections.emptyList()));
        assertThrows(BizException.class, () -> AssertUtils.notEmpty(new ArrayList<>()));
    }

    @Test
    void testNotEmpty_Collection_WithMessage() {
        String message = "集合不能为空";
        BizException ex = assertThrows(BizException.class, () -> AssertUtils.notEmpty(new ArrayList<>(), message));
        assertEquals(message, ex.getMessage());
    }

    // ============ notEmpty (Map) 测试 ============

    @Test
    void testNotEmpty_Map_WhenNotEmpty_NoException() {
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        assertDoesNotThrow(() -> AssertUtils.notEmpty(map));
    }

    @Test
    void testNotEmpty_Map_WhenNull_ThrowsException() {
        Map<?, ?> nullMap = null;
        assertThrows(BizException.class, () -> AssertUtils.notEmpty(nullMap));
    }

    @Test
    void testNotEmpty_Map_WhenEmpty_ThrowsException() {
        assertThrows(BizException.class, () -> AssertUtils.notEmpty(Collections.emptyMap()));
        assertThrows(BizException.class, () -> AssertUtils.notEmpty(new HashMap<>()));
    }

    @Test
    void testNotEmpty_Map_WithMessage() {
        String message = "Map不能为空";
        BizException ex = assertThrows(BizException.class, () -> AssertUtils.notEmpty(new HashMap<>(), message));
        assertEquals(message, ex.getMessage());
    }

    @Test
    void testNotEmpty_Map_WithCodeAndMessage() {
        String code = "MAP_EMPTY";
        String message = "Map为空错误";
        BizException ex = assertThrows(BizException.class, () -> AssertUtils.notEmpty(new HashMap<>(), code, message));
        assertEquals(code, ex.getErrCode());
        assertEquals(message, ex.getMessage());
    }

    // ============ 边界情况测试 ============

    @Test
    void testComplexConditions() {
        // 测试复杂条件
        int value = 10;
        assertDoesNotThrow(() -> AssertUtils.isTrue(value > 5 && value < 20, "VALUE_RANGE", "值必须在5-20之间"));
        assertThrows(BizException.class, () -> AssertUtils.isTrue(value > 100, "VALUE_RANGE", "值必须大于100"));
    }
}