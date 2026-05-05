package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 类说明：考勤打卡排班list项业务视图对象。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxCheckinScheduleListItemVO implements Serializable {

    /**
     * 字段说明：序列化版本号。
     *
     * @author cy
     * Copyright (c) CY
     */
    private static final long serialVersionUID = 1L;

    /**
     * 字段说明：成员 userId。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String userId;

    /**
     * 字段说明：排班日期。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Date scheduleDate;

    /**
     * 字段说明：考勤组ID。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer groupId;

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
}
