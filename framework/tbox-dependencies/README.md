# tbox-dependencies

统一依赖与版本管理模块（Maven BOM / `dependencyManagement`），用于让业务项目“只写依赖坐标、不写版本号”。

## 作用

- 统一管理 **TBox 全部 starter 的版本**（与 `${tbox.version}` 保持一致）
- 通过 import `spring-boot-dependencies` 统一管理 **Spring Boot 2.x 生态依赖版本**
- 对少数三方依赖做版本钉死（例如：`redisson-spring-boot-starter`、`knife4j`、`rocketmq-client`、`kafka-clients`、`xxl-job-core` 等）

## 使用方式

### 单模块项目

```xml
<properties>
  <!-- 对应你要使用的 tbox 版本 -->
  <tbox.version>1.0.4</tbox.version>
</properties>

<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.github.9527summer</groupId>
      <artifactId>tbox-dependencies</artifactId>
      <version>${tbox.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

然后在 `<dependencies>` 里按需引入 starter（不再写版本号）：

```xml
<dependency>
  <groupId>io.github.9527summer</groupId>
  <artifactId>tbox-all-spring-boot-starter</artifactId>
</dependency>
```

### 多模块项目

把上面的 `dependencyManagement` 放到 **父工程** `pom.xml`，子模块只声明需要的 starter 依赖即可。

## 说明

- `scope=import` 的作用是：把 `tbox-dependencies` 里的 `dependencyManagement` 合并进当前工程的 `dependencyManagement`。
- 它**不会**自动把依赖加到 classpath：依赖仍需在 `<dependencies>` 里显式声明。
