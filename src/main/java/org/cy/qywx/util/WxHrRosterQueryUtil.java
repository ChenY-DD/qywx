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
 * 企业微信「智慧人事 / 人事助手」花名册查询工具。
 * <p>
 * 该工具调用 {@code /cgi-bin/hr/*} 系列接口，需要使用智慧人事应用的独立 secret，
 * 不能复用通讯录或自建应用的 secret。配置项见 {@code wx.cp.hr.secret}。
 * <p>
 * 若调用返回 {@code errcode=48002 / 60011} 等权限错误，请确认：
 * <ul>
 *     <li>企业已开通「智慧人事」应用</li>
 *     <li>使用的是该应用的专属 secret，而非通讯录 secret</li>
 *     <li>管理员已授权该应用对应的可见范围</li>
 * </ul>
 */
public class WxHrRosterQueryUtil {

    private static final Logger log = LoggerFactory.getLogger(WxHrRosterQueryUtil.class);

    public static final String FIELD_SETTING_URL = "https://qyapi.weixin.qq.com/cgi-bin/hr/get_field_setting";
    public static final String STAFF_INFO_URL = "https://qyapi.weixin.qq.com/cgi-bin/hr/get_staff_info";

    private final WxCpService hrCpService;
    private final WxContactQueryUtil contactQueryUtil;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Executor executor;
    private final int maxRetryAttempts;
    private final long retryBackoffMillis;
    private final double requestsPerSecond;

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
     * 获取花名册字段配置。
     *
     * @return 字段配置 JSON
     */
    public JsonNode getFieldSetting() throws WxErrorException, IOException, InterruptedException {
        return getJson(FIELD_SETTING_URL, Map.of());
    }

    /**
     * 获取单个员工的花名册信息。
     *
     * @param userId 成员 userId
     * @return 花名册 JSON，包含字段值列表
     */
    public JsonNode getStaffInfo(String userId) throws WxErrorException, IOException, InterruptedException {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        return getJson(STAFF_INFO_URL, Map.of("userid", userId));
    }

    /**
     * 获取全企业所有在职成员的花名册信息。
     * <p>
     * 内部流程：先通过通讯录接口拉取全员 userId，再并发批量调用花名册接口，自带限流和指数退避重试。
     *
     * @return 全员花名册查询结果，包含成功与失败列表
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
     * 批量获取指定 userId 列表的花名册信息。
     *
     * @param userIds 成员 userId 集合
     * @return 花名册查询结果，包含成功与失败列表
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

    private long exponentialBackoff(int attempt) {
        if (retryBackoffMillis == 0L) {
            return 0L;
        }
        long backoff = retryBackoffMillis * (1L << (attempt - 1));
        long maxBackoff = retryBackoffMillis * 32;
        return Math.min(backoff, maxBackoff);
    }

    private Throwable unwrap(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c != c.getCause()) {
            c = c.getCause();
        }
        return c;
    }

    private static String urlEncode(String value) {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record RosterOutcome(String userId, JsonNode staffInfo, WxHrRosterFetchFailure failure) {
    }

    private static final class SimpleRateLimiter {
        private final long intervalNanos;
        private long nextAllowedNanos;

        private SimpleRateLimiter(double rps) {
            this.intervalNanos = rps <= 0D ? 0L : (long) (1_000_000_000D / rps);
            this.nextAllowedNanos = System.nanoTime();
        }

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
