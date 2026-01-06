# tbox-spring-support-spring-boot-starter

Spring 应用层支撑 Starter（Spring Boot 2.x / JDK 8+），提供一些“项目默认值”和通用的应用层组件。

## 功能

- `ApplicationContextHolder`：在非 Spring 管理对象中获取 Bean
- 默认 Jackson 配置（可开关）：
  - `JavaTimeModule`（`LocalDateTime/LocalDate/LocalTime`）
  - `Long/long/BigInteger` 序列化为字符串（避免前端精度丢失）
- 默认跨域 CORS（可开关）：`CorsFilter` 全放行（按需自行收敛）
- WebMvc 时间参数格式化（`yyyy-MM-dd HH:mm:ss` / `yyyy-MM-dd`）
- 默认全局异常处理器：`DefaultGlobalExceptionHandler`

## 引入依赖

```xml
<dependency>
  <groupId>io.github.9527summer</groupId>
  <artifactId>tbox-spring-support-spring-boot-starter</artifactId>
  <version>${tbox.version}</version>
</dependency>
```

## 配置项

```yaml
tbox:
  base:
    # 是否启用默认 CORS（默认 true）
    default-cors-config-enable: true
    # 是否启用默认 Jackson（默认 true）
    default-jackson-config-enable: true
```
