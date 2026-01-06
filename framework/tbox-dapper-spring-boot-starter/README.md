# TBox-Tracer 分布式追踪系统

TBox-Tracer 是一个轻量级的分布式追踪系统，基于Google Dapper论文的设计理念实现。它专为JDK 8和Spring Boot 2.x环境打造，提供了低侵入性的追踪功能，能够帮助开发者监控和分析微服务调用链路。

## 主要特性

- **HTTP请求追踪**：自动追踪进出的HTTP请求，包括Spring MVC控制器和HTTP客户端（RestTemplate、OkHttp、Apache HttpClient）
- **消息队列追踪**：支持RocketMQ和Kafka的消息发送和消费追踪，保持调用链路完整
- **调度任务追踪**：支持Spring @Scheduled、XXL-Job和Quartz等主流调度框架的任务执行追踪
- **异步线程追踪**：通过自定义的线程池和装饰器，确保异步任务执行时追踪上下文正确传递
- **性能指标收集**：集成Spring Boot Actuator和Micrometer，提供丰富的性能指标
- **简单配置**：追求开箱即用，提供合理的默认值和简单的配置选项
- **低侵入性**：与业务代码解耦，无需修改现有代码即可启用追踪功能
- **高可扩展性**：提供扩展点，可以轻松添加自定义组件和集成第三方系统

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

### 2. 添加配置（可选）

在`application.yml`中添加配置（默认已启用，无需额外配置）：

```yaml
tbox:
  tracer:
    enabled: true
    application-name: your-application-name
    # 其他可选配置...
```

### 3. 启动应用

启动应用后，TBox-Tracer将自动开始工作，为所有进出的HTTP请求、消息队列操作和调度任务添加追踪信息。

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
    expose-metrics: false                 # 是否暴露追踪指标到Actuator
    
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

### 1. TraceContext

`TraceContext`是TBox-Tracer的核心组件，用于存储和传递追踪信息。主要方法：

- `newRootContext()`：创建新的根上下文
- `createChildContext()`：创建子上下文
- `setCurrentContext()`：设置当前线程的上下文
- `removeContext()`：清除当前线程的上下文
- `setAttribute(key, value)`：设置上下文属性

### 2. Web请求追踪

TBox-Tracer自动为Spring MVC应用添加拦截器和过滤器，对所有HTTP请求进行追踪：

- `TracerWebInterceptor`：处理请求追踪上下文的创建和传递
- `TracerFilter`：在请求头中添加追踪信息

### 3. HTTP客户端追踪

支持主流的HTTP客户端库：

- `TracerRestTemplateInterceptor`：RestTemplate客户端拦截器
- `TracerOkHttpInterceptor`：OkHttp客户端拦截器
- `TracerHttpClientInterceptor`：Apache HttpClient拦截器

### 4. 消息队列追踪

支持RocketMQ和Kafka：

- `TracingRocketMQProducerHook`：RocketMQ生产者钩子
- `TracingRocketMQConsumerHook`：RocketMQ消费者钩子
- `TracingKafkaProducerInterceptor`：Kafka生产者拦截器
- `TracingKafkaConsumerInterceptor`：Kafka消费者拦截器

### 5. 调度任务追踪

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

### 3. 手动创建根上下文

在某些特殊场景（如消息处理、批处理任务）中，需要手动创建根上下文：

```java
@Component
public class BatchProcessor {

    public void processBatch(List<String> items) {
        // 创建根追踪上下文
        TraceContext rootContext = TraceContext.newRootContext();
        rootContext.setAttribute("batch.size", String.valueOf(items.size()));
        rootContext.setAttribute("batch.type", "daily-report");
        
        try {
            // 批处理逻辑...
        } finally {
            // 清理上下文
            TraceContext.removeContext();
        }
    }
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
- 确保使用`TracingTaskDecorator`装饰线程池
- 检查是否手动调用了`TraceContext.removeContext()`导致上下文被清除

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


### 与RocketMQ集成

```java
@Configuration
public class RocketMQConfig {

    @Autowired
    private TracerProperties tracerProperties;

    @Bean
    public DefaultMQProducer defaultMQProducer() throws MQClientException {
        DefaultMQProducer producer = new DefaultMQProducer("producer-group");
        producer.setNamesrvAddr("rocketmq-server:9876");
        
        // 添加追踪钩子
        producer.getDefaultMQProducerImpl().registerSendMessageHook(
            new TracingRocketMQProducerHook(tracerProperties.getApplicationName())
        );
        
        producer.start();
        return producer;
    }
    
    @Bean
    public DefaultMQPushConsumer defaultMQPushConsumer() throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("consumer-group");
        consumer.setNamesrvAddr("rocketmq-server:9876");
        consumer.subscribe("test-topic", "*");
        
        // 添加追踪钩子
        consumer.getDefaultMQPushConsumerImpl().registerConsumeMessageHook(
            new TracingRocketMQConsumerHook(tracerProperties.getApplicationName())
        );
        
        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            // 消费逻辑
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        
        consumer.start();
        return consumer;
    }
}
```

## 设计原理

TBox-Tracer基于Google Dapper论文的设计理念，主要概念包括：

- **TraceId**：全局唯一的标识符，用于关联整个调用链路
- **SpanId**：标识一次操作或调用
- **TraceContext**：包含追踪信息的上下文，传递于服务之间

系统通过在HTTP头、消息属性等载体中传递这些信息，实现跨服务、跨线程的调用链路追踪。核心流程包括：

1. 请求入口创建根上下文
2. 当服务调用其他服务时，创建子上下文并传递追踪信息
3. 所有操作（数据库访问、缓存、消息发送等）都附加追踪信息
4. 通过日志记录或指标收集追踪数据

## 版本兼容性

- Java 8+
- Spring Boot 2.x
- Spring Framework 5.x

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
