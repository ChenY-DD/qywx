# 企业微信考勤功能设计 — qywx-wecom-spring-boot-starter

> 状态：草案 · 设计稿
> 日期：2026-05-04
> 作者：CY
> 版权：CY

## 1. 目标与范围

为 `qywx-wecom-spring-boot-starter` 增加企业微信"打卡 / 考勤"模块，通过单一工具类 `WxCheckinQueryUtil` 提供：

- **考勤组**：列出企业全部考勤组配置（班次、地点、Wi-Fi、可用范围等）
- **打卡时间**：按时间 + 用户拉取原始打卡记录
- **迟到 / 早退 / 缺卡 / 旷工 / 地点异常 / 设备异常**：业务语义级查询，直接返回扁平的"人 + 日期 + 异常类型"列表
- **日报 / 月报**：完整的考勤日 / 月数据
- **排班**：用户排班信息

不在此次范围内：

- 写操作（添加/修改打卡规则、补卡审批）—— 当前模块定位是"查询"
- "全员"自动拉取 —— 由调用方主动传 userIds
- 按用户聚合的汇总 VO —— 第一版只输出"按 (user, date) 粒度"的扁平记录，聚合留给调用方

## 2. 架构总览

### 2.1 模式选型

复用主 `corp-secret`（与 `WxApprovalQueryUtil` 一致），不像 `WxHrRosterQueryUtil` 那样独立 secret。理由：

- 企业微信"打卡"在"应用管理"里是一个内置应用，但绝大多数自建应用已具备打卡接口权限
- WxJava 4.8.2 已通过 `WxCpCheckinService` 完整覆盖打卡接口，**无需走 `WxApiClient`**
- 减少配置项 —— 用户不再需要单独获取打卡 secret

### 2.2 与现有模块的关系

| 维度 | Approval | HR Roster | **Checkin (新增)** |
|---|---|---|---|
| Secret | 主 corp-secret | 独立 hr.secret | 主 corp-secret |
| HTTP 通道 | WxJava `getOaService()` | `WxApiClient` 直连 | WxJava `getCheckinService()` |
| 线程池 | `qywxApprovalQueryExecutor` | `qywxHrRosterExecutor` | `qywxCheckinExecutor` |
| 限流 | 内嵌 `SimpleRateLimiter` | 内嵌 `SimpleRateLimiter` | 内嵌 `SimpleRateLimiter` |
| 重试 | 指数退避 | 指数退避 | 指数退避 |
| 日期分段 | `segmentDays=29` | 不分段 | `segmentDays=30` |
| 用户分批 | 不分批 | 单人查询 | `userBatchSize=100` |
| 失败收集 | `WxApprovalDetailFetchFailure` | `WxHrRosterFetchFailure` | `WxCheckinFetchFailure` |
| 输出风格 | 强类型 VO | `JsonNode` | 强类型 VO |

## 3. 配置项

### 3.1 `WxCpProperties.Checkin`（新增嵌套类）

```java
public static class Checkin {
    private int segmentDays = 30;             // API 上限
    private int userBatchSize = 100;          // API 上限
    private int maxRetryAttempts = 3;
    private long retryBackoffMillis = 500L;
    private double requestsPerSecond = 0D;    // 0 = 不限流
    private int executorThreads = 8;
    // getters/setters
}
```

`WxCpProperties` 顶层增加 `private Checkin checkin = new Checkin();` + accessor。**没有 `secret` / `agentId` 字段**（复用主 corp-secret）。

### 3.2 用户 YAML 示例

```yaml
wx.cp:
  corp-id: ww1234abcd
  corp-secret: xxx          # 通讯录 + 审批 + 打卡 共用
  agent-id: 1000001
  hr:
    secret: yyy             # 智慧人事独立 secret
  checkin:                  # 仅调优参数
    segment-days: 30
    user-batch-size: 100
    requests-per-second: 5
    executor-threads: 8
```

不配 `wx.cp.checkin.*` 也能用，全部走默认值。

## 4. Auto-Configuration 接线

`QywxWecomAutoConfiguration` 增加：

