# tbox-distributedid-spring-boot-starter

分布式 ID 与兑换码生成 Starter，提供 3 类能力：

- **Snowflake ID（long）**：高吞吐、全局唯一、趋势递增
- **时间型 ID（long）**：形如 `yyMMddHHmmssSSS + nodeId(2位) + seq(2位)`，便于人工阅读/排查
- **兑换编号（String）**：基于 Snowflake long 的 Base62（`0-9a-zA-Z`）短码

## 快速使用

### 1) 引入依赖

```xml
<dependency>
  <groupId>io.github.9527summer</groupId>
  <artifactId>tbox-distributedid-spring-boot-starter</artifactId>
  <version>${tbox.version}</version>
</dependency>
```

### 2) 生成 ID

```java
// 默认 Snowflake（long）
long id = org.tbox.distributedid.utils.IdUtils.nextId();
String idStr = org.tbox.distributedid.utils.IdUtils.nextIdStr();

// 时间型 ID（long）
long timeId = org.tbox.distributedid.utils.IdUtils.nextTimeId();
String timeIdStr = org.tbox.distributedid.utils.IdUtils.nextTimeIdStr();

// 兑换编号（String，Base62）
String redeemCode = org.tbox.distributedid.utils.RedeemCodeUtils.nextRedeemCode();
```

### 3) 批量生成兑换编号

```java
List<String> codes = org.tbox.distributedid.utils.RedeemCodeUtils.nextRedeemCodes(1000);
```

## Snowflake 规格（以代码为准）

实现见 `org.tbox.distributedid.core.Snowflake`：

- **结构**：`timestamp(41 bits) + nodeId(10 bits) + sequence(12 bits)`
- **节点数**：`nodeId` 范围 `0~1023`（共 1024 个节点）
- **单节点吞吐上限（理论）**：`sequence` 每毫秒最多 `4096`（约 `4,096,000`/秒）
- **使用年限**：起始纪元 `twepoch=2010-11-04 01:42:54.657 UTC`，41 bit 毫秒时间约 69.68 年，
  理论上可用到 **`2080-07-10 17:30:30.208 UTC`**（超过后时间位溢出将导致 ID 冲突风险）
- **时钟回拨策略**：回拨 `< 2000ms` 时容忍（时间戳会“钉住”到 `lastTimestamp`），回拨 `>= 2000ms` 直接抛异常
- **并发策略**：`nextId()` 使用 `synchronized` 保证线程安全（单实例并发会串行）；适用于对 QPS 不极端的场景

> 说明：实际 QPS 受 CPU/锁竞争/JVM 等影响，以本项目 `SnowflakePerformanceTest` 输出为参考。

## 时间型 ID 规格（以代码为准）

实现见 `org.tbox.distributedid.core.TimeSnowflake`：

- **结构**：`yyMMddHHmmssSSS(15位) + nodeId(2位) + sequence(2位)`，拼成一个 `long`
- **节点数**：`nodeId` 范围 `0~99`（共 100 个节点）
- **单节点吞吐上限（理论）**：每毫秒最多 `100`（约 `100,000`/秒/节点）
- **集群吞吐上限（理论）**：100 节点合计约 `10,000`/毫秒（约 `10,000,000`/秒）
- **数值长度**：当年份为 `2010~2099` 时，数值长度可保持为 19 位；如果年份为 `2000~2009`，最左侧 `yy` 会导致数值长度变短（数值型 long 无法保留前导 0）
- **时钟回拨策略**：与 `Snowflake` 保持一致（回拨 `<2000ms` 容忍，否则抛异常）
- **并发策略**：`nextId()` 使用 `synchronized`，吞吐一般低于纯 Snowflake（更适合“破千 QPS 就很好”的业务）

## nodeId 分配方式

该 Starter 会同时初始化“默认 Snowflake”和“时间型 TimeSnowflake”两套生成器：

### Redis 模式（推荐集群）

当 classpath 存在 `RedisTemplate` 时：

- 默认 Snowflake：`org.tbox.distributedid.core.RedisIdGenerator`
  - 申请 key：`tbox:ids:registry:${spring.application.name}`
- 时间型：`org.tbox.distributedid.core.TimeRedisIdGenerator`
  - 申请 key：`tbox:ids:registry-time:${spring.application.name}`

分配逻辑：

- Redis ZSET 记录 nodeId 的“租约到期时间”（score=now+24h）
- 心跳续租：每 30s `ZADD` 更新 score
- 关闭时释放：`destroy()` 会 `ZREM` 移除当前 nodeId
- Lua：`src/main/resources/lua/chooseWorkIdLua.lua` 会从 `0..maxId` 线性扫描找可用 slot（节点上限小，通常可接受）

### 单机/开发模式（不保证集群唯一）

当没有 Redis 时：

- 默认 Snowflake：`org.tbox.distributedid.core.RandomIdGenerator` 使用 `Math.random()` 随机 `0~1023`
- 时间型：`org.tbox.distributedid.core.TimeRandomIdGenerator` 使用 `Math.random()` 随机 `0~99`

**注意**：随机 nodeId 不具备集群唯一性保障，仅适用于单机模式。

## 兑换编号（Redeem Code）

实现见：

- `org.tbox.distributedid.utils.RedeemCodeUtils`
- `org.tbox.distributedid.utils.Base62`

特性：

- 基于 Snowflake `long` 进行 Base62 编码（字符集 `0-9a-zA-Z`）
- 字符串长度通常不超过 **11**（因为 `2^64` 的 Base62 表示最多 11 位）
