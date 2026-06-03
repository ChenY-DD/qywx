package org.cy.qywx.util.robot;

import lombok.Data;

import java.io.Serializable;

/**
 * 类说明：智能机器人对话结果业务视图对象。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxRobotChatVO implements Serializable {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /**
     * 字段说明：机器人回复内容。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String reply;

    /**
     * 字段说明：会话 ID。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String sessionId;

    /**
     * 字段说明：消息 ID。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String msgId;
}
