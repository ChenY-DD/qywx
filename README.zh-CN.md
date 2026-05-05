# qywx-wecom-spring-boot-starter

[English](./README.md) | [中文](./README.zh-CN.md)

企业微信通讯录、审批、花名册、考勤查询 Spring Boot Starter。

本项目基于 `weixin-java-cp` 做二次封装，提供更贴近业务的工具类，让业务代码不用在每个服务里重复拼装底层 SDK 调用。

## 功能特性

- 自动装配 `WxCpService` 和常用工具类 Bean
- 通讯录查询：部门、成员、简要成员信息
- 审批查询：按时间分段、分页、重试、限流、失败收集
- HR 花名册查询：对接智慧人事 / 人事助手应用
- 考勤查询：打卡记录、日报、月报、排班、异常聚合
- 通用 `WxApiClient`：调用任意企业微信接口并自动注入 `access_token`
- SLF4J 日志：初始化、重试、完成、失败

## 环境要求

- Java 21+
- Spring Boot 3.5.x
- Maven

## 引入依赖

```xml
<dependency>
    <groupId>org.cy</groupId>
    <artifactId>qywx-wecom-spring-boot-starter</artifactId>
    <version>1.0.2</version>
</dependency>
```

## 配置

最小配置：

```properties
wx.cp.corp-id=wwxxxxxxxx
wx.cp.corp-secret=your-app-secret
wx.cp.agent-id=1000002
```

完整可选参数：

```properties
# 审批查询
wx.cp.approval.segment-days=29
wx.cp.approval.page-size=100
wx.cp.approval.max-retry-attempts=3
wx.cp.approval.retry-backoff-millis=300
wx.cp.approval.requests-per-second=0
wx.cp.approval.executor-threads=8

# HR 花名册。需要智慧人事 / 人事助手应用的专属 secret。
wx.cp.hr.secret=your-hr-app-secret
wx.cp.hr.agent-id=1000003
wx.cp.hr.max-retry-attempts=5
wx.cp.hr.retry-backoff-millis=1000
wx.cp.hr.requests-per-second=10
wx.cp.hr.executor-threads=8

# 考勤 / 打卡
wx.cp.checkin.segment-days=30
wx.cp.checkin.user-batch-size=100
wx.cp.checkin.max-retry-attempts=3
wx.cp.checkin.retry-backoff-millis=500
wx.cp.checkin.requests-per-second=0
wx.cp.checkin.executor-threads=8
```

说明：

- `wx.cp.corp-secret` 是主应用 secret，用于通讯录、审批、考勤和通用 API 调用。
- `wx.cp.hr.secret` 必须是智慧人事 / 人事助手应用的专属 secret，通讯录或自建应用 secret 不能访问 HR 花名册接口。
- `requests-per-second=0` 表示该模块不启用内置限流。

## 自动装配的 Bean

引入依赖并完成最小配置后，starter 会自动注册：

- `WxCpService`
- `WxContactQueryUtil`
- `WxApprovalQueryUtil`
- `WxCheckinQueryUtil`
- `WxApiClient`

配置 `wx.cp.hr.secret` 后，还会注册：

- `WxHrRosterQueryUtil`
- `qywxHrCpService`

如果业务项目已经提供了兼容的 `WxCpService` 或对应线程池 Bean，starter 会自动让出并复用业务侧 Bean。

## 快速开始

```java
@Service
public class WeComService {

    private final WxContactQueryUtil contacts;
    private final WxApprovalQueryUtil approvals;
    private final WxCheckinQueryUtil checkin;
    private final WxApiClient apiClient;

    public WeComService(
            WxContactQueryUtil contacts,
            WxApprovalQueryUtil approvals,
            WxCheckinQueryUtil checkin,
            WxApiClient apiClient
    ) {
        this.contacts = contacts;
        this.approvals = approvals;
        this.checkin = checkin;
        this.apiClient = apiClient;
    }

    public List<WxUserVO> users() throws WxErrorException {
        return contacts.getAllUsers();
    }

    public WxApprovalDetailQueryResult recentApprovals() throws WxErrorException {
        return approvals.queryApprovalDetails(WxDateRangeUtils.last3Days());
    }

    public WxAttendanceReportVO attendance(WxDateRange range, List<String> userIds) {
        return checkin.getAttendanceReport(range, userIds);
    }

    public JsonNode customApi() throws Exception {
        return apiClient.getJson(
                "https://qyapi.weixin.qq.com/cgi-bin/user/get",
                Map.of("userid", "zhangsan")
        );
    }
}
```

