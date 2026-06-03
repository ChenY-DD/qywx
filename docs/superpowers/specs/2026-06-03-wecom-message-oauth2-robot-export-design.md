# 设计文档：企业微信消息推送 / 网页授权 / 智能机器人 / 异步导出封装

- 日期：2026-06-03
- 状态：已确认（待写实现计划）
- 涉及版本：WxJava `weixin-java-cp` 4.8.2.B

## 1. 背景与目标

现有 starter 已封装通讯录、审批、智慧人事、打卡四类**查询**能力。本次新增 4 个 WxJava 能力模块的封装：

1. **消息推送** `WxCpMessageService`（写操作）
2. **网页授权登录** `WxCpOAuth2Service`（认证流程）
3. **智能机器人** `WxCpIntelligentRobotService`（CRUD + 对话）
4. **异步批量导出** `WxCpExportService`（提交→轮询）

目标：在保持现有装配/风格一致的前提下，把这 4 个模块封装为可注入的工具 Bean，放入 `util` 下各自的子包以区分职责。

## 2. 设计决策（已确认）

| 决策点 | 结论 |
|---|---|
| 封装风格 | **混合**：读取类（OAuth2 用户信息、导出结果、机器人查询/对话响应）转本地 VO；写入类（发消息、机器人请求）直接用 WxJava 入参对象 |
| 包结构 | 每模块一子包：`util.message` / `util.oauth2` / `util.robot` / `util.export`；现有查询 util 原位不动 |
| VO/Converter 位置 | 放各自子包内（不并入全局 `vo` 包），保持子包内聚 |
| 错误处理 | 捕获 WxJava 受检异常 `WxErrorException`，包装为自定义**非受检** `QywxApiException`（含 errCode/errMsg）抛出；不向外泄漏 `me.chanjar.*` 类型 |
| service 注入 | 4 个 Util 全部注入 **primary** `WxCpService`（与现有 contact/approval/checkin 一致） |
| 线程池 | 不引入 executor（均为单次或单任务轮询的同步操作） |
| 消息便捷方法 | 仅 `sendText` / `sendMarkdown` / `sendTextCard`（入参均为 String）；图文/文件/卡片等复杂类型走原始 `send(WxCpMessage)` |
| 导出边界 | 一站式轮询到拿到**加密文件下载链接**为止；**不下载、不解密**（属下一层，YAGNI） |

## 3. 非目标（YAGNI）

- 消息：不封装 `getStatistics`、互联企业消息、家校消息。
- OAuth2：不封装 `getTfaInfo`、`getSchoolUserInfo`、`getAuthUserInfo`。
- 导出：不下载加密文件、不做 `encodingAesKey` 解密、不解析明文数据。
- 不为这些模块新增并发线程池。

## 4. 包结构

```
org.cy.qywx
├── exception/
│   └── QywxApiException                 # RuntimeException：errCode + errMsg
├── util/                                # 现有查询 util 原位不动
│   ├── message/
│   │   ├── WxMessagePushUtil
│   │   ├── WxMessageConverter
│   │   └── WxMessageSendResultVO
│   ├── oauth2/
│   │   ├── WxOauth2Util
│   │   ├── WxOauth2Converter
│   │   ├── WxOauth2UserVO
│   │   └── WxOauth2UserDetailVO
│   ├── robot/
│   │   ├── WxIntelligentRobotUtil
│   │   ├── WxRobotConverter
│   │   ├── WxRobotVO
│   │   ├── WxRobotChatVO
│   │   └── WxRobotSendResultVO
│   └── export/
│       ├── WxExportUtil
│       ├── WxExportType                 # enum：SIMPLE_USER/USER/DEPARTMENT/TAG_USER
│       ├── WxExportConverter
│       ├── WxExportQueryOptions         # 轮询参数
│       ├── WxExportResultVO
│       └── WxExportDataVO
```

## 5. 各模块详细设计

### 5.1 message —— `WxMessagePushUtil`

```java
WxMessageSendResultVO send(WxCpMessage message)
WxMessageSendResultVO sendText(String toUser, String content)
WxMessageSendResultVO sendMarkdown(String toUser, String content)
WxMessageSendResultVO sendTextCard(String toUser, String title, String description, String url, String btnTxt)
void recall(String msgId)
```

- 便捷方法内部用 `WxCpMessage.TEXT()/MARKDOWN()/TEXTCARD()` builder 构建；不显式设置 agentId，依赖 `WxCpConfigStorage` 中的 agentId（即 primary service 的 agent）。
- `WxMessageSendResultVO` 字段（映射 `WxCpMessageSendResult`）：`msgId`、`responseCode`、`invalidUser`、`invalidParty`、`invalidTag`、`unlicensedUser`（均 String）。
- `send` 调用 `WxCpService.getMessageService().send(...)`；`recall` 调用 `recall(msgId)`。

### 5.2 oauth2 —— `WxOauth2Util`

```java
String buildAuthorizationUrl(String redirectUri, String scope, String state)
WxOauth2UserVO getUserInfo(String code)
WxOauth2UserDetailVO getUserDetail(String userTicket)
```

- `WxOauth2UserVO`（映射 `WxCpOauth2UserInfo`）：`userId`、`openId`、`userTicket`、`deviceId`、`externalUserId`、`expiresIn`。
- `WxOauth2UserDetailVO`（映射 `WxCpUserDetail`）：`userId`、`name`、`mobile`、`gender`、`email`、`avatar`、`qrCode`、`address`、`bizMail`。
- `buildAuthorizationUrl` 为纯字符串拼接，不发起网络调用，不抛 `QywxApiException`。

