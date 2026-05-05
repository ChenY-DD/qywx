package org.cy.qywx.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import org.cy.qywx.vo.WxUserVO;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * 类说明：HR花名册查询util工具。
 *
 * @author cy
 * Copyright (c) CY
 */
public class WxHrRosterQueryUtil {

    /**
     * 字段说明：日志。
     *
     * @author cy
     * Copyright (c) CY
     */
    private static final Logger log = LoggerFactory.getLogger(WxHrRosterQueryUtil.class);

    /**
     * 字段说明：字段配置URL。
     *
     * @author cy
     * Copyright (c) CY
     */
    public static final String FIELD_SETTING_URL = "https://qyapi.weixin.qq.com/cgi-bin/hr/get_field_setting";
    /**
     * 字段说明：员工信息URL。
     *
     * @author cy
     * Copyright (c) CY
     */
    public static final String STAFF_INFO_URL = "https://qyapi.weixin.qq.com/cgi-bin/hr/get_staff_info";

    /**
     * 字段说明：HR企业微信service。
     *
     * @author cy
     * Copyright (c) CY
     */
    private final WxCpService hrCpService;
    /**
     * 字段说明：contact查询util。
     *
     * @author cy
     * Copyright (c) CY
     */
    private final WxContactQueryUtil contactQueryUtil;
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
     * 字段说明：执行器。
     *
     * @author cy
     * Copyright (c) CY
     */
    private final Executor executor;
    /**
     * 字段说明：最大重试次数。
     *
     * @author cy
     * Copyright (c) CY
     */
    private final int maxRetryAttempts;
    /**
     * 字段说明：重试退避毫秒。
     *
     * @author cy
     * Copyright (c) CY
     */
    private final long retryBackoffMillis;
    /**
     * 字段说明：请求每秒。
     *
     * @author cy
     * Copyright (c) CY
     */
    private final double requestsPerSecond;

