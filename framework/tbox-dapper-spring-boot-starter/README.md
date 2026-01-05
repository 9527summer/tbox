# TBox-Tracer 分布式追踪系统

TBox-Tracer 是一个面向 **Spring Boot 3（Spring Framework 6）** 的轻量追踪适配层：以 **Micrometer Observation** 为统一入口，并使用 **Micrometer Tracing（OpenTelemetry bridge）** 输出 Trace/Span。

> 说明：历史上的 `TraceContext`（ThreadLocal 自研上下文）已删除；新系统建议全部基于 Observation/OTel。

## 主要特性

- **HTTP 请求追踪**：复用 Spring Boot 3 的 Web Observation/Tracing
- **响应头回显**：在响应头写入 `traceId` / `spanId`，便于排查
- **日志关联**：日志模板可输出 `traceId` / `spanId`（由 Spring Boot Tracing 自动写入 MDC）
- **调度任务追踪**：对 `@Scheduled` / `@XxlJob` 增加 Observation（生成 span）
- **异步上下文传播**：基于 Spring 的 `ContextPropagatingTaskDecorator` 传播上下文（Observation/Tracing/MDC）
- **OTLP HTTP 导出**：通过 OTel exporter 将 traces 发送到 OTel Collector（HTTP）
- **简单配置**：追求开箱即用，提供合理的默认值和简单的配置选项
- **低侵入性**：与业务代码解耦，无需修改现有代码即可启用追踪功能
- **标准化传播**：默认采用 W3C TraceContext 进行跨进程传播

## 快速开始

### 1. 添加依赖

在项目的`pom.xml`中添加以下依赖：

```xml
<dependency>
    <groupId>io.github.9527summer</groupId>
    <artifactId>tbox-dapper-spring-boot-starter</artifactId>
    <version>${tbox.version}</version>
</dependency>
```

### 2. 配置（推荐：W3C，仅用于日志串链路）

在 `application.yml` 中添加：

```yaml
tbox:
  tracer:
    enabled: true
    print-payload: true
    max-response-length: 2048

management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0
    propagation:
      type: w3c
```

### 3. 启动应用

启动应用后：

- Web 请求会自动生成 trace/span（由 Spring Boot 3 Web Observation/Tracing 完成）
- 响应头回显 `traceId` / `spanId`
- 日志可输出 `traceId/spanId`（MDC）
- 本 Starter 默认不包含 OTLP exporter 依赖，不做上报；如需上报建议由其它模块/agent 统一接入

## 详细配置指南

TBox-Tracer提供了丰富的配置选项，以下是完整的配置参考：

```yaml
tbox:
  tracer:
    # 基础配置
    enabled: true                         # 是否启用追踪功能，默认true
    application-name: your-app-name       # 应用名称，默认使用spring.application.name
    print-payload: true                   # 是否打印请求和响应内容，默认true（生产环境建议关闭）
    max-response-length: 2048             # 响应内容最大记录长度，超过将被截断
	    # expose-metrics: 由 Actuator/Micrometer 决定（此处不再提供自研开关）
    
    # 排除路径配置
    exclude-paths:                        # 不进行追踪的URL路径
      - /health
      - /metrics
      - /custom-path/**
```

### Spring Boot Actuator集成配置

要启用Prometheus格式的指标导出，添加以下配置：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

## 核心组件说明

### 1. Observation / Tracing（OTel）

TBox-Tracer 的核心是 Micrometer Observation/Tracing：

- Observation：统一的观测 API（span/metrics/events）
- Tracing（OTel bridge）：将 Observation 转换为 trace/span，并通过 OTLP 导出

### 2. Web请求追踪

对 Web 场景：

- 复用 Spring Boot 3 的 Web Observation/Tracing
- 由 Spring Boot Tracing 自动写入 MDC（日志模板可直接使用 `%X{traceId}` / `%X{spanId}`）

### 3. 调度任务追踪

支持多种调度框架：

- `ScheduledTaskTraceAspect`：Spring @Scheduled注解的切面
- `XxlJobTraceAspect`：XXL-Job注解的切面


### 6. 异步线程追踪

提供线程池和任务装饰器：

- `TracingTaskDecorator`：任务装饰器，确保异步任务传递追踪上下文
- `TracingThreadPoolBeanPostProcessor`：自动增强Spring线程池


### 7. 指标收集

集成Spring Boot Actuator和Micrometer：

- `TracerMetricsCollector`：指标收集器，记录请求数、响应时间等指标

## 最佳实践示例

### 1. 设置合理的排除路径

对于健康检查、指标接口等高频调用但不需要追踪的接口，应该配置排除路径：

```yaml
tbox:
  tracer:
    exclude-paths:
      - /actuator/**
      - /swagger-ui/**
      - /v3/api-docs/**
      - /health
      - /favicon.ico
      - /static/**
```

### 2. 异步线程中保持追踪上下文

使用线程池：

```java
@Configuration
public class ThreadPoolConfig {

    @Bean
    public Executor asyncExecutor(TaskDecorator tracingTaskDecorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("async-");
        // 使用追踪装饰器
        executor.setTaskDecorator(tracingTaskDecorator);
        executor.initialize();
        return executor;
    }
}
```

### 3. 手动创建 Observation（可选）

在某些非 Web 场景（如批处理任务）中，可以手动创建 Observation（会生成 span 并参与导出）：

```java
Observation observation = Observation.start("batch.process", observationRegistry);
try {
  // 批处理逻辑...
} catch (Throwable e) {
  observation.error(e);
  throw e;
} finally {
  observation.stop();
}
```


