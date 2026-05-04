package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 单人单日打卡日报 VO（来源 getcheckin_daydata.datas[]）。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinDayDataVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private String userId;

    /** 用户姓名 */
    private String name;

    /** 部门列表（companies / departs_name 拆分后） */
    private List<String> departments;

    /** 日报日期 */
    private Date date;

    /** 记录类型（1 固定上下班 / 3 班次 / 4 自由签到 / 5 加班 / 7 无规则 等） */
    private Integer recordType;

    /** 应工作时长（秒） */
    private Integer standardWorkSeconds;

    /** 弹性工作时长（秒，可正可负） */
    private Integer flexWorkSeconds;

    /** 规则工作时长（秒） */
    private Integer regularWorkSeconds;

    /** 最早打卡时间 */
    private Date earliestCheckinTime;

    /** 最晚打卡时间 */
    private Date latestCheckinTime;

    /** 异常项列表 */
    private List<WxCheckinExceptionVO> exceptions;

    /** 请假 / 外出项列表 */
    private List<WxCheckinLeaveVO> holidays;

    /** 加班信息 */
    private WxCheckinOvertimeVO overtime;

    /** 影响该日的审批单 */
    private List<WxCheckinApprovalItemVO> approvals;
}
