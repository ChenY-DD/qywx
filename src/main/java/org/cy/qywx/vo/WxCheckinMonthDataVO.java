package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 类说明：考勤打卡月数据业务视图对象。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxCheckinMonthDataVO implements Serializable {

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
     * 字段说明：名称。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String name;

    /**
     * 字段说明：部门列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<String> departments;

    /**
     * 字段说明：月日期。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Date monthDate;

    /**
     * 字段说明：工作天数。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer workDays;

    /**
     * 字段说明：排除天数。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer excludedDays;

    /**
     * 字段说明：工作秒。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer workSeconds;

    /**
     * 字段说明：标准工作秒。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer standardWorkSeconds;

    /**
     * 字段说明：异常列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<WxCheckinExceptionVO> exceptions;

    /**
     * 字段说明：假期列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<WxCheckinLeaveVO> holidays;

    /**
     * 字段说明：加班。
     *
     * @author cy
     * Copyright (c) CY
     */
    private WxCheckinOvertimeVO overtime;

    /**
     * 字段说明：approvals。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<WxCheckinApprovalItemVO> approvals;
}
