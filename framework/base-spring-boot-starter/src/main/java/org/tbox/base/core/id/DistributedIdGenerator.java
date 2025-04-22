package org.tbox.base.core.id;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Enumeration;
import java.util.UUID;

/**
 * 分布式ID生成器
 *
 * 基于雪花算法(Snowflake)的分布式ID生成器
 * 
 * 64位ID结构:
 * - 1位符号位，始终为0
 * - 41位时间戳(毫秒级)，支持69年
 * - 10位工作机器ID，支持1024个节点
 * - 12位序列号，支持每毫秒生成4096个ID
 * 
 * 特点:
 * - 高性能，无外部依赖，单实例每秒可生成约400万个ID
 * - 趋势递增，按时间递增排序，对分库分表友好
 * - 全局唯一，分布式环境下确保ID唯一性
 * - 自动适配物理机、Docker、k8s、ECS等多种部署环境
 */
public final class DistributedIdGenerator {
    private static final Logger log = LoggerFactory.getLogger(DistributedIdGenerator.class);

    // 开始时间戳 (2020-01-01 00:00:00)
    private static final long EPOCH = 1577808000000L;
    
    // 工作ID位数
    private static final long WORKER_ID_BITS = 10L;
    // 序列号位数
    private static final long SEQUENCE_BITS = 12L;
    
    // 工作ID最大值 (1023)
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    // 序列号掩码 (4095)
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
    
    // 工作ID左移位数 (12)
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    // 时间戳左移位数 (22)
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    
    // 工作机器ID
    private static final long workerId;
    // 序列号
    private static long sequence = 0L;
    // 上次生成ID的时间戳
    private static long lastTimestamp = -1L;
    
    static {
        workerId = generateWorkerId();
        log.info("DistributedIdGenerator initialized with workerId: {}", workerId);
    }
    
