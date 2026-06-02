package org.cy.qywx.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalComment;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalDetailResult;
import me.chanjar.weixin.cp.bean.oa.applydata.ApplyDataContent;
import me.chanjar.weixin.cp.bean.oa.applydata.ContentTitle;
import me.chanjar.weixin.cp.bean.oa.applydata.ContentValue;
import org.cy.qywx.vo.WxApprovalDetailVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 类说明：审批converter工具。
 *
 * @author cy
 * Copyright (c) CY
 */
public final class WxApprovalConverter {

    /**
     * 字段说明：一天秒。
     *
     * @author cy
     * Copyright (c) CY
     */
    private static final long ONE_DAY_SECONDS = 24L * 60 * 60;
    /**
     * 字段说明：默认时区。
     *
     * @author cy
     * Copyright (c) CY
     */
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
    /**
     * 字段说明：日志。
     *
     * @author cy
     * Copyright (c) CY
     */
    private static final Logger log = LoggerFactory.getLogger(WxApprovalConverter.class);
    /**
     * 字段说明：JSON 解析器。
     *
     * @author cy
     * Copyright (c) CY
     */
    private static final Gson GSON = new Gson();

    /**
     * 创建 审批converter工具实例。
     *
     * @author cy
     * Copyright (c) CY
     */
    private WxApprovalConverter() {
    }

    /**
     * 将企业微信原始对象转换为业务对象。
     *
     * @param detail 详情
     * @return 审批详情业务视图对象
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxApprovalDetailVO from(WxCpApprovalDetailResult detail) {
        return from(detail, Instant.now().getEpochSecond());
    }

    /**
     * 将企业微信原始对象转换为业务对象，并用原始 JSON 补全 WxJava 未映射的容器控件（如离职）。
     *
     * @param detail 详情
     * @param rawJson 审批详情接口的原始 JSON 响应
     * @return 审批详情业务视图对象
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxApprovalDetailVO from(WxCpApprovalDetailResult detail, String rawJson) {
        return from(detail, rawJson, Instant.now().getEpochSecond());
    }

    /**
     * 将企业微信原始对象转换为业务对象，并用原始 JSON 补全容器控件。
     *
     * @param detail 详情
     * @param rawJson 审批详情接口的原始 JSON 响应
     * @param nowEpochSecond nowepoch秒
     * @return 审批详情业务视图对象
     *
     * @author cy
     * Copyright (c) CY
     */
    static WxApprovalDetailVO from(WxCpApprovalDetailResult detail, String rawJson, long nowEpochSecond) {
        WxApprovalDetailVO vo = from(detail, nowEpochSecond);
        enrichFromRawJson(vo, rawJson);
        if (enrichNodesFromProcessList(vo, rawJson)) {
            // 节点已替换为 process_list 的完整流程，依赖节点时间的计时字段需重算
            fillTimingFields(vo, nowEpochSecond);
        }
        return vo;
    }

    /**
     * 将企业微信原始对象转换为业务对象。
     *
     * @param detail 详情
     * @param nowEpochSecond nowepoch秒
     * @return 审批详情业务视图对象
     *
     * @author cy
     * Copyright (c) CY
     */
    static WxApprovalDetailVO from(WxCpApprovalDetailResult detail, long nowEpochSecond) {
        if (detail == null || detail.getInfo() == null) {
            return null;
        }

        WxCpApprovalDetailResult.WxCpApprovalDetail info = detail.getInfo();
        WxApprovalDetailVO vo = new WxApprovalDetailVO();
        vo.setSpNo(info.getSpNo());
        vo.setSpName(info.getSpName());
        vo.setSpStatus(info.getSpStatus() == null ? null : info.getSpStatus().name());
        vo.setTemplateId(info.getTemplateId());
        vo.setApplyTime(info.getApplyTime());

        if (info.getApplier() != null) {
            vo.setApplicantUserId(info.getApplier().getUserId());
            vo.setApplicantPartyId(info.getApplier().getPartyId());
        }

        vo.setFormItems(convertFormItems(info.getApplyData() == null ? null : info.getApplyData().getContents()));
        vo.setNodes(Collections.emptyList());
        vo.setComments(convertComments(info.getComments()));
        fillTimingFields(vo, nowEpochSecond);
        return vo;
    }

