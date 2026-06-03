package org.cy.qywx.util.message;

import lombok.Data;

import java.io.Serializable;

/**
 * 类说明：消息推送结果业务视图对象。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxMessageSendResultVO implements Serializable {

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
     * 字段说明：用于撤回应用消息的 response_code。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String responseCode;

    /**
     * 字段说明：不合法的 userId，多个以竖线分隔。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String invalidUser;

    /**
     * 字段说明：不合法的 partyId（部门），多个以竖线分隔。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String invalidParty;

    /**
     * 字段说明：不合法的 tagId（标签），多个以竖线分隔。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String invalidTag;

    /**
     * 字段说明：没有基础接口许可（包含已过期）的 userId，多个以竖线分隔。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String unlicensedUser;
}
