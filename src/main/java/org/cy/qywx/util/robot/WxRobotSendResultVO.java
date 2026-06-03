package org.cy.qywx.util.robot;

import lombok.Data;

import java.io.Serializable;

/**
 * 类说明：智能机器人消息发送结果业务视图对象。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxRobotSendResultVO implements Serializable {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /**
     * 字段说明：消息 ID。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String msgId;

    /**
     * 字段说明：会话 ID。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String sessionId;
}
