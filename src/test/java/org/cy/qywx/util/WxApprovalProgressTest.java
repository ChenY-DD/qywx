package org.cy.qywx.util;

import org.cy.qywx.vo.WxApprovalDetailVO;
import org.cy.qywx.vo.WxApprovalProgressVO;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WxApprovalProgressTest {

    @Test
    void shouldAnalyzeInProgressApprovalChain() {
        WxApprovalDetailVO vo = new WxApprovalDetailVO();
        vo.setSpNo("sp-100");
        vo.setSpStatus("AUDITING");
        vo.setClosed(false);
        vo.setApplyTime(1_000L);
        vo.setCurrentDurationSeconds(4_000L);
        vo.setSpName("请假申请单");
        vo.setTemplateId("tpl-xyz");
        vo.setCreatedToday(false);
        vo.setOverdueOneDay(true);
        vo.setApplicantUserId("LiXiaoMin");
        vo.setApplicantPartyId("17");
        WxApprovalDetailVO.Node orSignNode = node(1, 2, detail("A", 2_000L)); // 或签：A 已处理 → 完成
        orSignNode.setSpStatus(2);                                            // 节点状态=同意
        vo.setNodes(List.of(
                node(2, null, detail("CC1", 0L)),                       // 抄送：应被排除
                orSignNode,
                node(1, 1, detail("B", 0L), detail("C", 0L)),           // 会签：均未处理 → 卡点
                node(3, 2, detail("D", 0L))                             // 办理：尚未开始
        ));

        WxApprovalProgressVO progress = WxApprovalConverter.toProgress(vo, 5_000L);

        assertNotNull(progress);
        assertEquals("sp-100", progress.getSpNo());
        assertEquals("请假申请单", progress.getSpName());
        assertEquals("审批中", progress.getSpStatus());
        assertEquals("tpl-xyz", progress.getTemplateId());
        assertEquals("LiXiaoMin", progress.getApplicantUserId());
        assertEquals("17", progress.getApplicantPartyId());
        assertTrue(Boolean.TRUE.equals(progress.getOverdueOneDay()));
        assertFalse(Boolean.TRUE.equals(progress.getClosed()));
        assertEquals(1_000L, progress.getApplyTime());
        assertEquals(4_000L, progress.getSubmittedToNowSeconds());
        assertEquals(3, progress.getNodeChain().size(), "抄送节点应被排除，只剩 3 个流转节点");

        WxApprovalProgressVO.NodeProgress first = progress.getNodeChain().get(0);
        assertEquals("审批", first.getNodeType());
        assertEquals("或签", first.getApvRel());
        assertEquals("同意", first.getSpStatus());
        assertEquals(List.of("A"), first.getApproverUserIds());
        assertEquals(1_000L, first.getStartTime());
        assertEquals(2_000L, first.getCompleteTime());
        assertEquals(1_000L, first.getDurationSeconds(), "第一个节点耗时 = 2000 - 1000");
        assertFalse(first.isBlocked());

        WxApprovalProgressVO.NodeProgress blocked = progress.getNodeChain().get(1);
        assertEquals("会签", blocked.getApvRel());
        assertEquals(List.of("B", "C"), blocked.getApproverUserIds());
        assertEquals(2_000L, blocked.getStartTime(), "卡点节点开始时刻 = 上一节点完成时刻");
        assertNull(blocked.getCompleteTime());
        assertNull(blocked.getDurationSeconds());
        assertTrue(blocked.isBlocked());
        assertEquals(List.of("B", "C"), blocked.getPendingUserIds());

        WxApprovalProgressVO.NodeProgress notStarted = progress.getNodeChain().get(2);
        assertEquals("办理", notStarted.getNodeType());
        assertNull(notStarted.getStartTime(), "卡点之后的节点尚未开始");
        assertNull(notStarted.getCompleteTime());
        assertFalse(notStarted.isBlocked());

        WxApprovalProgressVO.CurrentBlock block = progress.getCurrentBlock();
        assertNotNull(block);
        assertEquals(1, block.getNodeIndex());
        assertEquals(List.of("B", "C"), block.getBlockingUserIds());
        assertEquals(3_000L, block.getWaitingSeconds(), "已等待 = now(5000) - 节点开始(2000)");
        assertEquals(List.of("D"), block.getNextApproverUserIds());
    }

    @Test
    void shouldCompleteOrSignNodeWhenAnyApproverActed() {
        WxApprovalDetailVO vo = new WxApprovalDetailVO();
        vo.setSpStatus("AUDITING");
        vo.setClosed(false);
        vo.setApplyTime(1_000L);
        vo.setNodes(List.of(
                node(1, 2, detail("A", 1_500L), detail("B", 0L))        // 或签：A 处理、B 未处理
        ));

        WxApprovalProgressVO progress = WxApprovalConverter.toProgress(vo, 9_000L);

        WxApprovalProgressVO.NodeProgress only = progress.getNodeChain().get(0);
        assertEquals(1_500L, only.getCompleteTime(), "或签取最早处理时间作为完成时刻");
        assertEquals(500L, only.getDurationSeconds());
        assertFalse(only.isBlocked(), "或签已有人处理即完成，不应卡点");
        assertNull(progress.getCurrentBlock(), "无未完成节点则无卡点");
    }

    @Test
    void shouldClearCurrentBlockForClosedApproval() {
        WxApprovalDetailVO vo = new WxApprovalDetailVO();
        vo.setSpStatus("REJECTED");
        vo.setClosed(true);
        vo.setApplyTime(1_000L);
        vo.setCurrentDurationSeconds(8_000L);
        vo.setCloseTime(5_000L);
        vo.setCloseLoopDurationSeconds(4_000L);
        vo.setNodes(List.of(
                node(1, 1, detail("A", 2_000L)),                        // 已处理（驳回）
                node(1, 1, detail("B", 0L))                             // 未轮到
        ));

        WxApprovalProgressVO progress = WxApprovalConverter.toProgress(vo, 9_000L);

        assertTrue(Boolean.TRUE.equals(progress.getClosed()));
        assertEquals("已驳回", progress.getSpStatus());
        assertEquals(5_000L, progress.getCloseTime());
        assertEquals(4_000L, progress.getCloseLoopDurationSeconds());
        assertNull(progress.getCurrentBlock(), "已结束单不应有当前卡点");
        assertEquals(1_000L, progress.getNodeChain().get(0).getDurationSeconds(),
                "历史节点耗时仍照常计算 = 2000 - 1000");
        assertFalse(progress.getNodeChain().get(1).isBlocked(), "已结束单不标记卡点");
    }

    @Test
    void shouldReturnEmptyChainWhenNoNodes() {
        WxApprovalDetailVO vo = new WxApprovalDetailVO();
        vo.setSpNo("sp-empty");
        vo.setClosed(false);
        vo.setApplyTime(1_000L);
        vo.setNodes(Collections.emptyList());

        WxApprovalProgressVO progress = WxApprovalConverter.toProgress(vo, 5_000L);

        assertNotNull(progress);
        assertEquals("sp-empty", progress.getSpNo());
        assertTrue(progress.getNodeChain().isEmpty());
        assertNull(progress.getCurrentBlock());
    }

    /**
     * 构造一个流程节点。
     */
    private WxApprovalDetailVO.Node node(int nodeType, Integer apvRel, WxApprovalDetailVO.NodeDetail... details) {
        WxApprovalDetailVO.Node node = new WxApprovalDetailVO.Node();
        node.setNodeType(nodeType);
        node.setApvRel(apvRel);
        node.setDetails(List.of(details));
        return node;
    }

    /**
     * 构造一个节点子项（处理人）。spTime 为 0 表示未处理。
     */
    private WxApprovalDetailVO.NodeDetail detail(String userId, Long spTime) {
        WxApprovalDetailVO.NodeDetail detail = new WxApprovalDetailVO.NodeDetail();
        detail.setApproverUserId(userId);
        detail.setSpTime(spTime);
        return detail;
    }
}