```java
public static final String CHECKIN_EXECUTOR_BEAN_NAME = "qywxCheckinExecutor";

@Bean(name = CHECKIN_EXECUTOR_BEAN_NAME, destroyMethod = "shutdown")
@ConditionalOnMissingBean(name = CHECKIN_EXECUTOR_BEAN_NAME)
public ExecutorService qywxCheckinExecutor(WxCpProperties properties) {
    int threads = Math.max(1, properties.getCheckin().getExecutorThreads());
    return Executors.newFixedThreadPool(threads);
}

@Bean
@ConditionalOnMissingBean
@ConditionalOnBean(WxCpService.class)
public WxCheckinQueryUtil wxCheckinQueryUtil(
        WxCpService wxCpService,
        @Qualifier(CHECKIN_EXECUTOR_BEAN_NAME) ExecutorService checkinExecutor,
        WxCpProperties properties) {
    WxCpProperties.Checkin checkin = properties.getCheckin();
    WxCheckinQueryOptions options = new WxCheckinQueryOptions(
            checkin.getSegmentDays(),
            checkin.getUserBatchSize(),
            checkin.getMaxRetryAttempts(),
            checkin.getRetryBackoffMillis(),
            checkin.getRequestsPerSecond());
    return new WxCheckinQueryUtil(wxCpService, checkinExecutor, options);
}
```

`@ConditionalOnBean(WxCpService.class)` 已经隐含"主 corp-secret 必须配置"的前置条件。

## 5. 文件布局

### 5.1 新增（11 个 VO + 3 个 enum + 4 个 util 辅助类 + 1 个核心工具 + 1 个 converter）

```
org.cy.qywx.util/
├── WxCheckinQueryUtil.java              核心查询工具（公开）
├── WxCheckinConverter.java              WxJava bean → VO 静态转换
├── WxCheckinQueryOptions.java           record(segmentDays, userBatchSize, maxRetryAttempts, retryBackoffMillis, requestsPerSecond)
├── WxCheckinFetchFailure.java           单批次失败记录
├── WxCheckinRecordResult.java           record(records, failures)
├── WxCheckinDayDataResult.java          record(dayDataList, failures)
└── WxCheckinMonthDataResult.java        record(monthDataList, failures)

org.cy.qywx.vo/
├── WxCheckinGroupVO.java
├── WxCheckinScheduleVO.java
├── WxCheckinRangeVO.java
├── WxCheckinRecordVO.java
├── WxCheckinDayDataVO.java
├── WxCheckinMonthDataVO.java
├── WxCheckinExceptionVO.java
├── WxCheckinExceptionItemVO.java
├── WxCheckinLeaveVO.java
├── WxCheckinOvertimeVO.java
├── WxCheckinScheduleListItemVO.java
├── WxCheckinApprovalItemVO.java
├── WxAttendanceReportVO.java
└── enums/
    ├── WxCheckinTypeEnum.java
    ├── WxCheckinExceptionTypeEnum.java
    └── WxCheckinResultEnum.java
```

### 5.2 修改

```
org.cy.qywx.config/
├── WxCpProperties.java                  增加 Checkin 嵌套类
└── QywxWecomAutoConfiguration.java      增加 CHECKIN_EXECUTOR_BEAN_NAME / qywxCheckinExecutor / wxCheckinQueryUtil

CLAUDE.md                                 增加 Checkin 模块说明
README.md / README.zh-CN.md               增加 Checkin Query 章节及示例
pom.xml                                   1.0.1 → 1.0.2
```

### 5.3 编码规范

- **所有新增 class / 公开 method 加 Chinese Javadoc**：
  - 标准 Javadoc 标签：`@author CY`（Javadoc 原生支持）
  - 版权声明：在每个 class 的 Javadoc 描述末尾追加一行 `Copyright (c) CY`（Javadoc 没有 `@copyright` 标签，因此采用文字行而非标签形式）
- 字段注释使用单行 `// 中文说明`
- VO 全部使用 Lombok `@Data`（必要时 `@AllArgsConstructor` `@NoArgsConstructor`）
- 时间字段保持 `java.util.Date`（与项目现状一致），不引入 `LocalDate`

## 6. 公开 API 表

### 6.1 薄封装层

