package org.cy.qywx.util.export;

import me.chanjar.weixin.common.error.WxError;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpExportService;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.export.WxCpExportRequest;
import me.chanjar.weixin.cp.bean.export.WxCpExportResult;
import org.cy.qywx.exception.QywxApiException;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

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
    void exportAndWaitFailsFastOnFailedStatus() throws Exception {
        when(exportService.user(any(WxCpExportRequest.class))).thenReturn("JOB1");
        when(exportService.getResult("JOB1")).thenReturn(result(3));

        WxExportUtil util = newUtil(new WxExportQueryOptions(0L, 60_000L, 5));
        assertThatThrownBy(() -> util.exportAndWait(WxExportType.USER, new WxCpExportRequest()))
                .isInstanceOf(QywxApiException.class)
                .hasMessageContaining("failed");
        verify(exportService, times(1)).getResult("JOB1");
    }

    @Test
    void getResultWrapsWxErrorException() throws Exception {
        when(exportService.getResult("bad"))
                .thenThrow(new WxErrorException(WxError.builder().errorCode(60020).errorMsg("no").build()));
        WxExportUtil util = newUtil(WxExportQueryOptions.defaults());
        assertThatThrownBy(() -> util.getResult("bad")).isInstanceOf(QywxApiException.class);
    }

    @Test
    void generateEncodingAesKeyHas43ValidChars() {
        String key = WxExportUtil.generateEncodingAesKey();
        assertThat(key).hasSize(43);
        assertThat(key).matches("[A-Za-z0-9]{43}");
    }

    @Test
    void generateEncodingAesKeyIsRandom() {
        assertThat(WxExportUtil.generateEncodingAesKey())
                .isNotEqualTo(WxExportUtil.generateEncodingAesKey());
    }

    @Test
    void decryptRoundTrip() throws Exception {
        String key = WxExportUtil.generateEncodingAesKey();
        byte[] plain = "{\"userid\":\"zhangsan\"}".getBytes(StandardCharsets.UTF_8);
        assertThat(WxExportUtil.decrypt(encrypt(plain, key), key)).isEqualTo(plain);
    }

    @Test
    @SuppressWarnings("unchecked")
    void downloadAndDecryptReturnsPlaintextAfterVerifyingMd5() throws Exception {
        String key = WxExportUtil.generateEncodingAesKey();
        byte[] plain = "{\"userid\":\"zhangsan\"}".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = encrypt(plain, key);

        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(encrypted);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        WxExportUtil util = new WxExportUtil(wxCpService, WxExportQueryOptions.defaults(), httpClient);
        WxExportDataVO data = new WxExportDataVO();
        data.setUrl("https://front.wxwork.qq.com/downloadobject?authkey=x");
        data.setSize(encrypted.length);
        data.setMd5(md5Hex(encrypted));

        assertThat(util.downloadAndDecrypt(data, key)).isEqualTo(plain);
    }

    @Test
    @SuppressWarnings("unchecked")
    void downloadAndDecryptThrowsOnMd5Mismatch() throws Exception {
        String key = WxExportUtil.generateEncodingAesKey();
        byte[] encrypted = encrypt("x".getBytes(StandardCharsets.UTF_8), key);

        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(encrypted);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        WxExportUtil util = new WxExportUtil(wxCpService, WxExportQueryOptions.defaults(), httpClient);
        WxExportDataVO data = new WxExportDataVO();
        data.setUrl("https://front.wxwork.qq.com/downloadobject?authkey=x");
        data.setSize(encrypted.length);
        data.setMd5("00000000000000000000000000000000");

        assertThatThrownBy(() -> util.downloadAndDecrypt(data, key))
                .isInstanceOf(QywxApiException.class)
                .hasMessageContaining("md5");
    }

    private static byte[] encrypt(byte[] plain, String encodingAesKey) throws Exception {
        byte[] aesKey = Base64.getDecoder().decode(encodingAesKey + "=");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"),
                new IvParameterSpec(Arrays.copyOf(aesKey, 16)));
        return cipher.doFinal(plain);
    }

    private static String md5Hex(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(bytes));
    }
}
