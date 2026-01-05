# tbox-spring-support-spring-boot-starter

Spring 应用层支撑 Starter（Spring Boot 3 / JDK 17+）。

## 功能

- `ApplicationContextHolder`：获取 Spring 容器内 Bean 的工具
- Web 默认配置：Jackson / WebMvc / CORS 等常用默认值
- 默认全局异常处理器：统一输出结构化错误响应

## 使用方式

```xml
<dependency>
  <groupId>io.github.9527summer</groupId>
  <artifactId>tbox-spring-support-spring-boot-starter</artifactId>
</dependency>
```

默认采用自动配置方式生效；如你希望更细粒度可控（例如只要异常处理，不要 WebMvc/Jackson），可以再补开关项把各配置拆开。

