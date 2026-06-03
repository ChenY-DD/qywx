package org.cy.qywx.util.export;

import me.chanjar.weixin.common.error.WxError;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpExportService;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.export.WxCpExportRequest;
import me.chanjar.weixin.cp.bean.export.WxCpExportResult;
import org.cy.qywx.exception.QywxApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WxExportUtilTest {

    private final WxCpService wxCpService = mock(WxCpService.class);
    private final WxCpExportService exportService = mock(WxCpExportService.class);

    private WxExportUtil newUtil(WxExportQueryOptions options) {
        when(wxCpService.getExportService()).thenReturn(exportService);
        return new WxExportUtil(wxCpService, options);
    }

    private static WxCpExportResult result(int status) {
        WxCpExportResult r = new WxCpExportResult();
        r.setStatus(status);
        return r;
    }

    @Test
    void exportUserReturnsJobId() throws Exception {
        when(exportService.user(any(WxCpExportRequest.class))).thenReturn("JOB1");
        assertThat(newUtil(WxExportQueryOptions.defaults()).exportUser(new WxCpExportRequest())).isEqualTo("JOB1");
    }

    @Test
    void exportAndWaitPollsUntilFinished() throws Exception {
        when(exportService.user(any(WxCpExportRequest.class))).thenReturn("JOB1");
        when(exportService.getResult("JOB1")).thenReturn(result(1), result(2));

        WxExportUtil util = newUtil(new WxExportQueryOptions(0L, 60_000L, 5));
        WxExportResultVO vo = util.exportAndWait(WxExportType.USER, new WxCpExportRequest());

        assertThat(vo.getStatus()).isEqualTo(2);
        verify(exportService, times(2)).getResult("JOB1");
    }

    @Test
    void exportAndWaitThrowsOnTimeout() throws Exception {
        when(exportService.user(any(WxCpExportRequest.class))).thenReturn("JOB1");
        when(exportService.getResult("JOB1")).thenReturn(result(1));

        WxExportUtil util = newUtil(new WxExportQueryOptions(0L, 60_000L, 3));
        assertThatThrownBy(() -> util.exportAndWait(WxExportType.USER, new WxCpExportRequest()))
                .isInstanceOf(QywxApiException.class);
        verify(exportService, times(3)).getResult("JOB1");
    }

    @Test
    void getResultWrapsWxErrorException() throws Exception {
        when(exportService.getResult("bad"))
                .thenThrow(new WxErrorException(WxError.builder().errorCode(60020).errorMsg("no").build()));
        WxExportUtil util = newUtil(WxExportQueryOptions.defaults());
        assertThatThrownBy(() -> util.getResult("bad")).isInstanceOf(QywxApiException.class);
    }
}
