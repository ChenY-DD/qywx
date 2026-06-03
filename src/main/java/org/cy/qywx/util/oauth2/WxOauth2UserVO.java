package org.cy.qywx.util.oauth2;

import lombok.Data;

import java.io.Serializable;

/**
 * 类说明：网页授权用户标识业务视图对象。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxOauth2UserVO implements Serializable {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /**
     * 字段说明：成员UserId。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String userId;
    /**
     * 字段说明：非企业成员的标识OpenId。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String openId;
    /**
     * 字段说明：成员票据（用于获取成员敏感信息）。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String userTicket;
    /**
     * 字段说明：手机设备号。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String deviceId;
    /**
     * 字段说明：外部联系人ID。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String externalUserId;
    /**
     * 字段说明：成员票据有效期（秒）。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String expiresIn;
}