### 5.3 robot —— `WxIntelligentRobotUtil`

```java
String createRobot(WxCpIntelligentRobotCreateRequest req)        // 返回 robotId
void   updateRobot(WxCpIntelligentRobotUpdateRequest req)
void   deleteRobot(String robotId)
WxRobotVO getRobot(String robotId)
WxRobotChatVO chat(WxCpIntelligentRobotChatRequest req)
WxRobotSendResultVO sendMessage(WxCpIntelligentRobotSendMessageRequest req)
void   resetSession(String robotId, String chatId, String chatType)
```

- 写入方法直接用 WxJava 请求入参（`*Request`），符合混合风格。
- `createRobot` 的响应仅含 `robotId`，故直接返回 `String`。
- `WxRobotVO`（映射 `WxCpIntelligentRobot`）：`robotId`、`name`、`description`、`avatar`、`status`、`createTime`、`updateTime`。
- `WxRobotChatVO`（映射 `WxCpIntelligentRobotChatResponse`）：`reply`、`sessionId`、`msgId`。
- `WxRobotSendResultVO`（映射 `WxCpIntelligentRobotSendMessageResponse`）：`msgId`、`sessionId`。

### 5.4 export —— `WxExportUtil`

```java
// 底层：提交返 jobId
String exportSimpleUser(WxCpExportRequest req)
String exportUser(WxCpExportRequest req)
String exportDepartment(WxCpExportRequest req)
String exportTagUser(WxCpExportRequest req)
WxExportResultVO getResult(String jobId)
// 一站式：提交并轮询到完成/超时
WxExportResultVO exportAndWait(WxExportType type, WxCpExportRequest req)
```

- `WxExportType` 枚举映射四个底层提交方法。
- `WxExportResultVO`（映射 `WxCpExportResult`）：`status`（Integer）、`dataList`（`List<WxExportDataVO>`）。
- `WxExportDataVO`（映射 `WxCpExportResult.ExportData`）：`url`、`size`（Integer）、`md5`。
- **一站式轮询逻辑**：`exportAndWait` 先按 `type` 提交拿 jobId，然后每隔 `pollIntervalMillis` 调用一次 `getResult`，直到 `status` 表示完成即返回；超过 `maxPollAttempts` 次或累计超过 `pollTimeoutMillis` 仍未完成则抛 `QywxApiException`（导出超时）。轮询间隔用 `LockSupport.parkNanos`（与现有限流实现一致）。
- `status` 语义以官方为准（WeCom 导出通常 1=处理中、2=完成、3=异常）；具体完成判定值在实现时确认并以常量表达，不散落魔法数字。
- `WxCpExportRequest` 的 `encodingAesKey` 为必填（导出文件加密用），由调用方提供。

## 6. 横切设计

### 6.1 错误处理 —— `org.cy.qywx.exception.QywxApiException`

```java
public class QywxApiException extends RuntimeException {
    private final Integer errCode;
    private final String errMsg;
    // 由 WxErrorException 构造：errCode = e.getError().getErrorCode() 等
}
```

- 每个 Util 中调用 WxJava 的方法捕获 `WxErrorException`，转抛 `QywxApiException`。
- 非受检：调用方无需强制 try-catch；不泄漏 `me.chanjar.*`。

### 6.2 配置扩展 —— `WxCpProperties`

新增内部类 `Export`，前缀 `wx.cp.export`：

| 属性 | 默认值 | 说明 |
|---|---|---|
| `poll-interval-millis` | 2000 | 轮询间隔 |
| `poll-timeout-millis` | 60000 | 轮询总超时 |
| `max-poll-attempts` | 30 | 最大轮询次数 |

message/oauth2/robot 无新增配置。`WxExportQueryOptions` 为承载上述三项的不可变小对象（与现有 `WxCheckinQueryOptions` 风格一致）。

### 6.3 Bean 装配 —— `QywxWecomAutoConfiguration`

新增 4 个 `@Bean`，均 `@ConditionalOnMissingBean` + `@ConditionalOnBean(WxCpService.class)`，注入 primary `WxCpService`：

```java
WxMessagePushUtil wxMessagePushUtil(WxCpService wxCpService)
WxOauth2Util wxOauth2Util(WxCpService wxCpService)
WxIntelligentRobotUtil wxIntelligentRobotUtil(WxCpService wxCpService)
WxExportUtil wxExportUtil(WxCpService wxCpService, WxCpProperties properties)   // 注入轮询配置
```

## 7. 测试策略

沿用 Mockito + JUnit5 + AssertJ，mock `WxCpService.getXxxService()`：

- `WxMessagePushUtilTest`：验证 send/便捷方法转发正确的 `WxCpMessage`、结果转 VO、`WxErrorException` → `QywxApiException`。
- `WxOauth2UtilTest`：验证 url 拼接、getUserInfo/getUserDetail 转 VO。
- `WxIntelligentRobotUtilTest`：验证 7 个方法转发与 VO 转换。
- `WxExportUtilTest`：底层方法转发；**一站式**用 mock 序列「先返回处理中、再返回完成」验证轮询成功，mock「一直处理中」验证超时抛 `QywxApiException`。
- 各 Converter 纯单元测试（null 安全、字段映射）。

## 8. 兼容性与影响

- 纯新增：不改动现有类与包，无破坏性变更。
- 新增依赖：无（均使用已有 `weixin-java-cp` 能力）。
- README / README.zh-CN 增补 4 个模块的简介与配置说明（实现阶段一并处理）。