```java
// 考勤组
List<WxCheckinGroupVO> getCheckinGroups();

// 打卡记录
WxCheckinRecordResult getCheckinRecords(int type, Date start, Date end, Collection<String> userIds);
WxCheckinRecordResult getCheckinRecords(int type, WxDateRange range, Collection<String> userIds);
WxCheckinRecordResult getCheckinRecords(Date start, Date end, Collection<String> userIds);  // type=ALL
WxCheckinRecordResult getCheckinRecords(WxDateRange range, Collection<String> userIds);

// 日报
WxCheckinDayDataResult getCheckinDayData(Date start, Date end, Collection<String> userIds);
WxCheckinDayDataResult getCheckinDayData(WxDateRange range, Collection<String> userIds);

// 月报
WxCheckinMonthDataResult getCheckinMonthData(Date start, Date end, Collection<String> userIds);
WxCheckinMonthDataResult getCheckinMonthData(WxDateRange range, Collection<String> userIds);

// 排班
List<WxCheckinScheduleListItemVO> getScheduleList(Date start, Date end, Collection<String> userIds);
List<WxCheckinScheduleListItemVO> getScheduleList(WxDateRange range, Collection<String> userIds);
```

### 6.2 业务语义层

每个方法返回 `List<WxCheckinExceptionItemVO>`，按 (userId, date) 粒度。

```java
List<WxCheckinExceptionItemVO> getLatePersons(Date start, Date end, Collection<String> userIds);
List<WxCheckinExceptionItemVO> getLatePersons(WxDateRange range, Collection<String> userIds);

List<WxCheckinExceptionItemVO> getEarlyLeavePersons(...);
List<WxCheckinExceptionItemVO> getMissingCardPersons(...);
List<WxCheckinExceptionItemVO> getAbsentPersons(...);
List<WxCheckinExceptionItemVO> getLocationExceptions(...);
List<WxCheckinExceptionItemVO> getDeviceExceptions(...);
```

底层都是 `getCheckinDayData` + 按 `WxCheckinExceptionTypeEnum` 过滤。

### 6.3 组合报表层

```java
WxAttendanceReportVO getAttendanceReport(Date start, Date end, Collection<String> userIds);
WxAttendanceReportVO getAttendanceReport(WxDateRange range, Collection<String> userIds);
```

`WxAttendanceReportVO` 字段：

```java
@Data
public class WxAttendanceReportVO {
    private WxDateRange range;
    private int totalUsers;       // 入参 userIds 去重后数量
    private int reportedUsers;    // 实际返回了日报的人数
    private List<WxCheckinExceptionItemVO> late;
    private List<WxCheckinExceptionItemVO> earlyLeave;
    private List<WxCheckinExceptionItemVO> missingCard;
    private List<WxCheckinExceptionItemVO> absent;
    private List<WxCheckinExceptionItemVO> locationException;
    private List<WxCheckinExceptionItemVO> deviceException;
    private List<WxCheckinFetchFailure> failures;
}
```

只调一次 `getCheckinDayData`，按异常类型分桶，避免 6 次重复请求。

### 6.4 公开常量

```java
public static final int CHECKIN_TYPE_NORMAL = 1;     // 上下班打卡
public static final int CHECKIN_TYPE_OUTSIDE = 2;    // 外出打卡
public static final int CHECKIN_TYPE_ALL = 3;        // 全部
```

## 7. 内部分批 / 分段策略

### 7.1 二维拆分

```
日期分段（segmentDays，默认 30）
   ↓
用户分批（userBatchSize，默认 100）
   ↓
笛卡尔积 = 总调用次数
```

**例**：250 人 × 90 天 → 3 段 × 3 批 = 9 次 API 调用，全部并发。

### 7.2 执行流程

```
1. 入参校验：start、end 非 null 且 start <= end；userIds 非空
2. userIds 去重、过滤 blank
3. List<WxDateRange> segments = WxDateRangeUtils.segment(start, end, segmentDays)
4. List<List<String>> userBatches = partition(userIds, userBatchSize)
5. 笛卡尔积构造 List<Task>（每个 Task = 1 段日期 + 1 批用户）
6. 共享 SimpleRateLimiter 实例（限流跨所有 Task 生效）
7. CompletableFuture 并发执行，executor = qywxCheckinExecutor
8. allOf().join() 等全部完成
9. 合并：成功的合并到结果 List，失败的合并到 failures
10. 返回 *Result(successList, failures)
```

### 7.3 短路

- 去重后 userIds 为空 → 返回 `Result.empty()`，不发请求
- 单段单批 → 走同样的 `CompletableFuture` 路径，保持失败收集格式一致

### 7.4 失败粒度

```java
@Data @AllArgsConstructor
public class WxCheckinFetchFailure {
    private Date segmentStart;
    private Date segmentEnd;
    private List<String> userIdBatch;
    private int attempts;
    private String errorType;
    private String errorMessage;
}
```

