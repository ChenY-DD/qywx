# qywx-wecom-spring-boot-starter

[English](./README.md) | [中文](./README.zh-CN.md)

Spring Boot Starter for WeCom (WeChat Work) contact, approval, HR roster, attendance, message push, OAuth2 login, intelligent robot, and bulk export operations.

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
    <version>2.0.12</version>
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
- The async `exportAndWait` polls until the job finishes; it throws `QywxApiException` if the job is still unfinished after `max-poll-attempts` or `poll-timeout-millis` (whichever comes first), and fails fast when the job returns a failed status (status=3).

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

// lastDays(n) aligns to whole calendar days — n full days INCLUDING today.
// On 2026-06-04, lastDays(7) → 2026-05-29 00:00:00 .. 2026-06-04 23:59:59.999
WxDateRange last7Days = WxDateRangeUtils.lastDays(7);

// Rolling window keeping the exact clock time on both ends: [now - 7 days, now]
WxDateRange rolling = WxDateRangeUtils.lastDays(7, false);

// n full days EXCLUDING today, up to yesterday.
// On 2026-06-04, lastDaysBeforeToday(7) → 2026-05-28 00:00:00 .. 2026-06-03 23:59:59.999
WxDateRange last7DaysBeforeToday = WxDateRangeUtils.lastDaysBeforeToday(7);

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
// All members — pass only a date range; userIds are fetched automatically (throws WxErrorException).
WxCheckinRecordResult allRecords = wxCheckinQueryUtil.getAllCheckinRecords(range);
WxCheckinDayDataResult dayData = wxCheckinQueryUtil.getCheckinDayData(range, userIds);
WxCheckinMonthDataResult monthData = wxCheckinQueryUtil.getCheckinMonthData(range, userIds);
List<WxCheckinScheduleListItemVO> schedules = wxCheckinQueryUtil.getScheduleList(range, userIds);

List<WxCheckinExceptionItemVO> late = wxCheckinQueryUtil.getLatePersons(range, userIds);
WxAttendanceReportVO report = wxCheckinQueryUtil.getAttendanceReport(range, userIds);
```

**Query all members at once** — `getAllCheckinRecords` needs only a date range and fetches every active user id internally (it reuses the segment/batch fan-out, so it scales to large orgs):

```java
// All members, last 7 whole days — no need to collect userIds yourself.
WxCheckinRecordResult all = wxCheckinQueryUtil.getAllCheckinRecords(WxDateRangeUtils.lastDays(7));
for (WxCheckinRecordVO r : all.records()) {
    String userId = r.getUserId();
    // ... other fields
}
all.failures().forEach(f -> { /* per-batch failures, if any */ });

// Optional: restrict the checkin type (CHECKIN_TYPE_OUTSIDE / _NORMAL / _ALL)
WxCheckinRecordResult outside = wxCheckinQueryUtil.getAllCheckinRecords(
        WxCheckinQueryUtil.CHECKIN_TYPE_OUTSIDE, WxDateRangeUtils.lastDays(7));
```

Available exception helpers:

- `getLatePersons`
- `getEarlyLeavePersons`
- `getMissingCardPersons`
- `getAbsentPersons`
- `getLocationExceptions`
- `getDeviceExceptions`

### Message Push

`WxMessagePushUtil` injects the primary `WxCpService` and wraps WeCom **application message** sending and recall. All WeCom checked exceptions are converted to the unchecked `QywxApiException` (carrying `errCode` / `errMsg`).

```java
// 1) Text / Markdown / TextCard helpers (recipients are member userIds, '|'-separated, '@all' for everyone)
WxMessageSendResultVO r = wxMessagePushUtil.sendText("zhangsan|lisi", "Build finished ✅");
wxMessagePushUtil.sendMarkdown("zhangsan", "**Release succeeded**\n> v2.0.12 is live");
wxMessagePushUtil.sendTextCard("zhangsan",
        "Server alert", "CPU stuck at 95%, please act",
        "https://ops.example.com/alert/1", "Details");

// 2) Other message types (image/voice/video/news/file/card...): build a WxCpMessage, then send()
WxCpMessage file = WxCpMessage.FILE().toUser("zhangsan").mediaId("MEDIA_ID").build();
wxMessagePushUtil.send(file);

