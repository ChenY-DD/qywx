# qywx-wecom-files

企业微信审批与通讯录查询工具库（Maven `jar` 模块）。

## 打包与安装

在 `QyWxC` 目录执行（默认不混淆）：

```bash
mvn -DskipTests clean install
```

需要混淆（防反编译）时执行：

```bash
mvn -DskipTests clean package -P obfuscate
```

安装后会生成：

- 普通包：`qywx-wecom-files-0.0.1-SNAPSHOT.jar`
- 混淆包（启用 `obfuscate` profile 后）：`qywx-wecom-files-0.0.1-SNAPSHOT-obf.jar`

## 在业务项目中引用

```xml
<dependency>
    <groupId>org.cy</groupId>
    <artifactId>qywx-wecom-files</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## 配置项

需要在业务项目配置：

```properties
wx.cp.corp-id=your-corp-id
wx.cp.corp-secret=your-corp-secret
wx.cp.agent-id=1000002
```
