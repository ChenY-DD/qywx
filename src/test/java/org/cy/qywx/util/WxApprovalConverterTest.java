package org.cy.qywx.util;

import me.chanjar.weixin.cp.bean.oa.WxCpApprovalComment;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalDetailResult;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalRecord;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalRecordDetail;
import me.chanjar.weixin.cp.bean.oa.WxCpOperator;
import me.chanjar.weixin.cp.bean.oa.WxCpSpStatus;
import org.cy.qywx.vo.WxApprovalDetailVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WxApprovalConverterTest {

    @Test
    void shouldFillClosedLoopFieldsForClosedApproval() {
        WxCpApprovalDetailResult detailResult = new WxCpApprovalDetailResult();
        WxCpApprovalDetailResult.WxCpApprovalDetail detail = new WxCpApprovalDetailResult.WxCpApprovalDetail();
        detail.setSpNo("sp-1");
        detail.setSpName("请假");
        detail.setSpStatus(WxCpSpStatus.PASSED);
        detail.setTemplateId("temp-1");
        detail.setApplyTime(1_000L);

        WxCpApprovalRecord record = new WxCpApprovalRecord();
        WxCpApprovalRecordDetail recordDetail = new WxCpApprovalRecordDetail();
        recordDetail.setSpTime(3_600L);
        record.setDetails(List.of(recordDetail));
        detail.setSpRecords(new WxCpApprovalRecord[]{record});

        WxCpApprovalComment comment = new WxCpApprovalComment();
        comment.setCommentTime(4_000L);
        WxCpOperator operator = new WxCpOperator();
        operator.setUserId("u1");
        comment.setCommentUserInfo(operator);
        detail.setComments(List.of(comment));

        detailResult.setInfo(detail);

        WxApprovalDetailVO vo = WxApprovalConverter.from(detailResult, 200_000L);

        assertTrue(Boolean.TRUE.equals(vo.getClosed()));
        assertEquals(4_000L, vo.getCloseTime());
        assertEquals(3_000L, vo.getCloseLoopDurationSeconds());
        assertEquals(199_000L, vo.getCurrentDurationSeconds());
        assertFalse(Boolean.TRUE.equals(vo.getCreatedToday()));
        assertTrue(Boolean.TRUE.equals(vo.getOverdueOneDay()));
    }

    @Test
    void shouldFillRunningFieldsForAuditingApproval() {
        WxCpApprovalDetailResult detailResult = new WxCpApprovalDetailResult();
        WxCpApprovalDetailResult.WxCpApprovalDetail detail = new WxCpApprovalDetailResult.WxCpApprovalDetail();
        detail.setSpNo("sp-2");
        detail.setSpName("报销");
        detail.setSpStatus(WxCpSpStatus.AUDITING);
        detail.setTemplateId("temp-2");
        detail.setApplyTime(10L);
        detailResult.setInfo(detail);

        WxApprovalDetailVO vo = WxApprovalConverter.from(detailResult, 100_000L);

        assertFalse(Boolean.TRUE.equals(vo.getClosed()));
        assertEquals(null, vo.getCloseTime());
        assertEquals(null, vo.getCloseLoopDurationSeconds());
        assertEquals(99_990L, vo.getCurrentDurationSeconds());
        assertTrue(Boolean.TRUE.equals(vo.getOverdueOneDay()));
    }
}
