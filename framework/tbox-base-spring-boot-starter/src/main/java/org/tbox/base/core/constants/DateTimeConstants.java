package org.tbox.base.core.constants;

/**
 * 日期时间格式常量
 */
public final class DateTimeConstants {

    private DateTimeConstants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }

    /**
     * 日期时间格式: yyyy-MM-dd HH:mm:ss
     */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 日期格式: yyyy-MM-dd
     */
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 时间格式: HH:mm:ss
     */
    public static final String TIME_PATTERN = "HH:mm:ss";
}