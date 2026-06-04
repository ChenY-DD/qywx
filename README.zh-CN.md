# qywx-wecom-spring-boot-starter

[English](./README.md) | [中文](./README.zh-CN.md)

企业微信通讯录、审批、花名册、考勤、消息推送、网页授权、智能机器人、批量导出 Spring Boot Starter。

本项目基于 `weixin-java-cp` 做二次封装，提供更贴近业务的工具类，让业务代码不用在每个服务里重复拼装底层 SDK 调用。

## 功能特性

- 自动装配 `WxCpService` 和常用工具类 Bean
- 通讯录查询：部门、成员、简要成员信息
- 审批查询：按时间分段、分页、重试、限流、失败收集
- HR 花名册查询：对接智慧人事 / 人事助手应用
- 考勤查询：打卡记录、日报、月报、排班、异常聚合
- 消息推送：文本 / Markdown / 文本卡片，支持撤回
- 网页授权登录：构建授权链接、`code` 换取用户身份与敏感详情
- 智能机器人：机器人增删改查、对话、发消息、重置会话
- 异步批量导出：成员 / 部门 / 标签导出，提交并轮询到下载链接
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
    <version>2.0.9</version>
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

# 异步批量导出
wx.cp.export.poll-interval-millis=2000
wx.cp.export.poll-timeout-millis=60000
wx.cp.export.max-poll-attempts=30
```

说明：

- `wx.cp.corp-secret` 是主应用 secret，用于通讯录、审批、考勤和通用 API 调用。
- `wx.cp.hr.secret` 必须是智慧人事 / 人事助手应用的专属 secret，通讯录或自建应用 secret 不能访问 HR 花名册接口。
- `requests-per-second=0` 表示该模块不启用内置限流。
- 异步导出 `exportAndWait` 会持续轮询直到任务完成；超过 `max-poll-attempts` 次或 `poll-timeout-millis` 超时（以先到者为准）仍未完成则抛 `QywxApiException`，任务返回失败状态（status=3）时立即失败。

## 自动装配的 Bean

引入依赖并完成最小配置后，starter 会自动注册：

- `WxCpService`
- `WxContactQueryUtil`
- `WxApprovalQueryUtil`
- `WxCheckinQueryUtil`
- `WxApiClient`
- `WxMessagePushUtil`
- `WxOauth2Util`
- `WxIntelligentRobotUtil`
- `WxExportUtil`

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

#### `WxApprovalDetailVO` 字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `spNo` | `String` | 审批单号 |
| `spName` | `String` | 审批名称 |
| `spStatus` | `String` | 审批状态枚举名（如 `AUDITING`、`PASSED`、`REJECTED`） |
| `templateId` | `String` | 审批模板 ID |
| `applyTime` | `Long` | 提交时间（Unix 秒） |
| `closeTime` | `Long` | 关闭时间（Unix 秒）；未结束时为 `null` |
| `closeLoopDurationSeconds` | `Long` | 提交 → 关闭耗时（秒）；未结束时为 `null` |
| `currentDurationSeconds` | `Long` | 提交 → 当前持续时长（秒） |
| `closed` | `Boolean` | 是否已结束（状态非 `AUDITING`） |
| `createdToday` | `Boolean` | 是否今天提交 |
| `overdueOneDay` | `Boolean` | 是否未结束超过一天 |
| `applicantUserId` | `String` | 申请人 userId |
| `applicantPartyId` | `String` | 申请人所属部门 ID |
| `formItems` | `List<FormItem>` | 解析后的表单项 |
| `nodes` | `List<Node>` | 完整审批流程节点 |
| `comments` | `List<CommentItem>` | 评论列表 |

`FormItem` —— `title`、`control`、`value`。复合控件（如离职 Resignation）会递归解析，嵌套子控件被展开进 `value`，而非丢弃。

`Node` —— 来自 `process_list.node_list` 的完整流程（含旧 `sp_record` 漏掉的抄送、办理节点）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `nodeType` | `Integer` | 1=审批、2=抄送、3=办理 |
| `spStatus` | `Integer` | 节点状态；抄送节点为 `null` |
| `apvRel` | `Integer` | 多人办理方式（1=会签、2=或签、3=依次审批）；抄送节点为 `null` |
| `details` | `List<NodeDetail>` | 子节点（审批人 / 抄送人） |

`NodeDetail` —— `approverUserId`、`speech`、`spYj`（子节点状态；抄送为 `null`）、`spTime`（Unix 秒）。

`CommentItem` —— `userId`、`content`、`commentTime`（Unix 秒）。

#### `WxApprovalProgressVO` —— 审批流转进度分析

`WxApprovalConverter.toProgress(detail)` 基于 `WxApprovalDetailVO` 的 `nodes` 派生流转时效视图：谁审批了、每个节点耗时多久、当前卡在谁、下一个审批人是谁。仅统计审批（`node_type=1`）与办理（`node_type=3`）节点，抄送节点不计入。

```java
WxApprovalDetailVO detail = details.get(0);
WxApprovalProgressVO progress = WxApprovalConverter.toProgress(detail);

