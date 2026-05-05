package org.cy.qywx.vo;

import lombok.Data;
import org.cy.qywx.vo.enums.WxCheckinExceptionTypeEnum;

import java.io.Serializable;

/**
 * 类说明：考勤打卡异常业务视图对象。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxCheckinExceptionVO implements Serializable {

    /**
     * 字段说明：序列化版本号。
     *
     * @author cy
     * Copyright (c) CY
     */
    private static final long serialVersionUID = 1L;

    /**
     * 字段说明：异常类型。
     *
     * @author cy
     * Copyright (c) CY
     */
    private WxCheckinExceptionTypeEnum exceptionType;

    /**
     * 字段说明：count。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer count;

    /**
     * 字段说明：时长秒。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer durationSeconds;
}
