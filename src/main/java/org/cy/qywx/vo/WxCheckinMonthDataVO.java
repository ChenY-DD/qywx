package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 单人单月打卡月报 VO（来源 getcheckin_monthdata.datas[]）。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinMonthDataVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private String userId;

    /** 用户姓名 */
    private String name;

    /** 部门列表 */
    private List<String> departments;

    /** 月份首日 */
    private Date monthDate;

    /** 应出勤天数 */
    private Integer workDays;

    /** 排除天数（请假 / 外出等） */
    private Integer excludedDays;

    /** 实际工作时长（秒） */
    private Integer workSeconds;

    /** 标准工作时长（秒） */
    private Integer standardWorkSeconds;

    /** 月度异常汇总 */
    private List<WxCheckinExceptionVO> exceptions;

    /** 月度请假项 */
    private List<WxCheckinLeaveVO> holidays;

    /** 月度加班信息 */
    private WxCheckinOvertimeVO overtime;

    /** 月度相关审批 */
    private List<WxCheckinApprovalItemVO> approvals;
}
