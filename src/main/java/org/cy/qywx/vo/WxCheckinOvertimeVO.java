package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 加班信息 VO（来源 ot_info）。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinOvertimeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 加班状态 */
    private Integer otStatus;

    /** 加班时长（秒） */
    private Integer otDurationSeconds;

    /** 加班异常时长（秒）数组 */
    private List<Integer> exceptionDurationSeconds;
}
