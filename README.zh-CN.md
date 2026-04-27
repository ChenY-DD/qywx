# qywx-wecom-spring-boot-starter

[English](./README.md) | [中文](./README.zh-CN.md)

企业微信通讯录与审批查询 Spring Boot Starter。

## 项目说明

`qywx-wecom-spring-boot-starter` 是基于 `weixin-java-cp` 的二次封装。

目标是为业务提供稳定的查询 API，避免在业务层直接处理底层 SDK 细节。

代码特点：

- 业务代码只要 2 行，就可以完成企业微信人员、部门、审批查询
- 业务层不需要再拼装底层 SDK 调用
- 适合后台系统、内部平台、审批同步任务快速接入

本 starter 提供：

- 通讯录查询工具（部门、成员）
- 审批查询工具（分段分页、失败重试、请求限流、失败收集）
- 通用 API 客户端，支持调用任意企业微信接口并自动注入 token
- Spring Boot 自动装配与 `@ConfigurationProperties`

本 starter 不做：

- 不替代 `weixin-java-cp`
- 不覆盖企业微信全部 API

## 依赖引入

```xml
<dependency>
    <groupId>org.cy</groupId>
    <artifactId>qywx-wecom-spring-boot-starter</artifactId>
    <version>1.0.1</version>
</dependency>
```

## 配置项

```properties
wx.cp.corp-id=your-corp-id
wx.cp.corp-secret=your-corp-secret
wx.cp.agent-id=1000002

# 审批查询可选参数（已优化默认值以应对限流）
wx.cp.approval.segment-days=29
wx.cp.approval.page-size=100
wx.cp.approval.max-retry-attempts=5
wx.cp.approval.retry-backoff-millis=1000
wx.cp.approval.requests-per-second=10
wx.cp.approval.executor-threads=8
```

## 如何使用

引入依赖并完成配置后，starter 会自动注册以下 Bean：

- `WxCpService`
- `WxContactQueryUtil`
- `WxApprovalQueryUtil`
- `WxApiClient`

业务代码里可以直接注入使用：

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

最小示例：

```java
// 通讯录查询
List<WxUserVO> users = wxContactQueryUtil.getAllUsers();
List<WxDepartmentVO> departments = wxContactQueryUtil.getAllDepartments();
```

```java
// 使用日期范围工具进行审批查询
WxApprovalDetailQueryResult result = wxApprovalQueryUtil.queryApprovalDetails(WxDateRangeUtils.last3Days());
Map<String, List<WxApprovalDetailVO>> grouped = result.groupByTemplateId();

// 查询当月审批
List<WxApprovalDetailVO> monthlyApprovals = wxApprovalQueryUtil.getApprovalDetails(WxDateRangeUtils.currentMonth());

// 按模板 ID 查询今天的审批单号
List<String> spNos = wxApprovalQueryUtil.getApprovalSpNosByTemplateId("template_id", WxDateRangeUtils.today());
```

```java
// 调用任意企业微信 API，自动注入 access_token
String response = wxApiClient.get("https://qyapi.weixin.qq.com/cgi-bin/user/get", 
        Map.of("userid", "zhangsan"));

JsonNode json = wxApiClient.postJson("https://qyapi.weixin.qq.com/cgi-bin/message/send",
        "{\"touser\":\"zhangsan\",\"msgtype\":\"text\"}");
```

常见使用方式：

- 查询部门、成员时使用 `WxContactQueryUtil`
- 使用 `WxDateRangeUtils` 快速创建日期范围（今天、近三天、当月、当年、自定义）
- 只需要审批单号时使用 `WxApprovalQueryUtil#getApprovalSpNos(...)`
- 既要审批详情又要拿到失败记录时使用 `WxApprovalQueryUtil#queryApprovalDetails(...)`
- 需要按模板分组时使用 `WxApprovalQueryUtil#getApprovalDetailsGroupByTemplateId(...)`
- 调用工具类未覆盖的企业微信 API 时使用 `WxApiClient`

**日期范围示例：**

```java
// 查询近 3 天的审批
wxApprovalQueryUtil.queryApprovalDetails(WxDateRangeUtils.last3Days());

// 查询当月审批
wxApprovalQueryUtil.getApprovalDetails(WxDateRangeUtils.currentMonth());

// 查询今天的审批
wxApprovalQueryUtil.getApprovalSpNos(WxDateRangeUtils.today());

// 查询近 7 天的审批
wxApprovalQueryUtil.getApprovalDetails(WxDateRangeUtils.lastDays(7));

// 自定义日期范围
wxApprovalQueryUtil.queryApprovalDetails(
    WxDateRangeUtils.custom(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))
);
```

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

便捷的日期范围创建工具：

- `WxDateRange today()` - 今天 00:00:00 到当前时刻
- `WxDateRange last3Days()` - 当前时刻向前 3 天
- `WxDateRange lastDays(int days)` - 当前时刻向前 N 天
- `WxDateRange currentMonth()` - 本月 1 号 00:00:00 到当前时刻
- `WxDateRange currentYear()` - 今年 1 月 1 日 00:00:00 到当前时刻
- `WxDateRange custom(Date startTime, Date endTime)` - 使用 Date 自定义范围
- `WxDateRange custom(LocalDateTime startTime, LocalDateTime endTime)` - 使用 LocalDateTime 自定义范围
- `WxDateRange custom(LocalDate startDate, LocalDate endDate)` - 使用 LocalDate 自定义范围（结束日期自动扩展到 23:59:59）

