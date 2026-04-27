package org.cy.qywx.util;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalDetailResult;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalInfo;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalInfoQueryFilter;
import me.chanjar.weixin.cp.bean.oa.WxCpOaApprovalTemplateResult;
import org.cy.qywx.vo.WxApprovalDetailVO;
import org.cy.qywx.vo.WxApprovalTemplateVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

/**
 * 企业微信审批查询工具，封装审批单号、审批详情、模板及日期范围相关查询能力。
 */
public class WxApprovalQueryUtil {

    private static final Logger log = LoggerFactory.getLogger(WxApprovalQueryUtil.class);

    private final WxCpService wxCpService;
    private final Executor detailQueryExecutor;
    private final WxApprovalQueryOptions options;

    public WxApprovalQueryUtil(WxCpService wxCpService) {
        this(wxCpService, Runnable::run, WxApprovalQueryOptions.defaults());
    }

    public WxApprovalQueryUtil(
            WxCpService wxCpService,
            Executor detailQueryExecutor,
            WxApprovalQueryOptions options
    ) {
        this.wxCpService = wxCpService;
        this.detailQueryExecutor = detailQueryExecutor;
        this.options = options == null ? WxApprovalQueryOptions.defaults() : options;
        log.info("WxApprovalQueryUtil initialized with options: segmentDays={}, pageSize={}, maxRetryAttempts={}, retryBackoffMillis={}, requestsPerSecond={}",
                this.options.segmentDays(), this.options.pageSize(), this.options.maxRetryAttempts(),
                this.options.retryBackoffMillis(), this.options.requestsPerSecond());
    }

    @Deprecated(since = "1.0.4", forRemoval = true)
    public WxApprovalQueryUtil(
            WxCpService wxCpService,
            Executor detailQueryExecutor,
            WxApprovalQueryOptions options,
            Object ignoredMeterRegistry
    ) {
        this(wxCpService, detailQueryExecutor, options);
    }

    /**
     * 按时间范围查询审批单号。
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 审批单号列表
     */
    public List<String> getApprovalSpNos(Date startTime, Date endTime) throws WxErrorException {
        return getApprovalSpNos(startTime, endTime, List.of());
    }

    /**
     * 按日期范围对象查询审批单号。
     *
     * @param dateRange 日期范围
     * @return 审批单号列表
     */
    public List<String> getApprovalSpNos(WxDateRange dateRange) throws WxErrorException {
        return getApprovalSpNos(dateRange.startTime(), dateRange.endTime());
    }

    /**
     * 按模板 ID 和时间范围查询审批单号。
     *
     * @param templateId 模板 ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 审批单号列表
     */
    public List<String> getApprovalSpNosByTemplateId(String templateId, Date startTime, Date endTime) throws WxErrorException {
        return getApprovalSpNos(startTime, endTime, List.of(createTemplateFilter(templateId)));
    }

    /**
     * 按模板 ID 和日期范围对象查询审批单号。
     *
     * @param templateId 模板 ID
     * @param dateRange 日期范围
     * @return 审批单号列表
     */
    public List<String> getApprovalSpNosByTemplateId(String templateId, WxDateRange dateRange) throws WxErrorException {
        return getApprovalSpNosByTemplateId(templateId, dateRange.startTime(), dateRange.endTime());
    }

    /**
     * 获取指定时间范围内出现过的审批模板列表，包含模板 ID 和模板名称。
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 模板列表
     */
    public List<WxApprovalTemplateVO> getTemplates(Date startTime, Date endTime) throws WxErrorException {
        return toTemplateList(queryApprovalDetails(startTime, endTime).details());
    }

    /**
     * 获取指定日期范围内出现过的审批模板列表，包含模板 ID 和模板名称。
     *
     * @param dateRange 日期范围
     * @return 模板列表
     */
    public List<WxApprovalTemplateVO> getTemplates(WxDateRange dateRange) throws WxErrorException {
        return getTemplates(dateRange.startTime(), dateRange.endTime());
    }

    /**
     * 获取指定时间范围内出现过的审批模板映射，key 为模板 ID。
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 模板映射
     */
    public Map<String, WxApprovalTemplateVO> getTemplateMap(Date startTime, Date endTime) throws WxErrorException {
        return toTemplateMapByTemplates(getTemplates(startTime, endTime));
    }

