package org.cy.qywx.util;

import me.chanjar.weixin.cp.bean.oa.WxCpApprovalComment;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalDetailResult;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalRecord;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalRecordDetail;
import me.chanjar.weixin.cp.bean.oa.applydata.ApplyDataContent;
import me.chanjar.weixin.cp.bean.oa.applydata.ContentTitle;
import org.cy.qywx.vo.WxApprovalDetailVO;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
        vo.setNodes(convertNodes(info.getSpRecords()));
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
     * 转换节点列表。
     *
     * @param records 记录列表
     * @return 列表结果
     *
     * @author cy
     * Copyright (c) CY
     */
    private static List<WxApprovalDetailVO.Node> convertNodes(WxCpApprovalRecord[] records) {
        if (records == null || records.length == 0) {
            return Collections.emptyList();
        }

        return Stream.of(records).map(record -> {
            WxApprovalDetailVO.Node node = new WxApprovalDetailVO.Node();
            node.setNodeStatus(record.getStatus() == null ? null : record.getStatus().name());
            node.setApproverAttr(record.getApproverAttr() == null ? null : record.getApproverAttr().name());
            node.setDetails(convertNodeDetails(record.getDetails()));
            return node;
        }).toList();
    }

    /**
     * 转换节点详情列表。
     *
     * @param details 详情列表
     * @return 列表结果
     *
     * @author cy
     * Copyright (c) CY
     */
    private static List<WxApprovalDetailVO.NodeDetail> convertNodeDetails(List<WxCpApprovalRecordDetail> details) {
        if (details == null || details.isEmpty()) {
            return Collections.emptyList();
        }

        return details.stream().map(detail -> {
            WxApprovalDetailVO.NodeDetail nodeDetail = new WxApprovalDetailVO.NodeDetail();
            nodeDetail.setApproverUserId(detail.getApprover() == null ? null : detail.getApprover().getUserId());
            nodeDetail.setSpeech(detail.getSpeech());
            nodeDetail.setSpStatus(detail.getSpStatus() == null ? null : detail.getSpStatus().name());
            nodeDetail.setSpTime(detail.getSpTime());
            return nodeDetail;
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
        List<ContentTitle> titles = content.getTitles();
        if (titles == null || titles.isEmpty()) {
            return content.getId();
        }
        return titles.get(0).getText();
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
        if (content.getValue() == null) {
            return null;
        }
        if (content.getValue().getText() != null) {
            return content.getValue().getText();
        }
        if (content.getValue().getNewNumber() != null) {
            return content.getValue().getNewNumber();
        }
        if (content.getValue().getNewMoney() != null) {
            return content.getValue().getNewMoney();
        }
        if (content.getValue().getDate() != null) {
            return content.getValue().getDate().getTimestamp();
        }
        if (content.getValue().getSelector() != null && content.getValue().getSelector().getOptions() != null) {
            return content.getValue().getSelector().getOptions().stream()
                    .flatMap(option -> option.getValues() == null ? Stream.empty() : option.getValues().stream())
                    .map(ContentTitle::getText)
                    .collect(Collectors.joining(", "));
        }
        if (content.getValue().getMembers() != null && !content.getValue().getMembers().isEmpty()) {
            return content.getValue().getMembers().stream()
                    .map(member -> member.getUserId() + "(" + member.getName() + ")")
                    .collect(Collectors.joining(", "));
        }
        if (content.getValue().getDepartments() != null && !content.getValue().getDepartments().isEmpty()) {
            return content.getValue().getDepartments().stream()
                    .map(department -> department.getName() + "(" + department.getOpenApiId() + ")")
                    .collect(Collectors.joining(", "));
        }
        if (content.getValue().getFiles() != null && !content.getValue().getFiles().isEmpty()) {
            return content.getValue().getFiles().stream()
                    .map(file -> file.getFileName() + "(" + file.getFileId() + ")")
                    .collect(Collectors.joining(", "));
        }
        if (content.getValue().getChildren() != null && !content.getValue().getChildren().isEmpty()) {
            return "明细表行数:" + content.getValue().getChildren().size();
        }
        return null;
    }
}