## 常见用法

### 通讯录

```java
List<WxDepartmentVO> departments = wxContactQueryUtil.getAllDepartments();
List<WxUserVO> users = wxContactQueryUtil.getAllUsers();
List<WxUserVO> simpleUsers = wxContactQueryUtil.getAllUsersSimple();
WxUserVO user = wxContactQueryUtil.getUserById("zhangsan");
```

### 审批

```java
WxDateRange range = WxDateRangeUtils.currentMonth();

List<String> spNos = wxApprovalQueryUtil.getApprovalSpNos(range);
List<WxApprovalDetailVO> details = wxApprovalQueryUtil.getApprovalDetails(range);

WxApprovalDetailQueryResult result = wxApprovalQueryUtil.queryApprovalDetails(range);
Map<String, List<WxApprovalDetailVO>> byTemplate = result.groupByTemplateId();
List<WxApprovalDetailFetchFailure> failures = result.failures();
```

模板相关：

```java
List<WxApprovalTemplateVO> templates = wxApprovalQueryUtil.getTemplates(range);
WxCpOaApprovalTemplateResult template = wxApprovalQueryUtil.getTemplateDetail("template_id");
Map<String, String> templateIds = wxApprovalQueryUtil.getTemplateIdsBySpNos(spNos);
```

### 日期范围

```java
WxDateRange today = WxDateRangeUtils.today();
WxDateRange last7Days = WxDateRangeUtils.lastDays(7);
WxDateRange month = WxDateRangeUtils.currentMonth();
WxDateRange custom = WxDateRangeUtils.custom(
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 31)
);
```

### 自定义企业微信接口

`WxApiClient` 会自动注入 `access_token`，发送 HTTP 请求，检查 HTTP 状态码，并校验企业微信 `errcode`。

```java
String response = wxApiClient.get(
        "https://qyapi.weixin.qq.com/cgi-bin/user/get",
        Map.of("userid", "zhangsan")
);

JsonNode json = wxApiClient.postJson(
        "https://qyapi.weixin.qq.com/cgi-bin/message/send",
        "{\"touser\":\"zhangsan\",\"msgtype\":\"text\"}"
);
```

### HR 花名册

配置 `wx.cp.hr.secret` 后即可启用 HR 花名册能力。

```java
JsonNode fields = wxHrRosterQueryUtil.getFieldSetting();
JsonNode staff = wxHrRosterQueryUtil.getStaffInfo("zhangsan");

WxHrRosterResult batch = wxHrRosterQueryUtil.getStaffInfoBatch(List.of("zhangsan", "lisi"));
Map<String, JsonNode> success = batch.staffInfo();
List<WxHrRosterFetchFailure> failures = batch.failures();

WxHrRosterResult all = wxHrRosterQueryUtil.getAllStaffInfo();
```

如果企业微信返回 `errcode=48002` 或 `errcode=60011`，请确认智慧人事应用已启用、`wx.cp.hr.secret` 属于该应用，并且管理员已授权可见范围。

### 考勤 / 打卡

`WxCheckinQueryUtil` 复用主 `WxCpService`，内部会自动按企业微信限制拆分 userId 和日期范围。

