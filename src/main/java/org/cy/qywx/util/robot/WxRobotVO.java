package org.cy.qywx.util.robot;

import lombok.Data;

import java.io.Serializable;

/**
 * 类说明：智能机器人业务视图对象。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxRobotVO implements Serializable {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /**
     * 字段说明：机器人 ID。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String robotId;

    /**
     * 字段说明：机器人名称。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String name;

    /**
     * 字段说明：机器人描述。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String description;

    /**
     * 字段说明：机器人头像。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String avatar;

    /**
     * 字段说明：机器人状态。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Integer status;

    /**
     * 字段说明：创建时间。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Long createTime;

    /**
     * 字段说明：更新时间。
     *
     * @author cy
     * Copyright (c) CY
     */
    private Long updateTime;
}
