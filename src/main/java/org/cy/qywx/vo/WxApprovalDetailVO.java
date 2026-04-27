package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class WxApprovalDetailVO implements Serializable {

    private String spNo;
    private String spName;
    private String spStatus;
    private String templateId;
    private Long applyTime;
    private Long closeTime;
    private Long closeLoopDurationSeconds;
    private Long currentDurationSeconds;
    private Boolean closed;
    private Boolean createdToday;
    private Boolean overdueOneDay;
    private String applicantUserId;
    private String applicantPartyId;
    private List<FormItem> formItems;
    private List<Node> nodes;
    private List<CommentItem> comments;

    @Data
    public static class FormItem implements Serializable {
        private String title;
        private String control;
        private String value;
    }

    @Data
    public static class Node implements Serializable {
        private String nodeStatus;
        private String approverAttr;
        private List<NodeDetail> details;
    }

    @Data
    public static class NodeDetail implements Serializable {
        private String approverUserId;
        private String speech;
        private String spStatus;
        private Long spTime;
    }

    @Data
    public static class CommentItem implements Serializable {
        private String userId;
        private String content;
        private Long commentTime;
    }
}
