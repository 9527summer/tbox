# tbox-redis-spring-boot-starter

Redis 相关能力 Starter（Spring Boot 3 / JDK 17+）。

## 功能

- Redis 工具与封装（`RedisUtils`、分布式锁等）
- AOP 限流注解：`@RateLimit`（滑动窗口 / 令牌桶）
- 可选的 Redis 序列化配置：`RedisSerializerConfig`（需业务侧手动启用）

## 快速开始

### 1. 添加依赖

```xml
<dependency>
  <groupId>io.github.9527summer</groupId>
  <artifactId>tbox-redis-spring-boot-starter</artifactId>
  <version>${tbox.version}</version>
</dependency>
```

### 2. 使用限流注解

```java
@RestController
@RequestMapping("/api/demo")
public class DemoController {

  // key 不填：默认使用当前请求 URL（如 /api/demo/hello）
  @GetMapping("/hello")
  @RateLimit(prefix = "rate:")
  public String hello() {
    return "ok";
  }

  // SpEL：按用户维度限流（变相支持组合：可以在 SpEL 里拼接）
  @PostMapping("/submit")
  @RateLimit(key = "#userId", prefix = "rate:user:")
  public String submit(String userId) {
    return "ok";
  }
}
```

### 3.（可选）启用默认 Redis 序列化

本 Starter 不再自动注册 `RedisTemplate`/`StringRedisTemplate`（避免与业务侧配置或 Redisson 自动装配冲突）。

如需要使用 tbox 提供的默认序列化方式，可在业务侧手动启用：

```java
@Configuration
@Import(org.tbox.base.redis.config.RedisSerializerConfig.class)
public class RedisConfig {
}
```

## 限流模型

### 1) 滑动窗口（Redis ZSet + Lua）

- `mode = SLIDING_WINDOW`
- 主要参数：`maxRequests`、`windowSeconds`

### 2) 令牌桶（Redisson RRateLimiter）

- `mode = TOKEN_BUCKET`
- 主要参数：`permits`、`rate`、`interval`、`intervalUnit`
- 未配置 Redisson 时默认放行（与 `RateLimiterUtils` 的现有行为一致）

## 日志

当触发限流时，会输出 WARN 日志，包含 httpMethod/uri/key/mode/rule 等信息。

可按需在日志框架里单独配置该类的日志级别：

- `org.tbox.redis.ratelimit.aspect.RateLimitAspect`
