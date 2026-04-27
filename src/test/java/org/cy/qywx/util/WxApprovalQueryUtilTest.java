package org.cy.qywx.util;

import me.chanjar.weixin.cp.api.WxCpOaService;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalDetailResult;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalInfo;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalInfoQueryFilter;
import me.chanjar.weixin.cp.bean.oa.WxCpOaApprovalTemplateResult;
import org.cy.qywx.vo.WxApprovalDetailVO;
import org.cy.qywx.vo.WxApprovalTemplateVO;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.argThat;
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
        when(oaService.getApprovalInfo(any(Date.class), any(Date.class), nullable(String.class), anyInt(), nullable(List.class)))
                .thenAnswer(invocation -> {
                    int current = index.getAndIncrement();
                    WxCpApprovalInfo info = new WxCpApprovalInfo();
                    if (current == 0) {
                        info.setSpNoList(List.of("A-1"));
                        info.setNewNextCursor("cursor-1");
                    } else if (current == 1) {
                        info.setSpNoList(List.of("A-2"));
                        info.setNewNextCursor(null);
                    } else {
                        info.setSpNoList(List.of("B-1"));
                        info.setNewNextCursor(null);
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
                .getApprovalInfo(any(Date.class), any(Date.class), nullable(String.class), anyInt(), nullable(List.class));
    }

    @Test
    void shouldCollectFailuresAndRetryWhenDetailQueryFails() throws Exception {
        WxCpService wxCpService = mock(WxCpService.class);
        WxCpOaService oaService = mock(WxCpOaService.class);
        when(wxCpService.getOaService()).thenReturn(oaService);

        WxCpApprovalInfo info = new WxCpApprovalInfo();
        info.setSpNoList(List.of("sp-ok", "sp-fail"));
        info.setNewNextCursor(null);
        when(oaService.getApprovalInfo(any(Date.class), any(Date.class), nullable(String.class), anyInt(), nullable(List.class)))
                .thenReturn(info);

        WxCpApprovalDetailResult okResult = createApprovalDetailResult("sp-ok", "temp-1");
        when(oaService.getApprovalDetail("sp-ok")).thenReturn(okResult);
        when(oaService.getApprovalDetail("sp-fail")).thenThrow(new RuntimeException("mock boom"));

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
    void shouldQueryByTemplateIdAndResolveTemplatesBySpNos() throws Exception {
        WxCpService wxCpService = mock(WxCpService.class);
        WxCpOaService oaService = mock(WxCpOaService.class);
        when(wxCpService.getOaService()).thenReturn(oaService);

        WxCpApprovalInfo info = new WxCpApprovalInfo();
        info.setSpNoList(List.of("sp-1", "sp-2"));
        info.setNewNextCursor(null);
        when(oaService.getApprovalInfo(
                any(Date.class),
                any(Date.class),
                nullable(String.class),
                anyInt(),
                argThat(filters -> filters != null
                        && filters.size() == 1
                        && filters.get(0).getKey() == WxCpApprovalInfoQueryFilter.KEY.TEMPLATE_ID
                        && "temp-1".equals(filters.get(0).getValue()))
        )).thenReturn(info);

        when(oaService.getApprovalDetail("sp-1")).thenReturn(createApprovalDetailResult("sp-1", "temp-1"));
        when(oaService.getApprovalDetail("sp-2")).thenReturn(createApprovalDetailResult("sp-2", "temp-2"));

        WxCpOaApprovalTemplateResult template1 = new WxCpOaApprovalTemplateResult();
        WxCpOaApprovalTemplateResult template2 = new WxCpOaApprovalTemplateResult();
        when(oaService.getTemplateDetail("temp-1")).thenReturn(template1);
        when(oaService.getTemplateDetail("temp-2")).thenReturn(template2);

        WxApprovalQueryUtil util = new WxApprovalQueryUtil(
                wxCpService,
                DIRECT_EXECUTOR,
                new WxApprovalQueryOptions(29, 100, 3, 0, 0)
        );

        List<WxApprovalDetailVO> details = util.getApprovalDetailsByTemplateId("temp-1", new Date(0), new Date(1000));
        assertEquals(2, details.size());

        List<String> spNos = util.getApprovalSpNosByTemplateId("temp-1", WxDateRangeUtils.custom(new Date(0), new Date(1000)));
        assertEquals(List.of("sp-1", "sp-2"), spNos);

        Map<String, String> templateIdsBySpNo = util.getTemplateIdsBySpNos("sp-1", "sp-2");
        assertEquals("temp-1", templateIdsBySpNo.get("sp-1"));
        assertEquals("temp-2", templateIdsBySpNo.get("sp-2"));

        Map<String, WxCpOaApprovalTemplateResult> templateDetailsBySpNo = util.getTemplateDetailsBySpNos("sp-1", "sp-2");
        assertEquals(template1, templateDetailsBySpNo.get("sp-1"));
        assertEquals(template2, templateDetailsBySpNo.get("sp-2"));
    }

    @Test
    void shouldReturnTemplateListAndMap() throws Exception {
        WxCpService wxCpService = mock(WxCpService.class);
        WxCpOaService oaService = mock(WxCpOaService.class);
        when(wxCpService.getOaService()).thenReturn(oaService);

        WxCpApprovalInfo info = new WxCpApprovalInfo();
        info.setSpNoList(List.of("sp-1", "sp-2", "sp-3"));
        info.setNewNextCursor(null);
        when(oaService.getApprovalInfo(any(Date.class), any(Date.class), nullable(String.class), anyInt(), nullable(List.class)))
                .thenReturn(info);

        when(oaService.getApprovalDetail("sp-1")).thenReturn(createApprovalDetailResult("sp-1", "temp-1", "请假"));
        when(oaService.getApprovalDetail("sp-2")).thenReturn(createApprovalDetailResult("sp-2", "temp-2", "报销"));
        when(oaService.getApprovalDetail("sp-3")).thenReturn(createApprovalDetailResult("sp-3", "temp-1", "请假"));

        WxApprovalQueryUtil util = new WxApprovalQueryUtil(
                wxCpService,
                DIRECT_EXECUTOR,
                new WxApprovalQueryOptions(29, 100, 3, 0, 0)
        );

        List<WxApprovalTemplateVO> templates = util.getTemplates(new Date(0), new Date(1000));
        Map<String, WxApprovalTemplateVO> templateMap = util.getTemplateMap(new Date(0), new Date(1000));

        assertEquals(2, templates.size());
        assertEquals("temp-1", templates.get(0).getTemplateId());
        assertEquals("请假", templates.get(0).getTemplateName());
        assertEquals("报销", templateMap.get("temp-2").getTemplateName());
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

    private WxCpApprovalDetailResult createApprovalDetailResult(String spNo, String templateId) {
        return createApprovalDetailResult(spNo, templateId, null);
    }

    private WxCpApprovalDetailResult createApprovalDetailResult(String spNo, String templateId, String spName) {
        WxCpApprovalDetailResult detailResult = new WxCpApprovalDetailResult();
        WxCpApprovalDetailResult.WxCpApprovalDetail detail = new WxCpApprovalDetailResult.WxCpApprovalDetail();
        detail.setSpNo(spNo);
        detail.setTemplateId(templateId);
        detail.setSpName(spName);
        detailResult.setInfo(detail);
        return detailResult;
    }
}
