package org.cy.qywx.util;

import me.chanjar.weixin.cp.api.WxCpOaService;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalDetailResult;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalInfo;
import org.cy.qywx.vo.WxApprovalDetailVO;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WxApprovalQueryUtilTest {

    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    @Test
    void shouldQuerySpNosBySegmentAndCursor() throws Exception {
        WxCpService wxCpService = mock(WxCpService.class);
        WxCpOaService oaService = mock(WxCpOaService.class);
        when(wxCpService.getOaService()).thenReturn(oaService);

        AtomicInteger index = new AtomicInteger(0);
        when(oaService.getApprovalInfo(any(Date.class), any(Date.class), anyInt(), anyInt(), isNull()))
                .thenAnswer(invocation -> {
                    int current = index.getAndIncrement();
                    WxCpApprovalInfo info = new WxCpApprovalInfo();
                    if (current == 0) {
                        info.setSpNoList(List.of("A-1"));
                        info.setNextCursor(1);
                    } else if (current == 1) {
                        info.setSpNoList(List.of("A-2"));
                        info.setNextCursor(1);
                    } else {
                        info.setSpNoList(List.of("B-1"));
                        info.setNextCursor(0);
                    }
                    return info;
                });

        WxApprovalQueryUtil util = new WxApprovalQueryUtil(
                wxCpService,
                DIRECT_EXECUTOR,
                new WxApprovalQueryOptions(1, 100, 3, 0, 0)
        );

        Date start = new Date(0);
        Date end = new Date(2L * 24 * 60 * 60 * 1000);
        List<String> spNos = util.getApprovalSpNos(start, end);

        assertEquals(List.of("A-1", "A-2", "B-1"), spNos);
        verify(oaService, times(3))
                .getApprovalInfo(any(Date.class), any(Date.class), anyInt(), anyInt(), isNull());
    }

    @Test
    void shouldCollectFailuresAndRetryWhenDetailQueryFails() throws Exception {
        WxCpService wxCpService = mock(WxCpService.class);
        WxCpOaService oaService = mock(WxCpOaService.class);
        when(wxCpService.getOaService()).thenReturn(oaService);

        WxCpApprovalInfo info = new WxCpApprovalInfo();
        info.setSpNoList(List.of("sp-ok", "sp-fail"));
        info.setNextCursor(0);
        when(oaService.getApprovalInfo(any(Date.class), any(Date.class), anyInt(), anyInt(), isNull()))
                .thenReturn(info);

        WxCpApprovalDetailResult okResult = new WxCpApprovalDetailResult();
        WxCpApprovalDetailResult.WxCpApprovalDetail okDetail = new WxCpApprovalDetailResult.WxCpApprovalDetail();
        okDetail.setSpNo("sp-ok");
        okDetail.setTemplateId("temp-1");
        okResult.setInfo(okDetail);

        when(oaService.getApprovalDetail(eq("sp-ok"))).thenReturn(okResult);
        when(oaService.getApprovalDetail(eq("sp-fail"))).thenThrow(new RuntimeException("mock boom"));

        WxApprovalQueryUtil util = new WxApprovalQueryUtil(
                wxCpService,
                DIRECT_EXECUTOR,
                new WxApprovalQueryOptions(29, 100, 3, 0, 0)
        );

        WxApprovalDetailQueryResult result = util.queryApprovalDetails(new Date(0), new Date(1_000));
        List<WxApprovalDetailVO> details = result.details();
        List<WxApprovalDetailFetchFailure> failures = result.failures();

        assertEquals(1, details.size());
        assertEquals("sp-ok", details.get(0).getSpNo());
        assertEquals(1, failures.size());
        assertEquals("sp-fail", failures.get(0).spNo());
        assertEquals(3, failures.get(0).attempts());
        verify(oaService, times(1)).getApprovalDetail("sp-ok");
        verify(oaService, times(3)).getApprovalDetail("sp-fail");
    }

    @Test
    void shouldThrowWhenTimeRangeIsInvalid() {
        WxCpService wxCpService = mock(WxCpService.class);
        WxApprovalQueryUtil util = new WxApprovalQueryUtil(
                wxCpService,
                DIRECT_EXECUTOR,
                new WxApprovalQueryOptions(29, 100, 3, 0, 0)
        );

        assertThrows(IllegalArgumentException.class, () -> util.getApprovalSpNos(new Date(1000), new Date(1000)));
    }
}
