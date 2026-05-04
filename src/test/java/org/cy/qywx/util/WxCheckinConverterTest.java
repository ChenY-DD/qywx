package org.cy.qywx.util;

import me.chanjar.weixin.cp.bean.oa.WxCpCheckinData;
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinDayData;
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinMonthData;
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinSchedule;
import me.chanjar.weixin.cp.bean.oa.WxCpCropCheckinOption;
import org.cy.qywx.vo.WxCheckinDayDataVO;
import org.cy.qywx.vo.WxCheckinExceptionItemVO;
import org.cy.qywx.vo.WxCheckinExceptionVO;
import org.cy.qywx.vo.WxCheckinGroupVO;
import org.cy.qywx.vo.WxCheckinMonthDataVO;
import org.cy.qywx.vo.WxCheckinRecordVO;
import org.cy.qywx.vo.WxCheckinScheduleListItemVO;
import org.cy.qywx.vo.enums.WxCheckinExceptionTypeEnum;
import org.cy.qywx.vo.enums.WxCheckinResultEnum;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WxCheckinConverterTest {

    @Test
    void fromRecord_mapsCoreFields() {
        WxCpCheckinData src = new WxCpCheckinData();
        src.setUserId("u-001");
        src.setGroupName("Headquarters Group");
        src.setGroupId(10);
        src.setCheckinType("On Duty");
        src.setCheckinTime(1714800000L);
        src.setExceptionType("时间异常");
        src.setLocationTitle("Office SH");
        src.setWifiName("Office-5G");
        src.setMediaIds(List.of("m1", "m2"));

        WxCheckinRecordVO vo = WxCheckinConverter.fromRecord(src);

        assertThat(vo.getUserId()).isEqualTo("u-001");
        assertThat(vo.getGroupName()).isEqualTo("Headquarters Group");
        assertThat(vo.getCheckinType()).isEqualTo("On Duty");
        assertThat(vo.getCheckinTime().getTime()).isEqualTo(1714800000L * 1000L);
        assertThat(vo.getRawExceptionType()).isEqualTo("时间异常");
        assertThat(vo.getExceptionResult()).isEqualTo(WxCheckinResultEnum.TIME_EXCEPTION);
        assertThat(vo.getMediaIds()).containsExactly("m1", "m2");
    }

    @Test
    void fromRecord_handlesNullCheckinTime() {
        WxCpCheckinData src = new WxCpCheckinData();
        src.setUserId("u-002");

        WxCheckinRecordVO vo = WxCheckinConverter.fromRecord(src);

        assertThat(vo.getCheckinTime()).isNull();
        assertThat(vo.getExceptionResult()).isEqualTo(WxCheckinResultEnum.UNKNOWN);
    }

    @Test
    void fromRecord_nullSourceReturnsNull() {
        assertThat(WxCheckinConverter.fromRecord(null)).isNull();
    }

    @Test
    void fromDayData_mapsBaseInfoAndExceptions() {
        WxCpCheckinDayData src = new WxCpCheckinDayData();
        WxCpCheckinDayData.BaseInfo base = new WxCpCheckinDayData.BaseInfo();
        base.setName("Zhang San");
        base.setDepartsName("RD;OPS");
        base.setDate(1714800000);
        base.setRecordType(1);
        base.setAcctId("u-001");
        src.setBaseInfo(base);

        WxCpCheckinDayData.SummaryInfo summary = new WxCpCheckinDayData.SummaryInfo();
        summary.setStandardWorkSec(28800);
        summary.setRegularWorkSec(28800);
        summary.setEarliestTime(32400);
        summary.setLastestTime(64800);
        src.setSummaryInfo(summary);

        WxCpCheckinDayData.ExceptionInfos ex = new WxCpCheckinDayData.ExceptionInfos();
        ex.setException(1);
        ex.setCount(1);
        ex.setDuration(900);
        src.setExceptionInfos(List.of(ex));

        WxCheckinDayDataVO vo = WxCheckinConverter.fromDayData(src);

        assertThat(vo.getUserId()).isEqualTo("u-001");
        assertThat(vo.getName()).isEqualTo("Zhang San");
        assertThat(vo.getDepartments()).containsExactly("RD", "OPS");
        assertThat(vo.getRecordType()).isEqualTo(1);
        assertThat(vo.getStandardWorkSeconds()).isEqualTo(28800);
        assertThat(vo.getRegularWorkSeconds()).isEqualTo(28800);
        assertThat(vo.getDate().getTime()).isEqualTo(1714800000L * 1000L);
        assertThat(vo.getExceptions()).hasSize(1);
        assertThat(vo.getExceptions().get(0).getExceptionType()).isEqualTo(WxCheckinExceptionTypeEnum.LATE);
        assertThat(vo.getExceptions().get(0).getCount()).isEqualTo(1);
        assertThat(vo.getExceptions().get(0).getDurationSeconds()).isEqualTo(900);
    }

    @Test
    void fromDayData_nullBaseInfoIsTolerant() {
        WxCpCheckinDayData src = new WxCpCheckinDayData();

        WxCheckinDayDataVO vo = WxCheckinConverter.fromDayData(src);

        assertThat(vo).isNotNull();
        assertThat(vo.getUserId()).isNull();
        assertThat(vo.getExceptions()).isEmpty();
    }

    @Test
    void fromMonthData_mapsBaseInfoAndExceptions() {
        WxCpCheckinMonthData src = new WxCpCheckinMonthData();
        WxCpCheckinMonthData.BaseInfo base = new WxCpCheckinMonthData.BaseInfo();
        base.setName("Li Si");
        base.setDepartsName("Sales");
        base.setAcctId("u-002");
        src.setBaseInfo(base);

        WxCpCheckinMonthData.SummaryInfo summary = new WxCpCheckinMonthData.SummaryInfo();
        summary.setWorkDays(22);
        summary.setExceptDays(2);
        summary.setRegularWorkSec(160 * 3600);
        summary.setStandardWorkSec(176 * 3600);
        src.setSummaryInfo(summary);

        WxCpCheckinMonthData.ExceptionInfo ex = new WxCpCheckinMonthData.ExceptionInfo();
        ex.setException(4);
        ex.setCount(1);
        ex.setDuration(28800);
        src.setExceptionInfos(List.of(ex));

        WxCheckinMonthDataVO vo = WxCheckinConverter.fromMonthData(src);

        assertThat(vo.getUserId()).isEqualTo("u-002");
        assertThat(vo.getName()).isEqualTo("Li Si");
        assertThat(vo.getDepartments()).containsExactly("Sales");
        assertThat(vo.getWorkDays()).isEqualTo(22);
        assertThat(vo.getExcludedDays()).isEqualTo(2);
        assertThat(vo.getWorkSeconds()).isEqualTo(160 * 3600);
        assertThat(vo.getStandardWorkSeconds()).isEqualTo(176 * 3600);
        assertThat(vo.getExceptions()).hasSize(1);
        assertThat(vo.getExceptions().get(0).getExceptionType()).isEqualTo(WxCheckinExceptionTypeEnum.ABSENT);
    }

    @Test
    void fromGroup_mapsBasicFields() {
        WxCpCropCheckinOption src = new WxCpCropCheckinOption();
        src.setGroupId(7L);
        src.setGroupName("HQ");
        src.setGroupType(1L);
        src.setCreateTime(1714521600L);
        src.setNeedPhoto(true);
        src.setSyncHolidays(false);
        src.setWhiteUsers(List.of("admin"));

        WxCheckinGroupVO vo = WxCheckinConverter.fromGroup(src);

        assertThat(vo.getGroupId()).isEqualTo(7);
        assertThat(vo.getGroupName()).isEqualTo("HQ");
        assertThat(vo.getGroupType()).isEqualTo("1");
        assertThat(vo.getCreateTime().getTime()).isEqualTo(1714521600L * 1000L);
        assertThat(vo.getNeedPhoto()).isTrue();
        assertThat(vo.getSyncHolidays()).isFalse();
        assertThat(vo.getWhiteUsers()).containsExactly("admin");
    }

    @Test
    void fromScheduleItem_mapsCoreFields() {
        WxCpCheckinSchedule src = new WxCpCheckinSchedule();
        src.setUserid("u-007");
        src.setYearmonth(202404);
        src.setGroupid(3);
        src.setGroupName("RD Group");

        WxCheckinScheduleListItemVO vo = WxCheckinConverter.fromScheduleItem(src);

        assertThat(vo.getUserId()).isEqualTo("u-007");
        assertThat(vo.getGroupId()).isEqualTo(3);
        assertThat(vo.getScheduleDate()).isNotNull();
    }

    @Test
    void explodeExceptions_filtersByType() {
        WxCheckinDayDataVO day = new WxCheckinDayDataVO();
        day.setUserId("u-1");
        day.setName("Zhang San");
        day.setDate(new Date(1714800000L * 1000));
        day.setDepartments(List.of("RD"));

        WxCheckinExceptionVO late = new WxCheckinExceptionVO();
        late.setExceptionType(WxCheckinExceptionTypeEnum.LATE);
        late.setCount(2);
        late.setDurationSeconds(1800);

        WxCheckinExceptionVO absent = new WxCheckinExceptionVO();
        absent.setExceptionType(WxCheckinExceptionTypeEnum.ABSENT);
        absent.setCount(1);
        absent.setDurationSeconds(28800);

        day.setExceptions(List.of(late, absent));

        List<WxCheckinExceptionItemVO> lates =
                WxCheckinConverter.explodeExceptions(day, WxCheckinExceptionTypeEnum.LATE);
        assertThat(lates).hasSize(1);
        assertThat(lates.get(0).getExceptionType()).isEqualTo(WxCheckinExceptionTypeEnum.LATE);
        assertThat(lates.get(0).getExceptionCount()).isEqualTo(2);
        assertThat(lates.get(0).getExceptionDurationMinutes()).isEqualTo(30L);
        assertThat(lates.get(0).getUserId()).isEqualTo("u-1");
        assertThat(lates.get(0).getDepartments()).containsExactly("RD");

        List<WxCheckinExceptionItemVO> all = WxCheckinConverter.explodeExceptions(day, null);
        assertThat(all).hasSize(2);
    }

    @Test
    void explodeExceptions_emptyOrNullReturnEmpty() {
        WxCheckinDayDataVO empty = new WxCheckinDayDataVO();
        empty.setUserId("u-1");
        empty.setExceptions(List.of());

        assertThat(WxCheckinConverter.explodeExceptions(empty, null)).isEmpty();
        assertThat(WxCheckinConverter.explodeExceptions(null, null)).isEmpty();
    }
}
