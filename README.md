# qywx-wecom-spring-boot-starter

[English](./README.md) | [中文](./README.zh-CN.md)

Spring Boot Starter for WeCom (WeChat Work) contact, approval, HR roster, and attendance queries.

It wraps `weixin-java-cp` with business-oriented utilities, so application code can query common WeCom data without assembling low-level SDK calls in every service.

## Features

- Spring Boot auto-configuration for `WxCpService` and utility beans
- Contact queries for departments and members
- Approval queries with date segmentation, paging, retry, rate limiting, and failure collection
- HR roster queries for the 智慧人事 / 人事助手 application
- Attendance queries for checkin records, reports, schedules, and exception buckets
- Message push: text / markdown / text card, with recall
- OAuth2 web login: build the authorization URL and exchange `code` for user identity and detail
- Intelligent robot: robot CRUD, chat, send message, and reset session
- Async bulk export: member / department / tag export, submit and poll to the download link
- Generic `WxApiClient` for custom WeCom APIs with automatic `access_token` injection
- SLF4J logs for initialization, retries, completion, and failures

## Requirements

- Java 21+
- Spring Boot 3.5.x
- Maven

## Installation

```xml
<dependency>
    <groupId>org.cy</groupId>
    <artifactId>qywx-wecom-spring-boot-starter</artifactId>
    <version>2.0.8</version>
</dependency>
```

## Configuration

Minimal configuration:

```properties
wx.cp.corp-id=wwxxxxxxxx
wx.cp.corp-secret=your-app-secret
wx.cp.agent-id=1000002
```

Full optional tuning:

```properties
# Approval query
wx.cp.approval.segment-days=29
wx.cp.approval.page-size=100
wx.cp.approval.max-retry-attempts=3
wx.cp.approval.retry-backoff-millis=300
wx.cp.approval.requests-per-second=0
wx.cp.approval.executor-threads=8

# HR roster. Requires the dedicated HR app secret.
wx.cp.hr.secret=your-hr-app-secret
wx.cp.hr.agent-id=1000003
wx.cp.hr.max-retry-attempts=5
wx.cp.hr.retry-backoff-millis=1000
wx.cp.hr.requests-per-second=10
wx.cp.hr.executor-threads=8

# Attendance / checkin
wx.cp.checkin.segment-days=30
wx.cp.checkin.user-batch-size=100
wx.cp.checkin.max-retry-attempts=3
wx.cp.checkin.retry-backoff-millis=500
wx.cp.checkin.requests-per-second=0
wx.cp.checkin.executor-threads=8

# Async bulk export
wx.cp.export.poll-interval-millis=2000
wx.cp.export.poll-timeout-millis=60000
wx.cp.export.max-poll-attempts=30
```

Notes:

- `wx.cp.corp-secret` is the primary app secret used by contact, approval, attendance, and generic API calls.
- `wx.cp.hr.secret` must be the dedicated HR app secret. The contact or self-built app secret cannot access HR roster APIs.
- `requests-per-second=0` disables the built-in limiter for that module.

## Auto-Configured Beans

After the dependency and minimal configuration are present, the starter registers:

- `WxCpService`
- `WxContactQueryUtil`
- `WxApprovalQueryUtil`
- `WxCheckinQueryUtil`
- `WxApiClient`
- `WxMessagePushUtil`
- `WxOauth2Util`
- `WxIntelligentRobotUtil`
- `WxExportUtil`

When `wx.cp.hr.secret` is configured, it also registers:

- `WxHrRosterQueryUtil`
- `qywxHrCpService`

If your application already defines a compatible `WxCpService` or one of the executor beans, auto-configuration backs off and reuses your bean.

## Quick Start

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

## Common Usage

### Contact

```java
List<WxDepartmentVO> departments = wxContactQueryUtil.getAllDepartments();
List<WxUserVO> users = wxContactQueryUtil.getAllUsers();
List<WxUserVO> simpleUsers = wxContactQueryUtil.getAllUsersSimple();
WxUserVO user = wxContactQueryUtil.getUserById("zhangsan");
```

### Approval

```java
WxDateRange range = WxDateRangeUtils.currentMonth();

List<String> spNos = wxApprovalQueryUtil.getApprovalSpNos(range);
List<WxApprovalDetailVO> details = wxApprovalQueryUtil.getApprovalDetails(range);

WxApprovalDetailQueryResult result = wxApprovalQueryUtil.queryApprovalDetails(range);
Map<String, List<WxApprovalDetailVO>> byTemplate = result.groupByTemplateId();
List<WxApprovalDetailFetchFailure> failures = result.failures();
```