    /**
     * 执行 groupByTemplateId 相关逻辑。
     *
     * @param details 详情列表
     * @return 列表结果
     *
     * @author cy
     * Copyright (c) CY
     */
    public static Map<String, List<WxApprovalDetailVO>> groupByTemplateId(List<WxCpApprovalDetailResult> details) {
        if (details == null || details.isEmpty()) {
            return Collections.emptyMap();
        }

        return details.stream()
                .map(WxApprovalConverter::from)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        vo -> vo.getTemplateId() == null ? "UNKNOWN_TEMPLATE" : vo.getTemplateId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    /**
     * 执行 fillTimingFields 相关逻辑。
     *
     * @param vo vo
     * @param nowEpochSecond nowepoch秒
     *
     * @author cy
     * Copyright (c) CY
     */
    private static void fillTimingFields(WxApprovalDetailVO vo, long nowEpochSecond) {
        Long applyTime = vo.getApplyTime();
        if (applyTime == null) {
            return;
        }

        boolean closed = isClosed(vo.getSpStatus());
        Long closeTime = resolveCloseTime(vo);
        long currentDurationSeconds = Math.max(0L, nowEpochSecond - applyTime);

        vo.setClosed(closed);
        vo.setCloseTime(closeTime);
        vo.setCurrentDurationSeconds(currentDurationSeconds);
        vo.setCreatedToday(isSameLocalDate(applyTime, nowEpochSecond));
        vo.setOverdueOneDay(currentDurationSeconds > ONE_DAY_SECONDS);

        if (closed && closeTime != null) {
            vo.setCloseLoopDurationSeconds(Math.max(0L, closeTime - applyTime));
        }
    }

    /**
     * 判断是否是否已结束。
     *
     * @param spStatus 审批状态
     * @return boolean
     *
     * @author cy
     * Copyright (c) CY
     */
    private static boolean isClosed(String spStatus) {
        return spStatus != null && !"AUDITING".equals(spStatus);
    }

    /**
     * 判断是否samelocal日期。
     *
     * @param firstEpochSecond firstepoch秒
     * @param secondEpochSecond 秒epoch秒
     * @return boolean
     *
     * @author cy
     * Copyright (c) CY
     */
    private static boolean isSameLocalDate(long firstEpochSecond, long secondEpochSecond) {
        LocalDate first = Instant.ofEpochSecond(firstEpochSecond).atZone(DEFAULT_ZONE).toLocalDate();
        LocalDate second = Instant.ofEpochSecond(secondEpochSecond).atZone(DEFAULT_ZONE).toLocalDate();
        return first.equals(second);
    }

    /**
     * 解析关闭时间。
     *
     * @param vo vo
     * @return long
     *
     * @author cy
     * Copyright (c) CY
     */
    private static Long resolveCloseTime(WxApprovalDetailVO vo) {
        return Stream.concat(
                        vo.getNodes() == null ? Stream.empty() : vo.getNodes().stream()
                                .filter(Objects::nonNull)
                                .flatMap(node -> node.getDetails() == null ? Stream.empty() : node.getDetails().stream())
                                .map(WxApprovalDetailVO.NodeDetail::getSpTime),
                        vo.getComments() == null ? Stream.empty() : vo.getComments().stream()
                                .filter(Objects::nonNull)
                                .map(WxApprovalDetailVO.CommentItem::getCommentTime)
                )
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(null);
    }

    /**
     * 转换表单项列表。
     *
     * @param contents contents
     * @return 列表结果
     *
     * @author cy
     * Copyright (c) CY
     */
    private static List<WxApprovalDetailVO.FormItem> convertFormItems(List<ApplyDataContent> contents) {
        if (contents == null || contents.isEmpty()) {
            return Collections.emptyList();
        }

        return contents.stream().map(content -> {
            WxApprovalDetailVO.FormItem item = new WxApprovalDetailVO.FormItem();
            item.setTitle(resolveTitle(content));
            item.setControl(content.getControl());
            item.setValue(resolveValue(content));
            return item;
        }).toList();
    }

    /**
     * 转换评论列表。
     *
     * @param comments 评论列表
     * @return 列表结果
     *
     * @author cy
     * Copyright (c) CY
     */
    private static List<WxApprovalDetailVO.CommentItem> convertComments(List<WxCpApprovalComment> comments) {
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyList();
        }

        return comments.stream().map(comment -> {
            WxApprovalDetailVO.CommentItem item = new WxApprovalDetailVO.CommentItem();
            item.setUserId(comment.getCommentUserInfo() == null ? null : comment.getCommentUserInfo().getUserId());
            item.setContent(comment.getCommentContent());
            item.setCommentTime(comment.getCommentTime());
            return item;
        }).toList();
    }

