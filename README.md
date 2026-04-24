# qywx-wecom-spring-boot-starter

[English](./README.md) | [中文](./README.zh-CN.md)

A Spring Boot Starter for WeCom (WeChat Work) contact and approval querying.

## Project Overview

`qywx-wecom-spring-boot-starter` is a secondary wrapper built on top of `weixin-java-cp`.

It provides stable, business-friendly APIs for common querying scenarios and hides low-level SDK details.

What this starter provides:

- Contact querying utilities for departments and users
- Approval querying utilities with segmented paging, retry, rate limiting, and failure collection
- Spring Boot auto-configuration and `@ConfigurationProperties`

What this starter does not do:

- It does not replace `weixin-java-cp`
- It does not cover the full WeCom API surface

## Dependency

```xml
<dependency>
    <groupId>org.cy</groupId>
    <artifactId>qywx-wecom-spring-boot-starter</artifactId>
    <version>1.0.1</version>
</dependency>
```

## Required Configuration

```properties
wx.cp.corp-id=your-corp-id
wx.cp.corp-secret=your-corp-secret
wx.cp.agent-id=1000002

# Optional approval query tuning
wx.cp.approval.segment-days=29
wx.cp.approval.page-size=100
wx.cp.approval.max-retry-attempts=3
wx.cp.approval.retry-backoff-millis=300
wx.cp.approval.requests-per-second=0
wx.cp.approval.executor-threads=8
```

## How To Use

After adding the dependency and configuration, the starter auto-registers these beans:

- `WxCpService`
- `WxContactQueryUtil`
- `WxApprovalQueryUtil`

You can inject them directly in your Spring Boot application:

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

Typical usage:

- Use `WxContactQueryUtil` for department and user queries
- Use `WxApprovalQueryUtil#getApprovalSpNos(...)` when you only need approval numbers
- Use `WxApprovalQueryUtil#queryApprovalDetails(...)` when you need both successful details and failed records
- Use `WxApprovalQueryUtil#getApprovalDetailsGroupByTemplateId(...)` when grouping by template is needed

If your application already defines its own `WxCpService` or approval-query executor bean, the starter will back off and use your bean instead.

## Public APIs

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

## Local Build

```bash
mvn clean test
```
