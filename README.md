# qywx-wecom-spring-boot-starter

[English](./README.md) | [中文](./README.zh-CN.md)

A Spring Boot Starter for WeCom (WeChat Work) contact and approval querying.

## Project Overview

`qywx-wecom-spring-boot-starter` is a secondary wrapper built on top of `weixin-java-cp`.

It provides stable, business-friendly APIs for common querying scenarios and hides low-level SDK details.

Code characteristic:

- With only 2 lines of business code, you can query WeCom users, departments, and approvals
- No SDK assembly code is needed in the business layer
- Suitable for rapid integration in internal systems, admin platforms, and approval-sync jobs

What this starter provides:

- Contact querying utilities for departments and users
- Approval querying utilities with segmented paging, retry, rate limiting, and failure collection
- Generic API client for custom WeCom API calls with automatic token injection
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

# Optional approval query tuning (with optimized defaults for rate limiting)
wx.cp.approval.segment-days=29
wx.cp.approval.page-size=100
wx.cp.approval.max-retry-attempts=5
wx.cp.approval.retry-backoff-millis=1000
wx.cp.approval.requests-per-second=10
wx.cp.approval.executor-threads=8

# Optional HR roster (智慧人事 / 人事助手) — requires the dedicated HR app secret
wx.cp.hr.secret=your-hr-app-secret
wx.cp.hr.max-retry-attempts=5
wx.cp.hr.retry-backoff-millis=1000
wx.cp.hr.requests-per-second=10
wx.cp.hr.executor-threads=8
```

## How To Use

After adding the dependency and configuration, the starter auto-registers these beans:

- `WxCpService`
- `WxContactQueryUtil`
- `WxApprovalQueryUtil`
- `WxApiClient`
- `WxHrRosterQueryUtil` (only when `wx.cp.hr.secret` is configured)

You can inject them directly in your Spring Boot application:

```java
@Service
public class DemoService {

    private final WxContactQueryUtil wxContactQueryUtil;
    private final WxApprovalQueryUtil wxApprovalQueryUtil;
    private final WxApiClient wxApiClient;

    public DemoService(
            WxContactQueryUtil wxContactQueryUtil,
            WxApprovalQueryUtil wxApprovalQueryUtil,
            WxApiClient wxApiClient
    ) {
        this.wxContactQueryUtil = wxContactQueryUtil;
        this.wxApprovalQueryUtil = wxApprovalQueryUtil;
        this.wxApiClient = wxApiClient;
    }

    public List<WxUserVO> listUsers() throws WxErrorException {
        return wxContactQueryUtil.getAllUsers();
    }

    public WxApprovalDetailQueryResult queryApprovals(Date startTime, Date endTime) throws WxErrorException {
        return wxApprovalQueryUtil.queryApprovalDetails(startTime, endTime);
    }

    public JsonNode callCustomApi() throws Exception {
        return wxApiClient.getJson("https://qyapi.weixin.qq.com/cgi-bin/user/get", 
                Map.of("userid", "zhangsan"));
    }
}
```

Minimal examples:

```java
// Contact queries
List<WxUserVO> users = wxContactQueryUtil.getAllUsers();
List<WxDepartmentVO> departments = wxContactQueryUtil.getAllDepartments();
```

```java
// Approval queries with date range utilities
WxApprovalDetailQueryResult result = wxApprovalQueryUtil.queryApprovalDetails(WxDateRangeUtils.last3Days());
Map<String, List<WxApprovalDetailVO>> grouped = result.groupByTemplateId();

// Query approvals for current month
List<WxApprovalDetailVO> monthlyApprovals = wxApprovalQueryUtil.getApprovalDetails(WxDateRangeUtils.currentMonth());

// Query approvals by template ID for today
List<String> spNos = wxApprovalQueryUtil.getApprovalSpNosByTemplateId("template_id", WxDateRangeUtils.today());
```

```java
// Call any WeCom API with automatic access_token injection
String response = wxApiClient.get("https://qyapi.weixin.qq.com/cgi-bin/user/get", 
        Map.of("userid", "zhangsan"));

