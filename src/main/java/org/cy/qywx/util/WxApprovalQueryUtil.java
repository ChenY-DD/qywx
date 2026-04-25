package org.cy.qywx.util;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalDetailResult;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalInfo;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalInfoQueryFilter;
import me.chanjar.weixin.cp.bean.oa.WxCpOaApprovalTemplateResult;
import org.cy.qywx.vo.WxApprovalDetailVO;

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
     * 获取指定时间范围内审批数据涉及到的模板 ID 集合。
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 模板 ID 列表
     */
    public List<String> getTemplateIds(Date startTime, Date endTime) throws WxErrorException {
        return queryApprovalDetails(startTime, endTime).details().stream()
                .map(WxApprovalDetailVO::getTemplateId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 获取指定时间范围内审批数据涉及到的全部模板 ID。
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 模板 ID 列表
     */
    public List<String> getAllTemplateIds(Date startTime, Date endTime) throws WxErrorException {
        return getTemplateIds(startTime, endTime);
    }

    /**
     * 获取指定日期范围内审批数据涉及到的模板 ID 集合。
     *
     * @param dateRange 日期范围
     * @return 模板 ID 列表
     */
    public List<String> getTemplateIds(WxDateRange dateRange) throws WxErrorException {
        return getTemplateIds(dateRange.startTime(), dateRange.endTime());
    }

    /**
     * 获取指定日期范围内审批数据涉及到的全部模板 ID。
     *
     * @param dateRange 日期范围
     * @return 模板 ID 列表
     */
    public List<String> getAllTemplateIds(WxDateRange dateRange) throws WxErrorException {
        return getTemplateIds(dateRange);
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
        Map<String, String> result = new LinkedHashMap<>();
        for (String spNo : normalizeSpNos(spNos)) {
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

        try {
            for (Map.Entry<String, String> entry : templateIdsBySpNo.entrySet()) {
                String templateId = entry.getValue();
                if (templateId == null || templateId.isBlank()) {
                    result.put(entry.getKey(), null);
                    continue;
                }
                WxCpOaApprovalTemplateResult templateDetail = templateCache.computeIfAbsent(templateId, key -> {
                    try {
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

        Set<String> spNos = new LinkedHashSet<>();
        Date segmentStartTime = startTime;
        long segmentMillis = Math.max(1, options.segmentDays()) * 24L * 60 * 60 * 1000L;

        while (!segmentStartTime.after(endTime)) {
            Date segmentEndTime = new Date(Math.min(
                    segmentStartTime.getTime() + segmentMillis,
                    endTime.getTime()
            ));

            String cursor = null;
            while (true) {
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
            }

            if (segmentEndTime.equals(endTime)) {
                break;
            }
            segmentStartTime = new Date(segmentEndTime.getTime() + 1000);
        }

        return new ArrayList<>(spNos);
    }

    private WxApprovalDetailQueryResult queryApprovalDetails(
            Date startTime,
            Date endTime,
            List<WxCpApprovalInfoQueryFilter> filters
    ) throws WxErrorException {
        List<String> spNos = getApprovalSpNos(startTime, endTime, filters);
        if (spNos.isEmpty()) {
            return WxApprovalDetailQueryResult.empty();
        }

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

        return new WxApprovalDetailQueryResult(List.copyOf(details), List.copyOf(failures));
    }

    private DetailQueryOutcome fetchApprovalDetailWithRetry(String spNo, SimpleRateLimiter rateLimiter) {
        int maxAttempts = Math.max(1, options.maxRetryAttempts());
        Throwable lastError = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                rateLimiter.acquire();
                return new DetailQueryOutcome(fetchApprovalDetail(spNo), null);
            } catch (Throwable throwable) {
                lastError = unwrap(throwable);
                if (attempt < maxAttempts) {
                    sleepBackoff(attempt);
                }
            }
        }

        String errorType = lastError == null ? "UNKNOWN" : lastError.getClass().getSimpleName();
        String errorMessage = lastError == null ? "unknown error" : String.valueOf(lastError.getMessage());
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

    private List<String> toSpNoList(String... spNos) {
        return spNos == null ? List.of() : List.of(spNos);
    }

    private void sleepBackoff(int attempt) {
        long baseBackoff = Math.max(0L, options.retryBackoffMillis());
        long backoffMillis = baseBackoff * attempt;
        if (backoffMillis > 0) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(backoffMillis));
        }
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
        private final long intervalNanos;
        private long nextAllowedNanos;

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
                LockSupport.parkNanos(nextAllowedNanos - now);
                now = System.nanoTime();
            }
            nextAllowedNanos = Math.max(nextAllowedNanos, now) + intervalNanos;
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
