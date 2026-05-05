package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 类说明：考勤打卡排班业务视图对象。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxCheckinScheduleVO implements Serializable {

    /**
     * 字段说明：序列化版本号。
     *
     * @author cy
     * Copyright (c) CY
     */
    private static final long serialVersionUID = 1L;

    /**
     * 字段说明：排班ID。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer scheduleId;

    /**
     * 字段说明：排班名称。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String scheduleName;

    /**
     * 字段说明：时间section。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer timeSection;

    /**
     * 字段说明：禁用。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Boolean disabled;
}