// 3) Recall (msgId comes from the send result)
wxMessagePushUtil.recall(r.getMsgId());
```

Methods and parameters:

- `send(WxCpMessage message)` —— Send any type of application message.
  - `message`: the WeCom message, built with `WxCpMessage.TEXT()/MARKDOWN()/TEXTCARD()/IMAGE()/VOICE()/VIDEO()/NEWS()/MPNEWS()/FILE()/TASKCARD()/TEMPLATECARD()`; recipients are set on the builder via `toUser` (member userIds, '|'-separated, '@all' for everyone) / `toParty` (department ids) / `toTag` (tag ids); when `agentId` is not set, the configured app is used.
  - Returns `WxMessageSendResultVO`: `msgId` (message id, used for recall), `responseCode` (recall token), `invalidUser` / `invalidParty` / `invalidTag` (invalid members / departments / tags, '|'-separated), `unlicensedUser` (members without an active interface license).
- `sendText(String toUser, String content)` —— Send a text message.
  - `toUser`: recipient member userIds, '|'-separated, '@all' for everyone.
  - `content`: text content.
- `sendMarkdown(String toUser, String content)` —— Send a Markdown message.
  - `toUser`: as above.
  - `content`: Markdown content.
- `sendTextCard(String toUser, String title, String description, String url, String btnTxt)` —— Send a text card message.
  - `toUser`: as above.
  - `title`: card title.
  - `description`: card description.
  - `url`: link opened when the card is clicked.
  - `btnTxt`: button text (e.g. "Details").
- `recall(String msgId)` —— Recall a previously sent application message.
  - `msgId`: the message id from the send result.

### OAuth2 Web Login

`WxOauth2Util` injects the primary `WxCpService` and wraps WeCom **OAuth2 web login**: build the authorization URL → exchange `code` for the member identity → exchange `user_ticket` for sensitive member details. `buildAuthorizationUrl` is pure string concatenation (no network call); the other two convert checked exceptions to `QywxApiException`.

```java
// Step 1: build the authorization URL and redirect the user's browser
String authUrl = wxOauth2Util.buildAuthorizationUrl(
        "https://app.example.com/wecom/callback", // redirect URI (must be a trusted domain in the console)
        "snsapi_privateinfo",                      // scope: allows fetching sensitive info later
        "login_from_h5");                          // custom state, returned as-is in the callback

// Step 2: in the callback endpoint, exchange code for the member identity
WxOauth2UserVO user = wxOauth2Util.getUserInfo(code);
String userId = user.getUserId();

// Step 3 (optional, snsapi_privateinfo only): exchange user_ticket for sensitive details
if (user.getUserTicket() != null) {
    WxOauth2UserDetailVO detail = wxOauth2Util.getUserDetail(user.getUserTicket());
    String mobile = detail.getMobile();
}
```

Methods and parameters:

- `buildAuthorizationUrl(String redirectUri, String scope, String state)` —— Build the web authorization URL (pure concatenation, no request).
  - `redirectUri`: post-authorization callback URL; must be configured as a trusted domain in the WeCom console.
  - `scope`: authorization scope. `snsapi_base` (silent, returns userId only) / `snsapi_privateinfo` (manual, returns a user_ticket for fetching sensitive info).
  - `state`: custom state returned as-is after redirect; use it for CSRF protection or business pass-through.
  - Returns: the authorization URL string to redirect the user's browser to.
- `getUserInfo(String code)` —— Exchange the callback `code` for the member identity.
  - `code`: the one-time temporary ticket carried on the callback URL.
  - Returns `WxOauth2UserVO`: `userId` (member UserId), `openId` (identity for non-members), `userTicket` (member ticket, returned with `snsapi_privateinfo`, used in the next step), `deviceId`, `externalUserId` (external contact id), `expiresIn` (member ticket TTL in seconds).
- `getUserDetail(String userTicket)` —— Exchange the member ticket for sensitive details.
  - `userTicket`: the member ticket returned by `getUserInfo`.
  - Returns `WxOauth2UserDetailVO`: `userId`, `name`, `mobile`, `gender` (0/1/2), `email`, `avatar` (URL), `qrCode` (personal QR code), `address`, `bizMail` (corporate mailbox).

### Intelligent Robot

`WxIntelligentRobotUtil` injects the primary `WxCpService` and wraps WeCom **intelligent robot** CRUD plus conversation. Write operations take WxJava request objects directly; read operations are converted to local VOs; checked exceptions become `QywxApiException`.

```java
// Create a robot → returns robotId
WxCpIntelligentRobotCreateRequest create = new WxCpIntelligentRobotCreateRequest();
create.setName("Ops Assistant");
create.setDescription("Handles alerts and on-call questions");
String robotId = wxIntelligentRobotUtil.createRobot(create);

