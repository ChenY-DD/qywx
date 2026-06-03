package org.cy.qywx.util.oauth2;

import me.chanjar.weixin.cp.bean.WxCpOauth2UserInfo;
import me.chanjar.weixin.cp.bean.WxCpUserDetail;

/**
 * 类说明：网页授权对象转换工具。
 *
 * @author cy
 * Copyright (c) CY
 */
public final class WxOauth2Converter {

    /**
     * 创建网页授权对象转换工具实例。
     *
     * @author cy
     * Copyright (c) CY
     */
    private WxOauth2Converter() {
    }

    /**
     * 将企业微信网页授权用户标识转换为业务对象。
     *
     * @param info 网页授权用户标识
     * @return 网页授权用户标识业务视图对象
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxOauth2UserVO from(WxCpOauth2UserInfo info) {
        if (info == null) {
            return null;
        }
        WxOauth2UserVO vo = new WxOauth2UserVO();
        vo.setUserId(info.getUserId());
        vo.setOpenId(info.getOpenId());
        vo.setUserTicket(info.getUserTicket());
        vo.setDeviceId(info.getDeviceId());
        vo.setExternalUserId(info.getExternalUserId());
        vo.setExpiresIn(info.getExpiresIn());
        return vo;
    }

    /**
     * 将企业微信成员敏感详情转换为业务对象。
     *
     * @param detail 成员敏感详情
     * @return 成员敏感详情业务视图对象
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxOauth2UserDetailVO from(WxCpUserDetail detail) {
        if (detail == null) {
            return null;
        }
        WxOauth2UserDetailVO vo = new WxOauth2UserDetailVO();
        vo.setUserId(detail.getUserId());
        vo.setName(detail.getName());
        vo.setMobile(detail.getMobile());
        vo.setGender(detail.getGender());
        vo.setEmail(detail.getEmail());
        vo.setAvatar(detail.getAvatar());
        vo.setQrCode(detail.getQrCode());
        vo.setAddress(detail.getAddress());
        vo.setBizMail(detail.getBizMail());
        return vo;
    }
}
