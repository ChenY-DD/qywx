package org.cy.qywx.util;

import me.chanjar.weixin.cp.bean.oa.WxCpCheckinData;
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinDayData;
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinMonthData;
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinSchedule;
import me.chanjar.weixin.cp.bean.oa.WxCpCropCheckinOption;
import org.cy.qywx.vo.WxCheckinApprovalItemVO;
import org.cy.qywx.vo.WxCheckinDayDataVO;
import org.cy.qywx.vo.WxCheckinExceptionItemVO;
import org.cy.qywx.vo.WxCheckinExceptionVO;
import org.cy.qywx.vo.WxCheckinGroupVO;
import org.cy.qywx.vo.WxCheckinLeaveVO;
import org.cy.qywx.vo.WxCheckinMonthDataVO;
import org.cy.qywx.vo.WxCheckinOvertimeVO;
import org.cy.qywx.vo.WxCheckinRangeVO;
import org.cy.qywx.vo.WxCheckinRecordVO;
import org.cy.qywx.vo.WxCheckinScheduleListItemVO;
import org.cy.qywx.vo.enums.WxCheckinExceptionTypeEnum;
import org.cy.qywx.vo.enums.WxCheckinResultEnum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * WxJava 考勤 Bean 到本地 VO 的静态转换器。所有方法对 null 入参返回 {@code null}（或空集合，按方法语义）。
 *
 * @author CY
 * Copyright (c) CY
 */
public final class WxCheckinConverter {

    private WxCheckinConverter() {
    }

    /**
     * 将单条原始打卡 bean 转为 {@link WxCheckinRecordVO}。
     *
     * @param src 原始 bean
     * @return VO；入参为 null 时返回 null
     */
    public static WxCheckinRecordVO fromRecord(WxCpCheckinData src) {
        if (src == null) {
            return null;
        }
        WxCheckinRecordVO vo = new WxCheckinRecordVO();
        vo.setUserId(src.getUserId());
        vo.setGroupId(src.getGroupId());
        vo.setGroupName(src.getGroupName());
        vo.setScheduleId(src.getScheduleId());
        vo.setTimelineId(src.getTimelineId());
        vo.setCheckinType(src.getCheckinType());
        vo.setCheckinTime(unixToDate(src.getCheckinTime()));
        vo.setSchCheckinTime(unixToDate(src.getSchCheckinTime()));
        vo.setLocationTitle(src.getLocationTitle());
        vo.setLocationDetail(src.getLocationDetail());
        vo.setWifiName(src.getWifiName());
        vo.setWifiMac(src.getWifiMac());
        vo.setNotes(src.getNotes());
        vo.setMediaIds(src.getMediaIds());
        vo.setDeviceId(src.getDeviceId());
        vo.setLat(src.getLat());
        vo.setLng(src.getLng());
        vo.setRawExceptionType(src.getExceptionType());
        vo.setExceptionResult(WxCheckinResultEnum.fromText(src.getExceptionType()));
        return vo;
    }

    /**
     * 将日报 bean 转为 {@link WxCheckinDayDataVO}。
     *
     * @param src 日报 bean
     * @return VO；入参为 null 时返回 null
     */
    public static WxCheckinDayDataVO fromDayData(WxCpCheckinDayData src) {
        if (src == null) {
            return null;
        }
        WxCheckinDayDataVO vo = new WxCheckinDayDataVO();

        WxCpCheckinDayData.BaseInfo base = src.getBaseInfo();
        if (base != null) {
            vo.setUserId(base.getAcctId());
            vo.setName(base.getName());
            vo.setDepartments(splitSemicolon(base.getDepartsName()));
            vo.setDate(unixIntToDate(base.getDate()));
            vo.setRecordType(base.getRecordType());
        }

        WxCpCheckinDayData.SummaryInfo summary = src.getSummaryInfo();
        if (summary != null) {
            vo.setStandardWorkSeconds(summary.getStandardWorkSec());
            vo.setRegularWorkSeconds(summary.getRegularWorkSec());
            vo.setEarliestCheckinTime(unixIntToDate(summary.getEarliestTime()));
            vo.setLatestCheckinTime(unixIntToDate(summary.getLastestTime()));
        }

        vo.setExceptions(toDayExceptionList(src.getExceptionInfos()));
        vo.setHolidays(toLeaveListFromHolidayInfos(src.getHolidayInfos()));
        vo.setOvertime(toOvertimeFromDay(src.getOtInfo()));
        vo.setApprovals(toApprovalListFromDay(src.getSpItems()));
        return vo;
    }

