package org.cy.qywx.vo;

import lombok.Data;
import org.cy.qywx.util.WxCheckinFetchFailure;
import org.cy.qywx.util.WxDateRange;

import java.io.Serializable;
import java.util.List;

/**
 * 类说明：考勤报表业务视图对象。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxAttendanceReportVO implements Serializable {

    /**
     * 字段说明：序列化版本号。
     *
     * @author cy
     * Copyright (c) CY
     */
    private static final long serialVersionUID = 1L;

    /**
     * 字段说明：范围。
     *
     * @author cy
     * Copyright (c) CY
     */
    private WxDateRange range;

    /**
     * 字段说明：总成员列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private int totalUsers;

    /**
     * 字段说明：已上报成员列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private int reportedUsers;

    /**
     * 字段说明：迟到异常列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<WxCheckinExceptionItemVO> late;

    /**
     * 字段说明：早退异常列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<WxCheckinExceptionItemVO> earlyLeave;

    /**
     * 字段说明：缺卡异常列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<WxCheckinExceptionItemVO> missingCard;

    /**
     * 字段说明：旷工异常列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<WxCheckinExceptionItemVO> absent;

    /**
     * 字段说明：地点异常列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<WxCheckinExceptionItemVO> locationException;

    /**
     * 字段说明：设备异常列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<WxCheckinExceptionItemVO> deviceException;

    /**
     * 字段说明：批量查询失败记录列表。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<WxCheckinFetchFailure> failures;
}
