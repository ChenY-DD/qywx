package org.cy.qywx.util.message;

import me.chanjar.weixin.common.error.WxError;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpMessageService;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.message.WxCpMessage;
import me.chanjar.weixin.cp.bean.message.WxCpMessageSendResult;
import org.cy.qywx.exception.QywxApiException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WxMessagePushUtilTest {

    private final WxCpService wxCpService = mock(WxCpService.class);
    private final WxCpMessageService messageService = mock(WxCpMessageService.class);

    private WxMessagePushUtil newUtil() {
        when(wxCpService.getMessageService()).thenReturn(messageService);
        return new WxMessagePushUtil(wxCpService);
    }

    @Test
    void sendTextBuildsTextMessageAndMapsResult() throws Exception {
        WxCpMessageSendResult sdk = new WxCpMessageSendResult();
        sdk.setMsgId("MSG1");
        sdk.setInvalidUser("u2");
        when(messageService.send(any(WxCpMessage.class))).thenReturn(sdk);

        WxMessageSendResultVO vo = newUtil().sendText("u1", "hello");

        assertThat(vo.getMsgId()).isEqualTo("MSG1");
        assertThat(vo.getInvalidUser()).isEqualTo("u2");

        ArgumentCaptor<WxCpMessage> captor = ArgumentCaptor.forClass(WxCpMessage.class);
        verify(messageService).send(captor.capture());
        assertThat(captor.getValue().getMsgType()).isEqualTo("text");
        assertThat(captor.getValue().getToUser()).isEqualTo("u1");
        assertThat(captor.getValue().getContent()).isEqualTo("hello");
    }

    @Test
    void sendWrapsWxErrorException() throws Exception {
        when(messageService.send(any(WxCpMessage.class)))
                .thenThrow(new WxErrorException(WxError.builder().errorCode(40014).errorMsg("bad").build()));

        WxMessagePushUtil util = newUtil();
        assertThatThrownBy(() -> util.send(new WxCpMessage()))
                .isInstanceOf(QywxApiException.class)
                .satisfies(e -> assertThat(((QywxApiException) e).getErrCode()).isEqualTo(40014));
    }

    @Test
    void recallWrapsWxErrorException() throws Exception {
        when(wxCpService.getMessageService()).thenReturn(messageService);
        org.mockito.Mockito.doThrow(new WxErrorException(WxError.builder().errorCode(50002).errorMsg("no").build()))
                .when(messageService).recall("m1");

        WxMessagePushUtil util = new WxMessagePushUtil(wxCpService);
        assertThatThrownBy(() -> util.recall("m1")).isInstanceOf(QywxApiException.class);
    }
}
