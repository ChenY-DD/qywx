package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 请假 / 外出项 VO（来源 holiday_infos）。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinLeaveVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 审批单号 */
    private String spNumber;

    /** 假期类型描述 */
    private String spTitle;

    /** 假期时长（秒） */
    private Integer durationSeconds;

    /** 开始时间（unix 时间戳） */
    private Long startTime;

    /** 结束时间（unix 时间戳） */
    private Long endTime;
}
