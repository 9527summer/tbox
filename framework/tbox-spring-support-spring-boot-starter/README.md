# tbox-spring-support-spring-boot-starter

Spring 应用层支撑 Starter（Spring Boot 2.x / JDK 8+）。

## 功能

- `ApplicationContextHolder`：获取 Spring 容器内 Bean 的工具
- Web 默认配置：Jackson / WebMvc / CORS 等常用默认值
- 默认全局异常处理器：统一输出结构化错误响应

## 使用方式

```xml
<dependency>
  <groupId>io.github.9527summer</groupId>
  <artifactId>tbox-spring-support-spring-boot-starter</artifactId>
  <version>${tbox.version}</version>
</dependency>
```

