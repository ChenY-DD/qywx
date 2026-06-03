package org.cy.qywx.util.oauth2;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import org.cy.qywx.exception.QywxApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类说明：企业微信网页授权登录工具。
 *
 * @author cy
 * Copyright (c) CY
 */
public class WxOauth2Util {

    /** 日志 */
    private static final Logger log = LoggerFactory.getLogger(WxOauth2Util.class);

    /** 企业微信服务 */
    private final WxCpService wxCpService;

    /**
     * 构造网页授权登录工具。
     *
     * @param wxCpService 企业微信服务
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxOauth2Util(WxCpService wxCpService) {
        this.wxCpService = wxCpService;
        log.info("WxOauth2Util initialized");
    }

    /**
     * 构造网页授权登录链接（纯字符串拼接，不发起网络请求）。
     *
     * @param redirectUri 授权后回调地址
     * @param scope       应用授权作用域（snsapi_base / snsapi_privateinfo 等）
     * @param state       重定向后携带的自定义状态参数
     * @return 网页授权链接
     *
     * @author cy
     * Copyright (c) CY
     */
    public String buildAuthorizationUrl(String redirectUri, String scope, String state) {
        return wxCpService.getOauth2Service().buildAuthorizationUrl(redirectUri, scope, state);
    }

    /**
     * 根据授权 code 获取成员标识信息。
     *
     * @param code 网页授权回调返回的临时票据
     * @return 网页授权用户标识业务视图对象
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxOauth2UserVO getUserInfo(String code) {
        try {
            return WxOauth2Converter.from(wxCpService.getOauth2Service().getUserInfo(code));
        } catch (WxErrorException e) {
            log.error("oauth2 getUserInfo failed: {}", e.getMessage());
            throw QywxApiException.from(e);
        }
    }

    /**
     * 根据成员票据获取成员敏感详情。
     *
     * @param userTicket 成员票据
     * @return 成员敏感详情业务视图对象
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxOauth2UserDetailVO getUserDetail(String userTicket) {
        try {
            return WxOauth2Converter.from(wxCpService.getOauth2Service().getUserDetail(userTicket));
        } catch (WxErrorException e) {
            log.error("oauth2 getUserDetail failed: {}", e.getMessage());
            throw QywxApiException.from(e);
        }
    }
}