JsonNode json = wxApiClient.postJson("https://qyapi.weixin.qq.com/cgi-bin/message/send",
        "{\"touser\":\"zhangsan\",\"msgtype\":\"text\"}");
```

Typical usage:

- Use `WxContactQueryUtil` for department and user queries
- Use `WxDateRangeUtils` for convenient date range creation (today, last3Days, currentMonth, currentYear, custom)
- Use `WxApprovalQueryUtil#getApprovalSpNos(...)` when you only need approval numbers
- Use `WxApprovalQueryUtil#queryApprovalDetails(...)` when you need both successful details and failed records
- Use `WxApprovalQueryUtil#getApprovalDetailsGroupByTemplateId(...)` when grouping by template is needed
- Use `WxApiClient` for custom WeCom API calls not covered by the utility classes

**Date Range Examples:**

```java
// Query approvals for the last 3 days
wxApprovalQueryUtil.queryApprovalDetails(WxDateRangeUtils.last3Days());

// Query approvals for current month
wxApprovalQueryUtil.getApprovalDetails(WxDateRangeUtils.currentMonth());

// Query approvals for today
wxApprovalQueryUtil.getApprovalSpNos(WxDateRangeUtils.today());

// Query approvals for the last 7 days
wxApprovalQueryUtil.getApprovalDetails(WxDateRangeUtils.lastDays(7));

// Custom date range
wxApprovalQueryUtil.queryApprovalDetails(
    WxDateRangeUtils.custom(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))
);
```

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
- `List<String> getApprovalSpNos(WxDateRange dateRange)`
- `List<String> getApprovalSpNosByTemplateId(String templateId, Date startTime, Date endTime)`
- `List<String> getApprovalSpNosByTemplateId(String templateId, WxDateRange dateRange)`
- `List<WxApprovalDetailVO> getApprovalDetails(Date startTime, Date endTime)`
- `List<WxApprovalDetailVO> getApprovalDetails(WxDateRange dateRange)`
- `List<WxApprovalDetailVO> getApprovalDetailsByTemplateId(String templateId, Date startTime, Date endTime)`
- `Map<String, List<WxApprovalDetailVO>> getApprovalDetailsGroupByTemplateId(Date startTime, Date endTime)`
- `WxApprovalDetailQueryResult queryApprovalDetails(Date startTime, Date endTime)`
- `WxApprovalDetailQueryResult queryApprovalDetails(WxDateRange dateRange)`
- `WxApprovalDetailQueryResult queryApprovalDetailsByTemplateId(String templateId, Date startTime, Date endTime)`
- `WxApprovalDetailQueryResult queryApprovalDetailsByTemplateId(String templateId, WxDateRange dateRange)`
- `List<WxApprovalTemplateVO> getTemplates(Date startTime, Date endTime)`
- `List<WxApprovalTemplateVO> getTemplates(WxDateRange dateRange)`
- `Map<String, WxApprovalTemplateVO> getTemplateMap(Date startTime, Date endTime)`
- `Map<String, WxApprovalTemplateVO> getTemplateMap(WxDateRange dateRange)`
- `WxCpOaApprovalTemplateResult getTemplateDetail(String templateId)`
- `Map<String, String> getTemplateIdsBySpNos(String... spNos)`
- `Map<String, String> getTemplateIdsBySpNos(Collection<String> spNos)`
- `Map<String, WxCpOaApprovalTemplateResult> getTemplateDetailsBySpNos(String... spNos)`
- `Map<String, WxCpOaApprovalTemplateResult> getTemplateDetailsBySpNos(Collection<String> spNos)`

### WxDateRangeUtils

Convenient date range creation utilities:

- `WxDateRange today()` - From today 00:00:00 to now
- `WxDateRange last3Days()` - Last 3 days from now
- `WxDateRange lastDays(int days)` - Last N days from now
- `WxDateRange currentMonth()` - From 1st of current month to now
- `WxDateRange currentYear()` - From Jan 1st of current year to now
- `WxDateRange custom(Date startTime, Date endTime)` - Custom range with Date
- `WxDateRange custom(LocalDateTime startTime, LocalDateTime endTime)` - Custom range with LocalDateTime
- `WxDateRange custom(LocalDate startDate, LocalDate endDate)` - Custom range with LocalDate (end date extends to 23:59:59)

