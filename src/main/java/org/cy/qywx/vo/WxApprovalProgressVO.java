package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 类说明：审批流转进度与卡点分析视图对象。
 *
 * <p>由 {@link WxApprovalDetailVO} 的 {@code nodes}（来自 process_list.node_list）派生，
 * 提供审批人链路、各节点耗时、当前卡点（卡在谁 / 已等待多久 / 下一个审批人）等时效分析信息。
 * 仅统计审批（node_type=1）与办理（node_type=3）节点，抄送（node_type=2）不计入。</p>
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
     * 字段说明：审批是否已结束（复用 WxApprovalDetailVO.closed）。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Boolean closed;
    /**
     * 字段说明：审批申请提交时间（epoch 秒，复用 WxApprovalDetailVO.applyTime）。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Long applyTime;
    /**
     * 字段说明：从提交到当前的持续时长（秒，复用 WxApprovalDetailVO.currentDurationSeconds）。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Long submittedToNowSeconds;
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
         * 字段说明：节点类型（1=审批、3=办理）。
         *
         * @author cy
         * Copyright (c) CY
         */
        private Integer nodeType;
        /**
         * 字段说明：多人办理方式（1=会签、2=或签、3=依次审批）。
         *
         * @author cy
         * Copyright (c) CY
         */
        private Integer apvRel;
        /**
         * 字段说明：节点状态（对应 process_list.node_list[].sp_status）。
         *
         * @author cy
         * Copyright (c) CY
         */
        private Integer spStatus;
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
