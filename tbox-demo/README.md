# TBox框架演示项目

这是一个基于TBox框架的演示项目，展示了TBox框架的核心功能，包括异常处理、响应封装、分页和幂等性。

## 功能特性

- **用户管理**：基本的CRUD操作，演示异常处理和统一响应
- **分页功能**：集成PageHelper，自动处理分页信息
- **幂等性注解**：演示基于Redis的幂等性实现
  - 参数幂等：基于请求参数的幂等实现
  - SpEL幂等：基于SpEL表达式的幂等实现
  - 分布式锁幂等：基于Redis分布式锁的幂等实现

## 环境要求

- Java 17+
- MySQL 5.7+
- Redis 5.0+
- Maven 3.5+

## 快速开始

1. 克隆项目
2. 配置MySQL数据库
   - 创建数据库：`CREATE DATABASE tbox_demo;`
   - 在`application.properties`中配置数据库连接信息
3. 配置Redis
   - 在`application.properties`中配置Redis连接信息
   - 确保Redis服务已启动
4. 构建并运行项目
   ```bash
   mvn clean package
   java -jar target/tbox-demo-0.0.1-SNAPSHOT.jar
   ```

## API测试

### 用户管理API

- 列出用户（分页）：`GET http://localhost:8080/api/users?pageNum=1&pageSize=5`
- 获取单个用户：`GET http://localhost:8080/api/users/1`
- 创建用户：`POST http://localhost:8080/api/users`
- 更新用户：`PUT http://localhost:8080/api/users/1`
- 删除用户：`DELETE http://localhost:8080/api/users/1`
- 触发异常：`GET http://localhost:8080/api/users/error`

### 幂等性API测试

#### 1. 基于参数的幂等测试

```bash
# 首次请求 - 成功
curl -X POST http://localhost:8080/api/orders/param \
  -H "Content-Type: application/json" \
  -d '{"userId":"user1","productId":"product1","quantity":1}'

# 重复请求 - 返回幂等性错误
curl -X POST http://localhost:8080/api/orders/param \
  -H "Content-Type: application/json" \
  -d '{"userId":"user1","productId":"product1","quantity":1}'
```

#### 2. 基于SpEL表达式的幂等测试

```bash
# 首次请求 - 成功
curl -X POST http://localhost:8080/api/orders/spel \
  -H "Content-Type: application/json" \
  -d '{"userId":"user1","productId":"product1","quantity":1}'

# 重复请求 - 返回幂等性错误
curl -X POST http://localhost:8080/api/orders/spel \
  -H "Content-Type: application/json" \
  -d '{"userId":"user1","productId":"product1","quantity":1}'
```

#### 3. 基于分布式锁的幂等测试

```bash
# 首次请求 - 成功
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{"orderId":"order1","amount":100.00,"paymentMethod":"CREDIT_CARD"}'

# 30秒内的重复请求 - 返回锁定错误
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{"orderId":"order1","amount":100.00,"paymentMethod":"CREDIT_CARD"}'
```

## 幂等性原理

TBox框架提供了三种幂等实现方式：

1. **参数幂等（PARAM）**：
   - 根据请求参数生成唯一键
   - 使用Redis存储唯一键，设置过期时间
   - 重复请求会触发幂等异常

2. **SpEL幂等（SPEL）**：
   - 使用SpEL表达式计算唯一键
   - 适用于需要自定义幂等键的场景
   - 更灵活，支持复杂表达式

3. **分布式锁幂等（LOCK）**：
   - 基于Redis的分布式锁实现
   - 保证同一时间只有一个请求能够执行
   - 适用于防止并发操作的场景

## 异常处理

TBox框架提供了统一的异常处理机制：

- **BizException**：业务异常，返回标准业务错误响应
- **SysException**：系统异常，返回标准系统错误响应
- **RepeatConsumptionException**：重复消费异常，由幂等机制触发

## Lua 脚本测试（分配 nodeId）

本项目集成了 `tbox-distributedid-spring-boot-starter` 的 Lua 脚本 `lua/chooseWorkIdLua.lua`，用于在 Redis ZSET 中分配可用的 `nodeId`（member 为 nodeId，score 为过期时间）。

### 方式一：HTTP 接口（推荐）

启动后调用：

- `GET /debug/lua/chooseWorkId`：执行一次 Lua，返回 nodeId/registryKey/expireAt 等
- `GET /debug/lua/chooseWorkId/batch?count=10`：连续执行多次
- `GET /debug/lua/registry?limit=50`：查看当前 ZSET 占用情况（member + score）
- `DELETE /debug/lua/registry?nodeId=123`：手动释放（ZREM），便于重复测试

### 方式二：模拟“频繁重启/更换 IP”

通过 `CommandLineRunner` 在启动时自动循环执行 Lua，模拟重启时不断申请 nodeId：

在 `application-dev.properties`（或启动参数）增加：

```properties
# 开启模拟器
tbox.lua.sim.enabled=true
# 循环次数
tbox.lua.sim.iterations=50
# 过期时间（ms），建议设置小一点便于观察过期/回收
tbox.lua.sim.expireMs=3000
# 节点池大小（maxId），建议设置小一点便于快速“占满”
tbox.lua.sim.maxId=16

# keyMode=app：所有“重启”都使用同一个 registryKey（更贴近真实）
# keyMode=ip ：每次循环使用不同 key（用 key 后缀模拟 IP 变化）
tbox.lua.sim.keyMode=app

 # release=true：每次循环模拟“优雅停机”释放上一次分配的 nodeId（ZREM）
 # release=false：模拟“崩溃/强杀”，不释放，等待 expireMs 过期回收
 tbox.lua.sim.release=false

 # reclaim=true：增加“崩溃不释放 -> 等待过期 -> 再次分配复用”的回收验证（两阶段）
 tbox.lua.sim.reclaim=true
 # reclaimWaitMs=0：默认等待 expireMs+200ms；你也可以显式设置更长的等待时间
 tbox.lua.sim.reclaimWaitMs=0
```

然后启动应用，观察控制台日志中每次分配到的 nodeId 以及 expireAt。

#### 推荐参数（快速复现“崩溃不释放，靠过期回收”）

将 `maxId` 设置小一点，方便快速“占满”，例如：

```properties
tbox.lua.sim.enabled=true
tbox.lua.sim.expireMs=1000
tbox.lua.sim.maxId=3
tbox.lua.sim.iterations=6
tbox.lua.sim.keyMode=app
tbox.lua.sim.release=false
tbox.lua.sim.reclaim=true
```

预期现象：

- Phase 1（fill）：会很快出现 `nodeId=-1`（表示池已占满）
- 等待 `expireMs` 后 Phase 2（reclaim）：会再次分配到 `0..maxId`（表示过期 slot 被复用）