```java
WxDateRange range = WxDateRangeUtils.lastDays(30);
List<String> userIds = List.of("zhangsan", "lisi");

List<WxCheckinGroupVO> groups = wxCheckinQueryUtil.getCheckinGroups();
WxCheckinRecordResult records = wxCheckinQueryUtil.getCheckinRecords(range, userIds);
WxCheckinDayDataResult dayData = wxCheckinQueryUtil.getCheckinDayData(range, userIds);
WxCheckinMonthDataResult monthData = wxCheckinQueryUtil.getCheckinMonthData(range, userIds);
List<WxCheckinScheduleListItemVO> schedules = wxCheckinQueryUtil.getScheduleList(range, userIds);

List<WxCheckinExceptionItemVO> late = wxCheckinQueryUtil.getLatePersons(range, userIds);
WxAttendanceReportVO report = wxCheckinQueryUtil.getAttendanceReport(range, userIds);
```

可用异常快捷方法：

- `getLatePersons`
- `getEarlyLeavePersons`
- `getMissingCardPersons`
- `getAbsentPersons`
- `getLocationExceptions`
- `getDeviceExceptions`

## 可靠性策略

审批、HR 花名册、考勤工具内置：

- 大时间范围自动分段
- 企业微信限制下的 userId 分批
- 指数退避重试
- 可选的模块级限流
- 部分失败结果收集，便于补偿或告警

默认重试和限流参数偏保守。生产环境建议根据企业微信应用额度、任务规模和同步频率调整。

## 可观测性

starter 使用 SLF4J 记录关键操作：

- 初始化参数
- 查询完成和耗时
- 重试尝试
- 最终失败

## API 索引

### `WxContactQueryUtil`

- `getDepartments(Long departmentId)`
- `getAllDepartments()`
- `getDepartment(Long departmentId)`
- `getUsersByDepartment(Long departmentId, boolean fetchChild, Integer status)`
- `getAllUsers()`
- `getAllUsers(Integer status)`
- `getUsersSimpleByDepartment(Long departmentId, boolean fetchChild, Integer status)`
- `getAllUsersSimple()`
- `getAllUsersSimple(Integer status)`
- `getUserById(String userId)`

### `WxApprovalQueryUtil`

- `getApprovalSpNos(...)`
- `getApprovalSpNosByTemplateId(...)`
- `getApprovalDetails(...)`
- `getApprovalDetailsByTemplateId(...)`
- `getApprovalDetailsGroupByTemplateId(...)`
- `queryApprovalDetails(...)`
- `queryApprovalDetailsByTemplateId(...)`
- `getTemplates(...)`
- `getTemplateMap(...)`
- `getTemplateDetail(String templateId)`
- `getTemplateIdsBySpNos(...)`
- `getTemplateDetailsBySpNos(...)`

### `WxApiClient`

- `get(String url)`
- `get(String url, Map<String, String> params)`
- `post(String url, String body)`
- `post(String url, Map<String, String> params, String body)`
- `getJson(String url)`
- `getJson(String url, Map<String, String> params)`
- `postJson(String url, String body)`
- `postJson(String url, Map<String, String> params, String body)`

### `WxDateRangeUtils`

- `today()`
- `last3Days()`
- `lastDays(int days)`
- `currentMonth()`
- `currentYear()`
- `custom(Date startTime, Date endTime)`
- `custom(LocalDateTime startTime, LocalDateTime endTime)`
- `custom(LocalDate startDate, LocalDate endDate)`

### `WxHrRosterQueryUtil`

- `getFieldSetting()`
- `getStaffInfo(String userId)`
- `getStaffInfoBatch(Collection<String> userIds)`
- `getAllStaffInfo()`

### `WxCheckinQueryUtil`

- `getCheckinGroups()`
- `getCheckinRecords(...)`
- `getCheckinDayData(...)`
- `getCheckinMonthData(...)`
- `getScheduleList(...)`
- `getLatePersons(...)`
- `getEarlyLeavePersons(...)`
- `getMissingCardPersons(...)`
- `getAbsentPersons(...)`
- `getLocationExceptions(...)`
- `getDeviceExceptions(...)`
- `getAttendanceReport(...)`

## 本地构建

```bash
mvn clean test
```
