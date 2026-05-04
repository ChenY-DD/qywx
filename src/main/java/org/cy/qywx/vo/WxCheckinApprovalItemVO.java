package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 影响某一打卡日 / 月的审批单项 VO（来源 sp_items）。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinApprovalItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 审批单号 */
    private String spNumber;

    /** 审批类型 */
    private Integer type;

    /** 审批分类描述 */
    private String spTitle;

    /** 审批通过的工时（秒） */
    private Integer durationSeconds;

    /** 关联日期（unix 时间戳） */
    private Long timeUnix;
}