    // 私有构造函数，防止实例化
    private DistributedIdGenerator() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    /**
     * 检测当前是否在容器环境中运行
     * 
     * @return 是否在容器中运行
     */
    private static boolean isRunningInContainer() {
        // 检查是否在Docker中运行
        if (new File("/.dockerenv").exists()) {
            log.trace("Docker environment detected (/.dockerenv exists)");
            return true;
        }
        
        // 检查cgroup（适用于大多数容器环境）
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/1/cgroup"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("/docker") || line.contains("/kubepods") || line.contains("/ecs")) {
                    reader.close();
                    log.trace("Container environment detected in cgroups: {}", line);
                    return true;
                }
            }
            reader.close();
        } catch (IOException e) {
            // 忽略文件不存在异常，表示可能不在Linux环境
        }
        
        // 尝试检查容器特有的环境变量
        if (System.getenv("KUBERNETES_SERVICE_HOST") != null || 
            System.getenv("KUBERNETES_PORT") != null) {
            log.trace("Kubernetes environment detected via environment variables");
            return true;
        }
        
        if (System.getenv("ECS_CONTAINER_METADATA_URI") != null) {
            log.trace("ECS environment detected via environment variables");
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取容器ID或Pod名称
     * 
     * @return 容器唯一标识
     */
    private static String getContainerId() {
        // 尝试获取主机名（在K8s中通常是Pod名称）
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            if (hostname != null && !hostname.isEmpty()) {
                log.trace("Using hostname as container ID: {}", hostname);
                return hostname;
            }
        } catch (UnknownHostException e) {
            log.trace("Failed to get hostname", e);
        }
        
        // 尝试从Docker cgroup中提取容器ID
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/self/cgroup"));
            String line;
            while ((line = reader.readLine()) != null) {
                // Docker容器ID通常是64个字符，我们取其中的前12个即可区分
                if (line.contains("/docker")) {
                    String[] parts = line.split("/");
                    if (parts.length > 0) {
                        String lastPart = parts[parts.length - 1];
                        if (lastPart.length() >= 12) {
                            reader.close();
                            log.trace("Extracted Docker container ID: {}", lastPart);
                            return lastPart;
                        }
                    }
                }
                
                // Kubernetes Pod UID
                if (line.contains("/kubepods")) {
                    String[] parts = line.split("/");
                    for (String part : parts) {
                        if (part.startsWith("pod")) {
                            reader.close();
                            log.trace("Extracted Kubernetes pod ID: {}", part);
                            return part;
                        }
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            log.trace("Failed to read cgroup information", e);
        }
        
        // 使用IP地址作为备选
        try {
            InetAddress localAddress = InetAddress.getLocalHost();
            String ipAddress = localAddress.getHostAddress();
            if (ipAddress != null && !ipAddress.isEmpty()) {
                log.trace("Using IP address as container ID: {}", ipAddress);
                return ipAddress;
            }
        } catch (UnknownHostException e) {
            log.trace("Failed to get local IP address", e);
        }
        
        // 生成一个随机UUID作为最后的备选
        String uuid = UUID.randomUUID().toString();
        log.debug("Using random UUID as container ID: {}", uuid);
        return uuid;
    }
    
    /**
     * 生成工作机器ID
     * 自动适配不同环境:
     * 1. 容器环境：使用容器ID/Pod名称/主机名/IP地址
     * 2. 物理机环境：使用进程ID+MAC地址组合
     * 3. 其他环境：使用随机数
     * 
     * @return 工作机器ID (0-1023)
     */
    private static long generateWorkerId() {
        try {
            // 检查是否在容器环境中运行
            if (isRunningInContainer()) {
                // 获取容器唯一标识
                String containerId = getContainerId();
                
                // 计算容器ID的哈希值
                int containerHash = containerId.hashCode();
                
                // 确保在有效范围内
                long id = Math.abs(containerHash % (MAX_WORKER_ID + 1));
                log.debug("Container environment detected, workerId generated from container ID: {}", id);
                return id;
            }
            
            // 非容器环境，使用进程ID+MAC地址
            String processName = ManagementFactory.getRuntimeMXBean().getName();
            int pid = Integer.parseInt(processName.split("@")[0]);
            log.trace("Current process ID: {}", pid);
            
            // 尝试获取MAC地址
            byte[] mac = null;
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                if (network.isLoopback() || !network.isUp()) {
                    continue;
                }
                
                byte[] addr = network.getHardwareAddress();
                if (addr != null && addr.length > 0) {
                    mac = addr;
                    break;
                }
            }
            
            if (mac != null) {
                // 计算MAC地址的哈希值，与进程ID结合生成workerId
                int macHash = 0;
                for (byte b : mac) {
                    macHash = 31 * macHash + (b & 0xFF);
                }
                
                // 结合进程ID和MAC地址哈希值生成workerId
                long id = Math.abs((pid ^ macHash) % (MAX_WORKER_ID + 1));
                log.debug("Physical machine environment, workerId generated from PID and MAC: {}", id);
                return id;
            } else {
                // 无法获取MAC地址，使用进程ID直接计算
                long id = pid % (MAX_WORKER_ID + 1);
                log.debug("No MAC address available, workerId generated from PID: {}", id);
                return id;
            }
        } catch (Exception e) {
            log.warn("Error generating workerId, falling back to random number", e);
            
            // 出现异常时，使用随机数作为最后的备选
            SecureRandom random = new SecureRandom();
            // 使用种子保证同一应用重启后workerId保持一致（同时避免不同应用间的冲突）
            try {
                String hostname = InetAddress.getLocalHost().getHostName();
                random.setSeed(hostname.hashCode());
            } catch (Exception ex) {
                // 无法获取主机名，使用当前时间作为种子
            }
            
            long id = Math.abs(random.nextLong() % (MAX_WORKER_ID + 1));
            log.warn("Using random workerId: {}", id);
            return id;
        }
    }
    
    /**
     * 生成下一个分布式ID
     * 
     * @return 全局唯一的Long型ID
     */
    public static synchronized long nextId() {
        long timestamp = timeGen();
        
        // 检查是否出现时钟回拨（例如NTP调整）
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                // 如果回拨时间很短（<=5ms），等待一下
                try {
                    Thread.sleep(offset);
                    timestamp = timeGen();
                    if (timestamp < lastTimestamp) {
                        // 仍然存在回拨，报警等待
                        timestamp = waitNextMillis(lastTimestamp);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted while waiting for clock", e);
                }
            } else {
                // 时钟回拨超过容忍度，记录警告并等待
                log.warn("Clock moved backwards. Refusing to generate ID for {}ms", offset);
                timestamp = waitNextMillis(lastTimestamp);
            }
        }
        
        // 同一毫秒内，递增序列号
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            // 同一毫秒内序列号用尽，等待下一毫秒
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒，重置序列号
            sequence = 0L;
        }
        
        // 记录最后一次ID生成的时间戳
        lastTimestamp = timestamp;
        
        // 组合ID (时间戳 + 工作ID + 序列号)
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT) | (workerId << WORKER_ID_SHIFT) | sequence;
    }
    
    /**
     * 生成分布式ID的字符串表示
     * 
     * @return 16位字符串表示的ID
     */
    public static String nextIdString() {
        return Long.toHexString(nextId());
    }
    
    /**
     * 等待直到下一毫秒
     * 
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 新的时间戳
     */
    private static long waitNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }
    
    /**
     * 获取当前时间戳(毫秒)
     * 
     * @return 当前时间戳
     */
    private static long timeGen() {
        return Instant.now().toEpochMilli();
    }
    
    /**
     * 从ID中解析时间戳
     * 
     * @param id 分布式ID
     * @return 生成ID时的时间戳(毫秒)
     */
    public static long getTimestampFromId(long id) {
        return (id >> TIMESTAMP_SHIFT) + EPOCH;
    }
    
    /**
     * 获取当前工作机器ID
     * 
     * @return 当前工作机器ID
     */
    public static long getWorkerId() {
        return workerId;
    }
} 