    /**
     * 解析标题。
     *
     * @param content 内容
     * @return string
     *
     * @author cy
     * Copyright (c) CY
     */
    private static String resolveTitle(ApplyDataContent content) {
        return resolveLocalizedText(content.getTitles(), content.getId());
    }

    /**
     * 解析多语言文本，优先简体中文，其次繁体中文，再退回首个非空文本。
     *
     * @param titles 多语言文本列表
     * @param fallback 兜底文本
     * @return string
     *
     * @author cy
     * Copyright (c) CY
     */
    private static String resolveLocalizedText(List<ContentTitle> titles, String fallback) {
        if (titles == null || titles.isEmpty()) {
            return fallback;
        }
        String zhCn = null;
        String zhTw = null;
        String first = null;
        for (ContentTitle title : titles) {
            if (title == null || title.getText() == null || title.getText().isEmpty()) {
                continue;
            }
            if (first == null) {
                first = title.getText();
            }
            if ("zh_CN".equalsIgnoreCase(title.getLang()) && zhCn == null) {
                zhCn = title.getText();
            } else if ("zh_TW".equalsIgnoreCase(title.getLang()) && zhTw == null) {
                zhTw = title.getText();
            }
        }
        if (zhCn != null) {
            return zhCn;
        }
        if (zhTw != null) {
            return zhTw;
        }
        return first != null ? first : fallback;
    }

    /**
     * 解析值。
     *
     * @param content 内容
     * @return string
     *
     * @author cy
     * Copyright (c) CY
     */
    private static String resolveValue(ApplyDataContent content) {
        ContentValue value = content.getValue();
        if (value == null) {
            return null;
        }
        if (value.getText() != null) {
            return value.getText();
        }
        if (value.getNewNumber() != null) {
            return value.getNewNumber();
        }
        if (value.getNewMoney() != null) {
            return value.getNewMoney();
        }
        if (value.getDate() != null && value.getDate().getTimestamp() != null) {
            return resolveDate(value.getDate());
        }
        if (value.getSelector() != null && value.getSelector().getOptions() != null) {
            return value.getSelector().getOptions().stream()
                    .map(option -> resolveLocalizedText(option.getValues(), null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));
        }
        if (value.getMembers() != null && !value.getMembers().isEmpty()) {
            return value.getMembers().stream()
                    .map(member -> member.getUserId() + "(" + member.getName() + ")")
                    .collect(Collectors.joining(", "));
        }
        if (value.getDepartments() != null && !value.getDepartments().isEmpty()) {
            return value.getDepartments().stream()
                    .map(department -> department.getName() + "(" + department.getOpenApiId() + ")")
                    .collect(Collectors.joining(", "));
        }
        if (value.getFiles() != null && !value.getFiles().isEmpty()) {
            return value.getFiles().stream()
                    .map(file -> file.getFileName() + "(" + file.getFileId() + ")")
                    .collect(Collectors.joining(", "));
        }
        if (value.getVacation() != null) {
            return resolveVacation(value.getVacation());
        }
        if (value.getAttendance() != null) {
            return resolveAttendance(value.getAttendance());
        }
        if (value.getDateRange() != null) {
            return resolveDateRange(value.getDateRange());
        }
        if (value.getPunchCorrection() != null) {
            return resolvePunchCorrection(value.getPunchCorrection());
        }
        if (value.getLocation() != null) {
            return resolveLocation(value.getLocation());
        }
        if (value.getFormula() != null) {
            return value.getFormula().getValue();
        }
        if (value.getBankAccount() != null) {
            return resolveBankAccount(value.getBankAccount());
        }
        if (value.getRelatedApproval() != null && !value.getRelatedApproval().isEmpty()) {
            return resolveRelatedApproval(value.getRelatedApproval());
        }
        if (value.getChildren() != null && !value.getChildren().isEmpty()) {
            return resolveChildren(value.getChildren());
        }
        return null;
    }

    /**
     * 解析日期控件，将时间戳格式化为可读日期。
     *
     * @param date 日期值
     * @return string
     *
     * @author cy
     * Copyright (c) CY
     */
    private static String resolveDate(ContentValue.Date date) {
        Long epochSecond = parseEpochSecond(date.getTimestamp());
        if (epochSecond == null) {
            return date.getTimestamp();
        }
        return formatEpochSecond(epochSecond, "day".equalsIgnoreCase(date.getType()));
    }

