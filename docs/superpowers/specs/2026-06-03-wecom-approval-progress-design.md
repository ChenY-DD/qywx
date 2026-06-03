# 企业微信审批流转时效分析设计 — qywx-wecom-spring-boot-starter

> 状态：已实现 · 测试通过（WxApprovalProgressTest 4 项，全量 76 项）
> 日期：2026-06-03
> 作者：CY
> 版权：CY

## 1. 目标与范围

以已转换好的 `WxApprovalDetailVO`（含 `nodes`）为输入，派生独立的 `WxApprovalProgressVO`，
用于催办 / 时效看板，回答：审批流转到哪、卡在谁、各环节耗时、下一个审批人是谁。

**复用已有字段，不重复实现**：提交时间 = `applyTime`；提交到现在 = `currentDurationSeconds`。

不在范围：

- 网络拉取 —— 输入是已转换的本地 VO，不发起任何 WeCom 调用
- 写操作（催办、加签等）
- 多语言

## 2. 输入与依赖

`WxApprovalDetailVO`，关键依赖 `nodes`（**列表顺序即流转顺序**）：

- `Node{nodeType(1审批/2抄送/3办理), spStatus, apvRel, details}`
- `NodeDetail{approverUserId, spTime(未处理为 0/null), speech, spYj}`
- 整单：`applyTime` / `spStatus` / `closed` / `currentDurationSeconds`

## 3. 关键决策（已定）

| 维度 | 决策 |
|---|---|
| 会签 / 或签多人节点 | 按 `apvRel` 区分：或签任一处理即完成；会签 / 依次需全部处理完 |
| 链路范围 | 审批(`node_type=1`) + 办理(`node_type=3`)，按原顺序；抄送(2)仅旁路，不计入时间轴与卡点 |
| 输出形态 | 新建 `WxApprovalProgressVO`，不污染原 VO |
| 已结束单 | `currentBlock` 置 `null`；各历史节点耗时照常计算 |

## 4. 枚举核实（✅ 已通过官方文档确认）

来源：企业微信「获取审批申请详情」<https://developer.work.weixin.qq.com/document/path/91983>，
完整速查见 [`docs/wecom-approval-enums.md`](../../wecom-approval-enums.md)。

| 字段 | 取值 |
|---|---|
| `process_list.node_list[].apv_rel` | **1-会签；2-或签；3-依次审批** |
| `node_type` | 1-审批人；2-抄送人；3-办理人 |
| `sub_node_list[].sp_yj`（子节点状态） | 1审批中 / 2同意 / 3驳回 / 4转审 / 11退回 / 12加签 / 13同意并加签 / 14办理 / 15转交 |
| `sptime` | 操作时间戳，`0` 表示未操作 |

> ⚠️ **陷阱**：旧版 `sp_record.approverattr` 是「1-或签；2-会签」，与新版 `apv_rel`（1会签/2或签）**完全相反**。
> 本设计基于 `apv_rel`，因此**或签 = 2**。

## 5. 计算规则（节点时间轴）

- **开始时刻** = 上一链路节点的完成时刻；首个节点 = `applyTime`。
- **完成时刻**按 `apvRel`：
  - 或签（`2`）：任一审批人处理即完成 → 取 `details` 中非空 `spTime` 的**最早值**。
  - 会签（`1`）/ 依次（`3`）/ 其他：所有人处理完才完成 → 取**最晚** `spTime`；存在未处理人则未完成。
- 已完成节点 **`durationSeconds`** = 完成时刻 − 开始时刻（下限 0）。
- **当前卡点** = 第一个未完成的审批 / 办理节点；其 **`waitingSeconds`** = `now` − 该节点开始时刻。
- **未处理人** = 节点 `details` 中 `spTime` 为空（`null` 或 `0`）的 `approverUserId`。
- 卡点之后的节点尚未开始：`startTime` / `completeTime` / `durationSeconds` 均为 `null`。
- `closed == true`：清除卡点（`currentBlock = null`、节点 `blocked = false`），历史耗时保留。

## 6. 输出结构 `WxApprovalProgressVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| `spNo` | `String` | 审批单号 |
| `closed` | `Boolean` | 是否已结束（复用） |
| `applyTime` | `Long` | 提交时间，Unix 秒（复用） |
| `submittedToNowSeconds` | `Long` | 提交 → 当前时长，秒（复用 `currentDurationSeconds`） |
| `nodeChain` | `List<NodeProgress>` | 审批 + 办理节点，按流程顺序 |
| `currentBlock` | `CurrentBlock` | 当前卡点；已结束时 `null` |

- `NodeProgress`：`index`、`nodeType`、`apvRel`、`spStatus`、`approverUserIds`、`startTime`、`completeTime`、`durationSeconds`、`blocked`、`pendingUserIds`。
- `CurrentBlock`：`nodeIndex`、`blockingUserIds`、`waitingSeconds`、`nextApproverUserIds`。

## 7. 实现位置与风格

- 逻辑放在转换器 **`WxApprovalConverter`**（而非新建分析器）——与 `from(...)` 同源，复用 `nodes` 解析结果：
  - `public static WxApprovalProgressVO toProgress(WxApprovalDetailVO vo)` —— 默认 `Instant.now()`
  - `static WxApprovalProgressVO toProgress(WxApprovalDetailVO vo, long nowEpochSecond)` —— 包内重载，供单测注入时间
  - 与现有 `fillTimingFields(vo, nowEpochSecond)` 的 now 参数化风格一致
- **输入本地 VO、输出本地 VO**，全程不接触 `me.chanjar.weixin.*`，天然不泄漏外部类型。
- 私有 helper：`chainNodes` / `resolveNodeCompleteTime` / `approverUserIds` / `pendingUserIds` / `buildCurrentBlock`。

## 8. 测试

`WxApprovalProgressTest`，全部以固定 `now` 注入：

1. 进行中多节点链路（排除抄送、或签完成、会签卡点、未开始节点、下一审批人、`waiting` / `duration`）
2. 或签任一人处理即完成
3. 已结束单 `currentBlock` 置空、历史耗时照算
4. 空节点健壮性

## 9. 需求映射

| 原始需求 | 落点 |
|---|---|
| 审批的人 | `NodeProgress.approverUserIds` |
| 提交时间 | `applyTime`（复用） |
| 当前人是否审批 | `NodeProgress.blocked` / `pendingUserIds` / `completeTime` |
| 审批耗时多久 | `NodeProgress.durationSeconds`（单节点） |
| 下一个审批人 | `CurrentBlock.nextApproverUserIds` |
| 审批卡在哪个人 | `CurrentBlock.blockingUserIds` |
| 上个人到现在拖了多久 | `CurrentBlock.waitingSeconds` |
| 提交审批到现在多久 | `submittedToNowSeconds`（复用） |
