# tbox-idempotent-spring-boot-starter

幂等性控制 Starter，通过注解简化“防重复提交/防重复执行”的实现（Spring Boot 2.x / JDK 8+）。

## 功能

- `@Idempotent` 两种策略：
  - `PARAM`：基于“请求路径 + 用户标识 + 参数摘要”生成幂等 key
  - `SPEL`：基于“请求路径 + SpEL 表达式”生成幂等 key（更贴合业务幂等，如订单号）
- 基于 Redis 保存幂等 token，支持分布式场景

## 引入依赖

```xml
<dependency>
  <groupId>io.github.9527summer</groupId>
  <artifactId>tbox-idempotent-spring-boot-starter</artifactId>
  <version>${tbox.version}</version>
</dependency>
```

## 配置

```yaml
tbox:
  idempotent:
    timeout: 5  # token 默认过期时间（秒）
```

## 使用示例

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

  @PostMapping
  @Idempotent(type = IdempotentTypeEnum.PARAM, message = "订单已提交，请勿重复操作")
  public Result createOrder(@RequestBody OrderRequest request) {
    return Results.success();
  }

  @PostMapping("/pay")
  @Idempotent(type = IdempotentTypeEnum.SPEL, key = "#request.orderId", message = "订单正在支付中")
  public Result payOrder(@RequestBody PayRequest request) {
    return Results.success();
  }
}
```

## 自定义用户标识（PARAM 策略）

`PARAM` 策略需要“当前用户标识”。你可以提供一个 `UserIdProvider` 的实现覆盖默认逻辑：

```java
@Component
public class CustomUserIdProvider implements UserIdProvider {
  @Override
  public String getCurrentUserId() {
    return "your-user-id";
  }
}
```
