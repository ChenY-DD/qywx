package org.cy.qywx.vo;

import lombok.Data;
import org.cy.qywx.util.WxCheckinFetchFailure;
import org.cy.qywx.util.WxDateRange;

import java.io.Serializable;
import java.util.List;

/**
 * 一次性聚合的考勤报表 VO。{@link org.cy.qywx.util.WxCheckinQueryUtil#getAttendanceReport}
 * 返回此结构，避免调用方为 6 种异常类型分别发起请求。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxAttendanceReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 报表时间范围 */
    private WxDateRange range;

    /** 入参 userIds 去重后总数 */
    private int totalUsers;

    /** 实际返回了日报的人数（无打卡的会缺失） */
    private int reportedUsers;

    /** 迟到记录（按日扁平） */
    private List<WxCheckinExceptionItemVO> late;

    /** 早退记录 */
    private List<WxCheckinExceptionItemVO> earlyLeave;

    /** 缺卡记录 */
    private List<WxCheckinExceptionItemVO> missingCard;

    /** 旷工记录 */
    private List<WxCheckinExceptionItemVO> absent;

    /** 地点异常记录 */
    private List<WxCheckinExceptionItemVO> locationException;

    /** 设备异常记录 */
    private List<WxCheckinExceptionItemVO> deviceException;

    /** 失败的批次列表（如有） */
    private List<WxCheckinFetchFailure> failures;
}
