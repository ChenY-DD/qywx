package org.cy.qywx.util.oauth2;

import lombok.Data;

import java.io.Serializable;

/**
 * 类说明：网页授权成员敏感详情业务视图对象。
 *
 * @author cy
 * Copyright (c) CY
 */
@Data
public class WxOauth2UserDetailVO implements Serializable {

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
     * 字段说明：成员姓名。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String name;
    /**
     * 字段说明：手机号。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String mobile;
    /**
     * 字段说明：性别（0未定义/1男/2女）。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String gender;
    /**
     * 字段说明：邮箱。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String email;
    /**
     * 字段说明：头像URL。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String avatar;
    /**
     * 字段说明：员工个人二维码。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String qrCode;
    /**
     * 字段说明：地址。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String address;
    /**
     * 字段说明：企业邮箱。
     *
     * @author cy
     * Copyright (c) CY
     */
    private String bizMail;
}
