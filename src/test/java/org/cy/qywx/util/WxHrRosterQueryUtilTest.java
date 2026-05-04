package org.cy.qywx.util;

import com.fasterxml.jackson.databind.JsonNode;
import me.chanjar.weixin.cp.api.WxCpService;
import org.cy.qywx.vo.WxUserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WxHrRosterQueryUtilTest {

    private WxCpService hrCpService;
    private WxContactQueryUtil contactQueryUtil;
    private HttpClient httpClient;
    private WxHrRosterQueryUtil util;

    @BeforeEach
    void setUp() {
        hrCpService = mock(WxCpService.class);
        contactQueryUtil = mock(WxContactQueryUtil.class);
        httpClient = mock(HttpClient.class);
        util = new WxHrRosterQueryUtil(
                hrCpService,
                contactQueryUtil,
                Runnable::run,
                httpClient,
                3,
                0L,
                0D
        );
    }

    @Test
    void shouldFetchSingleStaffInfo() throws Exception {
        when(hrCpService.getAccessToken()).thenReturn("hr_token");
        HttpResponse<String> response = mockResponse(200, "{\"errcode\":0,\"info\":{\"userid\":\"zhangsan\"}}");
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(response);

        JsonNode info = util.getStaffInfo("zhangsan");

        assertNotNull(info);
        assertEquals(0, info.get("errcode").asInt());
        assertEquals("zhangsan", info.get("info").get("userid").asText());
    }

    @Test
    void shouldRejectBlankUserId() {
        assertThrows(IllegalArgumentException.class, () -> util.getStaffInfo(""));
        assertThrows(IllegalArgumentException.class, () -> util.getStaffInfo(null));
    }

    @Test
    void shouldReturnEmptyResultForEmptyInput() {
        WxHrRosterResult result = util.getStaffInfoBatch(List.of());

        assertTrue(result.staffInfo().isEmpty());
        assertTrue(result.failures().isEmpty());
    }

    @Test
    void shouldSeparateSuccessAndFailureInBatch() throws Exception {
        when(hrCpService.getAccessToken()).thenReturn("hr_token");
        HttpResponse<String> okResponse = mockResponse(200, "{\"errcode\":0,\"info\":{\"userid\":\"ok\"}}");
        HttpResponse<String> badResponse = mockResponse(200, "{\"errcode\":60011,\"errmsg\":\"no privilege\"}");
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenAnswer(invocation -> {
                    HttpRequest req = invocation.getArgument(0);
                    String url = req.uri().toString();
                    return url.contains("userid=bad") ? badResponse : okResponse;
                });

        WxHrRosterResult result = util.getStaffInfoBatch(List.of("ok1", "bad", "ok2"));

        assertEquals(2, result.staffInfo().size());
        assertTrue(result.staffInfo().containsKey("ok1"));
        assertTrue(result.staffInfo().containsKey("ok2"));
        assertEquals(1, result.failures().size());
        WxHrRosterFetchFailure failure = result.failures().get(0);
        assertEquals("bad", failure.userId());
        assertTrue(failure.errorMessage().contains("60011"));
    }

    @Test
    void shouldGetAllStaffInfoUsingContactUserIds() throws Exception {
        WxUserVO u1 = new WxUserVO();
        u1.setUserId("alice");
        WxUserVO u2 = new WxUserVO();
        u2.setUserId("bob");
        when(contactQueryUtil.getAllUsersSimple()).thenReturn(List.of(u1, u2));

        when(hrCpService.getAccessToken()).thenReturn("hr_token");
        HttpResponse<String> response = mockResponse(200, "{\"errcode\":0,\"info\":{}}");
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(response);

        WxHrRosterResult result = util.getAllStaffInfo();

        assertEquals(2, result.staffInfo().size());
        assertTrue(result.staffInfo().containsKey("alice"));
        assertTrue(result.staffInfo().containsKey("bob"));
        assertTrue(result.failures().isEmpty());
    }

    @Test
    void shouldRetryOnFailure() throws Exception {
        when(hrCpService.getAccessToken()).thenReturn("hr_token");
        HttpResponse<String> okResponse = mockResponse(200, "{\"errcode\":0,\"info\":{\"userid\":\"alice\"}}");
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenThrow(new IOException("boom1"))
                .thenThrow(new IOException("boom2"))
                .thenReturn(okResponse);

        WxHrRosterResult result = util.getStaffInfoBatch(List.of("alice"));

        assertEquals(1, result.staffInfo().size());
        assertTrue(result.failures().isEmpty());
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> mockResponse(int statusCode, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        return response;
    }
}
