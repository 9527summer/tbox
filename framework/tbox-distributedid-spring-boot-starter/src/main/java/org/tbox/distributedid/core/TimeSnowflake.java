package org.tbox.distributedid.core;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.LongSupplier;

/**
 * 基于时间戳的雪花算法
 * <p>
 * ID格式：yyMMddHHmmssSSS(15位) + NodeId(2位) + Sequence(2位) = 19位
 * 容量：100节点 × 100序列/毫秒 = 每毫秒1万个ID
 * <p>
 * 注意：该实现是“十进制拼接成 long”，会受 {@link Long#MAX_VALUE} 约束。
 * 在默认参数（nodeId 两位、sequence 两位）下，时间前缀最多到 92xxxx...，
 * 即在不发生 long 溢出的前提下，通常可用到 2092-12-31 23:59:59.999（受时区影响）。
 */
public class TimeSnowflake extends AbstractSnowflake implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final long MAX_NODE_ID = 99L;
    private static final long MAX_SEQUENCE = 99L;
    private static final long TIMESTAMP_MULTIPLIER = 10000L;
    private static final long NODE_ID_MULTIPLIER = 100L;
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();
    private static final long MAX_TIME_PREFIX = (Long.MAX_VALUE - (MAX_NODE_ID * NODE_ID_MULTIPLIER + MAX_SEQUENCE)) / TIMESTAMP_MULTIPLIER;

    private final long nodeId;
    private final LongSupplier timeMillisSupplier;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    // 缓存时间前缀（性能优化）
    private long cachedTimestamp = -1L;
    private long cachedTimePrefix = -1L;

    public TimeSnowflake(long nodeId) {
        this(nodeId, System::currentTimeMillis);
    }

    TimeSnowflake(long nodeId, LongSupplier timeMillisSupplier) {
        if (nodeId > MAX_NODE_ID || nodeId < 0) {
            throw new IllegalArgumentException("NodeId must be between 0 and " + MAX_NODE_ID);
        }
        if (timeMillisSupplier == null) {
            throw new IllegalArgumentException("timeMillisSupplier must not be null");
        }
        this.nodeId = nodeId;
        this.timeMillisSupplier = timeMillisSupplier;
    }

    @Override
    public synchronized long nextId() {
        long timestamp = currentTimeMillis();

        // 时钟回拨检查（与 Snowflake 保持一致）
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset < 2000) {
                timestamp = lastTimestamp;
            } else {
                throw new IllegalStateException("Clock moved backwards. Refusing to generate id for " + offset + "ms");
            }
        }

        if (timestamp == lastTimestamp) {
            sequence++;
            if (sequence > MAX_SEQUENCE) {
                timestamp = tilNextMillis(lastTimestamp);
                sequence = 0L;
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // 缓存优化：只有时间戳变化时才重新计算
        long timePrefix;
        if (timestamp == cachedTimestamp) {
            timePrefix = cachedTimePrefix;
        } else {
            timePrefix = formatTimestamp(timestamp);
            if (timePrefix > MAX_TIME_PREFIX) {
                throw new IllegalStateException("TimeSnowflake overflow: timePrefix=" + timePrefix + ", max=" + MAX_TIME_PREFIX);
            }
            cachedTimestamp = timestamp;
            cachedTimePrefix = timePrefix;
        }

        return (timePrefix * TIMESTAMP_MULTIPLIER) + (nodeId * NODE_ID_MULTIPLIER) + sequence;
    }

    /**
     * 时间格式化：yyMMddHHmmssSSS -> long
     * <p>
     * 说明：这里优先保证正确性和可维护性；并通过“按毫秒缓存”避免重复计算。
     */
    private long formatTimestamp(long timestamp) {
        LocalDateTime ldt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZONE_ID
        );

        long yy = ldt.getYear() % 100;
        long result = yy;
        result = result * 100 + ldt.getMonthValue();
        result = result * 100 + ldt.getDayOfMonth();
        result = result * 100 + ldt.getHour();
        result = result * 100 + ldt.getMinute();
        result = result * 100 + ldt.getSecond();
        result = result * 1000 + (timestamp % 1000);

        return result;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp == lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException(
                    "Clock moved backwards. Refusing to generate id for " + (lastTimestamp - timestamp) + "ms");
        }
        return timestamp;
    }

    private long currentTimeMillis() {
        return timeMillisSupplier.getAsLong();
    }

    // 解析方法
    public static long parseNodeId(long id) {
        return (id % TIMESTAMP_MULTIPLIER) / NODE_ID_MULTIPLIER;
    }

    public static long parseSequence(long id) {
        return id % NODE_ID_MULTIPLIER;
    }

    public static String parseTimestamp(long id) {
        long timePrefix = id / TIMESTAMP_MULTIPLIER;
        return String.valueOf(timePrefix);
    }
}