Template helpers:

```java
List<WxApprovalTemplateVO> templates = wxApprovalQueryUtil.getTemplates(range);
WxCpOaApprovalTemplateResult template = wxApprovalQueryUtil.getTemplateDetail("template_id");
Map<String, String> templateIds = wxApprovalQueryUtil.getTemplateIdsBySpNos(spNos);
```

#### `WxApprovalDetailVO` Fields

| Field | Type | Description |
|-------|------|-------------|
| `spNo` | `String` | Approval number |
| `spName` | `String` | Approval name |
| `spStatus` | `String` | Status enum name (e.g. `AUDITING`, `PASSED`, `REJECTED`) |
| `templateId` | `String` | Template ID |
| `applyTime` | `Long` | Submit time (epoch seconds) |
| `closeTime` | `Long` | Close time (epoch seconds); `null` while open |
| `closeLoopDurationSeconds` | `Long` | Submit → close duration (seconds); `null` while open |
| `currentDurationSeconds` | `Long` | Submit → now duration (seconds) |
| `closed` | `Boolean` | Finished (status is not `AUDITING`) |
| `createdToday` | `Boolean` | Submitted today |
| `overdueOneDay` | `Boolean` | Open for more than one day |
| `applicantUserId` | `String` | Applicant userId |
| `applicantPartyId` | `String` | Applicant department ID |
| `formItems` | `List<FormItem>` | Parsed form controls |
| `nodes` | `List<Node>` | Full approval flow |
| `comments` | `List<CommentItem>` | Comments |

`FormItem` — `title`, `control`, `value`. Composite controls (e.g. Resignation / 离职) are resolved recursively, so nested sub-controls are expanded into `value` rather than dropped.

`Node` — the full flow from `process_list.node_list` (includes the CC and handler nodes that the legacy `sp_record` omits):

| Field | Type | Description |
|-------|------|-------------|
| `nodeType` | `Integer` | 1 = approval, 2 = CC, 3 = handler |
| `spStatus` | `Integer` | Node status; `null` for CC nodes |
| `apvRel` | `Integer` | Multi-approver mode (1 countersign / 2 or-sign / 3 sequential); `null` for CC nodes |
| `details` | `List<NodeDetail>` | Sub-nodes (approvers / CC recipients) |

`NodeDetail` — `approverUserId`, `speech`, `spYj` (sub-node status; `null` for CC), `spTime` (epoch seconds).

`CommentItem` — `userId`, `content`, `commentTime` (epoch seconds).

#### `WxApprovalProgressVO` — Flow Progress Analysis

`WxApprovalConverter.toProgress(detail)` derives a flow-timing view from a `WxApprovalDetailVO`'s `nodes`: who approved, how long each node took, where the approval is currently stuck, and who is next. Only approval (`node_type=1`) and handler (`node_type=3`) nodes are counted; CC nodes are excluded.

```java
WxApprovalDetailVO detail = details.get(0);
WxApprovalProgressVO progress = WxApprovalConverter.toProgress(detail);

for (WxApprovalProgressVO.NodeProgress node : progress.getNodeChain()) {
    // node.getApproverUserIds(), node.getDurationSeconds(), node.isBlocked()
}

WxApprovalProgressVO.CurrentBlock block = progress.getCurrentBlock(); // null when finished
if (block != null) {
    block.getBlockingUserIds();      // who it is stuck on
    block.getWaitingSeconds();       // how long it has waited at this node
    block.getNextApproverUserIds();  // who is next
}
```

| Field | Type | Description |
|-------|------|-------------|
| `spNo` | `String` | Approval number |
| `spName` | `String` | Approval name |
| `spStatus` | `String` | Status as Chinese text (审批中 / 已通过 / 已驳回 / 已撤销 / 通过后撤销 / 已删除 / 已支付) |
| `templateId` | `String` | Template ID |
| `applyTime` | `Long` | Submit time, epoch seconds |
| `closeTime` | `Long` | Close time, epoch seconds; `null` while open |
| `closeLoopDurationSeconds` | `Long` | Submit → close duration, seconds; `null` while open |
| `submittedToNowSeconds` | `Long` | Submit → now duration, seconds |
| `closed` | `Boolean` | Finished |
| `createdToday` | `Boolean` | Submitted today |
| `overdueOneDay` | `Boolean` | Open for more than one day |
| `applicantUserId` | `String` | Applicant userId |
| `applicantPartyId` | `String` | Applicant department ID |
| `nodeChain` | `List<NodeProgress>` | Approval + handler nodes in flow order |
| `currentBlock` | `CurrentBlock` | Current bottleneck; `null` when finished |

