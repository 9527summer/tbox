package org.tbox.distributedid.utils;

import org.tbox.distributedid.core.AbstractSnowflake;

import java.util.ArrayList;
import java.util.List;

/**
 * 兑换码/兑换编号生成工具。
 * <p>
 * 特点：
 * <ul>
 *   <li>字符集：0-9a-zA-Z（大小写字母 + 数字）</li>
 *   <li>长度：基于 Snowflake long 的 Base62，通常不超过 11 位</li>
 * </ul>
 */
public final class RedeemCodeUtils {

    private RedeemCodeUtils() {
    }

    /**
     * 基于框架默认 ID（Snowflake）生成兑换码。
     * <p>
     * 注意：如果未正确初始化分布式ID生成器，可能会抛出空指针异常。
     */
    public static String nextRedeemCode() {
        return Base62.encodeUnsigned(IdUtils.nextId());
    }

    /**
     * 使用指定的雪花算法实例生成兑换码（便于单测/脱离 Spring 使用）。
     */
    public static String nextRedeemCode(AbstractSnowflake snowflake) {
        if (snowflake == null) {
            throw new IllegalArgumentException("snowflake must not be null");
        }
        return Base62.encodeUnsigned(snowflake.nextId());
    }

    /**
     * 批量生成兑换码（基于框架默认 ID / Snowflake）。
     */
    public static List<String> nextRedeemCodes(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
        List<String> codes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            codes.add(Base62.encodeUnsigned(IdUtils.nextId()));
        }
        return codes;
    }

    /**
     * 批量生成兑换码（使用指定雪花实例）。
     */
    public static List<String> nextRedeemCodes(AbstractSnowflake snowflake, int count) {
        if (snowflake == null) {
            throw new IllegalArgumentException("snowflake must not be null");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
        List<String> codes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            codes.add(Base62.encodeUnsigned(snowflake.nextId()));
        }
        return codes;
    }

    /**
     * 将一个 long（如 Snowflake ID）转换为兑换码。
     */
    public static String fromId(long id) {
        return Base62.encodeUnsigned(id);
    }
}
