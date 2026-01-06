# TBox Base Spring Boot Starter

TBox基础框架组件，提供常用工具类和基础功能支持。

## 主要功能

- 分布式ID生成器(雪花算法)
- 通用工具类
- 基础数据结构
- 日志TraceID集成

## 分布式ID生成器

### 简介

基于雪花算法(Snowflake)的分布式ID生成器，支持多种部署环境：
- 物理机环境：基于进程ID+MAC地址生成workerId
- 容器环境：基于容器ID/主机名自动生成workerId
- 支持Docker、Kubernetes、ECS等多种容器环境

### 使用方式

```java
// 生成Long型ID
Long id = IdUtils.generateLongId();

// 生成16位十六进制字符串ID
String strId = IdUtils.generateId();

// 从ID中提取生成时间
long timestamp = IdUtils.getTimestampFromId(id);
```

## 配置说明

```yaml
tbox:
  tracer:
    enabled: true                 # 启用追踪（默认true）
    print-payload: true           # 是否打印请求/响应内容（默认true）
    max-payload-length: 1024      # 打印内容最大长度（默认1024）
    expose-metrics: false         # 是否暴露指标（默认false）
    exclude-patterns:             # 排除路径配置，不会被追踪
      - /actuator/**
      - /swagger-ui/**
      - /v3/api-docs/**
      - /swagger-resources/**
      - /favicon.ico
      - /error
    log-level: INFO               # 接口出参入参的日志级别（默认INFO）
```

注意：
- HTTP客户端追踪默认开启，无需额外配置
- 接口的请求参数和响应数据使用INFO级别记录，便于问题排查

## 日志TraceID集成

为便于分布式环境下的日志追踪，TBox支持在日志中自动添加TraceID和SpanID。

### MDC日志配置

#### Log4j2配置示例

在`log4j2.xml`中添加TraceID到日志格式：

```xml
<Configuration>
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] [%X{spanId}] %-5level %logger{36} - %msg%n"/>
        </Console>
        <RollingFile name="RollingFile" fileName="logs/app.log"
                     filePattern="logs/app-%d{yyyy-MM-dd}-%i.log">
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] [%X{spanId}] %-5level %logger{36} - %msg%n"/>
            <Policies>
                <TimeBasedTriggeringPolicy/>
                <SizeBasedTriggeringPolicy size="10 MB"/>
            </Policies>
            <DefaultRolloverStrategy max="10"/>
        </RollingFile>
    </Appenders>
    <Loggers>
        <!-- Web请求日志配置 -->
        <Logger name="org.tbox.base.web.trace" level="INFO" additivity="false">
            <AppenderRef ref="Console"/>
            <AppenderRef ref="RollingFile"/>
        </Logger>
        <Root level="INFO">
            <AppenderRef ref="Console"/>
            <AppenderRef ref="RollingFile"/>
        </Root>
    </Loggers>
</Configuration>
```

#### Logback配置示例

在`logback.xml`中添加TraceID到日志格式：

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] [%X{spanId}] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/app.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/app-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] [%X{spanId}] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- Web请求日志配置 -->
    <logger name="org.tbox.base.web.trace" level="INFO" />
    
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="FILE" />
    </root>
</configuration>
```

### 接口日志格式示例

接口的请求参数和响应数据以INFO级别记录，格式如下：

```
2023-05-05 12:34:56.789 [http-nio-8080-exec-1] [a1b2c3d4e5f6g7h8] [i9j0k1l2m3n4] INFO  o.t.base.web.trace.WebTraceAspect - [请求开始] 接口:/api/users/123, 方法:GET, IP:192.168.1.100, 参数:{"userId":"123"}
2023-05-05 12:34:56.795 [http-nio-8080-exec-1] [a1b2c3d4e5f6g7h8] [i9j0k1l2m3n4] INFO  o.t.base.web.trace.WebTraceAspect - [请求结束] 接口:/api/users/123, 状态:200, 耗时:6ms, 响应:{"id":"123","name":"张三","age":30}
```

### 日志格式示例

启用TraceID后，日志输出格式如下：

```
2023-05-05 12:34:56.789 [http-nio-8080-exec-1] [a1b2c3d4e5f6g7h8] [i9j0k1l2m3n4] INFO  c.e.controller.UserController - 处理用户请求: id=123
2023-05-05 12:34:56.790 [http-nio-8080-exec-1] [a1b2c3d4e5f6g7h8] [i9j0k1l2m3n4] INFO  c.e.service.UserService - 查询用户信息: id=123
2023-05-05 12:34:56.795 [http-nio-8080-exec-1] [a1b2c3d4e5f6g7h8] [i9j0k1l2m3n4] INFO  c.e.service.UserService - 用户信息查询完成, 耗时: 5ms
```

通过这样的配置，可以轻松地在分布式系统中追踪请求流程，定位问题。当请求流经多个服务时，相同的TraceID将被保留并记录在各个服务的日志中。

### ID生成器日志配置

ID生成器使用SLF4J记录日志，支持以下日志级别：

- **INFO**: 记录workerId初始化等基本信息
- **WARN**: 记录时钟回拨、随机ID生成等异常情况
- **DEBUG**: 记录workerId生成策略等调试信息
- **TRACE**: 记录详细环境检测信息

#### Log4j2配置示例

在`log4j2.xml`中添加以下配置启用TRACE级别日志：

```xml
<Configuration>
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </Console>
    </Appenders>
    <Loggers>
        <!-- ID生成器日志配置 -->
        <Logger name="org.tbox.base.core.id.DistributedIdGenerator" level="TRACE" additivity="false">
            <AppenderRef ref="Console"/>
        </Logger>
        <Root level="INFO">
            <AppenderRef ref="Console"/>
        </Root>
    </Loggers>
</Configuration>
```

#### Logback配置示例

在`logback.xml`中添加以下配置启用TRACE级别日志：

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- ID生成器日志配置 -->
    <logger name="org.tbox.base.core.id.DistributedIdGenerator" level="TRACE" />
    
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
```

### TRACE级别日志格式

启用TRACE级别后，ID生成器会输出详细的环境检测日志，格式如下：

```
2023-05-05 12:34:56.789 [main] TRACE o.t.base.core.id.DistributedIdGenerator - Docker environment detected (/.dockerenv exists)
2023-05-05 12:34:56.790 [main] TRACE o.t.base.core.id.DistributedIdGenerator - Using hostname as container ID: my-service-pod-123456
2023-05-05 12:34:56.791 [main] TRACE o.t.base.core.id.DistributedIdGenerator - Current process ID: 1234
```

环境检测日志可帮助诊断workerId生成策略，特别是在不同部署环境中排查ID生成器的工作状态。

## 依赖引入

```xml
<dependency>
    <groupId>io.github.9527summer</groupId>
    <artifactId>tbox-base-spring-boot-starter</artifactId>
    <version>${tbox.version}</version>
</dependency>
``` 
