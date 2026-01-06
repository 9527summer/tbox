# tbox-dapper-spring-boot-starter（TBox-Tracer）

面向 **Spring Boot 2.x（javax）/ JDK 8** 的轻量追踪组件：基于 `TraceContext`（ThreadLocal + MDC）实现 trace 串联，目标是让日志天然可按 traceId 串起来。

## 功能

- **Web 入口追踪**：自动创建/恢复 `TraceContext`，写入 MDC（`traceId/spanId/...`），并在响应头回显 trace 信息
- **接口出入参日志**（可选）：`WebTraceAspect` 打印请求参数/响应内容（支持排除路径与最大长度）
- **异步上下文传播**：对 `ThreadPoolTaskExecutor` 注入 `TaskDecorator`，传播 `TraceContext`/MDC
- **定时任务追踪**：对 `@Scheduled`、`@XxlJob` 增加 trace 串联
- **MQ trace 透传**：提供 Kafka / RocketMQ 的 producer/consumer 拦截器/钩子（需按各客户端方式接入）
- **HTTP client trace 透传**：提供 `RestTemplate` / OkHttp / Apache HttpClient 的拦截器（需按各客户端方式接入）

## 引入依赖

```xml
<dependency>
  <groupId>io.github.9527summer</groupId>
  <artifactId>tbox-dapper-spring-boot-starter</artifactId>
  <version>${tbox.version}</version>
</dependency>
```

## 配置

```yaml
tbox:
  tracer:
    enabled: true
    application-name: ${spring.application.name}
    print-payload: true
    max-response-length: 2048
    exclude-paths:
      - /actuator/**
      - /swagger-ui/**
      - /v3/api-docs/**
      - /error
```

## 日志模板（MDC）

`TraceContext` 会写入 MDC：`traceId` / `spanId` / `parentSpanId` / `appName`。

Logback 示例：

```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] [%X{spanId}] %-5level %logger{36} - %msg%n</pattern>
```

## HTTP Client 接入说明

本 starter 会在 Spring 容器中提供对应的拦截器 Bean，你需要把它们“挂到你使用的 client 上”：

- RestTemplate：把 `TracerRestTemplateInterceptor` 加到 `RestTemplate#setInterceptors(...)`
- OkHttp：把 `TracerOkHttpInterceptor` 加到 `OkHttpClient.Builder#addInterceptor(...)`
- Apache HttpClient：把 `TracerHttpClientInterceptor#getRequestInterceptor()` / `getResponseInterceptor()` 注册到 HttpClient 构建器

## Kafka 接入说明

Kafka 通过配置 `interceptor.classes` 生效（示例：producer/consumer 的 properties 中配置 `TracingKafkaProducerInterceptor` / `TracingKafkaConsumerInterceptor`）。

## RocketMQ 接入说明

RocketMQ 需要把 `SendMessageHook` / `ConsumeMessageHook` 注册到 producer/consumer（例如：注册 `TracingRocketMQProducerHook` / `TracingRocketMQConsumerHook`）。
