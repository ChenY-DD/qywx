package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 排班列表单项 VO（来源 getcheckinschedulist）。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinScheduleListItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private String userId;

    /** 排班日期 */
    private Date scheduleDate;

    /** 考勤组 ID */
    private Integer groupId;

    /** 班次 ID（0 表示休息） */
    private Integer scheduleId;

    /** 班次名称 */
    private String scheduleName;
}
