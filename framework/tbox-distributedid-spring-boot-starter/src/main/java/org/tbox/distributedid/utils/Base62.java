package org.tbox.distributedid.utils;

/**
 * Base62 编解码工具（0-9a-zA-Z）。
 * <p>
 * 适用于将 long（包含无符号语义）编码成更短的字符串表示。
 */
public final class Base62 {

    private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int BASE = ALPHABET.length;

    private Base62() {
    }

    /**
     * 将 long 按无符号语义编码为 Base62。
     */
    public static String encodeUnsigned(long value) {
        if (value == 0L) {
            return "0";
        }

        char[] buf = new char[11];
        int charPos = buf.length;
        long v = value;

        while (Long.compareUnsigned(v, 0L) > 0) {
            long q = Long.divideUnsigned(v, BASE);
            int r = (int) Long.remainderUnsigned(v, BASE);
            buf[--charPos] = ALPHABET[r];
            v = q;
        }

        return new String(buf, charPos, buf.length - charPos);
    }

    /**
     * 将 Base62 解码为 long（按无符号语义解析，返回的 long 可能为负数）。
     */
    public static long decodeUnsigned(String base62) {
        if (base62 == null || base62.isEmpty()) {
            throw new IllegalArgumentException("base62 must not be empty");
        }

        long result = 0L;
        for (int i = 0; i < base62.length(); i++) {
            int digit = indexOf(base62.charAt(i));
            if (digit < 0) {
                throw new IllegalArgumentException("Invalid base62 char: " + base62.charAt(i));
            }
            // 使用 long 溢出语义（mod 2^64）来实现无符号累加
            result = result * BASE + digit;
        }
        return result;
    }

    private static int indexOf(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'z') return 10 + (c - 'a');
        if (c >= 'A' && c <= 'Z') return 36 + (c - 'A');
        return -1;
    }
}
