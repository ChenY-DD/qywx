package org.cy.qywx.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 类说明：API客户端。
 *
 * @author cy
 * Copyright (c) CY
 */
public class WxApiClient {

    /**
     * 字段说明：日志。
     *
     * @author cy
     * Copyright (c) CY
     */
    private static final Logger log = LoggerFactory.getLogger(WxApiClient.class);

    /**
     * 字段说明：企业微信企业微信service。
     *
     * @author cy
     * Copyright (c) CY
     */
    private final WxCpService wxCpService;
    /**
     * 字段说明：HTTP客户端。
     *
     * @author cy
     * Copyright (c) CY
     */
    private final HttpClient httpClient;
    /**
     * 字段说明：对象映射器。
     *
     * @author cy
     * Copyright (c) CY
     */
    private final ObjectMapper objectMapper;

    /**
     * 创建 API客户端实例。
     *
     * @param wxCpService 企业微信企业微信service
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxApiClient(WxCpService wxCpService) {
        this(wxCpService, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    /**
     * 创建 API客户端实例。
     *
     * @param wxCpService 企业微信企业微信service
     * @param httpClient HTTP客户端
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxApiClient(WxCpService wxCpService, HttpClient httpClient) {
        this.wxCpService = wxCpService;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
        log.info("WxApiClient initialized");
    }

    /**
     * 创建 API客户端实例。
     *
     * @param wxCpService 企业微信企业微信service
     * @param httpClient HTTP客户端
     * @param ignoredMeterRegistry ignoredmeterregistry
     *
     * @author cy
     * Copyright (c) CY
     */
    @Deprecated(since = "1.0.4", forRemoval = true)
    public WxApiClient(WxCpService wxCpService, HttpClient httpClient, Object ignoredMeterRegistry) {
        this(wxCpService, httpClient);
    }

