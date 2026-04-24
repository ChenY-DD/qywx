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

## 如何使用

引入依赖并完成配置后，starter 会自动注册以下 Bean：

- `WxCpService`
- `WxContactQueryUtil`
- `WxApprovalQueryUtil`

业务代码里可以直接注入使用：

```java
@Service
public class DemoService {

    private final WxContactQueryUtil wxContactQueryUtil;
    private final WxApprovalQueryUtil wxApprovalQueryUtil;

    public DemoService(
            WxContactQueryUtil wxContactQueryUtil,
            WxApprovalQueryUtil wxApprovalQueryUtil
    ) {
        this.wxContactQueryUtil = wxContactQueryUtil;
        this.wxApprovalQueryUtil = wxApprovalQueryUtil;
    }

    public List<WxUserVO> listUsers() throws WxErrorException {
        return wxContactQueryUtil.getAllUsers();
    }

    public WxApprovalDetailQueryResult queryApprovals(Date startTime, Date endTime) throws WxErrorException {
        return wxApprovalQueryUtil.queryApprovalDetails(startTime, endTime);
    }
}
```

常见使用方式：

- 查询部门、成员时使用 `WxContactQueryUtil`
- 只需要审批单号时使用 `WxApprovalQueryUtil#getApprovalSpNos(...)`
- 既要审批详情又要拿到失败记录时使用 `WxApprovalQueryUtil#queryApprovalDetails(...)`
- 需要按模板分组时使用 `WxApprovalQueryUtil#getApprovalDetailsGroupByTemplateId(...)`

如果业务项目已经自己提供了 `WxCpService` 或审批查询线程池 Bean，starter 会自动让出，直接复用业务侧 Bean。

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
