# 企业微信审批接口枚举值参考

> 来源：企业微信官方文档「获取审批申请详情」
> <https://developer.work.weixin.qq.com/document/path/91983>
> 整理日期：2026-06-03

本文件汇总企业微信审批详情接口（`getapprovaldetail`）返回字段的枚举取值，供 `WxApprovalConverter` /
`WxApprovalDetailVO` / `WxApprovalProgressVO` 的实现与维护参考。**请以官方文档最新版为准。**

本 starter 基于新版 `process_list.node_list` 解析审批流程（不使用旧版 `sp_record`）。

---

## ⚠️ 注意：`apv_rel` 多人办理方式取值

`process_list.node_list[].apv_rel`：**`1`-会签；`2`-或签；`3`-依次审批**。

不要凭直觉记反——「或签」是 `2`。**本 starter 的 `WxApprovalProgressVO` 据此判定**：
`apv_rel == 2`（或签）时任一审批人通过即节点完成；会签 / 依次审批需全部处理完。

---

## 1. 申请单状态 `info.sp_status`（整单状态）

| 值 | 含义 |
|----|------|
| 1 | 审批中 |
| 2 | 已通过 |
| 3 | 已驳回 |
| 4 | 已撤销 |
| 6 | 通过后撤销 |
| 7 | 已删除 |
| 10 | 已支付 |

> WxJava 映射为枚举 `WxCpSpStatus`（`AUDITING` / `PASSED` / `REJECTED` …）；
> `WxApprovalDetailVO.spStatus` 存的是该枚举的 `name()`。

## 2. 审批流程 `info.process_list.node_list[]`

`WxApprovalDetailVO.nodes` 即来源于此。

| 字段 | 含义 / 枚举 |
|------|-------------|
| `node_type` | 节点类型：**1-审批人；2-抄送人；3-办理人** |
| `sp_status` | 节点状态：1-审批中；2-同意；3-驳回；4-转审；11-退回给指定审批人；12-加签；13-同意并加签；14-办理；15-转交 |
| `apv_rel` | 多人办理方式：**1-会签；2-或签；3-依次审批** |
| `sub_node_list[]` | 子节点（审批人 / 抄送人 / 办理人）列表 |

### `sub_node_list[]` 子节点字段

| 字段 | 含义 / 枚举 |
|------|-------------|
| `userid` | 处理人 userid |
| `speech` | 审批 / 办理意见 |
| `sp_yj` | 子节点状态：1-审批中；2-同意；3-驳回；4-转审；11-退回给指定审批人；12-加签；13-同意并加签；14-办理；15-转交 |
| `sptime` | 操作时间戳，**0 表示未操作** |
| `media_ids` | 附件（微盘文件无法获取） |

> 注意：子节点的处理状态字段名是 **`sp_yj`**（不是 `sp_status`），枚举与节点 `sp_status` 一致。
> `WxApprovalDetailVO.NodeDetail.spYj` 即对应此字段。

## 3. 表单控件类型 `apply_data.contents[].control`

`Text`-文本；`Textarea`-多行文本；`Number`-数字；`Money`-金额；`Date`-日期/日期+时间；
`Selector`-单选/多选；`Contact`-成员/部门；`Tips`-说明文字；`File`-附件；`Table`-明细；
`Attendance`-假勤；`Vacation`-请假；`PunchCorrection`-补卡；`DateRange`-时长。

---

## 与本 starter 字段映射

| 接口字段 | 本地字段 |
|----------|----------|
| `info.sp_status` | `WxApprovalDetailVO.spStatus`（枚举 name） |
| `info.apply_time` | `WxApprovalDetailVO.applyTime` |
| `info.process_list.node_list` | `WxApprovalDetailVO.nodes` |
| `node_list[].node_type / sp_status / apv_rel` | `Node.nodeType / spStatus / apvRel` |
| `sub_node_list[].userid / speech / sp_yj / sptime` | `NodeDetail.approverUserId / speech / spYj / spTime` |
| （派生）流转进度与卡点 | `WxApprovalProgressVO`（基于 `nodes` 计算） |
