package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 类说明：考勤打卡天数据业务视图对象。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxCheckinDayDataVO implements Serializable {

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
     * 字段说明：日期。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Date date;

    /**
     * 字段说明：记录类型。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer recordType;

    /**
     * 字段说明：标准工作秒。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer standardWorkSeconds;

    /**
     * 字段说明：弹性工作秒。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer flexWorkSeconds;

    /**
     * 字段说明：规则工作秒。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer regularWorkSeconds;

    /**
     * 字段说明：最早考勤打卡时间。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Date earliestCheckinTime;

    /**
     * 字段说明：最晚考勤打卡时间。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Date latestCheckinTime;

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