    /**
     * 将月报 bean 转为 {@link WxCheckinMonthDataVO}。
     *
     * @param src 月报 bean
     * @return VO；入参为 null 时返回 null
     */
    public static WxCheckinMonthDataVO fromMonthData(WxCpCheckinMonthData src) {
        if (src == null) {
            return null;
        }
        WxCheckinMonthDataVO vo = new WxCheckinMonthDataVO();

        WxCpCheckinMonthData.BaseInfo base = src.getBaseInfo();
        if (base != null) {
            vo.setUserId(base.getAcctId());
            vo.setName(base.getName());
            vo.setDepartments(splitSemicolon(base.getDepartsName()));
        }

        WxCpCheckinMonthData.SummaryInfo summary = src.getSummaryInfo();
        if (summary != null) {
            vo.setWorkDays(summary.getWorkDays());
            vo.setExcludedDays(summary.getExceptDays());
            vo.setWorkSeconds(summary.getRegularWorkSec());
            vo.setStandardWorkSeconds(summary.getStandardWorkSec());
        }

        vo.setExceptions(toMonthExceptionList(src.getExceptionInfos()));
        vo.setApprovals(toApprovalListFromMonth(src.getSpItems()));
        return vo;
    }

    /**
     * 将考勤组 bean 转为 {@link WxCheckinGroupVO}。
     *
     * @param src 考勤组 bean
     * @return VO；入参为 null 时返回 null
     */
    public static WxCheckinGroupVO fromGroup(WxCpCropCheckinOption src) {
        if (src == null) {
            return null;
        }
        WxCheckinGroupVO vo = new WxCheckinGroupVO();
        vo.setGroupId(src.getGroupId() == null ? null : src.getGroupId().intValue());
        vo.setGroupName(src.getGroupName());
        vo.setGroupType(src.getGroupType() == null ? null : String.valueOf(src.getGroupType()));
        vo.setCreateTime(unixToDate(src.getCreateTime()));
        vo.setNeedPhoto(src.getNeedPhoto());
        vo.setNoteCanUseLocalPic(src.getNoteCanUseLocalPic());
        vo.setAllowCheckinOffWorkday(src.getAllowCheckinOffWorkDay());
        vo.setAllowApplyOffWorkday(src.getAllowApplyOffWorkDay());
        vo.setSyncHolidays(src.getSyncHolidays());
        vo.setWhiteUsers(src.getWhiteUsers());

        if (src.getRange() != null) {
            WxCheckinRangeVO range = new WxCheckinRangeVO();
            range.setPartyIds(src.getRange().getPartyid());
            range.setUserIds(src.getRange().getUserid());
            range.setTagIds(src.getRange().getTagid());
            vo.setRange(range);
        }

        vo.setSpeWorkdays(speWorkdaysToDates(src.getSpeWorkdays()));
        vo.setSpeOffdays(speOffDaysToDates(src.getSpeOffDays()));
        return vo;
    }

    /**
     * 将排班 bean 转为 {@link WxCheckinScheduleListItemVO}。
     *
     * @param src 排班 bean
     * @return VO；入参为 null 时返回 null
     */
    public static WxCheckinScheduleListItemVO fromScheduleItem(WxCpCheckinSchedule src) {
        if (src == null) {
            return null;
        }
        WxCheckinScheduleListItemVO vo = new WxCheckinScheduleListItemVO();
        vo.setUserId(src.getUserid());
        vo.setGroupId(src.getGroupid());
        vo.setScheduleDate(yearMonthToFirstOfMonth(src.getYearmonth()));
        return vo;
    }

    /**
     * 将日报 VO 中嵌入的异常项展开为业务语义层的扁平异常行。
     *
     * @param dayData 日报 VO
     * @param filter  仅保留指定异常类型；传 null 表示全部保留
     * @return 扁平异常行列表，可能为空
     */
    public static List<WxCheckinExceptionItemVO> explodeExceptions(WxCheckinDayDataVO dayData,
                                                                   WxCheckinExceptionTypeEnum filter) {
        if (dayData == null || dayData.getExceptions() == null || dayData.getExceptions().isEmpty()) {
            return Collections.emptyList();
        }
        List<WxCheckinExceptionItemVO> out = new ArrayList<>();
        for (WxCheckinExceptionVO ex : dayData.getExceptions()) {
            if (ex == null || ex.getExceptionType() == null) {
                continue;
            }
            if (filter != null && filter != ex.getExceptionType()) {
                continue;
            }
            WxCheckinExceptionItemVO item = new WxCheckinExceptionItemVO();
            item.setUserId(dayData.getUserId());
            item.setName(dayData.getName());
            item.setDepartments(dayData.getDepartments());
            item.setDate(dayData.getDate());
            item.setExceptionType(ex.getExceptionType());
            item.setExceptionCount(ex.getCount());
            item.setExceptionDurationMinutes(ex.getDurationSeconds() == null ? null
                    : (long) (ex.getDurationSeconds() / 60));
            out.add(item);
        }
        return out;
    }

    // -------------------- private helpers --------------------

    private static List<WxCheckinExceptionVO> toDayExceptionList(
            List<WxCpCheckinDayData.ExceptionInfos> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<WxCheckinExceptionVO> out = new ArrayList<>(source.size());
        for (WxCpCheckinDayData.ExceptionInfos info : source) {
            if (info == null) {
                continue;
            }
            WxCheckinExceptionVO vo = new WxCheckinExceptionVO();
            vo.setExceptionType(WxCheckinExceptionTypeEnum.fromCode(info.getException()));
            vo.setCount(info.getCount());
            vo.setDurationSeconds(info.getDuration());
            out.add(vo);
        }
        return out;
    }

