package org.cy.qywx.util.robot;

import me.chanjar.weixin.common.error.WxError;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpIntelligentRobotService;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.intelligentrobot.WxCpIntelligentRobot;
import me.chanjar.weixin.cp.bean.intelligentrobot.WxCpIntelligentRobotChatRequest;
import me.chanjar.weixin.cp.bean.intelligentrobot.WxCpIntelligentRobotChatResponse;
import me.chanjar.weixin.cp.bean.intelligentrobot.WxCpIntelligentRobotCreateRequest;
import me.chanjar.weixin.cp.bean.intelligentrobot.WxCpIntelligentRobotCreateResponse;
import org.cy.qywx.exception.QywxApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WxIntelligentRobotUtilTest {

    private final WxCpService wxCpService = mock(WxCpService.class);
    private final WxCpIntelligentRobotService robotService = mock(WxCpIntelligentRobotService.class);

    private WxIntelligentRobotUtil newUtil() {
        when(wxCpService.getIntelligentRobotService()).thenReturn(robotService);
        return new WxIntelligentRobotUtil(wxCpService);
    }

    @Test
    void createRobotReturnsRobotId() throws Exception {
        WxCpIntelligentRobotCreateResponse resp = new WxCpIntelligentRobotCreateResponse();
        resp.setRobotId("r1");
        when(robotService.createRobot(any(WxCpIntelligentRobotCreateRequest.class))).thenReturn(resp);

        assertThat(newUtil().createRobot(new WxCpIntelligentRobotCreateRequest())).isEqualTo("r1");
    }

    @Test
    void getRobotMapsToVo() throws Exception {
        WxCpIntelligentRobot robot = new WxCpIntelligentRobot();
        robot.setRobotId("r1");
        robot.setName("bot");
        robot.setStatus(1);
        when(robotService.getRobot("r1")).thenReturn(robot);

        WxRobotVO vo = newUtil().getRobot("r1");
        assertThat(vo.getRobotId()).isEqualTo("r1");
        assertThat(vo.getName()).isEqualTo("bot");
        assertThat(vo.getStatus()).isEqualTo(1);
    }

    @Test
    void chatMapsToVo() throws Exception {
        WxCpIntelligentRobotChatResponse resp = new WxCpIntelligentRobotChatResponse();
        resp.setReply("hi");
        resp.setSessionId("s1");
        when(robotService.chat(any(WxCpIntelligentRobotChatRequest.class))).thenReturn(resp);

        WxRobotChatVO vo = newUtil().chat(new WxCpIntelligentRobotChatRequest());
        assertThat(vo.getReply()).isEqualTo("hi");
        assertThat(vo.getSessionId()).isEqualTo("s1");
    }

    @Test
    void getRobotWrapsWxErrorException() throws Exception {
        when(robotService.getRobot("bad"))
                .thenThrow(new WxErrorException(WxError.builder().errorCode(60020).errorMsg("no").build()));
        WxIntelligentRobotUtil util = newUtil();
        assertThatThrownBy(() -> util.getRobot("bad")).isInstanceOf(QywxApiException.class);
    }
}
