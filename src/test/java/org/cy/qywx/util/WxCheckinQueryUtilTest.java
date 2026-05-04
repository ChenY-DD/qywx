package org.cy.qywx.util;

import me.chanjar.weixin.cp.api.WxCpOaService;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinDayData;
import me.chanjar.weixin.cp.bean.oa.WxCpCropCheckinOption;
import org.cy.qywx.vo.WxCheckinGroupVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

class WxCheckinQueryUtilTest {

    WxCpService cpService;
    WxCpOaService oaService;
    WxCheckinQueryUtil util;

    @BeforeEach
    void setUp() {
        cpService = Mockito.mock(WxCpService.class);
        oaService = Mockito.mock(WxCpOaService.class);
        when(cpService.getOaService()).thenReturn(oaService);
        util = new WxCheckinQueryUtil(
                cpService,
                Executors.newFixedThreadPool(4),
                new WxCheckinQueryOptions(30, 100, 3, 1L, 0D));
    }

    @Test
    void getCheckinGroups_returnsConvertedList() throws Exception {
        WxCpCropCheckinOption option = new WxCpCropCheckinOption();
        option.setGroupId(7L);
        option.setGroupName("HQ");
        when(oaService.getCropCheckinOption()).thenReturn(List.of(option));

        List<WxCheckinGroupVO> result = util.getCheckinGroups();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGroupId()).isEqualTo(7);
        assertThat(result.get(0).getGroupName()).isEqualTo("HQ");
    }

    @Test
    void getCheckinGroups_emptyResponseReturnsEmpty() throws Exception {
        when(oaService.getCropCheckinOption()).thenReturn(List.of());

        assertThat(util.getCheckinGroups()).isEmpty();
    }

    @Test
    void getCheckinDayData_segmentsAndBatches() throws Exception {
        AtomicInteger callCount = new AtomicInteger();
        when(oaService.getCheckinDayData(any(), any(), anyList())).thenAnswer(inv -> {
            callCount.incrementAndGet();
            WxCpCheckinDayData d = new WxCpCheckinDayData();
            WxCpCheckinDayData.BaseInfo base = new WxCpCheckinDayData.BaseInfo();
            base.setName("u");
            base.setAcctId("u" + callCount.get());
            d.setBaseInfo(base);
            return List.of(d);
        });

        Date start = new Date(1714521600000L);
        Date end = new Date(start.getTime() + 90L * 86_400_000L);
        List<String> userIds = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            userIds.add("u" + i);
        }

        WxCheckinDayDataResult result = util.getCheckinDayData(start, end, userIds);

        assertThat(callCount.get()).isEqualTo(9);
        assertThat(result.dayDataList()).hasSize(9);
        assertThat(result.failures()).isEmpty();
    }

    @Test
    void getCheckinDayData_collectsFailuresAfterRetry() throws Exception {
        when(oaService.getCheckinDayData(any(), any(), anyList()))
                .thenThrow(new RuntimeException("boom"));

        Date start = new Date(1714521600000L);
        Date end = new Date(start.getTime() + 86_400_000L);
        List<String> userIds = List.of("u1");

        WxCheckinDayDataResult result = util.getCheckinDayData(start, end, userIds);

        assertThat(result.dayDataList()).isEmpty();
        assertThat(result.failures()).hasSize(1);
        assertThat(result.failures().get(0).errorMessage()).contains("boom");
        assertThat(result.failures().get(0).attempts()).isEqualTo(3);
    }

    @Test
    void getCheckinDayData_emptyUsersShortCircuit() throws Exception {
        Date start = new Date(1714521600000L);
        Date end = new Date(start.getTime() + 86_400_000L);

        WxCheckinDayDataResult result = util.getCheckinDayData(start, end, List.of());

        assertThat(result.dayDataList()).isEmpty();
        assertThat(result.failures()).isEmpty();
        Mockito.verifyNoInteractions(oaService);
    }
}