    /**
     * 解析假勤控件（出差/外出/加班等），输出起止时间与时长。
     *
     * @param attendance 假勤值
     * @return string
     *
     * @author cy
     * Copyright (c) CY
     */
    private static String resolveAttendance(ContentValue.Attendance attendance) {
        return resolveDateRange(attendance.getDateRange());
    }

    /**
     * 解析请假控件，输出假期类型与时长。
     *
     * @param vacation 请假值
     * @return string
     *
     * @author cy
     * Copyright (c) CY
     */
    private static String resolveVacation(ContentValue.Vacation vacation) {
        String type = null;
        if (vacation.getSelector() != null && vacation.getSelector().getOptions() != null) {
            type = vacation.getSelector().getOptions().stream()
                    .map(option -> resolveLocalizedText(option.getValues(), null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));
        }
        String duration = vacation.getAttendance() == null ? null
                : resolveDateRange(vacation.getAttendance().getDateRange());
        return Stream.of(type, duration)
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(", "));
    }

    /**
     * 解析时长/日期区间控件，输出起止时间与时长秒数。
     *
     * @param range 日期区间值
     * @return string
     *
     * @author cy
     * Copyright (c) CY
     */
    private static String resolveDateRange(ContentValue.Attendance.DataRange range) {
        if (range == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        String begin = formatEpochSecond(range.getBegin(), false);
        String end = formatEpochSecond(range.getEnd(), false);
        if (begin != null || end != null) {
            builder.append(begin == null ? "" : begin)
                    .append(" ~ ")
                    .append(end == null ? "" : end);
        }
        if (range.getDuration() != null) {
            if (builder.length() > 0) {
                builder.append("，");
            }
            builder.append("时长").append(range.getDuration()).append("秒");
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    /**
     * 解析补卡控件，输出补卡状态与时间。
     *
     * @param punchCorrection 补卡值
     * @return string
     *
     * @author cy
     * Copyright (c) CY
     */
    private static String resolvePunchCorrection(ContentValue.PunchCorrection punchCorrection) {
        StringBuilder builder = new StringBuilder();
        if (punchCorrection.getState() != null) {
            builder.append(punchCorrection.getState());
        }
        String time = formatEpochSecond(punchCorrection.getTime(), false);
        if (time != null) {
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append(time);
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    /**
     * 解析位置控件，输出位置标题与详细地址。
     *
     * @param location 位置值
     * @return string
     *
     * @author cy
     * Copyright (c) CY
     */
    private static String resolveLocation(ContentValue.Location location) {
        return Stream.of(location.getTitle(), location.getAddress())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
    }

    /**
     * 解析银行账户控件，输出账户名与账号。
     *
     * @param bankAccount 银行账户值
     * @return string
     *
     * @author cy
     * Copyright (c) CY
     */
    private static String resolveBankAccount(ContentValue.BankAccount bankAccount) {
        return Stream.of(bankAccount.getAccountName(), bankAccount.getAccountNumber())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
    }

    /**
     * 解析关联审批单控件，输出模板名与关联审批单号。
     *
     * @param relatedApprovals 关联审批单列表
     * @return string
     *
     * @author cy
     * Copyright (c) CY
     */
    private static String resolveRelatedApproval(List<ContentValue.RelatedApproval> relatedApprovals) {
        return relatedApprovals.stream()
                .filter(Objects::nonNull)
                .map(approval -> {
                    String name = approval.getTemplateNames() == null ? null
                            : approval.getTemplateNames().stream()
                                    .filter(Objects::nonNull)
                                    .map(ContentValue.TemplateName::getText)
                                    .filter(Objects::nonNull)
                                    .findFirst()
                                    .orElse(null);
                    String spNo = approval.getSpNo();
                    if (name != null && spNo != null) {
                        return name + "(" + spNo + ")";
                    }
                    return spNo != null ? spNo : name;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
    }

    /**
     * 解析明细表控件，展开每行子控件的标题与值。
     *
     * @param children 明细表行列表
     * @return string
     *
     * @author cy
     * Copyright (c) CY
     */
    private static String resolveChildren(List<ContentValue.Child> children) {
        StringBuilder builder = new StringBuilder();
        int rowIndex = 1;
        for (ContentValue.Child child : children) {
            if (child == null || child.getList() == null || child.getList().isEmpty()) {
                continue;
            }
            String row = child.getList().stream()
                    .filter(Objects::nonNull)
                    .map(sub -> resolveTitle(sub) + ":" + resolveValue(sub))
                    .collect(Collectors.joining(", "));
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append("第").append(rowIndex++).append("行[").append(row).append("]");
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    /**
     * 将 Unix 时间戳字符串解析为秒值。
     *
     * @param timestamp 时间戳字符串
     * @return long
     *
     * @author cy
     * Copyright (c) CY
     */
    private static Long parseEpochSecond(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(timestamp.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 将 Unix 时间戳秒值格式化为可读日期或日期时间。
     *
     * @param epochSecond 时间戳秒值
     * @param dateOnly 是否仅日期
     * @return string
     *
     * @author cy
     * Copyright (c) CY
     */
    private static String formatEpochSecond(Long epochSecond, boolean dateOnly) {
        if (epochSecond == null) {
            return null;
        }
        ZonedDateTime zonedDateTime = Instant.ofEpochSecond(epochSecond).atZone(DEFAULT_ZONE);
        return dateOnly ? zonedDateTime.toLocalDate().toString() : zonedDateTime.toLocalDateTime().toString();
    }

    /**
     * 用原始 JSON 补全 WxJava 未映射的容器控件（如离职 Resignation）的值。
     *
     * @param vo 审批详情业务视图对象
     * @param rawJson 审批详情接口的原始 JSON 响应
     *
     * @author cy
     * Copyright (c) CY
     */
    private static void enrichFromRawJson(WxApprovalDetailVO vo, String rawJson) {
        if (vo == null || vo.getFormItems() == null || vo.getFormItems().isEmpty()
                || rawJson == null || rawJson.isBlank()) {
            return;
        }
        JsonArray contents = extractContents(rawJson);
        if (contents == null) {
            return;
        }
        List<WxApprovalDetailVO.FormItem> items = vo.getFormItems();
        int size = Math.min(items.size(), contents.size());
        for (int i = 0; i < size; i++) {
            WxApprovalDetailVO.FormItem item = items.get(i);
            if (item.getValue() != null) {
                continue;
            }
            JsonElement contentElement = contents.get(i);
            if (!contentElement.isJsonObject()) {
                continue;
            }
            String resolved = resolveContainerValue(contentElement.getAsJsonObject());
            if (resolved != null && !resolved.isBlank()) {
                item.setValue(resolved);
            }
        }
    }

    /**
     * 从原始 JSON 中提取审批表单控件数组。
     *
     * @param rawJson 审批详情接口的原始 JSON 响应
     * @return JSON数组
     *
     * @author cy
     * Copyright (c) CY
     */
    private static JsonArray extractContents(String rawJson) {
        try {
            JsonElement root = JsonParser.parseString(rawJson);
            if (!root.isJsonObject()) {
                return null;
            }
            JsonElement info = root.getAsJsonObject().get("info");
            if (info == null || !info.isJsonObject()) {
                return null;
            }
            JsonElement applyData = info.getAsJsonObject().get("apply_data");
            if (applyData == null || !applyData.isJsonObject()) {
                return null;
            }
            JsonElement contents = applyData.getAsJsonObject().get("contents");
            return contents != null && contents.isJsonArray() ? contents.getAsJsonArray() : null;
        } catch (RuntimeException e) {
            log.warn("Failed to parse raw approval JSON for special control enrichment: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析容器型控件（如离职）的值，递归展开其内部子控件的标题与值。
     *
     * @param contentObject 控件 JSON 对象
     * @return string
     *
     * @author cy
     * Copyright (c) CY
     */
    private static String resolveContainerValue(JsonObject contentObject) {
        JsonElement valueElement = contentObject.get("value");
        if (valueElement == null || !valueElement.isJsonObject()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        collectChildControls(valueElement, parts);
        return parts.isEmpty() ? null : String.join(" | ", parts);
    }

    /**
     * 递归收集复合控件 value 内的所有子控件并提取「标题: 值」。
     *
     * 复合控件（如离职 Resignation 的 value.resignation.*）可能把子控件包在若干层没有
     * {@code control} 字段的对象之下；此处对任意此类包装层递归下钻，因此不限于离职模块，
     * 任何带 control 的叶子控件无论嵌套多深都会被解析。
     *
     * @param element 当前 JSON 元素
     * @param parts 收集到的「标题: 值」片段
     *
     * @author cy
     * Copyright (c) CY
     */
    private static void collectChildControls(JsonElement element, List<String> parts) {
        if (element == null) {
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectChildControls(child, parts);
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("control")) {
            ApplyDataContent sub = GSON.fromJson(object, ApplyDataContent.class);
            String subValue = resolveValue(sub);
            if (subValue != null && !subValue.isBlank()) {
                parts.add(resolveTitle(sub) + ": " + subValue);
            }
            return;
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            collectChildControls(entry.getValue(), parts);
        }
    }

    /**
     * 用原始 JSON 的 process_list.node_list 替换节点列表（WxJava 未映射该字段）。
     *
     * @param vo 审批详情业务视图对象
     * @param rawJson 审批详情接口的原始 JSON 响应
     * @return 是否成功用 process_list 节点替换了 vo 的节点列表
     *
     * @author cy
     * Copyright (c) CY
     */
    private static boolean enrichNodesFromProcessList(WxApprovalDetailVO vo, String rawJson) {
        if (vo == null || rawJson == null || rawJson.isBlank()) {
            return false;
        }
        JsonArray nodeList = extractProcessNodeList(rawJson);
        if (nodeList == null || nodeList.isEmpty()) {
            return false;
        }
        List<WxApprovalDetailVO.Node> nodes = new ArrayList<>();
        for (JsonElement element : nodeList) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject nodeObject = element.getAsJsonObject();
            WxApprovalDetailVO.Node node = new WxApprovalDetailVO.Node();
            node.setNodeType(getInteger(nodeObject, "node_type"));
            node.setSpStatus(getInteger(nodeObject, "sp_status"));
            node.setApvRel(getInteger(nodeObject, "apv_rel"));
            node.setDetails(convertSubNodes(nodeObject.get("sub_node_list")));
            nodes.add(node);
        }
        vo.setNodes(nodes);
        return true;
    }

    /**
     * 从原始 JSON 中提取审批流程节点数组 info.process_list.node_list。
     *
     * @param rawJson 审批详情接口的原始 JSON 响应
     * @return JSON数组
     *
     * @author cy
     * Copyright (c) CY
     */
    private static JsonArray extractProcessNodeList(String rawJson) {
        try {
            JsonElement root = JsonParser.parseString(rawJson);
            if (!root.isJsonObject()) {
                return null;
            }
            JsonElement info = root.getAsJsonObject().get("info");
            if (info == null || !info.isJsonObject()) {
                return null;
            }
            JsonElement processList = info.getAsJsonObject().get("process_list");
            if (processList == null || !processList.isJsonObject()) {
                return null;
            }
            JsonElement nodeList = processList.getAsJsonObject().get("node_list");
            return nodeList != null && nodeList.isJsonArray() ? nodeList.getAsJsonArray() : null;
        } catch (RuntimeException e) {
            log.warn("Failed to parse process_list.node_list from raw approval JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 转换流程节点的子节点（审批人 / 抄送人）列表。
     *
     * @param subNodeListElement sub_node_list JSON 元素
     * @return 列表结果
     *
     * @author cy
     * Copyright (c) CY
     */
    private static List<WxApprovalDetailVO.NodeDetail> convertSubNodes(JsonElement subNodeListElement) {
        if (subNodeListElement == null || !subNodeListElement.isJsonArray()) {
            return Collections.emptyList();
        }
        List<WxApprovalDetailVO.NodeDetail> details = new ArrayList<>();
        for (JsonElement element : subNodeListElement.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject subNodeObject = element.getAsJsonObject();
            WxApprovalDetailVO.NodeDetail detail = new WxApprovalDetailVO.NodeDetail();
            detail.setApproverUserId(getString(subNodeObject, "userid"));
            detail.setSpeech(getString(subNodeObject, "speech"));
            detail.setSpYj(getInteger(subNodeObject, "sp_yj"));
            detail.setSpTime(getLong(subNodeObject, "sptime"));
            details.add(detail);
        }
        return details;
    }

    /**
     * 安全读取 JSON 对象中的整数字段。
     *
     * @param object JSON 对象
     * @param key 字段名
     * @return 整数值，字段缺失或非数字时为 null
     *
     * @author cy
     * Copyright (c) CY
     */
    private static Integer getInteger(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        try {
            return element.getAsInt();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 安全读取 JSON 对象中的长整数字段。
     *
     * @param object JSON 对象
     * @param key 字段名
     * @return 长整数值，字段缺失或非数字时为 null
     *
     * @author cy
     * Copyright (c) CY
     */
    private static Long getLong(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        try {
            return element.getAsLong();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 安全读取 JSON 对象中的字符串字段。
     *
     * @param object JSON 对象
     * @param key 字段名
     * @return 字符串值，字段缺失或非基本类型时为 null
     *
     * @author cy
     * Copyright (c) CY
     */
    private static String getString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }
}
