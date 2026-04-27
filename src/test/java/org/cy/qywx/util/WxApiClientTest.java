package org.cy.qywx.util;

import com.fasterxml.jackson.databind.JsonNode;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WxApiClientTest {

    private WxCpService wxCpService;
    private HttpClient httpClient;
    private WxApiClient wxApiClient;

    @BeforeEach
    void setUp() {
        wxCpService = mock(WxCpService.class);
        httpClient = mock(HttpClient.class);
        wxApiClient = new WxApiClient(wxCpService, httpClient);
    }

    @Test
    void shouldAppendAccessTokenToGetRequest() throws Exception {
        when(wxCpService.getAccessToken()).thenReturn("test_token_123");

        HttpResponse<String> mockResponse = createMockResponse(200, "{\"errcode\":0,\"data\":\"success\"}");
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(mockResponse);

        String response = wxApiClient.get("https://qyapi.weixin.qq.com/cgi-bin/user/get");

        assertNotNull(response);
        assertTrue(response.contains("success"));
    }

    @Test
    void shouldAppendAccessTokenAndParamsToGetRequest() throws Exception {
        when(wxCpService.getAccessToken()).thenReturn("test_token_123");

        HttpResponse<String> mockResponse = createMockResponse(200, "{\"errcode\":0,\"userid\":\"zhangsan\"}");
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(mockResponse);

        String response = wxApiClient.get(
                "https://qyapi.weixin.qq.com/cgi-bin/user/get",
                Map.of("userid", "zhangsan")
        );

        assertNotNull(response);
        assertTrue(response.contains("zhangsan"));
    }

    @Test
    void shouldAppendAccessTokenToPostRequest() throws Exception {
        when(wxCpService.getAccessToken()).thenReturn("test_token_123");

        HttpResponse<String> mockResponse = createMockResponse(200, "{\"errcode\":0,\"msgid\":\"msg_123\"}");
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(mockResponse);

        String requestBody = "{\"touser\":\"zhangsan\",\"msgtype\":\"text\"}";
        String response = wxApiClient.post("https://qyapi.weixin.qq.com/cgi-bin/message/send", requestBody);

        assertNotNull(response);
        assertTrue(response.contains("msg_123"));
    }

    @Test
    void shouldParseJsonResponse() throws Exception {
        when(wxCpService.getAccessToken()).thenReturn("test_token_123");

        HttpResponse<String> mockResponse = createMockResponse(200, "{\"errcode\":0,\"name\":\"张三\",\"department\":[1,2]}");
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(mockResponse);

        JsonNode jsonNode = wxApiClient.getJson("https://qyapi.weixin.qq.com/cgi-bin/user/get");

        assertNotNull(jsonNode);
        assertEquals(0, jsonNode.get("errcode").asInt());
        assertEquals("张三", jsonNode.get("name").asText());
        assertEquals(2, jsonNode.get("department").size());
    }

    @Test
    void shouldThrowExceptionWhenAccessTokenIsNull() throws Exception {
        when(wxCpService.getAccessToken()).thenReturn(null);

        assertThrows(IllegalStateException.class, () ->
                wxApiClient.get("https://qyapi.weixin.qq.com/cgi-bin/user/get")
        );
    }

    @Test
    void shouldThrowExceptionWhenHttpStatusIsNot200() throws Exception {
        when(wxCpService.getAccessToken()).thenReturn("test_token_123");

        HttpResponse<String> mockResponse = createMockResponse(500, "Internal Server Error");
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(mockResponse);

        assertThrows(IOException.class, () ->
                wxApiClient.get("https://qyapi.weixin.qq.com/cgi-bin/user/get")
        );
    }

    @Test
    void shouldThrowExceptionWhenErrcodeIsNotZero() throws Exception {
        when(wxCpService.getAccessToken()).thenReturn("test_token_123");

        HttpResponse<String> mockResponse = createMockResponse(200, "{\"errcode\":40014,\"errmsg\":\"invalid access_token\"}");
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(mockResponse);

        assertThrows(IOException.class, () ->
                wxApiClient.get("https://qyapi.weixin.qq.com/cgi-bin/user/get")
        );
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> createMockResponse(int statusCode, String body) {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(statusCode);
        when(mockResponse.body()).thenReturn(body);
        return mockResponse;
    }
}