    private static List<WxCheckinExceptionVO> toMonthExceptionList(
            List<WxCpCheckinMonthData.ExceptionInfo> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<WxCheckinExceptionVO> out = new ArrayList<>(source.size());
        for (WxCpCheckinMonthData.ExceptionInfo info : source) {
            if (info == null) {
                continue;
            }
            WxCheckinExceptionVO vo = new WxCheckinExceptionVO();
            vo.setExceptionType(WxCheckinExceptionTypeEnum.fromCode(info.getException()));
            vo.setCount(info.getCount());
            vo.setDurationSeconds(info.getDuration());
            out.add(vo);
        }
        return out;
    }

    private static List<WxCheckinLeaveVO> toLeaveListFromHolidayInfos(
            List<WxCpCheckinDayData.HolidayInfos> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<WxCheckinLeaveVO> out = new ArrayList<>(source.size());
        for (WxCpCheckinDayData.HolidayInfos h : source) {
            if (h == null) {
                continue;
            }
            WxCheckinLeaveVO vo = new WxCheckinLeaveVO();
            vo.setSpNumber(h.getSpNumber());
            vo.setSpTitle(extractFirstHolidayTitleText(h));
            out.add(vo);
        }
        return out;
    }

    private static String extractFirstHolidayTitleText(WxCpCheckinDayData.HolidayInfos h) {
        if (h == null || h.getSpTitle() == null || h.getSpTitle().getData() == null
                || h.getSpTitle().getData().isEmpty()) {
            return null;
        }
        return h.getSpTitle().getData().get(0).getText();
    }

    private static WxCheckinOvertimeVO toOvertimeFromDay(WxCpCheckinDayData.OtInfo source) {
        if (source == null) {
            return null;
        }
        WxCheckinOvertimeVO vo = new WxCheckinOvertimeVO();
        vo.setOtStatus(source.getOtStatus());
        vo.setOtDurationSeconds(source.getOtDuration());
        vo.setExceptionDurationSeconds(source.getExceptionDuration());
        return vo;
    }

    private static List<WxCheckinApprovalItemVO> toApprovalListFromDay(
            List<WxCpCheckinDayData.SpItem> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<WxCheckinApprovalItemVO> out = new ArrayList<>(source.size());
        for (WxCpCheckinDayData.SpItem s : source) {
            if (s == null) {
                continue;
            }
            WxCheckinApprovalItemVO vo = new WxCheckinApprovalItemVO();
            vo.setType(s.getType());
            vo.setSpTitle(s.getName());
            vo.setDurationSeconds(s.getDuration());
            out.add(vo);
        }
        return out;
    }

    private static List<WxCheckinApprovalItemVO> toApprovalListFromMonth(
            List<WxCpCheckinMonthData.SpItem> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<WxCheckinApprovalItemVO> out = new ArrayList<>(source.size());
        for (WxCpCheckinMonthData.SpItem s : source) {
            if (s == null) {
                continue;
            }
            WxCheckinApprovalItemVO vo = new WxCheckinApprovalItemVO();
            vo.setType(s.getType());
            vo.setDurationSeconds(s.getDuration());
            out.add(vo);
        }
        return out;
    }

    private static List<Date> speWorkdaysToDates(
            List<me.chanjar.weixin.cp.bean.oa.WxCpCheckinGroupBase.SpeWorkday> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<Date> out = new ArrayList<>(source.size());
        for (var d : source) {
            if (d != null && d.getTimestamp() != null) {
                out.add(unixToDate(d.getTimestamp()));
            }
        }
        return out;
    }

    private static List<Date> speOffDaysToDates(
            List<me.chanjar.weixin.cp.bean.oa.WxCpCheckinGroupBase.SpeOffDay> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<Date> out = new ArrayList<>(source.size());
        for (var d : source) {
            if (d != null && d.getTimestamp() != null) {
                out.add(unixToDate(d.getTimestamp()));
            }
        }
        return out;
    }

    private static List<String> splitSemicolon(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split("[;；]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static Date unixToDate(Long seconds) {
        if (seconds == null || seconds <= 0L) {
            return null;
        }
        return new Date(seconds * 1000L);
    }

    private static Date unixIntToDate(Integer seconds) {
        if (seconds == null || seconds <= 0) {
            return null;
        }
        return new Date(((long) seconds) * 1000L);
    }

    private static Date yearMonthToFirstOfMonth(Integer yearMonth) {
        if (yearMonth == null || yearMonth <= 0) {
            return null;
        }
        int year = yearMonth / 100;
        int month = yearMonth % 100;
        if (year < 1970 || month < 1 || month > 12) {
            return null;
        }
        Calendar cal = new GregorianCalendar(year, month - 1, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