    /**
     * 创建 HR花名册查询util工具实例。
     *
     * @param hrCpService HR企业微信service
     * @param contactQueryUtil contact查询util
     * @param executor 执行器
     * @param maxRetryAttempts 最大重试次数
     * @param retryBackoffMillis 重试退避毫秒
     * @param requestsPerSecond 请求每秒
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxHrRosterQueryUtil(
            WxCpService hrCpService,
            WxContactQueryUtil contactQueryUtil,
            Executor executor,
            int maxRetryAttempts,
            long retryBackoffMillis,
            double requestsPerSecond
    ) {
        this(hrCpService, contactQueryUtil, executor,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
                maxRetryAttempts, retryBackoffMillis, requestsPerSecond);
    }

    /**
     * 创建 HR花名册查询util工具实例。
     *
     * @param hrCpService HR企业微信service
     * @param contactQueryUtil contact查询util
     * @param executor 执行器
     * @param httpClient HTTP客户端
     * @param maxRetryAttempts 最大重试次数
     * @param retryBackoffMillis 重试退避毫秒
     * @param requestsPerSecond 请求每秒
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxHrRosterQueryUtil(
            WxCpService hrCpService,
            WxContactQueryUtil contactQueryUtil,
            Executor executor,
            HttpClient httpClient,
            int maxRetryAttempts,
            long retryBackoffMillis,
            double requestsPerSecond
    ) {
        this.hrCpService = hrCpService;
        this.contactQueryUtil = contactQueryUtil;
        this.executor = executor == null ? Runnable::run : executor;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
        this.maxRetryAttempts = Math.max(1, maxRetryAttempts);
        this.retryBackoffMillis = Math.max(0L, retryBackoffMillis);
        this.requestsPerSecond = requestsPerSecond;
        log.info("WxHrRosterQueryUtil initialized: maxRetryAttempts={}, retryBackoffMillis={}, requestsPerSecond={}",
                this.maxRetryAttempts, this.retryBackoffMillis, this.requestsPerSecond);
    }

    /**
     * 获取字段配置。
     *
     * @return 字段配置
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     * @throws IOException HTTP 请求或响应解析失败时抛出
     * @throws InterruptedException 线程等待被中断时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public JsonNode getFieldSetting() throws WxErrorException, IOException, InterruptedException {
        return getJson(FIELD_SETTING_URL, Map.of());
    }

    /**
     * 获取员工信息。
     *
     * @param userId 成员 userId
     * @return 员工信息
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     * @throws IOException HTTP 请求或响应解析失败时抛出
     * @throws InterruptedException 线程等待被中断时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public JsonNode getStaffInfo(String userId) throws WxErrorException, IOException, InterruptedException {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        return getJson(STAFF_INFO_URL, Map.of("userid", userId));
    }

    /**
     * 获取全部员工信息。
     *
     * @return 全部员工信息
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxHrRosterResult getAllStaffInfo() throws WxErrorException {
        List<WxUserVO> users = contactQueryUtil.getAllUsersSimple();
        List<String> userIds = users.stream()
                .map(WxUserVO::getUserId)
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .distinct()
                .toList();
        return getStaffInfoBatch(userIds);
    }

    /**
     * 获取员工信息批次。
     *
     * @param userIds 成员 userId 集合
     * @return 员工信息批次
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxHrRosterResult getStaffInfoBatch(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return WxHrRosterResult.empty();
        }
        log.info("Fetching HR roster for {} userIds", userIds.size());
        long startTime = System.currentTimeMillis();

        SimpleRateLimiter rateLimiter = new SimpleRateLimiter(requestsPerSecond);

        List<CompletableFuture<RosterOutcome>> futures = userIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .map(userId -> CompletableFuture.supplyAsync(
                        () -> fetchStaffInfoWithRetry(userId, rateLimiter),
                        executor
                ))
                .toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture<?>[]::new)).join();

        Map<String, JsonNode> success = new LinkedHashMap<>();
        List<WxHrRosterFetchFailure> failures = new ArrayList<>();
        for (CompletableFuture<RosterOutcome> future : futures) {
            RosterOutcome outcome = future.join();
            if (outcome.staffInfo() != null) {
                success.put(outcome.userId(), outcome.staffInfo());
            } else if (outcome.failure() != null) {
                failures.add(outcome.failure());
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("HR roster batch query completed: success={}, failures={}, durationMs={}",
                success.size(), failures.size(), duration);
        if (!failures.isEmpty()) {
            log.warn("Failed to fetch {} HR rosters, sample: {}",
                    failures.size(),
                    failures.stream().map(WxHrRosterFetchFailure::userId).limit(10).toList());
        }
        return new WxHrRosterResult(Map.copyOf(success), List.copyOf(failures));
    }

    /**
     * 拉取员工信息with重试。
     *
     * @param userId 成员 userId
     * @param rateLimiter ratelimiter
     * @return 花名册outcome
     *
     * @author cy
     * Copyright (c) CY
     */
    private RosterOutcome fetchStaffInfoWithRetry(String userId, SimpleRateLimiter rateLimiter) {
        Throwable lastError = null;
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                rateLimiter.acquire();
                JsonNode info = getStaffInfo(userId);
                return new RosterOutcome(userId, info, null);
            } catch (Throwable t) {
                lastError = unwrap(t);
                log.warn("Failed to fetch HR roster for userId={}, attempt {}/{}: {}",
                        userId, attempt, maxRetryAttempts, lastError.getMessage());
                if (attempt < maxRetryAttempts) {
                    long backoff = exponentialBackoff(attempt);
                    if (backoff > 0) {
                        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(backoff));
                    }
                }
            }
        }
        String type = lastError == null ? "UNKNOWN" : lastError.getClass().getSimpleName();
        String msg = lastError == null ? "unknown error" : String.valueOf(lastError.getMessage());
        log.error("Failed to fetch HR roster for userId={} after {} attempts: {} - {}",
                userId, maxRetryAttempts, type, msg);
        return new RosterOutcome(userId, null,
                new WxHrRosterFetchFailure(userId, maxRetryAttempts, type, msg));
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
    private JsonNode getJson(String url, Map<String, String> params)
            throws WxErrorException, IOException, InterruptedException {
        String accessToken = hrCpService.getAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("HR access_token is not available");
        }
        StringBuilder fullUrl = new StringBuilder(url);
        fullUrl.append(url.contains("?") ? "&" : "?")
                .append("access_token=").append(urlEncode(accessToken));
        for (Map.Entry<String, String> e : params.entrySet()) {
            fullUrl.append("&").append(urlEncode(e.getKey())).append("=").append(urlEncode(e.getValue()));
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl.toString()))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP error: " + response.statusCode() + ", body: " + response.body());
        }
        JsonNode json = objectMapper.readTree(response.body());
        int errcode = json.path("errcode").asInt(-1);
        if (errcode != 0 && errcode != -1) {
            String errmsg = json.path("errmsg").asText("unknown error");
            throw new IOException("WeChat HR API error: errcode=" + errcode + ", errmsg=" + errmsg);
        }
        return json;
    }

    /**
     * 执行 exponentialBackoff 相关逻辑。
     *
     * @param attempt attempt
     * @return long
     *
     * @author cy
     * Copyright (c) CY
     */
    private long exponentialBackoff(int attempt) {
        if (retryBackoffMillis == 0L) {
            return 0L;
        }
        long backoff = retryBackoffMillis * (1L << (attempt - 1));
        long maxBackoff = retryBackoffMillis * 32;
        return Math.min(backoff, maxBackoff);
    }

    /**
     * 执行 unwrap 相关逻辑。
     *
     * @param t t
     * @return throwable
     *
     * @author cy
     * Copyright (c) CY
     */
    private Throwable unwrap(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c != c.getCause()) {
            c = c.getCause();
        }
        return c;
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
    private static String urlEncode(String value) {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 记录说明：花名册outcome。
     *
     * @param userId 成员 userId
     * @param staffInfo 员工信息
     * @param failure failure
     *
     * @author cy
     * Copyright (c) CY
     */
    private record RosterOutcome(String userId, JsonNode staffInfo, WxHrRosterFetchFailure failure) {
    }

    /**
     * 类说明：simpleratelimiter。
     *
     * @author cy
     * Copyright (c) CY
     */
    private static final class SimpleRateLimiter {
        /**
         * 字段说明：intervalnanos。
         *
         * @author cy
         * Copyright (c) CY
         */
        private final long intervalNanos;
        /**
         * 字段说明：nextallowednanos。
         *
         * @author cy
         * Copyright (c) CY
         */
        private long nextAllowedNanos;

        /**
         * 创建 simpleratelimiter实例。
         *
         * @param rps rps
         *
         * @author cy
         * Copyright (c) CY
         */
        private SimpleRateLimiter(double rps) {
            this.intervalNanos = rps <= 0D ? 0L : (long) (1_000_000_000D / rps);
            this.nextAllowedNanos = System.nanoTime();
        }

        /**
         * 执行 acquire 相关逻辑。
         *
         * @author cy
         * Copyright (c) CY
         */
        private synchronized void acquire() {
            if (intervalNanos <= 0L) {
                return;
            }
            long now = System.nanoTime();
            if (now < nextAllowedNanos) {
                LockSupport.parkNanos(nextAllowedNanos - now);
                now = System.nanoTime();
            }
            nextAllowedNanos = Math.max(nextAllowedNanos, now) + intervalNanos;
        }
    }
}
