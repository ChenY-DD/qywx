package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 类说明：审批详情业务视图对象。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxApprovalDetailVO implements Serializable {

    /**
     * 字段说明：审批单号。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String spNo;
    /**
     * 字段说明：审批名称。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String spName;
    /**
     * 字段说明：审批状态。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String spStatus;
    /**
     * 字段说明：审批模板 ID。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String templateId;
    /**
     * 字段说明：审批申请提交时间。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Long applyTime;
    /**
     * 字段说明：审批关闭时间。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Long closeTime;
    /**
     * 字段说明：审批从提交到关闭的耗时（秒）。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Long closeLoopDurationSeconds;
    /**
     * 字段说明：审批从提交到当前的持续时长（秒）。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Long currentDurationSeconds;
    /**
     * 字段说明：审批是否已结束。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Boolean closed;
    /**
     * 字段说明：审批是否今天创建。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Boolean createdToday;
    /**
     * 字段说明：审批是否已超过一天未结束。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Boolean overdueOneDay;
    /**
     * 字段说明：申请人 userId。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String applicantUserId;
    /**
     * 字段说明：申请人所属部门 ID。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String applicantPartyId;
    /**
     * 字段说明：表单项列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<FormItem> formItems;
    /**
     * 字段说明：审批流程节点列表（来自 process_list.node_list，含审批 / 抄送 / 办理节点）。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<Node> nodes;
    /**
     * 字段说明：评论列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<CommentItem> comments;

    /**
     * 类说明：表单项。
     *
     * @author cy
     * Copyright (c) CY
     */
    @Data
    public static class FormItem implements Serializable {
        /**
         * 字段说明：标题。
         *
         * @author cy
         * Copyright (c) CY
         */
        private String title;
        /**
         * 字段说明：控件。
         *
         * @author cy
         * Copyright (c) CY
         */
        private String control;
        /**
         * 字段说明：值。
         *
         * @author cy
         * Copyright (c) CY
         */
        private String value;
    }

    /**
     * 类说明：节点。
     *
     * @author cy
     * Copyright (c) CY
     */
    @Data
    public static class Node implements Serializable {
        /**
         * 字段说明：节点类型。1=审批、2=抄送、3=办理（对应 process_list.node_list[].node_type）。
         *
         * @author cy
         * Copyright (c) CY
         */
        private Integer nodeType;
        /**
         * 字段说明：节点审批状态（对应 sp_status；抄送节点无此字段，为 null）。
         *
         * @author cy
         * Copyright (c) CY
         */
        private Integer spStatus;
        /**
         * 字段说明：多人办理方式（对应 apv_rel：1会签 / 2或签 / 3依次审批；抄送节点无此字段，为 null）。
         *
         * @author cy
         * Copyright (c) CY
         */
        private Integer apvRel;
        /**
         * 字段说明：子节点（审批人 / 抄送人）列表（对应 sub_node_list）。
         *
         * @author cy
         * Copyright (c) CY
         */
        private List<NodeDetail> details;
    }

    /**
     * 类说明：节点详情。
     *
     * @author cy
     * Copyright (c) CY
     */
    @Data
    public static class NodeDetail implements Serializable {
        /**
         * 字段说明：成员 userId（对应 sub_node_list[].userid）。
         *
         * @author cy
         * Copyright (c) CY
         */
        private String approverUserId;
        /**
         * 字段说明：审批意见（对应 speech；抄送节点无）。
         *
         * @author cy
         * Copyright (c) CY
         */
        private String speech;
        /**
         * 字段说明：子节点状态（对应 sub_node_list[].sp_yj：1审批中 / 2同意 / 3驳回 / 4转审 / 11退回 / 12加签 / 13同意并加签 / 14办理 / 15转交；抄送节点无此字段，为 null）。
         *
         * @author cy
         * Copyright (c) CY
         */
        private Integer spYj;
        /**
         * 字段说明：审批 / 抄送时间（对应 sptime）。
         *
         * @author cy
         * Copyright (c) CY
         */
        private Long spTime;
    }

    /**
     * 类说明：评论项。
     *
     * @author cy
     * Copyright (c) CY
     */
    @Data
    public static class CommentItem implements Serializable {
        /**
         * 字段说明：成员 userId。
         *
         * @author cy
         * Copyright (c) CY
         */
        private String userId;
        /**
         * 字段说明：内容。
         *
         * @author cy
         * Copyright (c) CY
         */
        private String content;
        /**
         * 字段说明：评论时间。
         *
         * @author cy
         * Copyright (c) CY
         */
        private Long commentTime;
    }
}
