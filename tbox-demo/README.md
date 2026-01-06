# tbox-demo

TBox `master`（Spring Boot 2.x / JDK 8）演示项目，用于快速验证各 starter 的开箱即用效果。

## 依赖概览

- Web：`spring-boot-starter-web`
- DB：MyBatis + PageHelper（MySQL，启动时执行 `schema.sql` / `data.sql`）
- Redis：`spring-boot-starter-data-redis`
- Redisson：`redisson-spring-boot-starter`
- TBox：`tbox-all-spring-boot-starter`（并额外引入 `tbox-dapper-spring-boot-starter` 用于追踪演示）

## 本地运行

1) 修改 `tbox-demo/src/main/resources/application.properties` 中的 MySQL/Redis/Redisson 配置为你自己的环境  
2) 启动：

```bash
mvn clean package
java -jar target/tbox-demo-0.0.1-SNAPSHOT.jar
```

## 接口示例

- 用户 CRUD（分页）：`GET /api/users?pageNum=1&pageSize=10`
- 触发业务异常：`GET /api/users/error`
- Trace 验证（用于观察 WebTraceAspect 是否出现 /error 二次分发导致的重复日志）：
  - `POST /api/trace/ok`
  - `POST /api/trace/biz-exception`
  - `POST /api/trace/error-dispatch`

> 幂等注解 `@Idempotent` 的演示代码目前在部分 controller 中被注释；如需验证幂等能力，可自行取消注释后测试重复提交效果。
