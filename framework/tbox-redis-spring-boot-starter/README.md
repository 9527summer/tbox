# tbox-redis-spring-boot-starter

Redis 相关能力 Starter（Spring Boot 2.x / JDK 8+）。

## 功能

- Redis 工具与封装（`RedisUtils`、分布式锁等）
- AOP 限流注解：`@RateLimit`（滑动窗口 / 令牌桶）

## 关于 RedisTemplate/序列化

本 starter **不再自动注册** `RedisTemplate` / `StringRedisTemplate`（避免与业务方/Redisson 的配置冲突）。

如需使用本项目提供的默认序列化模板，可在业务侧显式引入：

```java
@Configuration
@Import(org.tbox.base.redis.config.RedisSerializerConfig.class)
public class MyRedisConfig {}
```

## 快速开始

```xml
<dependency>
  <groupId>io.github.9527summer</groupId>
  <artifactId>tbox-redis-spring-boot-starter</artifactId>
  <version>${tbox.version}</version>
</dependency>
```
