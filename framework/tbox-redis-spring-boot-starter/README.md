# tbox-redis-spring-boot-starter

Redis 相关能力 Starter（Spring Boot 2.x / JDK 8+）。

## 功能

- Redis 工具与封装：`RedisUtils` / `CacheUtils`
- Redisson 分布式锁：`LockUtils`
- AOP 限流注解：`@RateLimit`（滑动窗口 / 令牌桶）

## 引入依赖

```xml
<dependency>
  <groupId>io.github.9527summer</groupId>
  <artifactId>tbox-redis-spring-boot-starter</artifactId>
  <version>${tbox.version}</version>
</dependency>
```

## 关于 RedisTemplate/序列化

本 starter **不再自动注册** `RedisTemplate` / `StringRedisTemplate`（避免与业务方或 Redisson 自动装配冲突）。

如需使用本项目提供的默认序列化模板，可在业务侧显式引入：

```java
@Configuration
@Import(org.tbox.base.redis.config.RedisSerializerConfig.class)
public class MyRedisConfig {}
```

## 使用限流注解

```java
@RestController
@RequestMapping("/api/demo")
public class DemoController {

  // key 不填：默认使用当前请求 URI（如 /api/demo/hello）
  @GetMapping("/hello")
  @RateLimit(prefix = "rate:")
  public String hello() {
    return "ok";
  }

  // SpEL：按用户维度限流（可在 SpEL 里组合拼 key）
  @PostMapping("/submit")
  @RateLimit(key = "#userId", prefix = "rate:user:")
  public String submit(String userId) {
    return "ok";
  }
}
```

当触发限流时会输出 WARN 日志（含 httpMethod/uri/key/mode/rule 等信息），并抛出 `BizException`（错误码 `RATE_LIMIT_ERROR`）。

## 使用分布式锁（Redisson）

```java
import org.tbox.base.redis.utils.LockUtils;

LockUtils.executeWithLock("order:lock:" + orderId, () -> {
  // do something
  return null;
});
```
