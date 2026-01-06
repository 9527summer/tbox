# tbox-base-spring-boot-starter

基础“核心层”能力（偏通用类型与工具），尽量不依赖 Spring Web/Redis 等重量级组件。

> 说明：分布式追踪在 `tbox-dapper-spring-boot-starter`；Redis 工具/锁/限流在 `tbox-redis-spring-boot-starter`；分布式 ID 在 `tbox-distributedid-spring-boot-starter`。

## 功能

- 统一返回结构：`Result` / `Results`
- 基础异常体系：`BaseException` / `BizException` / `SysException`
- 基础枚举：`StandardErrorCodeEnum` 等
- 常用工具：`JsonUtils`、`AssertUtils`、`DateTimeConstants`

## 引入依赖

```xml
<dependency>
  <groupId>io.github.9527summer</groupId>
  <artifactId>tbox-base-spring-boot-starter</artifactId>
  <version>${tbox.version}</version>
</dependency>
```

## 使用示例

```java
import org.tbox.base.core.response.Result;
import org.tbox.base.core.response.Results;

public class Demo {
  public Result<String> ok() {
    return Results.success("ok");
  }
}
```
