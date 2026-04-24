package org.cy.qywx.util;


import me.chanjar.weixin.cp.bean.oa.WxCpApprovalComment;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalDetailResult;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalRecord;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalRecordDetail;
import me.chanjar.weixin.cp.bean.oa.applydata.ApplyDataContent;
import me.chanjar.weixin.cp.bean.oa.applydata.ContentTitle;
import org.cy.qywx.vo.WxApprovalDetailVO;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class WxApprovalConverter {

    private WxApprovalConverter() {
    }

    public static WxApprovalDetailVO from(WxCpApprovalDetailResult detail) {
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
        return vo;
    }

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

    private static String resolveTitle(ApplyDataContent content) {
        List<ContentTitle> titles = content.getTitles();
        if (titles == null || titles.isEmpty()) {
            return content.getId();
        }
        return titles.get(0).getText();
    }

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
