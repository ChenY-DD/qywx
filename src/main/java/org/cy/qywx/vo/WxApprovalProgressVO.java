package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 类说明：审批流转进度与卡点分析视图对象。
 *
 * <p>由 {@link WxApprovalDetailVO} 派生，除携带审批单基本信息外，基于其 {@code nodes}
 * （来自 process_list.node_list）计算审批人链路、各节点耗时、当前卡点（卡在谁 / 已等待多久 /
 * 下一个审批人）等时效分析信息。仅统计审批（node_type=1）与办理（node_type=3）节点，
 * 抄送（node_type=2）不计入。所有枚举字段均输出中文文本而非原始数值。</p>
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxApprovalProgressVO implements Serializable {

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
     * 字段说明：审批状态中文文本（如 审批中 / 已通过 / 已驳回 / 已撤销 / 通过后撤销 / 已删除 / 已支付）。
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
     * 字段说明：审批申请提交时间（epoch 秒）。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Long applyTime;
    /**
     * 字段说明：审批关闭时间（epoch 秒）；未结束时为 null。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Long closeTime;
    /**
     * 字段说明：审批从提交到关闭的耗时（秒）；未结束时为 null。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Long closeLoopDurationSeconds;
    /**
     * 字段说明：从提交到当前的持续时长（秒，复用 WxApprovalDetailVO.currentDurationSeconds）。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Long submittedToNowSeconds;
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
     * 字段说明：审批流转链路（仅审批 / 办理节点，按流程顺序，排除抄送）。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<NodeProgress> nodeChain;
    /**
     * 字段说明：当前卡点；审批已结束时为 null。
     *
     * @author cy
     * Copyright (c) CY
     */
    private CurrentBlock currentBlock;

    /**
     * 类说明：单个流转节点的进度信息。
     *
     * @author cy
     * Copyright (c) CY
     */
    @Data
    public static class NodeProgress implements Serializable {
        /**
         * 字段说明：链路序号（0 起）。
         *
         * @author cy
         * Copyright (c) CY
         */
        private int index;
        /**
         * 字段说明：节点类型中文文本（审批 / 办理）。
         *
         * @author cy
         * Copyright (c) CY
         */
        private String nodeType;
        /**
         * 字段说明：多人办理方式中文文本（会签 / 或签 / 依次审批）。
         *
         * @author cy
         * Copyright (c) CY
         */
        private String apvRel;
        /**
         * 字段说明：节点状态中文文本（审批中 / 同意 / 驳回 / 转审 / 退回 / 加签 / 同意并加签 / 办理 / 转交）。
         *
         * @author cy
         * Copyright (c) CY
         */
        private String spStatus;
        /**
         * 字段说明：本节点的处理人 userId 列表。
         *
         * @author cy
         * Copyright (c) CY
         */
        private List<String> approverUserIds;
        /**
         * 字段说明：节点开始时刻（上一节点完成时刻；首个节点为 applyTime）。未开始为 null。
         *
         * @author cy
         * Copyright (c) CY
         */
        private Long startTime;
        /**
         * 字段说明：节点完成时刻（epoch 秒）。未完成为 null。
         *
         * @author cy
         * Copyright (c) CY
         */
        private Long completeTime;
        /**
         * 字段说明：本节点耗时（秒，completeTime - startTime）。未完成为 null。
         *
         * @author cy
         * Copyright (c) CY
         */
        private Long durationSeconds;
        /**
         * 字段说明：是否为当前卡点节点。
         *
         * @author cy
         * Copyright (c) CY
         */
        private boolean blocked;
        /**
         * 字段说明：本节点尚未处理的人 userId 列表（sptime 为空）。
         *
         * @author cy
         * Copyright (c) CY
         */
        private List<String> pendingUserIds;
    }

    /**
     * 类说明：当前卡点信息。
     *
     * @author cy
     * Copyright (c) CY
     */
    @Data
    public static class CurrentBlock implements Serializable {
        /**
         * 字段说明：卡在链路中第几个节点（nodeChain 下标）。
         *
         * @author cy
         * Copyright (c) CY
         */
        private Integer nodeIndex;
        /**
         * 字段说明：卡在哪些人 userId（当前节点未处理人）。
         *
         * @author cy
         * Copyright (c) CY
         */
        private List<String> blockingUserIds;
        /**
         * 字段说明：当前卡点已等待时长（秒，now - 节点开始时刻）。
         *
         * @author cy
         * Copyright (c) CY
         */
        private Long waitingSeconds;
        /**
         * 字段说明：下一个审批 / 办理节点的处理人 userId 列表。
         *
         * @author cy
         * Copyright (c) CY
         */
        private List<String> nextApproverUserIds;
    }
}
