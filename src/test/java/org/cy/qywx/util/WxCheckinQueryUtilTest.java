package org.cy.qywx.util;

import me.chanjar.weixin.cp.api.WxCpOaService;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinData;
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinDayData;
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinMonthData;
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinSchedule;
import me.chanjar.weixin.cp.bean.oa.WxCpCropCheckinOption;
import org.cy.qywx.vo.WxAttendanceReportVO;
import org.cy.qywx.vo.WxCheckinExceptionItemVO;
import org.cy.qywx.vo.WxCheckinGroupVO;
import org.cy.qywx.vo.WxCheckinScheduleListItemVO;
import org.cy.qywx.vo.enums.WxCheckinExceptionTypeEnum;
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
import static org.mockito.ArgumentMatchers.eq;
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

    private WxCpCheckinDayData buildDayData(String userId, int dateUnix,
                                            int exceptionCode, int count, int durationSec) {
        WxCpCheckinDayData d = new WxCpCheckinDayData();
        WxCpCheckinDayData.BaseInfo base = new WxCpCheckinDayData.BaseInfo();
        base.setName("u");
        base.setAcctId(userId);
        base.setDate(dateUnix);
        d.setBaseInfo(base);

        WxCpCheckinDayData.ExceptionInfos ex = new WxCpCheckinDayData.ExceptionInfos();
        ex.setException(exceptionCode);
        ex.setCount(count);
        ex.setDuration(durationSec);
        d.setExceptionInfos(List.of(ex));
        return d;
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

    @Test
    void getCheckinRecords_appliesType3WhenOmitted() throws Exception {
        WxCpCheckinData d = new WxCpCheckinData();
        d.setUserId("u1");
        d.setCheckinTime(1714521600L);
        when(oaService.getCheckinData(eq(3), any(), any(), anyList())).thenReturn(List.of(d));

        Date start = new Date(1714521600000L);
        Date end = new Date(start.getTime() + 86_400_000L);

        WxCheckinRecordResult result = util.getCheckinRecords(start, end, List.of("u1"));

        assertThat(result.records()).hasSize(1);
        assertThat(result.records().get(0).getUserId()).isEqualTo("u1");
    }

    @Test
    void getCheckinRecords_passesExplicitType() throws Exception {
        WxCpCheckinData d = new WxCpCheckinData();
        d.setUserId("u1");
        d.setCheckinTime(1714521600L);
        when(oaService.getCheckinData(eq(2), any(), any(), anyList())).thenReturn(List.of(d));

        Date start = new Date(1714521600000L);
        Date end = new Date(start.getTime() + 86_400_000L);

        WxCheckinRecordResult result = util.getCheckinRecords(
                WxCheckinQueryUtil.CHECKIN_TYPE_OUTSIDE, start, end, List.of("u1"));

        assertThat(result.records()).hasSize(1);
    }

    @Test
    void getCheckinMonthData_returnsConvertedList() throws Exception {
        WxCpCheckinMonthData m = new WxCpCheckinMonthData();
        WxCpCheckinMonthData.BaseInfo base = new WxCpCheckinMonthData.BaseInfo();
        base.setName("Li Si");
        base.setAcctId("u1");
        m.setBaseInfo(base);
        when(oaService.getCheckinMonthData(any(), any(), anyList())).thenReturn(List.of(m));

        Date start = new Date(1714521600000L);
        Date end = new Date(start.getTime() + 28L * 86_400_000L);

        WxCheckinMonthDataResult result = util.getCheckinMonthData(start, end, List.of("u1"));

        assertThat(result.monthDataList()).hasSize(1);
        assertThat(result.monthDataList().get(0).getName()).isEqualTo("Li Si");
        assertThat(result.monthDataList().get(0).getUserId()).isEqualTo("u1");
    }

    @Test
    void getScheduleList_returnsFlatList() throws Exception {
        WxCpCheckinSchedule s = new WxCpCheckinSchedule();
        s.setUserid("u1");
        s.setYearmonth(202404);
        when(oaService.getCheckinScheduleList(any(), any(), anyList())).thenReturn(List.of(s));

        Date start = new Date(1714521600000L);
        Date end = new Date(start.getTime() + 86_400_000L);

        List<WxCheckinScheduleListItemVO> result = util.getScheduleList(start, end, List.of("u1"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("u1");
    }

    @Test
    void getLatePersons_filtersToLateOnly() throws Exception {
        WxCpCheckinDayData d = buildDayData("u1", 1714521600, 1, 1, 600);
        WxCpCheckinDayData.ExceptionInfos absent = new WxCpCheckinDayData.ExceptionInfos();
        absent.setException(4);
        absent.setCount(1);
        absent.setDuration(28800);
        d.setExceptionInfos(new ArrayList<>(d.getExceptionInfos()));
        d.getExceptionInfos().add(absent);

        when(oaService.getCheckinDayData(any(), any(), anyList())).thenReturn(List.of(d));

        Date start = new Date(1714521600000L);
        Date end = new Date(start.getTime() + 86_400_000L);

        List<WxCheckinExceptionItemVO> lates = util.getLatePersons(start, end, List.of("u1"));

        assertThat(lates).hasSize(1);
        assertThat(lates.get(0).getExceptionType()).isEqualTo(WxCheckinExceptionTypeEnum.LATE);
        assertThat(lates.get(0).getExceptionCount()).isEqualTo(1);
    }

    @Test
    void getAttendanceReport_singleApiCallMultipleBuckets() throws Exception {
        AtomicInteger callCount = new AtomicInteger();
        when(oaService.getCheckinDayData(any(), any(), anyList())).thenAnswer(inv -> {
            callCount.incrementAndGet();
            WxCpCheckinDayData d = buildDayData("u1", 1714521600, 1, 1, 600);
            WxCpCheckinDayData.ExceptionInfos absent = new WxCpCheckinDayData.ExceptionInfos();
            absent.setException(4);
            absent.setCount(1);
            absent.setDuration(0);
            d.setExceptionInfos(new ArrayList<>(d.getExceptionInfos()));
            d.getExceptionInfos().add(absent);
            return List.of(d);
        });

        Date start = new Date(1714521600000L);
        Date end = new Date(start.getTime() + 86_400_000L);

        WxAttendanceReportVO report = util.getAttendanceReport(start, end, List.of("u1"));

        assertThat(callCount.get()).isEqualTo(1);
        assertThat(report.getLate()).hasSize(1);
        assertThat(report.getAbsent()).hasSize(1);
        assertThat(report.getEarlyLeave()).isEmpty();
        assertThat(report.getMissingCard()).isEmpty();
        assertThat(report.getTotalUsers()).isEqualTo(1);
        assertThat(report.getReportedUsers()).isEqualTo(1);
    }
}