### 4. 生产环境性能优化

在生产环境中，可以通过以下配置降低追踪系统对性能的影响：

```yaml
tbox:
  tracer:
    print-payload: false         # 关闭请求和响应内容打印
    max-response-length: 1024    # 降低响应内容最大记录长度
    # 排除高频调用路径
    exclude-paths:
      - /actuator/**
      - /health
      - /metrics
      - /static/**
```

## 常见问题解答

### 1. 追踪上下文丢失问题

**问题**：在异步环境中，追踪上下文丢失导致无法追踪完整调用链路。

**解决方案**：
- 确保线程池使用上下文传播（本 Starter 会自动为 `ThreadPoolTaskExecutor` 增加 `TaskDecorator`）
- 确保没有自行创建“无上下文”的新线程（推荐使用 Spring 管理的线程池/TaskExecutor）

### 2. 重复的追踪ID问题

**问题**：某些请求产生了相同的追踪ID，导致无法区分不同请求。

**解决方案**：
- 检查是否在高并发环境中复用了相同的追踪上下文
- 确保每个请求都创建新的根上下文或子上下文
- 升级到最新版本的TBox-Tracer，修复了早期版本的ID生成问题

### 3. 日志量过大问题

**问题**：启用追踪功能后，日志量急剧增加。

**解决方案**：
- 调整日志级别：`logging.level.org.tbox.dapper=INFO`
- 关闭请求内容打印：`tbox.tracer.print-payload=false`
- 配置合适的排除路径，避免对高频接口进行追踪
- 使用采样策略，只追踪部分请求

### 4. 与Spring Cloud Sleuth兼容性问题

**问题**：同时使用TBox-Tracer和Spring Cloud Sleuth导致冲突。

**解决方案**：
- 选择其中一种追踪系统使用，不建议同时启用两种
- 如需迁移，可以先在部分服务中启用TBox-Tracer，逐步替换Sleuth


## 集成案例

### 与ELK集成

配置Logback追踪日志：

1. 配置`logback-spring.xml`：

```xml
异常堆栈需要traceId可选
<conversionRule conversionWord="traceStack"
                converterClass="org.tbox.dapper.logback.TraceIdThrowableProxyConverter"/>

<property name="CONSOLE_LOG_PATTERN"
          value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] [%X{spanId}] %-5level %logger{36} - %msg%n %traceStack"/>
```

2. 异常堆栈增加traceId

```xml

<conversionRule conversionWord="traceStack"
                converterClass="org.tbox.dapper.logback.TraceIdThrowableProxyConverter"/>

<property name="CONSOLE_LOG_PATTERN"
          value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] [%X{spanId}] %-5level %logger{36} - %msg%n %traceStack"/>
```

### 与Prometheus和Grafana集成

确保已启用指标导出：

```yaml
tbox:
  tracer:
    expose-metrics: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```


### 与消息队列集成（建议走 OTel 生态）

本 Starter 仍建议优先采用 OTel 生态方案（如 OTel Java agent / 相关 instrumentation），保持一致的 W3C 传播与 OTLP 导出。

同时，为了满足“仅在日志中串联 MQ trace”的轻量需求，Starter 提供了 **Kafka（Spring Kafka）** 的最小能力：

- 生产端：发送时自动往 Kafka headers 写入 `traceparent`
- 消费端：在监听方法执行前，从 headers 提取 `traceId/spanId` 写入 MDC（`%X{traceId}` / `%X{spanId}`）

启用条件：

- 依赖中存在 `spring-kafka`（或 `spring-boot-starter-kafka`）
- `tbox.tracer.enabled=true`（默认 true）

同时支持 **RocketMQ（rocketmq-client / rocketmq-spring）**：

- 生产端：发送前把 `traceparent` 写入 Message user properties
- 消费端：消费前从 Message user properties 读取 `traceparent` 并写入 MDC（`%X{traceId}` / `%X{spanId}`）

启用条件：

- 依赖中存在 `rocketmq-client`（或引入 `rocketmq-spring-boot-starter` 间接带上 client）
- `tbox.tracer.enabled=true`（默认 true）

## 设计原理

TBox-Tracer基于Google Dapper论文的设计理念，主要概念包括：

- **TraceId**：全局唯一的标识符，用于关联整个调用链路
- **SpanId**：标识一次操作或调用
- **Context（W3C TraceContext）**：跨进程传播的上下文（`traceparent`/`tracestate`）

系统通过在 HTTP 头等载体中传递这些信息，实现跨服务、跨线程的调用链路追踪。核心流程包括：

1. 请求入口创建根 span（由 Spring Boot Web Observation/Tracing 完成）
2. 下游调用自动创建子 span，并通过 W3C 头部传播
3. 通过 OTLP HTTP 上报到 OTel Collector
4. 日志通过 MDC 关联 traceId/spanId（并支持在响应头回显）

## 版本兼容性

- Java 17+
- Spring Boot 3.x
- Spring Framework 6.x

## 扩展TBox-Tracer

TBox-Tracer提供了多个扩展点，可以根据需要进行定制：

- 自定义拦截器
- 自定义指标收集器
- 自定义导出器

请参考源码中的接口和抽象类了解更多扩展方式。

## 贡献指南

我们欢迎社区贡献，如果你想参与项目开发，请：

1. Fork仓库
2. 创建特性分支（`feature/your-feature-name`）
3. 提交更改
4. 推送到分支
5. 创建Pull Request

## 许可证

[Apache License 2.0](LICENSE) 
