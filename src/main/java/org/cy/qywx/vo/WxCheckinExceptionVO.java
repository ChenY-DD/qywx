package org.cy.qywx.vo;

import lombok.Data;
import org.cy.qywx.vo.enums.WxCheckinExceptionTypeEnum;

import java.io.Serializable;

/**
 * 嵌入在日报 / 月报内的异常项 VO。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinExceptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 异常类型枚举 */
    private WxCheckinExceptionTypeEnum exceptionType;

    /** 当日 / 当月异常次数 */
    private Integer count;

    /** 异常时长（秒） */
    private Integer durationSeconds;
}