    /**
     * 发送 GET 请求并返回响应文本。
     *
     * @param url 企业微信 API 地址
     * @return string
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     * @throws IOException HTTP 请求或响应解析失败时抛出
     * @throws InterruptedException 线程等待被中断时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public String get(String url) throws WxErrorException, IOException, InterruptedException {
        return get(url, Map.of());
    }

    /**
     * 发送 GET 请求并返回响应文本。
     *
     * @param url 企业微信 API 地址
     * @param params URL 查询参数
     * @return string
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     * @throws IOException HTTP 请求或响应解析失败时抛出
     * @throws InterruptedException 线程等待被中断时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public String get(String url, Map<String, String> params) throws WxErrorException, IOException, InterruptedException {
        log.debug("GET request: url={}, params={}", url, params);
        long startTime = System.nanoTime();

        try {
            String fullUrl = buildUrl(url, params);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String result = handleResponse(response);

            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            log.debug("GET request completed: url={}, statusCode={}, durationMs={}", url, response.statusCode(), duration);

            return result;
        } catch (Exception e) {
            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            log.error("GET request failed: url={}, durationMs={}, error={}", url, duration, e.getMessage());
            throw e;
        }
    }

    /**
     * 发送 POST 请求并返回响应文本。
     *
     * @param url 企业微信 API 地址
     * @param body JSON 请求体
     * @return string
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     * @throws IOException HTTP 请求或响应解析失败时抛出
     * @throws InterruptedException 线程等待被中断时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public String post(String url, String body) throws WxErrorException, IOException, InterruptedException {
        return post(url, Map.of(), body);
    }

    /**
     * 发送 POST 请求并返回响应文本。
     *
     * @param url 企业微信 API 地址
     * @param params URL 查询参数
     * @param body JSON 请求体
     * @return string
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     * @throws IOException HTTP 请求或响应解析失败时抛出
     * @throws InterruptedException 线程等待被中断时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public String post(String url, Map<String, String> params, String body) throws WxErrorException, IOException, InterruptedException {
        log.debug("POST request: url={}, params={}, bodyLength={}", url, params, body != null ? body.length() : 0);
        long startTime = System.nanoTime();

        try {
            String fullUrl = buildUrl(url, params);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String result = handleResponse(response);

            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            log.debug("POST request completed: url={}, statusCode={}, durationMs={}", url, response.statusCode(), duration);

            return result;
        } catch (Exception e) {
            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            log.error("POST request failed: url={}, durationMs={}, error={}", url, duration, e.getMessage());
            throw e;
        }
    }

    /**
     * 发送 GET 请求并解析 JSON 响应。
     *
     * @param url 企业微信 API 地址
     * @return JSON
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     * @throws IOException HTTP 请求或响应解析失败时抛出
     * @throws InterruptedException 线程等待被中断时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public JsonNode getJson(String url) throws WxErrorException, IOException, InterruptedException {
        return getJson(url, Map.of());
    }

    /**
     * 发送 GET 请求并解析 JSON 响应。
     *
     * @param url 企业微信 API 地址
     * @param params URL 查询参数
     * @return JSON
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     * @throws IOException HTTP 请求或响应解析失败时抛出
     * @throws InterruptedException 线程等待被中断时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public JsonNode getJson(String url, Map<String, String> params) throws WxErrorException, IOException, InterruptedException {
        String response = get(url, params);
        return objectMapper.readTree(response);
    }

    /**
     * 发送 POST 请求并解析 JSON 响应。
     *
     * @param url 企业微信 API 地址
     * @param body JSON 请求体
     * @return JSON节点
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     * @throws IOException HTTP 请求或响应解析失败时抛出
     * @throws InterruptedException 线程等待被中断时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public JsonNode postJson(String url, String body) throws WxErrorException, IOException, InterruptedException {
        return postJson(url, Map.of(), body);
    }

    /**
     * 发送 POST 请求并解析 JSON 响应。
     *
     * @param url 企业微信 API 地址
     * @param params URL 查询参数
     * @param body JSON 请求体
     * @return JSON节点
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     * @throws IOException HTTP 请求或响应解析失败时抛出
     * @throws InterruptedException 线程等待被中断时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public JsonNode postJson(String url, Map<String, String> params, String body) throws WxErrorException, IOException, InterruptedException {
        String response = post(url, params, body);
        return objectMapper.readTree(response);
    }

    /**
     * 构建URL。
     *
     * @param url 企业微信 API 地址
     * @param params URL 查询参数
     * @return string
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    private String buildUrl(String url, Map<String, String> params) throws WxErrorException {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url must not be blank");
        }

        String accessToken = wxCpService.getAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("access_token is not available");
        }

        StringBuilder urlBuilder = new StringBuilder(url);
        String separator = url.contains("?") ? "&" : "?";
        urlBuilder.append(separator).append("access_token=").append(urlEncode(accessToken));

        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                urlBuilder.append("&")
                        .append(urlEncode(entry.getKey()))
                        .append("=")
                        .append(urlEncode(entry.getValue()));
            }
        }

        return urlBuilder.toString();
    }

    /**
     * 处理response。
     *
     * @param response response
     * @return string
     * @throws IOException HTTP 请求或响应解析失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    private String handleResponse(HttpResponse<String> response) throws IOException {
        if (response.statusCode() != 200) {
            log.error("HTTP error: statusCode={}, body={}", response.statusCode(), response.body());
            throw new IOException("HTTP error: " + response.statusCode() + ", body: " + response.body());
        }

        String body = response.body();
        JsonNode jsonNode = objectMapper.readTree(body);
        int errcode = jsonNode.path("errcode").asInt(-1);

        if (errcode != 0 && errcode != -1) {
            String errmsg = jsonNode.path("errmsg").asText("unknown error");
            log.error("WeChat API error: errcode={}, errmsg={}", errcode, errmsg);
            throw new IOException("WeChat API error: errcode=" + errcode + ", errmsg=" + errmsg);
        }

        return body;
    }

    /**
     * 对 URL 参数值进行编码。
     *
     * @param value 值
     * @return string
     *
     * @author cy
     * Copyright (c) CY
     */
    private String urlEncode(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