一个批次失败不影响其他批次合并。调用方拿到部分结果 + 失败列表。

### 7.5 复用基础设施

- `WxDateRangeUtils.segment(...)` —— 已有，被 Approval 使用
- `SimpleRateLimiter` 内嵌静态类 —— 跟 Approval / HR 一样照搬，不抽公共类（YAGNI）
- 指数退避：base × 2^(attempt-1)，封顶 base × 32（与 Approval 一致）

### 7.6 日志规范

- `INFO`：开始 / 结束（含成功 / 失败计数 + 耗时）
- `WARN`：单批次重试、最终失败（前 10 个 userId）
- `DEBUG`：每次实际 API 调用 detail

## 8. VO 与枚举设计

### 8.1 枚举（`org.cy.qywx.vo.enums`）

```java
WxCheckinTypeEnum         {NORMAL=1, OUTSIDE=2, ALL=3}
WxCheckinExceptionTypeEnum {LATE=1, EARLY_LEAVE=2, MISSING_CARD=3, ABSENT=4,
                            LOCATION_EXCEPTION=5, DEVICE_EXCEPTION=6}
WxCheckinResultEnum       {NORMAL, TIME_EXCEPTION, LOCATION_EXCEPTION,
                            NOT_CHECKED_IN, INVALID, UNKNOWN}
```

每个枚举提供：
- `getCode()` / `getDescription()`
- `fromCode(int)` / `fromText(String)`，未匹配返回 `UNKNOWN` 或 null

### 8.2 核心 VO 字段

#### `WxCheckinGroupVO`（考勤组）

```
groupId, groupName, groupType,
checkInDayMonth, needPhoto, noteCanUseLocalPic,
allowCheckinOffWorkday, allowApplyOffWorkday,
schedules           List<WxCheckinScheduleVO>
ranges              List<WxCheckinRangeVO>
speWorkdays         List<Date>
speOffdays          List<Date>
syncHolidays
```

#### `WxCheckinRecordVO`（单条打卡）

```
userId, groupId, groupName, scheduleId,
checkinType         String  上班/下班/外出
checkinTime         Date
locationTitle, locationDetail, wifiName, wifiMac,
notes, mediaIds     List<String>
schedCheckinTime    Date
locationFlag        int    0=普通 1=外勤
exceptionType       WxCheckinResultEnum
deviceId, deviceName
```

#### `WxCheckinDayDataVO`（单人单日日报）

```
baseInfo:
    date            Date
    recordType
    name, departments, acctId, dayType
    ruleInfo: groupId, groupName, scheduleId, scheduleName, checkinTimes
summaryInfo:
    standardWorkSec, flexWorkSec, regularWorkSec
    earliestTime, latestTime
holidayInfos        List<WxCheckinLeaveVO>
exceptionInfos      List<WxCheckinExceptionVO>
otInfo:             WxCheckinOvertimeVO
spItems             List<WxCheckinApprovalItemVO>
```

#### `WxCheckinExceptionVO`（嵌在 DayData 内）

```
count               次数
duration            秒
exceptionType       WxCheckinExceptionTypeEnum
```

#### `WxCheckinExceptionItemVO`（业务方法返回的扁平行）

```
userId, name, departments,
date                Date
exceptionType       WxCheckinExceptionTypeEnum
exceptionCount      int
exceptionDurationMinutes  long      // 由 WeCom 返回的秒数 / 60 折算（向下取整）
```

#### `WxCheckinMonthDataVO`（单人单月）

```
baseInfo            同 DayData.baseInfo（date 是月份）
summaryInfo:
    workDays, excludedDays, workSec, standardWorkSec
exceptionInfos      List<WxCheckinExceptionVO>
holidayInfos        List<WxCheckinLeaveVO>
otInfo              WxCheckinOvertimeVO
spItems             List<WxCheckinApprovalItemVO>
```

#### 周边 VO

`WxCheckinScheduleVO` / `WxCheckinRangeVO` / `WxCheckinLeaveVO` / `WxCheckinOvertimeVO` / `WxCheckinScheduleListItemVO` / `WxCheckinApprovalItemVO` —— 字段直接对应官方 schema，使用 `@Data`。

### 8.3 转换器 `WxCheckinConverter`

静态方法，与 `WxApprovalConverter` 风格一致：

