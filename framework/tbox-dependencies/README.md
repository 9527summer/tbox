# tbox-dependencies

统一依赖与版本管理模块（Maven `dependencyManagement`）。

## 使用方式

在业务项目 `pom.xml` 中 import 一次：

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

然后在 `dependencies` 里按需引入各 Starter（不再写版本号）。

