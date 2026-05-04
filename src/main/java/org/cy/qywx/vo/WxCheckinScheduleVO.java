package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 考勤组班次 VO。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinScheduleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 班次 ID */
    private Integer scheduleId;

    /** 班次名称 */
    private String scheduleName;

    /** 时段总数 */
    private Integer timeSection;

    /** 是否禁用 */
    private Boolean disabled;
}
