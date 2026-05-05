package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 类说明：考勤打卡加班业务视图对象。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxCheckinOvertimeVO implements Serializable {

    /**
     * 字段说明：序列化版本号。
     *
     * @author cy
     * Copyright (c) CY
     */
    private static final long serialVersionUID = 1L;

    /**
     * 字段说明：ot状态。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer otStatus;

    /**
     * 字段说明：ot时长秒。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer otDurationSeconds;

    /**
     * 字段说明：异常时长秒。
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<Integer> exceptionDurationSeconds;
}
