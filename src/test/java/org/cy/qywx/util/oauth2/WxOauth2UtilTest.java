package org.cy.qywx.util.oauth2;

import me.chanjar.weixin.common.error.WxError;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpOAuth2Service;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.WxCpOauth2UserInfo;
import me.chanjar.weixin.cp.bean.WxCpUserDetail;
import org.cy.qywx.exception.QywxApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WxOauth2UtilTest {

    private final WxCpService wxCpService = mock(WxCpService.class);
    private final WxCpOAuth2Service oauth2Service = mock(WxCpOAuth2Service.class);

    private WxOauth2Util newUtil() {
        when(wxCpService.getOauth2Service()).thenReturn(oauth2Service);
        return new WxOauth2Util(wxCpService);
    }

    @Test
    void buildAuthorizationUrlDelegates() {
        when(oauth2Service.buildAuthorizationUrl("https://cb", "snsapi_base", "st")).thenReturn("https://auth");
        assertThat(newUtil().buildAuthorizationUrl("https://cb", "snsapi_base", "st")).isEqualTo("https://auth");
    }

    @Test
    void getUserInfoMapsToVo() throws Exception {
        WxCpOauth2UserInfo info = new WxCpOauth2UserInfo();
        info.setUserId("u1");
        info.setUserTicket("t1");
        when(oauth2Service.getUserInfo("code1")).thenReturn(info);

        WxOauth2UserVO vo = newUtil().getUserInfo("code1");
        assertThat(vo.getUserId()).isEqualTo("u1");
        assertThat(vo.getUserTicket()).isEqualTo("t1");
    }

    @Test
    void getUserDetailMapsToVo() throws Exception {
        WxCpUserDetail detail = new WxCpUserDetail();
        detail.setUserId("u1");
        detail.setName("Alice");
        detail.setMobile("123");
        when(oauth2Service.getUserDetail("ticket1")).thenReturn(detail);

        WxOauth2UserDetailVO vo = newUtil().getUserDetail("ticket1");
        assertThat(vo.getName()).isEqualTo("Alice");
        assertThat(vo.getMobile()).isEqualTo("123");
    }

    @Test
    void getUserInfoWrapsWxErrorException() throws Exception {
        when(oauth2Service.getUserInfo("bad"))
                .thenThrow(new WxErrorException(WxError.builder().errorCode(40029).errorMsg("invalid code").build()));
        WxOauth2Util util = newUtil();
        assertThatThrownBy(() -> util.getUserInfo("bad")).isInstanceOf(QywxApiException.class);
    }
}