for (WxApprovalProgressVO.NodeProgress node : progress.getNodeChain()) {
    // node.getApproverUserIds()、node.getDurationSeconds()、node.isBlocked()
}

WxApprovalProgressVO.CurrentBlock block = progress.getCurrentBlock(); // 已结束时为 null
if (block != null) {
    block.getBlockingUserIds();      // 卡在哪些人
    block.getWaitingSeconds();       // 在当前节点已等待多久
    block.getNextApproverUserIds();  // 下一个审批人
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `spNo` | `String` | 审批单号 |
| `spName` | `String` | 审批名称 |
| `spStatus` | `String` | 审批状态中文文本（审批中 / 已通过 / 已驳回 / 已撤销 / 通过后撤销 / 已删除 / 已支付） |
| `templateId` | `String` | 审批模板 ID |
| `applyTime` | `Long` | 提交时间，Unix 秒 |
| `closeTime` | `Long` | 关闭时间，Unix 秒；未结束为 `null` |
| `closeLoopDurationSeconds` | `Long` | 提交 → 关闭耗时，秒；未结束为 `null` |
| `submittedToNowSeconds` | `Long` | 提交 → 当前时长，秒 |
| `closed` | `Boolean` | 是否已结束 |
| `createdToday` | `Boolean` | 是否今天提交 |
| `overdueOneDay` | `Boolean` | 是否未结束超过一天 |
| `applicantUserId` | `String` | 申请人 userId |
| `applicantPartyId` | `String` | 申请人部门 ID |
| `nodeChain` | `List<NodeProgress>` | 审批 + 办理节点，按流程顺序 |
| `currentBlock` | `CurrentBlock` | 当前卡点；已结束时为 `null` |

所有枚举字段均输出**中文文本**而非原始数字。

`NodeProgress` —— `index`、`nodeType`（审批 / 办理）、`apvRel`（会签 / 或签 / 依次审批）、`spStatus`（审批中 / 同意 / 驳回 / …）、`approverUserIds`、`startTime`、`completeTime`、`durationSeconds`（单节点耗时；未完成为 `null`）、`blocked`、`pendingUserIds`。

`CurrentBlock` —— `nodeIndex`、`blockingUserIds`、`waitingSeconds`（now − 节点开始）、`nextApproverUserIds`。

节点完成判定按办理方式：或签任一审批人处理即完成（取最早处理时间）；会签 / 依次审批需全部处理（取最晚处理时间）。

审批按**提交时间**（`applyTime`）查询，而非按是否闭环。

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

### 消息推送

`WxMessagePushUtil` 注入主 `WxCpService`，封装企业微信**应用消息**的发送与撤回；所有企业微信受检异常统一转为非受检的 `QywxApiException`（含 `errCode` / `errMsg`）。

```java
// 1) 文本 / Markdown / 文本卡片便捷方法（接收人为成员 userId，多个用 | 分隔，@all 发给全部）
WxMessageSendResultVO r = wxMessagePushUtil.sendText("zhangsan|lisi", "构建完成 ✅");
wxMessagePushUtil.sendMarkdown("zhangsan", "**发布成功**\n> 版本 v2.0.9 已上线");
wxMessagePushUtil.sendTextCard("zhangsan",
        "服务器告警", "CPU 持续 95%，请尽快处理",
        "https://ops.example.com/alert/1", "查看详情");

// 2) 其他消息类型（图片/语音/视频/图文/文件/卡片等）：用 WxCpMessage builder 构造后调 send()
WxCpMessage file = WxCpMessage.FILE().toUser("zhangsan").mediaId("MEDIA_ID").build();
wxMessagePushUtil.send(file);

// 3) 撤回（msgId 来自发送结果）
wxMessagePushUtil.recall(r.getMsgId());
```

方法与参数：

- `send(WxCpMessage message)` —— 发送任意类型应用消息。
  - `message`：企业微信消息对象，用 `WxCpMessage.TEXT()/MARKDOWN()/TEXTCARD()/IMAGE()/VOICE()/VIDEO()/NEWS()/MPNEWS()/FILE()/TASKCARD()/TEMPLATECARD()` 等 builder 构造；接收人由 builder 的 `toUser`（成员 userId，`|` 分隔，`@all` 全部）/ `toParty`（部门 id，`|` 分隔）/ `toTag`（标签 id，`|` 分隔）设置；不设 `agentId` 时使用配置中的应用。
  - 返回 `WxMessageSendResultVO`：`msgId`（消息 ID，撤回用）、`responseCode`（撤回票据）、`invalidUser` / `invalidParty` / `invalidTag`（无效的成员 / 部门 / 标签，`|` 分隔）、`unlicensedUser`（未付费许可的成员）。
- `sendText(String toUser, String content)` —— 发送文本消息。
  - `toUser`：接收成员 userId，多个用 `|` 分隔，`@all` 发给全部。
  - `content`：文本内容。
- `sendMarkdown(String toUser, String content)` —— 发送 Markdown 消息。
  - `toUser`：同上。
  - `content`：Markdown 内容。
- `sendTextCard(String toUser, String title, String description, String url, String btnTxt)` —— 发送文本卡片消息。
  - `toUser`：同上。
  - `title`：卡片标题。
  - `description`：卡片描述。
  - `url`：点击卡片后跳转的链接。
  - `btnTxt`：按钮文字（如「查看详情」）。
- `recall(String msgId)` —— 撤回已发送的应用消息。
  - `msgId`：发送结果中的消息 ID。

### 网页授权登录

`WxOauth2Util` 注入主 `WxCpService`，封装企业微信**网页授权（OAuth2）**登录：构造授权链接 → 用 `code` 换取成员标识 → 用 `user_ticket` 换取成员敏感详情。`buildAuthorizationUrl` 为纯字符串拼接、不发起网络请求；另两个方法的受检异常转为 `QywxApiException`。

```java
// 第 1 步：构造授权链接，重定向用户浏览器
String authUrl = wxOauth2Util.buildAuthorizationUrl(
        "https://app.example.com/wecom/callback", // 授权后回调地址（需在后台配置可信域名）
        "snsapi_privateinfo",                      // 作用域：可进一步换取敏感信息
        "login_from_h5");                          // 自定义 state，回调时原样带回

// 第 2 步：在回调接口中用 code 换取成员标识
WxOauth2UserVO user = wxOauth2Util.getUserInfo(code);
String userId = user.getUserId();

// 第 3 步（可选，仅 snsapi_privateinfo）：用 user_ticket 换取成员敏感详情
if (user.getUserTicket() != null) {
    WxOauth2UserDetailVO detail = wxOauth2Util.getUserDetail(user.getUserTicket());
    String mobile = detail.getMobile();
}
```

方法与参数：

- `buildAuthorizationUrl(String redirectUri, String scope, String state)` —— 构造网页授权链接（纯拼接，不发请求）。
  - `redirectUri`：授权后回调地址，需在企业微信后台配置为可信域名。
  - `scope`：授权作用域。`snsapi_base`（静默授权，仅返回 userId）/ `snsapi_privateinfo`（手动授权，可返回 user_ticket 以获取敏感信息）。
  - `state`：重定向后原样带回的自定义状态参数，用于防 CSRF 或业务透传。
  - 返回：授权链接字符串，引导用户浏览器跳转。
- `getUserInfo(String code)` —— 用回调 `code` 换取成员标识。
  - `code`：网页授权回调地址上携带的一次性临时票据。
  - 返回 `WxOauth2UserVO`：`userId`（成员 UserId）、`openId`（非企业成员标识）、`userTicket`（成员票据，`snsapi_privateinfo` 时返回，用于下一步）、`deviceId`（设备号）、`externalUserId`（外部联系人 ID）、`expiresIn`（成员票据有效期，秒）。
- `getUserDetail(String userTicket)` —— 用成员票据换取敏感详情。
  - `userTicket`：`getUserInfo` 返回的成员票据。
  - 返回 `WxOauth2UserDetailVO`：`userId`、`name`（姓名）、`mobile`（手机号）、`gender`（性别，0/1/2）、`email`（邮箱）、`avatar`（头像 URL）、`qrCode`（个人二维码）、`address`（地址）、`bizMail`（企业邮箱）。

### 智能机器人

`WxIntelligentRobotUtil` 注入主 `WxCpService`，封装企业微信**智能机器人**的增删改查与对话能力。写操作直接使用 WxJava 请求对象入参，读操作转为本地 VO；受检异常转为 `QywxApiException`。

```java
// 创建机器人 → 返回 robotId
WxCpIntelligentRobotCreateRequest create = new WxCpIntelligentRobotCreateRequest();
create.setName("运维助手");
create.setDescription("处理告警与值班问询");
String robotId = wxIntelligentRobotUtil.createRobot(create);

// 查询详情
WxRobotVO robot = wxIntelligentRobotUtil.getRobot(robotId);

// 对话（同一 sessionId 在多轮之间传递以延续上下文）
WxCpIntelligentRobotChatRequest chat = new WxCpIntelligentRobotChatRequest();
chat.setRobotId(robotId);
chat.setUserid("zhangsan");
chat.setMessage("今天谁值班？");
WxRobotChatVO reply = wxIntelligentRobotUtil.chat(chat);
String answer = reply.getReply();

// 更新
WxCpIntelligentRobotUpdateRequest update = new WxCpIntelligentRobotUpdateRequest();
update.setRobotId(robotId);
update.setName("智能运维助手");
wxIntelligentRobotUtil.updateRobot(update);

// 重置会话 / 删除
wxIntelligentRobotUtil.resetSession(robotId, "single", reply.getSessionId());
wxIntelligentRobotUtil.deleteRobot(robotId);
```

方法与参数：

- `createRobot(WxCpIntelligentRobotCreateRequest req)` —— 创建智能机器人，返回新建机器人的 `robotId`。
  - `req.name`：机器人名称。
  - `req.description`：机器人描述。
  - `req.avatar`：机器人头像（素材 mediaId）。
- `updateRobot(WxCpIntelligentRobotUpdateRequest req)` —— 更新智能机器人。
  - `req.robotId`：目标机器人 ID（必填）。
  - `req.name` / `req.description` / `req.avatar`：要更新的名称 / 描述 / 头像。
  - `req.status`：机器人状态。
- `deleteRobot(String robotId)` —— 删除智能机器人。
  - `robotId`：机器人 ID。
- `getRobot(String robotId)` —— 获取智能机器人详情。
  - `robotId`：机器人 ID。
  - 返回 `WxRobotVO`：`robotId`、`name`（名称）、`description`（描述）、`avatar`（头像）、`status`（状态）、`createTime` / `updateTime`（创建 / 更新时间戳）。
- `chat(WxCpIntelligentRobotChatRequest req)` —— 与智能机器人对话。
  - `req.robotId`：机器人 ID。
  - `req.userid`：发起对话的成员 userId。
  - `req.message`：发送给机器人的消息内容。
  - `req.sessionId`：会话 ID，用于延续多轮上下文；首轮可留空，后续传入上一轮返回的 sessionId。
  - 返回 `WxRobotChatVO`：`reply`（机器人回复内容）、`sessionId`（会话 ID，下一轮带上）、`msgId`（消息 ID）。
- `sendMessage(WxCpIntelligentRobotSendMessageRequest req)` —— 通过智能机器人发送消息。
  - `req.robotId` / `req.userid` / `req.message` / `req.sessionId`：含义同上。
  - `req.msgId`：消息 ID。
  - 返回 `WxRobotSendResultVO`：`msgId`、`sessionId`。
- `resetSession(String robotId, String chatType, String chatId)` —— 重置机器人会话。
  - `robotId`：机器人 ID。
  - `chatType`：会话类型。
  - `chatId`：会话 ID。

### 异步批量导出

`WxExportUtil` 注入主 `WxCpService`，封装企业微信**异步批量导出**（成员简易信息 / 成员详细信息 / 部门 / 标签成员）。提供「提交 → 拿 jobId → 查结果」的分步 API，以及一站式「提交并轮询至完成 / 超时」的 `exportAndWait`。**边界止于拿到加密文件下载链接（url / size / md5），不负责下载与解密**。轮询参数由 `wx.cp.export.*` 配置（见上文「配置」一节）。

```java
WxCpExportRequest req = new WxCpExportRequest();
req.setEncodingAesKey("<43 位 EncodingAESKey>"); // 必填，企业微信用它加密导出文件
// req.setTagId(100);    // 导出标签成员时设置标签 id
// req.setBlockSize(...) // 可选，每块数据条数

// 方式一：一站式（推荐）——提交并轮询到完成；status=3（异常）或超时抛 QywxApiException
WxExportResultVO result = wxExportUtil.exportAndWait(WxExportType.USER, req);
for (WxExportDataVO data : result.getDataList()) {
    String url = data.getUrl();   // 加密文件下载链接（下载与解密由调用方自行处理）
    Integer size = data.getSize();
    String md5 = data.getMd5();
}

// 方式二：分步——自行掌控轮询节奏
String jobId = wxExportUtil.exportDepartment(req);
WxExportResultVO r = wxExportUtil.getResult(jobId);
if (r.getStatus() != null && r.getStatus() == 2) {   // 2 = 完成
    // 处理 r.getDataList()
}
```

方法与参数：

- `exportSimpleUser(WxCpExportRequest req)` —— 提交「导出成员（简易信息）」任务，返回 `jobId`。
- `exportUser(WxCpExportRequest req)` —— 提交「导出成员（详细信息）」任务，返回 `jobId`。
- `exportDepartment(WxCpExportRequest req)` —— 提交「导出部门」任务，返回 `jobId`。
- `exportTagUser(WxCpExportRequest req)` —— 提交「导出标签成员」任务，返回 `jobId`。
  - 以上 `req` 字段：`encodingAesKey`（必填，43 位，企业微信用于加密导出文件）、`blockSize`（可选，每块数据条数）、`tagId`（导出标签成员时的标签 id）。
- `getResult(String jobId)` —— 按 `jobId` 查询导出结果。
  - `jobId`：提交任务返回的任务号。
  - 返回 `WxExportResultVO`：`status`（1=处理中，2=完成，3=异常）、`dataList`（`List<WxExportDataVO>`，每项含 `url` 加密文件下载链接、`size` 文件字节大小、`md5` 文件摘要）。
- `exportAndWait(WxExportType type, WxCpExportRequest req)` —— 一站式提交并轮询至完成返回；`status=3` 立即抛 `QywxApiException`，超过 `maxPollAttempts` 次或 `pollTimeoutMillis` 总时长仍未完成则抛超时异常。
  - `type`：导出任务类型枚举 `WxExportType`（`SIMPLE_USER` / `USER` / `DEPARTMENT` / `TAG_USER`）。
  - `req`：同上。

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

## 本地构建

```bash
mvn clean test
```
