# qywx-wecom-spring-boot-starter

[English](./README.md) | [中文](./README.zh-CN.md)

企业微信通讯录与审批查询 Spring Boot Starter。

## 项目说明

`qywx-wecom-spring-boot-starter` 是基于 `weixin-java-cp` 的二次封装。

目标是为业务提供稳定的查询 API，避免在业务层直接处理底层 SDK 细节。

本 starter 提供：

- 通讯录查询工具（部门、成员）
- 审批查询工具（分段分页、失败重试、请求限流、失败收集）
- Spring Boot 自动装配与 `@ConfigurationProperties`

本 starter 不做：

- 不替代 `weixin-java-cp`
- 不覆盖企业微信全部 API

## 依赖引入

```xml
<dependency>
    <groupId>org.cy</groupId>
    <artifactId>qywx-wecom-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 配置项

```properties
wx.cp.corp-id=your-corp-id
wx.cp.corp-secret=your-corp-secret
wx.cp.agent-id=1000002

# 审批查询可选参数
wx.cp.approval.segment-days=29
wx.cp.approval.page-size=100
wx.cp.approval.max-retry-attempts=3
wx.cp.approval.retry-backoff-millis=300
wx.cp.approval.requests-per-second=0
wx.cp.approval.executor-threads=8
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
- `WxApprovalDetailQueryResult queryApprovalDetails(Date startTime, Date endTime)`

## 本地构建

```bash
mvn clean test
```