// Get details
WxRobotVO robot = wxIntelligentRobotUtil.getRobot(robotId);

// Chat (carry the same sessionId across turns to keep context)
WxCpIntelligentRobotChatRequest chat = new WxCpIntelligentRobotChatRequest();
chat.setRobotId(robotId);
chat.setUserid("zhangsan");
chat.setMessage("Who is on call today?");
WxRobotChatVO reply = wxIntelligentRobotUtil.chat(chat);
String answer = reply.getReply();

// Update
WxCpIntelligentRobotUpdateRequest update = new WxCpIntelligentRobotUpdateRequest();
update.setRobotId(robotId);
update.setName("Smart Ops Assistant");
wxIntelligentRobotUtil.updateRobot(update);

// Reset session / delete
wxIntelligentRobotUtil.resetSession(robotId, "single", reply.getSessionId());
wxIntelligentRobotUtil.deleteRobot(robotId);
```

Methods and parameters:

- `createRobot(WxCpIntelligentRobotCreateRequest req)` —— Create a robot; returns the new `robotId`.
  - `req.name`: robot name.
  - `req.description`: robot description.
  - `req.avatar`: robot avatar (media id).
- `updateRobot(WxCpIntelligentRobotUpdateRequest req)` —— Update a robot.
  - `req.robotId`: target robot id (required).
  - `req.name` / `req.description` / `req.avatar`: fields to update.
  - `req.status`: robot status.
- `deleteRobot(String robotId)` —— Delete a robot.
  - `robotId`: robot id.
- `getRobot(String robotId)` —— Get robot details.
  - `robotId`: robot id.
  - Returns `WxRobotVO`: `robotId`, `name`, `description`, `avatar`, `status`, `createTime` / `updateTime` (timestamps).
- `chat(WxCpIntelligentRobotChatRequest req)` —— Chat with the robot.
  - `req.robotId`: robot id.
  - `req.userid`: userId of the member starting the chat.
  - `req.message`: message sent to the robot.
  - `req.sessionId`: session id for multi-turn context; leave empty on the first turn, then pass the sessionId returned previously.
  - Returns `WxRobotChatVO`: `reply` (robot reply), `sessionId` (carry into the next turn), `msgId`.
- `sendMessage(WxCpIntelligentRobotSendMessageRequest req)` —— Send a message through the robot.
  - `req.robotId` / `req.userid` / `req.message` / `req.sessionId`: as above.
  - `req.msgId`: message id.
  - Returns `WxRobotSendResultVO`: `msgId`, `sessionId`.
- `resetSession(String robotId, String chatType, String chatId)` —— Reset a robot session.
  - `robotId`: robot id.
  - `chatType`: chat type.
  - `chatId`: chat id.

### Async Export

`WxExportUtil` injects the primary `WxCpService` and wraps WeCom **async bulk export** (simple user / detailed user / department / tag members). It offers a stepwise API (submit → get jobId → poll result) and the one-shot `exportAndWait` (submit and poll until finished / timeout). After polling returns the encrypted download link (url / size / md5), use `downloadAndDecrypt(...)` to download, verify integrity, and decrypt the file back to plaintext. Polling is configured via `wx.cp.export.*` (see Configuration above).

```java
// Generate the EncodingAESKey once and keep it (reuse the same key to decrypt); the starter provides a helper
String encodingAesKey = WxExportUtil.generateEncodingAesKey();
WxCpExportRequest req = new WxCpExportRequest();
req.setEncodingAesKey(encodingAesKey); // required; WeCom uses it to encrypt the export file
// req.setTagId(100);    // set when exporting tag members
// req.setBlockSize(...) // optional, records per block

// Option A: one-shot (recommended) — submit and poll until finished; status=3 (failed) or timeout throws QywxApiException
WxExportResultVO result = wxExportUtil.exportAndWait(WxExportType.USER, req);

