package org.cy.qywx.exception;

import me.chanjar.weixin.common.error.WxError;
import me.chanjar.weixin.common.error.WxErrorException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QywxApiExceptionTest {

    @Test
    void fromExtractsErrCodeAndMsg() {
        WxError error = WxError.builder().errorCode(40014).errorMsg("invalid access_token").build();
        QywxApiException ex = QywxApiException.from(new WxErrorException(error));

        assertThat(ex.getErrCode()).isEqualTo(40014);
        assertThat(ex.getErrMsg()).isEqualTo("invalid access_token");
        assertThat(ex.getMessage()).contains("40014").contains("invalid access_token");
    }

    @Test
    void fromHandlesNullError() {
        // WxJava's WxErrorException(String) does NOT leave getError() null; it
        // builds a WxError with the sentinel errorCode -99 and the given message.
        QywxApiException ex = QywxApiException.from(new WxErrorException("boom"));
        assertThat(ex.getErrCode()).isEqualTo(-99);
        assertThat(ex.getErrMsg()).isEqualTo("boom");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    void fromTreatsTrulyNullWxErrorAsNullCode() {
        // The implementation's null-guard path: construct an exception whose
        // getError() is genuinely null (anonymous subclass overriding getError).
        WxErrorException nullErrorEx = new WxErrorException("fallback") {
            @Override
            public WxError getError() {
                return null;
            }
        };
        QywxApiException ex = QywxApiException.from(nullErrorEx);
        assertThat(ex.getErrCode()).isNull();
        // errMsg falls back to e.getMessage(), which carries the original text.
        assertThat(ex.getErrMsg()).contains("fallback");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
