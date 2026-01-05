# tbox-all-spring-boot-starter

全家桶 Starter：一次性引入 TBox 的全部功能（适合脚手架/快速验证）。

## 包含内容

- `tbox-base-spring-boot-starter`
- `tbox-spring-support-spring-boot-starter`
- `tbox-redis-spring-boot-starter`
- `tbox-distributedid-spring-boot-starter`
- `tbox-idempotent-spring-boot-starter`
- `tbox-dapper-spring-boot-starter`

## 使用方式

推荐先 import `tbox-dependencies`，然后只引入本 starter：

```xml
<dependency>
  <groupId>io.github.9527summer</groupId>
  <artifactId>tbox-all-spring-boot-starter</artifactId>
</dependency>
```

各模块的启用方式以对应 starter 的 README 为准（一般是“引入依赖即自动配置 + 少量开关配置”）。