**Examples:**

```java
// Query last 3 days
wxApprovalQueryUtil.getApprovalDetails(WxDateRangeUtils.last3Days());

// Query current month
wxApprovalQueryUtil.getApprovalDetails(WxDateRangeUtils.currentMonth());

// Query custom range
wxApprovalQueryUtil.queryApprovalDetails(
    WxDateRangeUtils.custom(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))
);
```

### WxApiClient

Generic client for calling any WeCom API with automatic `access_token` injection:

- `String get(String url)` - GET request
- `String get(String url, Map<String, String> params)` - GET request with query parameters
- `String post(String url, String body)` - POST request with JSON body
- `String post(String url, Map<String, String> params, String body)` - POST request with query parameters and JSON body
- `JsonNode getJson(String url)` - GET request returning parsed JSON
- `JsonNode getJson(String url, Map<String, String> params)` - GET request with parameters returning parsed JSON
- `JsonNode postJson(String url, String body)` - POST request returning parsed JSON
- `JsonNode postJson(String url, Map<String, String> params, String body)` - POST request with parameters returning parsed JSON

**Usage Example:**

```java
// Simple GET request
String response = wxApiClient.get("https://qyapi.weixin.qq.com/cgi-bin/user/get", 
        Map.of("userid", "zhangsan"));

// POST request with JSON body
String postResponse = wxApiClient.post("https://qyapi.weixin.qq.com/cgi-bin/message/send",
        "{\"touser\":\"zhangsan\",\"msgtype\":\"text\",\"text\":{\"content\":\"Hello\"}}");

// Parse response as JSON
JsonNode json = wxApiClient.getJson("https://qyapi.weixin.qq.com/cgi-bin/department/list");
int errcode = json.get("errcode").asInt();
```

The `WxApiClient` automatically:
- Injects `access_token` into the URL
- Handles HTTP communication
- Validates response status codes
- Checks WeCom API error codes (`errcode`)

### WxHrRosterQueryUtil

HR roster (花名册) queries against the 智慧人事 / 人事助手 application. **Requires the dedicated HR app secret** (`wx.cp.hr.secret`); the contact secret cannot access HR endpoints.

- `JsonNode getFieldSetting()` — fetch roster field configuration (`/cgi-bin/hr/get_field_setting`)
- `JsonNode getStaffInfo(String userId)` — fetch a single employee's roster (`/cgi-bin/hr/get_staff_info`)
- `WxHrRosterResult getStaffInfoBatch(Collection<String> userIds)` — concurrent batch fetch with rate limiting and exponential backoff retry
- `WxHrRosterResult getAllStaffInfo()` — fetch the full company roster (resolves user IDs via `WxContactQueryUtil` first, then batch-fetches)

`WxHrRosterResult` exposes:
- `Map<String, JsonNode> staffInfo()` — successful results keyed by `userId`
- `List<WxHrRosterFetchFailure> failures()` — per-user failures with attempt count and error info

**Usage Example:**

```java
@Service
public class RosterService {
    private final WxHrRosterQueryUtil wxHrRosterQueryUtil;

    public RosterService(WxHrRosterQueryUtil wxHrRosterQueryUtil) {
        this.wxHrRosterQueryUtil = wxHrRosterQueryUtil;
    }

    public WxHrRosterResult fullRoster() throws WxErrorException {
        return wxHrRosterQueryUtil.getAllStaffInfo();
    }

    public JsonNode oneEmployee(String userId) throws Exception {
        return wxHrRosterQueryUtil.getStaffInfo(userId);
    }
}
```

> If you receive `errcode=48002` or `errcode=60011`, verify that the 智慧人事 app is enabled, the configured secret belongs to that app, and the admin has authorized the visible scope.

## Rate Limiting & Retry Strategy

The starter includes built-in rate limiting and exponential backoff retry to prevent hitting WeCom API limits:

