# TBox Framework（Spring Boot 2.x）

TBox 是一套面向 **Spring Boot 2.x（javax）/ JDK 8** 的开箱即用 Starter 集合，提供分布式追踪、幂等、Redis 工具/锁/限流、分布式 ID 等常用能力。

> 说明：本仓库同时维护 Spring Boot 3.x 版本（Jakarta）实现，见 `boot3` 分支；`master` 分支以 Spring Boot 2.x 为准。

## 特性

- **模块化 Starter**：按需引入，自动配置生效（Spring Boot 2.x 使用 `spring.factories` 注册）
- **分布式追踪（TBox-Tracer）**：基于 `TraceContext`（ThreadLocal + MDC），支持 Web/异步线程池/MQ/定时任务的 trace 串联
- **幂等控制**：`@Idempotent`（PARAM / SPEL），基于 Redis 实现分布式幂等
- **Redis 能力**：工具类、Redisson 分布式锁、AOP 限流注解 `@RateLimit`
- **分布式 ID / 兑换码**：Snowflake、时间型 ID、Base62 兑换码（可选 Redis 方式分配 nodeId）

## 项目结构

```
tbox/
├── framework/                  # 核心框架模块
│   ├── tbox-base-spring-boot-starter/           # 核心类型/工具（尽量不依赖 Spring Web/Redis）
│   ├── tbox-spring-support-spring-boot-starter/ # Spring 应用层支撑（默认 Jackson/CORS/异常处理等）
│   ├── tbox-redis-spring-boot-starter/          # Redis 工具/锁/限流（不自动注册 RedisTemplate）
│   ├── tbox-distributedid-spring-boot-starter/  # 分布式 ID/兑换码（可选 Redis）
│   ├── tbox-dapper-spring-boot-starter/         # 分布式追踪（TraceContext + MDC）
│   ├── tbox-idempotent-spring-boot-starter/     # 幂等性控制（Redis）
│   ├── tbox-dependencies/                       # 统一依赖/版本管理（import 到 dependencyManagement）
│   └── tbox-all-spring-boot-starter/            # 全家桶（聚合 starter）
└── tbox-demo/                 # 示例项目
```

## 快速开始

### 1) Import `tbox-dependencies`（推荐）

在业务项目 `pom.xml` 中 import 一次：

```xml
<properties>
    <tbox.version>1.0.4</tbox.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.9527summer</groupId>
            <artifactId>tbox-dependencies</artifactId>
            <version>${tbox.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

然后在 `<dependencies>` 中按需引入 starter（不再写版本号）：

```xml
<!-- 使用全部功能 -->
<dependency>
    <groupId>io.github.9527summer</groupId>
    <artifactId>tbox-all-spring-boot-starter</artifactId>
</dependency>

<!-- 或者单独使用某个模块 -->
<dependency>
    <groupId>io.github.9527summer</groupId>
    <artifactId>tbox-dapper-spring-boot-starter</artifactId>
</dependency>
```

### 2) 常用配置

```yaml
# application.yml
tbox:
  tracer:
    enabled: true
    application-name: my-application
    print-payload: true          # 是否打印请求/响应（WebTraceAspect）
    max-response-length: 2048    # 响应最大打印长度
    exclude-paths:
      - /actuator/**
      - /swagger-ui/**

  idempotent:
    timeout: 5                   # 幂等 token 过期（秒）
```

### 3) 日志关联（traceId/spanId）

TBox-Tracer 会写入 MDC：`traceId` / `spanId` / `parentSpanId` / `appName`。你只需要在日志模板里输出 MDC 即可串联日志。

Logback 示例：

```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] [%X{spanId}] %-5level %logger{36} - %msg%n</pattern>
```

### 4) 使用幂等控制

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @PostMapping
    @Idempotent(type = IdempotentTypeEnum.PARAM, message = "订单已提交，请勿重复操作")
    public Result createOrder(@RequestBody OrderRequest request) {
        return Results.success();
    }
}
```

### 5) 使用分布式锁（Redisson）

```java
import org.tbox.base.redis.utils.LockUtils;

LockUtils.executeWithLock("order:process:" + orderId, 3, TimeUnit.SECONDS, () -> {
    processOrder(orderId);
});
```

## Actuator / Micrometer

本仓库不强制引入 `spring-boot-starter-actuator`。如业务需要健康检查/指标/Prometheus，请自行引入 Actuator 并按 Spring Boot 方式配置 `management.endpoints.*`。

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