**示例：**

```java
// 查询近 3 天
wxApprovalQueryUtil.getApprovalDetails(WxDateRangeUtils.last3Days());

// 查询当月
wxApprovalQueryUtil.getApprovalDetails(WxDateRangeUtils.currentMonth());

// 自定义范围
wxApprovalQueryUtil.queryApprovalDetails(
    WxDateRangeUtils.custom(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))
);
```

### WxApiClient

通用客户端，用于调用任意企业微信 API，自动注入 `access_token`：

- `String get(String url)` - GET 请求
- `String get(String url, Map<String, String> params)` - 带查询参数的 GET 请求
- `String post(String url, String body)` - 带 JSON 请求体的 POST 请求
- `String post(String url, Map<String, String> params, String body)` - 带查询参数和 JSON 请求体的 POST 请求
- `JsonNode getJson(String url)` - GET 请求并解析为 JSON
- `JsonNode getJson(String url, Map<String, String> params)` - 带参数的 GET 请求并解析为 JSON
- `JsonNode postJson(String url, String body)` - POST 请求并解析为 JSON
- `JsonNode postJson(String url, Map<String, String> params, String body)` - 带参数的 POST 请求并解析为 JSON

**使用示例：**

```java
// 简单的 GET 请求
String response = wxApiClient.get("https://qyapi.weixin.qq.com/cgi-bin/user/get", 
        Map.of("userid", "zhangsan"));

// 带 JSON 请求体的 POST 请求
String postResponse = wxApiClient.post("https://qyapi.weixin.qq.com/cgi-bin/message/send",
        "{\"touser\":\"zhangsan\",\"msgtype\":\"text\",\"text\":{\"content\":\"你好\"}}");

// 解析响应为 JSON
JsonNode json = wxApiClient.getJson("https://qyapi.weixin.qq.com/cgi-bin/department/list");
int errcode = json.get("errcode").asInt();
```

`WxApiClient` 自动处理：
- 在 URL 中注入 `access_token`
- HTTP 通信
- 验证 HTTP 状态码
- 检查企业微信 API 错误码（`errcode`）

## 限流与重试策略

本 starter 内置了限流和指数退避重试机制，防止触发企业微信 API 限制：

- **默认限流**：每秒 10 次请求（可通过 `wx.cp.approval.requests-per-second` 配置）
- **重试次数**：5 次，指数退避（1s → 2s → 4s → 8s → 16s）
- **分段查询**：将大时间范围拆分为 29 天的片段，避免分页限制

这些默认值已针对生产环境优化，有助于防止 `429 Too Many Requests` 错误。

## 可观测性

本 starter 通过日志和指标提供全面的可观测性：

### 日志（SLF4J）

所有关键操作都会记录适当级别的日志：

- **INFO**：查询完成、初始化、统计摘要
- **WARN**：重试尝试、详情获取失败
- **ERROR**：所有重试耗尽后的最终失败
- **DEBUG**：详细的请求/响应信息、限流器统计

日志示例：
```
INFO  WxApprovalQueryUtil - WxApprovalQueryUtil initialized with options: segmentDays=29, pageSize=100, maxRetryAttempts=5, retryBackoffMillis=1000, requestsPerSecond=10.0
INFO  WxApprovalQueryUtil - Approval spNo query completed: totalSpNos=150, totalApiCalls=2, durationMs=1250
INFO  WxApprovalQueryUtil - Fetching details for 150 approval spNos
WARN  WxApprovalQueryUtil - Failed to fetch approval detail for spNo=sp-123, attempt 1/5: rate limit exceeded
INFO  WxApprovalQueryUtil - Approval details query completed: totalDetails=148, totalFailures=2, durationMs=15320
```

### 指标（Micrometer）

当 `MeterRegistry` 可用时（例如使用 Spring Boot Actuator），starter 会自动记录：

**计时器**（带 `success` 标签）：
- `wx.approval.spnos.query` - 审批单号查询耗时
- `wx.approval.details.query` - 审批详情查询耗时
- `wx.approval.detail.fetch` - 单个详情获取耗时
- `wx.api.request` - API 请求耗时（带 `method` 标签：GET/POST）

**计数器**：
- `wx.approval.spnos.total` - 获取的审批单号总数
- `wx.approval.details.success` - 成功获取的详情数
- `wx.approval.details.failure` - 失败的详情数
- `wx.approval.detail.retry` - 重试次数
- `wx.api.http.error` - HTTP 错误数
- `wx.api.wechat.error.{errcode}` - 按错误码分类的企业微信 API 错误

**Spring Boot Actuator 示例：**

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

指标将自动导出到 `/actuator/prometheus` 端点。

## 本地构建

```bash
mvn clean test
```
