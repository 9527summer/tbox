# TBox Framework

TBox是一个轻量级的Java企业应用开发框架，提供开箱即用的脚手架组件，帮助开发者快速构建高质量的企业级应用。框架基于Spring Boot 2.x开发，完全兼容JDK 8，提供了分布式追踪、幂等性控制、分布式锁等常用功能。

## 核心特性

- **低侵入性**：所有组件采用AOP和自动配置方式实现，对业务代码无侵入
- **模块化设计**：按功能划分多个独立Starter，可按需引入
- **完善的分布式追踪**：支持HTTP请求、RPC调用、消息队列、定时任务的全链路追踪
- **幂等性控制**：提供简单易用的幂等注解，支持多种幂等策略
- **分布式锁工具**：基于Redis的分布式锁实现，支持可重入锁和自动续期
- **性能监控**：与Spring Boot Actuator和Micrometer集成的性能指标采集
- **Spring生态兼容**：完全兼容Spring Boot生态系统，可与各种组件无缝集成

## 项目结构

```
tbox/
├── framework/                  # 核心框架模块
│   ├── base-spring-boot-starter/           # 基础功能模块
│   ├── tbox-distributedid-spring-boot-starter/ # 分布式ID/兑换码模块
│   ├── dapper-spring-boot-starter/         # 分布式追踪模块  
│   ├── idempotent-spring-boot-starter/     # 幂等性控制模块
│   └── tbox-all-spring-boot-starter/       # 全功能包装模块
└── tbox-demo/                 # 示例项目
```

## 模块介绍

### base-spring-boot-starter

基础功能模块，提供常用工具类和核心抽象：

- **统一响应**：标准化的API响应格式
- **异常处理**：全局异常处理机制
- **分布式锁**：基于Redis的分布式锁实现
- **缓存工具**：Redis缓存抽象和工具类
- **工具集**：JSON处理、ID生成、断言等工具类

### dapper-spring-boot-starter

分布式追踪模块，灵感来自Google Dapper论文：

- **HTTP追踪**：Web请求和HTTP客户端调用追踪
- **消息队列追踪**：支持RocketMQ和Kafka的生产消费追踪
- **定时任务追踪**：Spring @Scheduled和XXL-Job任务追踪
- **异步任务追踪**：线程池和CompletableFuture的上下文传递
- **指标收集**：请求数量、响应时间等性能指标收集

### idempotent-spring-boot-starter

幂等性控制模块，通过注解简化幂等实现：

- **多种幂等策略**：支持参数级和表达式级幂等控制
- **灵活配置**：可自定义幂等键生成和验证逻辑
- **超时控制**：支持幂等键的过期时间设置
- **分布式支持**：基于Redis实现分布式幂等控制

## 快速开始

### 1. 添加依赖

```xml
<!-- 使用全部功能 -->
<dependency>
    <groupId>org.tbox</groupId>
    <artifactId>tbox-all-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 或者单独使用某个模块 -->
<dependency>
    <groupId>org.tbox</groupId>
    <artifactId>dapper-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 基础配置

```yaml
# application.yml
tbox:
  # 追踪配置
  tracer:
    enabled: true
    application-name: my-application
    print-payload: true
    max-response-length: 2048
    exclude-paths:
      - /actuator/**
      - /swagger-ui/**
    
  # 幂等控制配置
  idempotent:
      timeout: 5
```

### 3. 使用分布式追踪

分布式追踪功能会自动对HTTP请求、HTTP客户端调用、消息队列操作进行追踪，并记录关键指标。


### 4. 使用幂等控制

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    // 基于参数的幂等控制
    @PostMapping
    @Idempotent(type = IdempotentTypeEnum.PARAM, message = "订单已提交，请勿重复操作")
    public Result createOrder(@RequestBody OrderRequest request) {
        // 创建订单逻辑...
        return Results.success();
    }
}
```

### 5. 使用分布式锁

```java
// 简单锁
String lockKey = "order:process:" + orderId;
try {
    if (RedisLockUtils.lock(lockKey, 10, TimeUnit.SECONDS)) {
        // 获取锁成功，处理业务逻辑
        processOrder(orderId);
    } else {
        // 获取锁失败
        throw new BizException("订单正在处理中，请稍后再试");
    }
} finally {
    // 释放锁
    RedisLockUtils.unlock(lockKey);
}

// 带请求ID的锁（可以验证锁的持有者）
String requestId = UUID.randomUUID().toString();
try {
    if (RedisLockUtils.lock(lockKey, requestId, 10, TimeUnit.SECONDS)) {
        // 获取锁成功，处理业务逻辑
        processOrder(orderId);
    } else {
        // 获取锁失败
        throw new BizException("订单正在处理中，请稍后再试");
    }
} finally {
    // 释放锁（验证锁的持有者）
    RedisLockUtils.unlock(lockKey, requestId);
}
```

## 高级配置

### 追踪模块高级配置

```yaml
tbox:
  tracer:      
    # 暴露追踪指标到Actuator
    expose-metrics: true
```

### 自定义排除路径

```yaml
tbox:
  tracer:
    exclude-paths:
      - /api/health/**
      - /api/public/**
```

## 监控与管理

TBox集成了Spring Boot Actuator，可以通过Actuator端点查看和管理系统状态：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

访问以下端点获取系统信息：

- `/actuator/health` - 系统健康状态
- `/actuator/metrics` - 系统指标
- `/actuator/prometheus` - Prometheus格式的指标数据

## 贡献指南

欢迎为TBox Framework做出贡献！请遵循以下步骤：

1. Fork本仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交变更 (`git commit -m 'Add some amazing feature'`)
4. 推送到远程分支 (`git push origin feature/amazing-feature`)
5. 创建Pull Request

## 许可证

TBox Framework使用Apache 2.0许可证，详情请参阅[LICENSE](LICENSE)文件。

## 联系方式

如有问题或建议，欢迎提交Issue或Pull Request。 
