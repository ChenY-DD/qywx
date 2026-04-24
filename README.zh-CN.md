# qywx-wecom-files

[English](./README.md) | [中文](./README.zh-CN.md)

企业微信通讯录与审批查询工具库（Maven `jar` 模块）。

## 项目说明

`qywx-wecom-files` 是基于 `weixin-java-cp` 的二次封装。

它将常见企业场景做了统一封装，让业务服务可以直接使用稳定 API，而不是频繁直接调用底层 SDK。

本封装提供：

- 通讯录查询能力：部门、成员查询，以及成员主部门信息补全
- 审批查询能力：审批单号（`spNo`）查询、审批详情查询、按模板 ID 分组
- 可直接注入的 Spring 组件（`@Component` / `@Configuration`）

本封装不做：

- 不替代 `weixin-java-cp`
- 不覆盖企业微信全部 API
- 只聚焦当前仓库已实现的查询能力

## 依赖引入

在业务项目 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>org.cy</groupId>
    <artifactId>qywx-wecom-files</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## 必要配置

在业务项目中配置企业微信参数：

```properties
wx.cp.corp-id=your-corp-id
wx.cp.corp-secret=your-corp-secret
wx.cp.agent-id=1000002
```

## Spring 扫描要求

本库类位于 `org.cy.qywx` 包下，业务项目需要确保扫描该包：

```java
@SpringBootApplication(scanBasePackages = {"你的业务包", "org.cy.qywx"})
public class Application {
}
```

## 对外方法

### WxContactQueryUtil

- `List<WxDepartmentVO> getDepartments(Long departmentId)`
- `List<WxDepartmentVO> getAllDepartments()`
- `WxDepartmentVO getDepartment(Long departmentId)`
- `List<WxUserVO> getUsersByDepartment(Long departmentId, boolean fetchChild, Integer status)`
- `List<WxUserVO> getAllUsers()`
- `List<WxUserVO> getAllUsers(Integer status)`
- `List<WxUserVO> getUsersSimpleByDepartment(Long departmentId, boolean fetchChild, Integer status)`
- `List<WxUserVO> getAllUsersSimple()`
- `List<WxUserVO> getAllUsersSimple(Integer status)`
- `WxUserVO getUserById(String userId)`

### WxApprovalQueryUtil

- `List<String> getApprovalSpNos(Date startTime, Date endTime)`
- `List<WxApprovalDetailVO> getApprovalDetails(Date startTime, Date endTime)`
- `Map<String, List<WxApprovalDetailVO>> getApprovalDetailsGroupByTemplateId(Date startTime, Date endTime)`

## 使用示例

```java
@Autowired
private WxContactQueryUtil wxContactQueryUtil;

@Autowired
private WxApprovalQueryUtil wxApprovalQueryUtil;

@Test
void demo() throws Exception {
    List<WxUserVO> users = wxContactQueryUtil.getAllUsers();

    Date start = new Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000);
    Date end = new Date();
    List<WxApprovalDetailVO> details = wxApprovalQueryUtil.getApprovalDetails(start, end);

    System.out.println(users.size());
    System.out.println(details.size());
}
```

## 本地构建

在 `QyWxC` 目录执行：

```bash
mvn clean install
```
