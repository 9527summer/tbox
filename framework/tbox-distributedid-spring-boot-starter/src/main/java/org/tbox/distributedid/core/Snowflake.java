package org.tbox.distributedid.core;

import java.io.Serializable;
import java.util.Date;

/**
 * Twitter的Snowflake 算法<br>
 * 分布式系统中，有一些需要使用全局唯一ID的场景，有些时候我们希望能使用一种简单一些的ID，并且希望ID能够按照时间有序生成。
 *
 * <p>
 * snowflake的结构如下(每部分用-分开):<br>
 *
 * <pre>
 * 0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000
 * </pre>
 * <p>
 * 第一位为未使用(符号位表示正数)，接下来的41位为毫秒级时间(41位的长度可以使用69年)<br>
 * 然后是5位datacenterId和5位workerId(10位的长度最多支持部署1024个节点）<br>
 * 最后12位是毫秒内的计数（12位的计数顺序号支持每个节点每毫秒产生4096个ID序号）
 * <p>
 * 并且可以通过生成的id反推出生成时间,datacenterId和workerId
 * <p>
 * 说明：
 * <ul>
 *   <li>本实现使用 synchronized 保证线程安全，单实例并发会串行；适用于对 QPS 要求不极端的场景。</li>
 *   <li>时钟回拨在 2 秒内会进行容忍（时间戳被“钉住”到 lastTimestamp），超过 2 秒直接抛异常。</li>
 * </ul>
 * <p>
 * 参考：http://www.cnblogs.com/relucent/p/4955340.html
 *
 * @author Looly
 * @since 3.0.1
 */
public class Snowflake extends AbstractSnowflake implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long twepoch;
    private final long nodeIdBits = 10L;
    // 最大支持机器节点数0~1023
    private final long maxNodeId = -1L ^ (-1L << nodeIdBits);
    // 序列号12位
    private final long sequenceBits = 12L;
    // 机器节点左移12位
    private final long nodeIdShift = sequenceBits;
    // 时间毫秒数左移22位
    private final long timestampLeftShift = sequenceBits + nodeIdBits;
    // 序列掩码，用于限定序列最大值不能超过4095
    @SuppressWarnings("FieldCanBeLocal")
    private final long sequenceMask = ~(-1L << sequenceBits);// 4095

    private final long nodeId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    /**
     * 构造
     *
     * @param nodeId 节点ID (0~1023)
     */
    public Snowflake(long nodeId) {
        this(null, nodeId);
    }

    /**
     * @param epochDate 初始化时间起点（null表示默认起始日期）,后期修改会导致id重复,如果要修改连nodeId，慎用
     * @param nodeId    节点ID (0~1023)
     */
    public Snowflake(Date epochDate, long nodeId) {
        if (null != epochDate) {
            this.twepoch = epochDate.getTime();
        } else {
            // Thu, 04 Nov 2010 01:42:54 GMT
            this.twepoch = 1288834974657L;
        }
        if (nodeId > maxNodeId || nodeId < 0) {
            throw new IllegalArgumentException(String.format("node Id can't be greater than %d or less than 0", maxNodeId));
        }
        this.nodeId = nodeId;
    }

    /**
     * 根据Snowflake的ID，获取节点id
     *
     * @param id snowflake算法生成的id
     * @return 所属节点的id
     */
    public long getNodeId(long id) {
        return id >> nodeIdShift & ~(-1L << nodeIdBits);
    }

    /**
     * 根据Snowflake的ID，获取生成时间
     *
     * @param id snowflake算法生成的id
     * @return 生成的时间
     */
    public long getGenerateDateTime(long id) {
        return (id >> timestampLeftShift & ~(-1L << 41L)) + twepoch;
    }

    /**
     * 下一个ID
     *
     * @return ID
     */
    @Override
    public synchronized long nextId() {
        long timestamp = genTime();
        if (timestamp < this.lastTimestamp) {
            if (this.lastTimestamp - timestamp < 2000) {
                // 容忍2秒内的回拨，避免NTP校时造成的异常
                timestamp = lastTimestamp;
            } else {
                // 如果服务器时间有问题(时钟后退) 报错。
                throw new IllegalStateException(String.format("Clock moved backwards. Refusing to generate id for %dms", lastTimestamp - timestamp));
            }
        }

        if (timestamp == this.lastTimestamp) {
            final long sequence = (this.sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
            this.sequence = sequence;
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - twepoch) << timestampLeftShift) | (nodeId << nodeIdShift) | sequence;
    }



    // ------------------------------------------------------------------------------------------------------------------------------------ Private method start

    /**
     * 循环等待下一个时间
     *
     * @param lastTimestamp 上次记录的时间
     * @return 下一个时间
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = genTime();
        // 循环直到操作系统时间戳变化
        while (timestamp == lastTimestamp) {
            timestamp = genTime();
        }
        if (timestamp < lastTimestamp) {
            // 如果发现新的时间戳比上次记录的时间戳数值小，说明操作系统时间发生了倒退，报错
            throw new IllegalStateException(
                    String.format("Clock moved backwards. Refusing to generate id for %dms", lastTimestamp - timestamp));
        }
        return timestamp;
    }

    /**
     * 生成时间戳
     *
     * @return 时间戳
     */
    private long genTime() {
        return System.currentTimeMillis();
    }
}