    /**
     * 获取指定日期范围内出现过的审批模板映射，key 为模板 ID。
     *
     * @param dateRange 日期范围
     * @return 模板映射
     */
    public Map<String, WxApprovalTemplateVO> getTemplateMap(WxDateRange dateRange) throws WxErrorException {
        return getTemplateMap(dateRange.startTime(), dateRange.endTime());
    }

    /**
     * 根据模板 ID 获取审批模板详情。
     *
     * @param templateId 模板 ID
     * @return 模板详情
     */
    public WxCpOaApprovalTemplateResult getTemplateDetail(String templateId) throws WxErrorException {
        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException("templateId must not be blank");
        }
        return wxCpService.getOaService().getTemplateDetail(templateId);
    }

    /**
     * 根据多个审批单号查询它们对应的模板 ID。
     *
     * @param spNos 审批单号，可变参数
     * @return key 为审批单号，value 为模板 ID
     */
    public Map<String, String> getTemplateIdsBySpNos(String... spNos) throws WxErrorException {
        return getTemplateIdsBySpNos(toSpNoList(spNos));
    }

    /**
     * 根据多个审批单号查询它们对应的模板 ID。
     *
     * @param spNos 审批单号集合
     * @return key 为审批单号，value 为模板 ID
     */
    public Map<String, String> getTemplateIdsBySpNos(Collection<String> spNos) throws WxErrorException {
        SimpleRateLimiter rateLimiter = new SimpleRateLimiter(options.requestsPerSecond());
        Map<String, String> result = new LinkedHashMap<>();
        for (String spNo : normalizeSpNos(spNos)) {
            if (options.requestsPerSecond() > 0) {
                rateLimiter.acquire();
            }
            WxApprovalDetailVO detail = fetchApprovalDetail(spNo);
            result.put(spNo, detail == null ? null : detail.getTemplateId());
        }
        return result;
    }

    /**
     * 根据多个审批单号查询它们对应的模板详情。
     *
     * @param spNos 审批单号，可变参数
     * @return key 为审批单号，value 为模板详情
     */
    public Map<String, WxCpOaApprovalTemplateResult> getTemplateDetailsBySpNos(String... spNos) throws WxErrorException {
        return getTemplateDetailsBySpNos(toSpNoList(spNos));
    }

    /**
     * 根据多个审批单号查询它们对应的模板详情。
     *
     * @param spNos 审批单号集合
     * @return key 为审批单号，value 为模板详情
     */
    public Map<String, WxCpOaApprovalTemplateResult> getTemplateDetailsBySpNos(Collection<String> spNos) throws WxErrorException {
        Map<String, String> templateIdsBySpNo = getTemplateIdsBySpNos(spNos);
        Map<String, WxCpOaApprovalTemplateResult> templateCache = new LinkedHashMap<>();
        Map<String, WxCpOaApprovalTemplateResult> result = new LinkedHashMap<>();
        SimpleRateLimiter rateLimiter = new SimpleRateLimiter(options.requestsPerSecond());

        try {
            for (Map.Entry<String, String> entry : templateIdsBySpNo.entrySet()) {
                String templateId = entry.getValue();
                if (templateId == null || templateId.isBlank()) {
                    result.put(entry.getKey(), null);
                    continue;
                }
                WxCpOaApprovalTemplateResult templateDetail = templateCache.computeIfAbsent(templateId, key -> {
                    try {
                        if (options.requestsPerSecond() > 0) {
                            rateLimiter.acquire();
                        }
                        return getTemplateDetail(key);
                    } catch (WxErrorException e) {
                        throw new TemplateQueryRuntimeException(e);
                    }
                });
                result.put(entry.getKey(), templateDetail);
            }
        } catch (TemplateQueryRuntimeException e) {
            throw e.cause;
        }
        return result;
    }

    /**
     * 按时间范围查询审批详情。
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 审批详情列表
     */
    public List<WxApprovalDetailVO> getApprovalDetails(Date startTime, Date endTime) throws WxErrorException {
        return queryApprovalDetails(startTime, endTime).details();
    }

    /**
     * 按日期范围对象查询审批详情。
     *
     * @param dateRange 日期范围
     * @return 审批详情列表
     */
    public List<WxApprovalDetailVO> getApprovalDetails(WxDateRange dateRange) throws WxErrorException {
        return getApprovalDetails(dateRange.startTime(), dateRange.endTime());
    }

    /**
     * 按模板 ID 和时间范围查询审批详情。
     *
     * @param templateId 模板 ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 审批详情列表
     */
    public List<WxApprovalDetailVO> getApprovalDetailsByTemplateId(String templateId, Date startTime, Date endTime)
            throws WxErrorException {
        return queryApprovalDetailsByTemplateId(templateId, startTime, endTime).details();
    }

    /**
     * 按时间范围查询审批详情，并按模板 ID 分组。
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 按模板 ID 分组后的审批详情
     */
    public Map<String, List<WxApprovalDetailVO>> getApprovalDetailsGroupByTemplateId(Date startTime, Date endTime)
            throws WxErrorException {
        return queryApprovalDetails(startTime, endTime).groupByTemplateId();
    }

    /**
     * 按时间范围查询审批详情，并返回成功与失败结果。
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 审批查询结果
     */
    public WxApprovalDetailQueryResult queryApprovalDetails(Date startTime, Date endTime) throws WxErrorException {
        return queryApprovalDetails(startTime, endTime, List.of());
    }

    /**
     * 按日期范围对象查询审批详情，并返回成功与失败结果。
     *
     * @param dateRange 日期范围
     * @return 审批查询结果
     */
    public WxApprovalDetailQueryResult queryApprovalDetails(WxDateRange dateRange) throws WxErrorException {
        return queryApprovalDetails(dateRange.startTime(), dateRange.endTime());
    }

    /**
     * 按模板 ID 和时间范围查询审批详情，并返回成功与失败结果。
     *
     * @param templateId 模板 ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 审批查询结果
     */
    public WxApprovalDetailQueryResult queryApprovalDetailsByTemplateId(String templateId, Date startTime, Date endTime)
            throws WxErrorException {
        return queryApprovalDetails(startTime, endTime, List.of(createTemplateFilter(templateId)));
    }

    /**
     * 按模板 ID 和日期范围对象查询审批详情，并返回成功与失败结果。
     *
     * @param templateId 模板 ID
     * @param dateRange 日期范围
     * @return 审批查询结果
     */
    public WxApprovalDetailQueryResult queryApprovalDetailsByTemplateId(String templateId, WxDateRange dateRange)
            throws WxErrorException {
        return queryApprovalDetailsByTemplateId(templateId, dateRange.startTime(), dateRange.endTime());
    }

    private List<String> getApprovalSpNos(
            Date startTime,
            Date endTime,
            List<WxCpApprovalInfoQueryFilter> filters
    ) throws WxErrorException {
        validateTimeRange(startTime, endTime);

        log.debug("Starting approval spNo query: startTime={}, endTime={}, filters={}", startTime, endTime, filters);
        long queryStartTime = System.currentTimeMillis();

        SimpleRateLimiter rateLimiter = new SimpleRateLimiter(options.requestsPerSecond());
        Set<String> spNos = new LinkedHashSet<>();
        Date segmentStartTime = startTime;
        long segmentMillis = Math.max(1, options.segmentDays()) * 24L * 60 * 60 * 1000L;
        int totalApiCalls = 0;

        while (!segmentStartTime.after(endTime)) {
            Date segmentEndTime = new Date(Math.min(
                    segmentStartTime.getTime() + segmentMillis,
                    endTime.getTime()
            ));

            String cursor = null;
            int pageCount = 0;
            while (true) {
                if (options.requestsPerSecond() > 0) {
                    rateLimiter.acquire();
                }

                totalApiCalls++;
                WxCpApprovalInfo approvalInfo = wxCpService.getOaService().getApprovalInfo(
                        segmentStartTime,
                        segmentEndTime,
                        cursor,
                        Math.max(1, options.pageSize()),
                        filters.isEmpty() ? null : filters
                );

                if (approvalInfo.getSpNoList() != null && !approvalInfo.getSpNoList().isEmpty()) {
                    spNos.addAll(approvalInfo.getSpNoList());
                }

                String nextCursor = approvalInfo.getNewNextCursor();
                if (nextCursor == null || nextCursor.isBlank() || Objects.equals(nextCursor, cursor)) {
                    break;
                }
                cursor = nextCursor;
                pageCount++;
            }

            if (segmentEndTime.equals(endTime)) {
                break;
            }
            segmentStartTime = new Date(segmentEndTime.getTime() + 1000);
        }

        long queryDuration = System.currentTimeMillis() - queryStartTime;
        log.info("Approval spNo query completed: totalSpNos={}, totalApiCalls={}, durationMs={}",
                spNos.size(), totalApiCalls, queryDuration);

        return new ArrayList<>(spNos);
    }

    private WxApprovalDetailQueryResult queryApprovalDetails(
            Date startTime,
            Date endTime,
            List<WxCpApprovalInfoQueryFilter> filters
    ) throws WxErrorException {
        log.debug("Starting approval details query: startTime={}, endTime={}, filters={}", startTime, endTime, filters);
        long queryStartTime = System.currentTimeMillis();

        List<String> spNos = getApprovalSpNos(startTime, endTime, filters);
        if (spNos.isEmpty()) {
            log.info("No approval spNos found for the given time range");
            return WxApprovalDetailQueryResult.empty();
        }

        log.info("Fetching details for {} approval spNos", spNos.size());
        SimpleRateLimiter rateLimiter = new SimpleRateLimiter(options.requestsPerSecond());
        List<CompletableFuture<DetailQueryOutcome>> futures = spNos.stream()
                .map(spNo -> CompletableFuture.supplyAsync(
                        () -> fetchApprovalDetailWithRetry(spNo, rateLimiter),
                        detailQueryExecutor
                ))
                .toList();

        CompletableFuture<?>[] waits = futures.toArray(CompletableFuture<?>[]::new);
        CompletableFuture.allOf(waits).join();

        List<WxApprovalDetailVO> details = new ArrayList<>();
        List<WxApprovalDetailFetchFailure> failures = new ArrayList<>();
        for (CompletableFuture<DetailQueryOutcome> future : futures) {
            DetailQueryOutcome outcome = future.join();
            if (outcome.detail() != null) {
                details.add(outcome.detail());
            } else if (outcome.failure() != null) {
                failures.add(outcome.failure());
            }
        }

        long queryDuration = System.currentTimeMillis() - queryStartTime;
        log.info("Approval details query completed: totalDetails={}, totalFailures={}, durationMs={}",
                details.size(), failures.size(), queryDuration);

        if (!failures.isEmpty()) {
            log.warn("Failed to fetch {} approval details: {}", failures.size(),
                    failures.stream().map(WxApprovalDetailFetchFailure::spNo).limit(10).toList());
        }

        return new WxApprovalDetailQueryResult(List.copyOf(details), List.copyOf(failures));
    }

    private DetailQueryOutcome fetchApprovalDetailWithRetry(String spNo, SimpleRateLimiter rateLimiter) {
        int maxAttempts = Math.max(1, options.maxRetryAttempts());
        Throwable lastError = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                rateLimiter.acquire();
                long startTime = System.nanoTime();
                WxApprovalDetailVO detail = fetchApprovalDetail(spNo);
                long duration = System.nanoTime() - startTime;

                if (attempt > 1) {
                    log.debug("Successfully fetched approval detail for spNo={} on attempt {}, durationMs={}",
                            spNo, attempt, TimeUnit.NANOSECONDS.toMillis(duration));
                }
                return new DetailQueryOutcome(detail, null);
            } catch (Throwable throwable) {
                lastError = unwrap(throwable);
                log.warn("Failed to fetch approval detail for spNo={}, attempt {}/{}: {}",
                        spNo, attempt, maxAttempts, lastError.getMessage());

                if (attempt < maxAttempts) {
                    long backoffMillis = calculateExponentialBackoff(attempt);
                    if (backoffMillis > 0) {
                        log.debug("Retrying after {}ms backoff for spNo={}", backoffMillis, spNo);
                        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(backoffMillis));
                    }
                }
            }
        }

        String errorType = lastError == null ? "UNKNOWN" : lastError.getClass().getSimpleName();
        String errorMessage = lastError == null ? "unknown error" : String.valueOf(lastError.getMessage());

        log.error("Failed to fetch approval detail for spNo={} after {} attempts: {} - {}",
                spNo, maxAttempts, errorType, errorMessage);

        return new DetailQueryOutcome(
                null,
                new WxApprovalDetailFetchFailure(spNo, maxAttempts, errorType, errorMessage)
        );
    }

    private WxApprovalDetailVO fetchApprovalDetail(String spNo) throws WxErrorException {
        WxCpApprovalDetailResult detail = wxCpService.getOaService().getApprovalDetail(spNo);
        return WxApprovalConverter.from(detail);
    }

    private WxCpApprovalInfoQueryFilter createTemplateFilter(String templateId) {
        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException("templateId must not be blank");
        }
        WxCpApprovalInfoQueryFilter filter = new WxCpApprovalInfoQueryFilter();
        filter.setKey(WxCpApprovalInfoQueryFilter.KEY.TEMPLATE_ID);
        filter.setValue(templateId);
        return filter;
    }

    private List<String> normalizeSpNos(Collection<String> spNos) {
        if (spNos == null || spNos.isEmpty()) {
            throw new IllegalArgumentException("spNos must not be empty");
        }
        List<String> normalized = spNos.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(spNo -> !spNo.isEmpty())
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("spNos must not be empty");
        }
        return normalized;
    }

    private List<WxApprovalTemplateVO> toTemplateList(List<WxApprovalDetailVO> details) {
        return new ArrayList<>(toTemplateMapByDetails(details).values());
    }

    private Map<String, WxApprovalTemplateVO> toTemplateMapByDetails(List<WxApprovalDetailVO> details) {
        Map<String, WxApprovalTemplateVO> templateMap = new LinkedHashMap<>();
        for (WxApprovalDetailVO detail : details) {
            if (detail == null || detail.getTemplateId() == null || detail.getTemplateId().isBlank()) {
                continue;
            }
            templateMap.computeIfAbsent(detail.getTemplateId(), key -> {
                WxApprovalTemplateVO template = new WxApprovalTemplateVO();
                template.setTemplateId(detail.getTemplateId());
                template.setTemplateName(detail.getSpName());
                return template;
            });
        }
        return templateMap;
    }

    private Map<String, WxApprovalTemplateVO> toTemplateMapByTemplates(List<WxApprovalTemplateVO> templates) {
        Map<String, WxApprovalTemplateVO> templateMap = new LinkedHashMap<>();
        for (WxApprovalTemplateVO template : templates) {
            if (template == null || template.getTemplateId() == null || template.getTemplateId().isBlank()) {
                continue;
            }
            templateMap.put(template.getTemplateId(), template);
        }
        return templateMap;
    }

    private List<String> toSpNoList(String... spNos) {
        return spNos == null ? List.of() : List.of(spNos);
    }

    private void sleepBackoff(int attempt) {
        long backoffMillis = calculateExponentialBackoff(attempt);
        if (backoffMillis > 0) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(backoffMillis));
        }
    }

    private long calculateExponentialBackoff(int attempt) {
        long baseBackoff = Math.max(0L, options.retryBackoffMillis());
        if (baseBackoff == 0L) {
            return 0L;
        }
        long exponentialBackoff = baseBackoff * (1L << (attempt - 1));
        long maxBackoff = baseBackoff * 32;
        return Math.min(exponentialBackoff, maxBackoff);
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current != current.getCause()) {
            current = current.getCause();
        }
        return current;
    }

    private void validateTimeRange(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime and endTime must not be null");
        }
        if (!startTime.before(endTime)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
    }

    private record DetailQueryOutcome(
            WxApprovalDetailVO detail,
            WxApprovalDetailFetchFailure failure
    ) {
    }

    private static final class SimpleRateLimiter {
        private static final Logger log = LoggerFactory.getLogger(SimpleRateLimiter.class);
        private final long intervalNanos;
        private long nextAllowedNanos;
        private long totalAcquires = 0;
        private long totalWaitTimeNanos = 0;

        private SimpleRateLimiter(double requestsPerSecond) {
            if (requestsPerSecond <= 0D) {
                this.intervalNanos = 0L;
            } else {
                this.intervalNanos = (long) (1_000_000_000D / requestsPerSecond);
            }
            this.nextAllowedNanos = System.nanoTime();
        }

        private synchronized void acquire() {
            if (intervalNanos <= 0L) {
                return;
            }
            long now = System.nanoTime();
            if (now < nextAllowedNanos) {
                long waitTime = nextAllowedNanos - now;
                totalWaitTimeNanos += waitTime;
                LockSupport.parkNanos(waitTime);
                now = System.nanoTime();
            }
            nextAllowedNanos = Math.max(nextAllowedNanos, now) + intervalNanos;
            totalAcquires++;

            if (totalAcquires % 100 == 0) {
                log.debug("Rate limiter stats: totalAcquires={}, avgWaitTimeMs={}",
                        totalAcquires, TimeUnit.NANOSECONDS.toMillis(totalWaitTimeNanos / totalAcquires));
            }
        }
    }

    private static final class TemplateQueryRuntimeException extends RuntimeException {
        private final WxErrorException cause;

        private TemplateQueryRuntimeException(WxErrorException cause) {
            super(cause);
            this.cause = cause;
        }
    }

}
