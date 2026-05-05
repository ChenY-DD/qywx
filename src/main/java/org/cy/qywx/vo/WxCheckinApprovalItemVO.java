package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 类说明：考勤打卡审批项业务视图对象。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxCheckinApprovalItemVO implements Serializable {

    /**
     * 字段说明：序列化版本号。
     *
     * @author cy
     * Copyright (c) CY
     */
    private static final long serialVersionUID = 1L;

    /**
     * 字段说明：审批number。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String spNumber;

    /**
     * 字段说明：类型。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer type;

    /**
     * 字段说明：审批标题。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String spTitle;

    /**
     * 字段说明：时长秒。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer durationSeconds;

    /**
     * 字段说明：时间unix。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Long timeUnix;
}
