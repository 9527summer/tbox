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

- Java 8+
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