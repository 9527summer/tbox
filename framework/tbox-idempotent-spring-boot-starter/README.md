# tbox-idempotent-spring-boot-starter

幂等性控制模块，通过注解简化幂等实现，防止接口重复提交。

## 功能特性

- **PARAM策略**：基于请求路径 + 用户ID + 参数哈希，适用于防止同一用户重复提交
- **SPEL策略**：基于请求路径 + SpEL表达式提取的值，适用于业务级幂等（如订单号）
- **超时控制**：支持幂等键的过期时间设置
- **分布式支持**：基于Redis实现分布式幂等控制

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.github.9527summer</groupId>
    <artifactId>tbox-idempotent-spring-boot-starter</artifactId>
    <version>1.0.4</version>
</dependency>
```

### 2. 配置

```yaml
tbox:
  idempotent:
    timeout: 5  # 默认超时时间（秒）
```

### 3. 使用

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    /**
     * PARAM策略：基于 路径 + 用户ID + 参数哈希
     */
    @PostMapping
    @Idempotent(type = IdempotentTypeEnum.PARAM, message = "订单已提交，请勿重复操作")
    public Result createOrder(@RequestBody OrderRequest request) {
        return Results.success();
    }

    /**
     * SPEL策略：基于 路径 + SpEL表达式的值
     */
    @PostMapping("/pay")
    @Idempotent(type = IdempotentTypeEnum.SPEL, key = "#request.orderId", message = "订单正在支付中")
    public Result payOrder(@RequestBody PayRequest request) {
        return Results.success();
    }

    /**
     * SPEL策略：多参数组合
     */
    @PostMapping("/bindProduct")
    @Idempotent(type = IdempotentTypeEnum.SPEL, key = "#userId + ':' + #productId", keyTimeout = 10)
    public Result bindProduct(String userId, String productId) {
        return Results.success();
    }
}
```

## 策略对比

| 策略 | Key组成 | 适用场景 |
|------|---------|----------|
| PARAM | path + userId + 参数hash | 同一用户对同一接口的重复请求 |
| SPEL | path + SpEL表达式值 | 业务级幂等（如订单号、流水号） |

## 注解参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| type | IdempotentTypeEnum | PARAM | 幂等策略类型 |
| key | String | "" | SpEL表达式（仅SPEL策略需要） |
| message | String | "您操作太快，请稍后再试" | 重复请求提示信息 |
| keyTimeout | long | 5 | 幂等键超时时间（秒） |

## 自定义用户ID提供者

PARAM策略需要获取当前用户ID，可自定义实现：

```java
@Component
public class CustomUserIdProvider implements UserIdProvider {
    @Override
    public String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
```

## 依赖要求

- Redis
- Redisson