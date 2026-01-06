# tbox-redis-spring-boot-starter

Redis 相关能力 Starter（Spring Boot 2.x / JDK 8+）。

## 功能

- Redis 工具与封装（`RedisUtils`、分布式锁等）
- AOP 限流注解：`@RateLimit`（滑动窗口 / 令牌桶）

## 快速开始

```xml
<dependency>
  <groupId>io.github.9527summer</groupId>
  <artifactId>tbox-redis-spring-boot-starter</artifactId>
  <version>${tbox.version}</version>
</dependency>
```

