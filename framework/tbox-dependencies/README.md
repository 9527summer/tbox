# tbox-dependencies

统一依赖与版本管理模块（Maven `dependencyManagement`）。

## 作用

- 把 Spring Boot / Spring Framework / Micrometer / Redisson 等常用依赖版本集中到一个地方维护
- 业务项目只需要 import 一次，然后各 starter / 业务依赖只写坐标，不再写版本号

## 使用方式

在业务项目 `pom.xml` 中：

```xml
<properties>
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

然后按需引入 starter：

```xml
<dependencies>
  <dependency>
    <groupId>io.github.9527summer</groupId>
    <artifactId>tbox-all-spring-boot-starter</artifactId>
  </dependency>
</dependencies>
```

