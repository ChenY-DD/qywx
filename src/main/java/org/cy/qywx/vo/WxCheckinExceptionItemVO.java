package org.cy.qywx.vo;

import lombok.Data;
import org.cy.qywx.vo.enums.WxCheckinExceptionTypeEnum;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 类说明：考勤打卡异常项业务视图对象。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxCheckinExceptionItemVO implements Serializable {

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
     * 字段说明：异常类型。
     *
     * @author cy
     * Copyright (c) CY
     */
    private WxCheckinExceptionTypeEnum exceptionType;

    /**
     * 字段说明：异常count。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer exceptionCount;

    /**
     * 字段说明：异常时长分钟。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Long exceptionDurationMinutes;
}