```java
public static WxCheckinGroupVO     fromGroup(WxCpCheckinOption.Group);
public static WxCheckinRecordVO    fromRecord(WxCpCheckinData);
public static WxCheckinDayDataVO   fromDayData(WxCpCheckinDayData);
public static WxCheckinMonthDataVO fromMonthData(WxCpCheckinMonthData);
public static WxCheckinScheduleListItemVO fromScheduleItem(WxCpCheckinSchedule);

/** 把日报数据展开为业务语义方法用的扁平异常行 */
public static List<WxCheckinExceptionItemVO> explodeExceptions(
        WxCheckinDayDataVO dayData,
        WxCheckinExceptionTypeEnum filter   // null = 不过滤
);
```

不允许任何 `me.chanjar.weixin.*` 类型从 `util` 包外泄。

## 9. 测试策略

### 9.1 测试矩阵

| 测试类 | 类型 | 主要 case |
|---|---|---|
| `WxCheckinConverterTest` | 纯单元 | Group/Record/DayData/MonthData 各 schema 字段映射；空字段、null 字段降级；异常类型 enum 映射 |
| `WxCheckinQueryUtilTest` | Mockito | 1) 输入校验；2) 90 天 → 3 段；3) 250 人 → 3 批；4) 90 天 × 250 人 → 9 次；5) 重试成功；6) 重试耗尽收集失败；7) 限流 RPS=2 时实际间隔 ≥500ms；8) 业务方法过滤正确；9) `getAttendanceReport` 单次拉取多桶；10) 空 userId 短路；11) 单段单批走 CompletableFuture |
| `QywxWecomAutoConfigurationTest` | ApplicationContextRunner | 1) 配 corp-secret 时 `WxCheckinQueryUtil` + `qywxCheckinExecutor` 注册；2) 不配时不注册；3) 自定义 Checkin properties 透传 |

### 9.2 测试夹具

- Mock `WxCpService.getCheckinService()`
- 直接构造 `WxCpCheckinDayData` / `WxCpCheckinData` 对象作为返回值
- 不做网络 mock

## 10. 文档变更

### 10.1 `CLAUDE.md`（QyWxC 内层）

在 "Two `WxCpService` instances" 章节后追加段落："Checkin 复用主 `WxCpService`（与 Approval 一致），通过 `WxCpService.getCheckinService()` 调用 WxJava。"

在 "Cross-cutting patterns" 章节追加："**用户分批**：`getcheckindata` / `getcheckin_daydata` 等接口单次 ≤100 userId，由 `WxCheckinQueryUtil` 内部按 `userBatchSize` 拆分后并发，调用方无感。"

### 10.2 `README.md` / `README.zh-CN.md`

新增 "Checkin Query" 章节，含：

- 配置示例
- `getCheckinGroups()` 示例
- `getLatePersons(...)` 示例
- `getAttendanceReport(...)` 示例

### 10.3 `pom.xml`

`<version>1.0.1</version>` → `<version>1.0.2</version>`（向后兼容新增功能）

## 11. 风险与权衡

| 项 | 决定 | 理由 |
|---|---|---|
| 复用 corp-secret 而非独立 secret | 复用 | 与 Approval 一致；多数自建应用已有打卡权限；少一项配置 |
| 未提供"自动全员"重载 | 不提供 | YAGNI；调用方已能从 `WxContactQueryUtil.getAllUsersSimple()` 拿全员；保留显式控制 |
| 未提供按用户聚合的 summary VO | 不提供 | YAGNI；扁平结构 + stream 一行可聚合；首版定位"取数" |
| 限流器内嵌而非抽公共类 | 内嵌 | 与 Approval / HR 一致；避免跨工具状态耦合 |
| `CHECKIN_TYPE_*` 常量公开 | 公开 | 避免调用方记忆魔法数字 1/2/3 |
| 异常字符串 `WxCheckinResultEnum` 用 enum 而非 String | enum + raw | enum 提供类型安全，未识别值落入 `UNKNOWN` 不丢数据 |
| 6 个业务语义方法是否合并到一个 enum 入参方法 | 不合并 | `getLatePersons` 比 `getExceptionPersons(LATE)` 更可读；性能上 `getAttendanceReport` 已经覆盖一次取多桶的场景 |

## 12. 后续可能的扩展（不在本次范围）

- 写操作：补卡审批提交、考勤组规则增删改
- "按用户聚合" 报表 VO
- "全员"自动拉取重载
- WebHook 接收打卡事件