All enum fields are emitted as **Chinese text**, not raw numbers.

`NodeProgress` — `index`, `nodeType` (审批 / 办理), `apvRel` (会签 / 或签 / 依次审批), `spStatus` (审批中 / 同意 / 驳回 / …), `approverUserIds`, `startTime`, `completeTime`, `durationSeconds` (per node; `null` if unfinished), `blocked`, `pendingUserIds`.

`CurrentBlock` — `nodeIndex`, `blockingUserIds`, `waitingSeconds` (now − node start), `nextApproverUserIds`.

Per-node completion follows the approval mode: or-sign completes when any approver acts (earliest action time); countersign / sequential need all approvers (latest action time).

Approvals are queried by **submit time** (`applyTime`), not by open/closed state.

### Date Ranges

```java
WxDateRange today = WxDateRangeUtils.today();
WxDateRange last7Days = WxDateRangeUtils.lastDays(7);
WxDateRange month = WxDateRangeUtils.currentMonth();
WxDateRange custom = WxDateRangeUtils.custom(
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 31)
);
```

### Custom WeCom APIs

`WxApiClient` injects `access_token`, sends the HTTP request, checks HTTP status, and validates WeCom `errcode`.

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

### HR Roster

Enable HR roster support with `wx.cp.hr.secret`.

```java
JsonNode fields = wxHrRosterQueryUtil.getFieldSetting();
JsonNode staff = wxHrRosterQueryUtil.getStaffInfo("zhangsan");

WxHrRosterResult batch = wxHrRosterQueryUtil.getStaffInfoBatch(List.of("zhangsan", "lisi"));
Map<String, JsonNode> success = batch.staffInfo();
List<WxHrRosterFetchFailure> failures = batch.failures();

WxHrRosterResult all = wxHrRosterQueryUtil.getAllStaffInfo();
```

If WeCom returns `errcode=48002` or `errcode=60011`, verify that the HR application is enabled, `wx.cp.hr.secret` belongs to that application, and the administrator has authorized the visible scope.

### Attendance / Checkin

`WxCheckinQueryUtil` reuses the primary `WxCpService`. It handles WeCom limits internally by splitting user IDs and date ranges.

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

Available exception helpers:

- `getLatePersons`
- `getEarlyLeavePersons`
- `getMissingCardPersons`
- `getAbsentPersons`
- `getLocationExceptions`
- `getDeviceExceptions`

## Reliability

Approval, HR roster, and attendance utilities include:

- Date segmentation for large ranges
- User batching where WeCom imposes per-request user limits
- Retry with exponential backoff
- Optional module-level rate limiting
- Failure result objects for partial failures

Default retry and limiter values are intentionally conservative. Tune them to match your WeCom app quota and job size.

## Observability

The starter logs key operations with SLF4J:

- initialization options
- query completion and duration
- retry attempts
- final failures

## API Index

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

### `WxMessagePushUtil`

- `send(WxCpMessage message)`
- `sendText(String toUser, String content)`
- `sendMarkdown(String toUser, String content)`
- `sendTextCard(String toUser, String title, String description, String url, String btnTxt)`
- `recall(String msgId)`

### `WxOauth2Util`

- `buildAuthorizationUrl(String redirectUri, String scope, String state)`
- `getUserInfo(String code)`
- `getUserDetail(String userTicket)`

### `WxIntelligentRobotUtil`

- `createRobot(WxCpIntelligentRobotCreateRequest req)`
- `updateRobot(WxCpIntelligentRobotUpdateRequest req)`
- `deleteRobot(String robotId)`
- `getRobot(String robotId)`
- `chat(WxCpIntelligentRobotChatRequest req)`
- `sendMessage(WxCpIntelligentRobotSendMessageRequest req)`
- `resetSession(String robotId, String chatType, String chatId)`

### `WxExportUtil`

- `exportSimpleUser(WxCpExportRequest req)`
- `exportUser(WxCpExportRequest req)`
- `exportDepartment(WxCpExportRequest req)`
- `exportTagUser(WxCpExportRequest req)`
- `getResult(String jobId)`
- `exportAndWait(WxExportType type, WxCpExportRequest req)`

## Local Build

```bash
mvn clean test
```