- **Default rate limit**: 10 requests/second (configurable via `wx.cp.approval.requests-per-second`)
- **Retry attempts**: 5 times with exponential backoff (1s → 2s → 4s → 8s → 16s)
- **Segment-based querying**: Splits large date ranges into 29-day segments to avoid pagination limits

These defaults are optimized for production use and help prevent `429 Too Many Requests` errors.

## Observability

The starter provides comprehensive observability through logging and metrics:

### Logging (SLF4J)

All key operations are logged with appropriate levels:

- **INFO**: Query completion, initialization, summary statistics
- **WARN**: Retry attempts, failed detail fetches
- **ERROR**: Final failures after all retries exhausted
- **DEBUG**: Detailed request/response information, rate limiter stats

Example logs:
```
INFO  WxApprovalQueryUtil - WxApprovalQueryUtil initialized with options: segmentDays=29, pageSize=100, maxRetryAttempts=5, retryBackoffMillis=1000, requestsPerSecond=10.0
INFO  WxApprovalQueryUtil - Approval spNo query completed: totalSpNos=150, totalApiCalls=2, durationMs=1250
INFO  WxApprovalQueryUtil - Fetching details for 150 approval spNos
WARN  WxApprovalQueryUtil - Failed to fetch approval detail for spNo=sp-123, attempt 1/5: rate limit exceeded
INFO  WxApprovalQueryUtil - Approval details query completed: totalDetails=148, totalFailures=2, durationMs=15320
```

### Metrics (Micrometer)

When `MeterRegistry` is available (e.g., with Spring Boot Actuator), the starter automatically records:

**Timers** (with `success` tag):
- `wx.approval.spnos.query` - Approval spNo query duration
- `wx.approval.details.query` - Approval details query duration
- `wx.approval.detail.fetch` - Individual detail fetch duration
- `wx.api.request` - API request duration (with `method` tag: GET/POST)

**Counters**:
- `wx.approval.spnos.total` - Total spNos fetched
- `wx.approval.details.success` - Successful detail fetches
- `wx.approval.details.failure` - Failed detail fetches
- `wx.approval.detail.retry` - Retry attempts
- `wx.api.http.error` - HTTP errors
- `wx.api.wechat.error.{errcode}` - WeCom API errors by error code

**Example with Spring Boot Actuator:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Metrics will be automatically exported to `/actuator/prometheus` endpoint.

## Checkin Query

`WxCheckinQueryUtil` queries WeCom attendance data. It reuses the primary `WxCpService` (no separate secret) and exposes:

- `getCheckinGroups()` — list all attendance group configurations
- `getCheckinRecords(start, end, userIds)` — raw checkin records (use `CHECKIN_TYPE_NORMAL/OUTSIDE/ALL` for finer control)
- `getCheckinDayData(start, end, userIds)` — daily report
- `getCheckinMonthData(start, end, userIds)` — monthly report
- `getScheduleList(start, end, userIds)` — schedule list
- Business-level helpers: `getLatePersons / getEarlyLeavePersons / getMissingCardPersons / getAbsentPersons / getLocationExceptions / getDeviceExceptions`
- One-shot aggregate: `getAttendanceReport(start, end, userIds)` returning all six exception buckets in one API call

The starter hides WeCom's `≤100 userIds / call` and `≤30 days / call` limits via internal 2-D batching with retry and rate-limit knobs under `wx.cp.checkin.*`.

```yaml
wx.cp:
  corp-id: ww1234abcd
  corp-secret: xxx
  agent-id: 1000001
  checkin:
    segment-days: 30
    user-batch-size: 100
    requests-per-second: 5
    executor-threads: 8
```

```java
@Autowired
private WxCheckinQueryUtil checkin;

List<WxCheckinGroupVO> groups = checkin.getCheckinGroups();

WxDateRange april = ...;          // build a 30-day range
List<String> userIds = ...;        // explicit list — no auto-fetch
List<WxCheckinExceptionItemVO> late = checkin.getLatePersons(april, userIds);

WxAttendanceReportVO report = checkin.getAttendanceReport(april, userIds);
report.getLate();
report.getAbsent();
report.getFailures();
```

## Local Build

```bash
mvn clean test
```