// Download + verify size/md5 + decrypt every shard back to plaintext (usually JSON).
// IMPORTANT: pass the SAME encodingAesKey used at submit time, or the file cannot be decrypted.
List<byte[]> shards = wxExportUtil.downloadAndDecrypt(result, encodingAesKey);
for (byte[] plain : shards) {
    String json = new String(plain, StandardCharsets.UTF_8);   // data_0.json, data_1.json, ...
}

// Option B: stepwise — control the polling cadence yourself
String jobId = wxExportUtil.exportDepartment(req);
WxExportResultVO r = wxExportUtil.getResult(jobId);
if (r.getStatus() != null && r.getStatus() == 2) {   // 2 = finished
    // handle r.getDataList()
}
```

Methods and parameters:

- `exportSimpleUser(WxCpExportRequest req)` —— Submit a "simple user" export; returns `jobId`.
- `exportUser(WxCpExportRequest req)` —— Submit a "detailed user" export; returns `jobId`.
- `exportDepartment(WxCpExportRequest req)` —— Submit a "department" export; returns `jobId`.
- `exportTagUser(WxCpExportRequest req)` —— Submit a "tag members" export; returns `jobId`.
  - `req` fields: `encodingAesKey` (required, 43 chars, used by WeCom to encrypt the file), `blockSize` (optional, records per block), `tagId` (tag id when exporting tag members).
- `getResult(String jobId)` —— Query an export result by `jobId`.
  - `jobId`: the job number returned on submit.
  - Returns `WxExportResultVO`: `status` (1=processing, 2=finished, 3=failed), `dataList` (`List<WxExportDataVO>`, each with `url` encrypted download link, `size` bytes, `md5`).
- `exportAndWait(WxExportType type, WxCpExportRequest req)` —— Submit and poll until finished; throws `QywxApiException` immediately on `status=3`, or a timeout exception after `maxPollAttempts` polls or `pollTimeoutMillis` total.
  - `type`: export type enum `WxExportType` (`SIMPLE_USER` / `USER` / `DEPARTMENT` / `TAG_USER`).
  - `req`: as above.
- `downloadAndDecrypt(WxExportResultVO result, String encodingAesKey)` —— Download every shard in `dataList`, verify size/md5 (of the ciphertext) and decrypt; returns `List<byte[]>` of plaintext (order matches `dataList`). Empty list when `dataList` is empty.
- `downloadAndDecrypt(WxExportDataVO data, String encodingAesKey)` —— Same for a single shard; returns the plaintext `byte[]`.
- `decrypt(byte[] encrypted, String encodingAesKey)` (static) —— Pure AES-256-CBC / PKCS#7 decryption (key = `Base64Decode(encodingAesKey + "=")`, IV = first 16 bytes); use when you already hold the ciphertext.
  - `encodingAesKey` must be the **same** 43-char key used at submit time, otherwise the file cannot be decrypted. `size`/`md5` are verified against the **ciphertext** (download integrity), not the plaintext.

**Where `encodingAesKey` comes from**: you generate and keep it yourself — WeCom does not issue it or hand it out in the console. It is a fixed **43-character** string from `a-z` / `A-Z` / `0-9` (62 chars), i.e. the Base64 encoding of an AES key; `Base64.getDecoder().decode(encodingAesKey + "=")` yields the 32-byte (AES-256) key. WeCom encrypts the export file with it and returns an encrypted download link, which you then decrypt with the **same** key. Generate one and keep it fixed for reuse; you may also reuse the EncodingAESKey configured under "App → Receive Messages / API receive" in the WeCom console (identical format). Pass that same key to `downloadAndDecrypt(...)` and the starter downloads the link, verifies size/md5, and decrypts the file back to plaintext.

```java
// The starter provides WxExportUtil.generateEncodingAesKey() to create a 43-char key (generate once, keep it, reuse to decrypt)
String encodingAesKey = WxExportUtil.generateEncodingAesKey();
```

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
- `lastDays(int days, boolean alignToWholeDay)`
- `lastDaysBeforeToday(int days)`
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
- `getAllCheckinRecords(WxDateRange range)`
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
- `downloadAndDecrypt(WxExportResultVO result, String encodingAesKey)`
- `downloadAndDecrypt(WxExportDataVO data, String encodingAesKey)`
- `decrypt(byte[] encrypted, String encodingAesKey)` (static)
- `generateEncodingAesKey()` (static)

## Local Build

```bash
mvn clean test
```
