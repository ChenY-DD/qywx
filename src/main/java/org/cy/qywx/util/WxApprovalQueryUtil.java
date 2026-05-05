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
 * 类说明：审批查询util工具。
 *
 * @author cy
 * Copyright (c) CY
 */
public class WxApprovalQueryUtil {

    /**
     * 字段说明：日志。
     *
     * @author cy
     * Copyright (c) CY
     */
    private static final Logger log = LoggerFactory.getLogger(WxApprovalQueryUtil.class);

    /**
     * 字段说明：企业微信企业微信service。
     *
     * @author cy
     * Copyright (c) CY
     */
    private final WxCpService wxCpService;
    /**
     * 字段说明：详情查询执行器。
     *
     * @author cy
     * Copyright (c) CY
     */
    private final Executor detailQueryExecutor;
    /**
     * 字段说明：配置选项。
     *
     * @author cy
     * Copyright (c) CY
     */
    private final WxApprovalQueryOptions options;

    /**
     * 创建 审批查询util工具实例。
     *
     * @param wxCpService 企业微信企业微信service
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxApprovalQueryUtil(WxCpService wxCpService) {
        this(wxCpService, Runnable::run, WxApprovalQueryOptions.defaults());
    }

    /**
     * 创建 审批查询util工具实例。
     *
     * @param wxCpService 企业微信企业微信service
     * @param detailQueryExecutor 详情查询执行器
     * @param options 配置选项
     *
     * @author cy
     * Copyright (c) CY
     */
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

    /**
     * 创建 审批查询util工具实例。
     *
     * @param wxCpService 企业微信企业微信service
     * @param detailQueryExecutor 详情查询执行器
     * @param options 配置选项
     * @param ignoredMeterRegistry ignoredmeterregistry
     *
     * @author cy
     * Copyright (c) CY
     */
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
     * 获取审批审批单号列表。
     *
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     * @return 审批审批单号列表
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public List<String> getApprovalSpNos(Date startTime, Date endTime) throws WxErrorException {
        return getApprovalSpNos(startTime, endTime, List.of());
    }

    /**
     * 获取审批审批单号列表。
     *
     * @param dateRange 日期范围
     * @return 审批审批单号列表
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public List<String> getApprovalSpNos(WxDateRange dateRange) throws WxErrorException {
        return getApprovalSpNos(dateRange.startTime(), dateRange.endTime());
    }

    /**
     * 获取审批审批单号列表by模板ID。
     *
     * @param templateId 审批模板 ID
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     * @return 审批审批单号列表by模板ID
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public List<String> getApprovalSpNosByTemplateId(String templateId, Date startTime, Date endTime) throws WxErrorException {
        return getApprovalSpNos(startTime, endTime, List.of(createTemplateFilter(templateId)));
    }

    /**
     * 获取审批审批单号列表by模板ID。
     *
     * @param templateId 审批模板 ID
     * @param dateRange 日期范围
     * @return 审批审批单号列表by模板ID
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public List<String> getApprovalSpNosByTemplateId(String templateId, WxDateRange dateRange) throws WxErrorException {
        return getApprovalSpNosByTemplateId(templateId, dateRange.startTime(), dateRange.endTime());
    }

    /**
     * 获取templates。
     *
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     * @return templates
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public List<WxApprovalTemplateVO> getTemplates(Date startTime, Date endTime) throws WxErrorException {
        return toTemplateList(queryApprovalDetails(startTime, endTime).details());
    }

    /**
     * 获取templates。
     *
     * @param dateRange 日期范围
     * @return templates
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public List<WxApprovalTemplateVO> getTemplates(WxDateRange dateRange) throws WxErrorException {
        return getTemplates(dateRange.startTime(), dateRange.endTime());
    }

    /**
     * 获取模板map。
     *
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     * @return 模板map
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public Map<String, WxApprovalTemplateVO> getTemplateMap(Date startTime, Date endTime) throws WxErrorException {
        return toTemplateMapByTemplates(getTemplates(startTime, endTime));
    }

    /**
     * 获取模板map。
     *
     * @param dateRange 日期范围
     * @return 模板map
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public Map<String, WxApprovalTemplateVO> getTemplateMap(WxDateRange dateRange) throws WxErrorException {
        return getTemplateMap(dateRange.startTime(), dateRange.endTime());
    }

    /**
     * 获取模板详情。
     *
     * @param templateId 审批模板 ID
     * @return 模板详情
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxCpOaApprovalTemplateResult getTemplateDetail(String templateId) throws WxErrorException {
        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException("templateId must not be blank");
        }
        return wxCpService.getOaService().getTemplateDetail(templateId);
    }

    /**
     * 获取模板ID列表by审批单号列表。
     *
     * @param spNos 审批单号集合
     * @return 模板ID列表by审批单号列表
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public Map<String, String> getTemplateIdsBySpNos(String... spNos) throws WxErrorException {
        return getTemplateIdsBySpNos(toSpNoList(spNos));
    }

    /**
     * 获取模板ID列表by审批单号列表。
     *
     * @param spNos 审批单号集合
     * @return 模板ID列表by审批单号列表
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
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
     * 获取模板详情列表by审批单号列表。
     *
     * @param spNos 审批单号集合
     * @return 模板详情列表by审批单号列表
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public Map<String, WxCpOaApprovalTemplateResult> getTemplateDetailsBySpNos(String... spNos) throws WxErrorException {
        return getTemplateDetailsBySpNos(toSpNoList(spNos));
    }

    /**
     * 获取模板详情列表by审批单号列表。
     *
     * @param spNos 审批单号集合
     * @return 模板详情列表by审批单号列表
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
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
     * 获取审批详情列表。
     *
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     * @return 审批详情列表
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public List<WxApprovalDetailVO> getApprovalDetails(Date startTime, Date endTime) throws WxErrorException {
        return queryApprovalDetails(startTime, endTime).details();
    }

    /**
     * 获取审批详情列表。
     *
     * @param dateRange 日期范围
     * @return 审批详情列表
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public List<WxApprovalDetailVO> getApprovalDetails(WxDateRange dateRange) throws WxErrorException {
        return getApprovalDetails(dateRange.startTime(), dateRange.endTime());
    }

    /**
     * 获取审批详情列表by模板ID。
     *
     * @param templateId 审批模板 ID
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     * @return 审批详情列表by模板ID
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public List<WxApprovalDetailVO> getApprovalDetailsByTemplateId(String templateId, Date startTime, Date endTime)
            throws WxErrorException {
        return queryApprovalDetailsByTemplateId(templateId, startTime, endTime).details();
    }

    /**
     * 获取审批详情列表考勤组by模板ID。
     *
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     * @return 审批详情列表考勤组by模板ID
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public Map<String, List<WxApprovalDetailVO>> getApprovalDetailsGroupByTemplateId(Date startTime, Date endTime)
            throws WxErrorException {
        return queryApprovalDetails(startTime, endTime).groupByTemplateId();
    }

    /**
     * 查询审批详情列表。
     *
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     * @return 审批详情查询结果
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxApprovalDetailQueryResult queryApprovalDetails(Date startTime, Date endTime) throws WxErrorException {
        return queryApprovalDetails(startTime, endTime, List.of());
    }

    /**
     * 查询审批详情列表。
     *
     * @param dateRange 日期范围
     * @return 审批详情查询结果
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxApprovalDetailQueryResult queryApprovalDetails(WxDateRange dateRange) throws WxErrorException {
        return queryApprovalDetails(dateRange.startTime(), dateRange.endTime());
    }

    /**
     * 查询审批详情列表by模板ID。
     *
     * @param templateId 审批模板 ID
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     * @return 审批详情查询结果
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxApprovalDetailQueryResult queryApprovalDetailsByTemplateId(String templateId, Date startTime, Date endTime)
            throws WxErrorException {
        return queryApprovalDetails(startTime, endTime, List.of(createTemplateFilter(templateId)));
    }

    /**
     * 查询审批详情列表by模板ID。
     *
     * @param templateId 审批模板 ID
     * @param dateRange 日期范围
     * @return 审批详情查询结果
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    public WxApprovalDetailQueryResult queryApprovalDetailsByTemplateId(String templateId, WxDateRange dateRange)
            throws WxErrorException {
        return queryApprovalDetailsByTemplateId(templateId, dateRange.startTime(), dateRange.endTime());
    }

    /**
     * 获取审批审批单号列表。
     *
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     * @param filters filters
     * @return 审批审批单号列表
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
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

    /**
     * 查询审批详情列表。
     *
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     * @param filters filters
     * @return 审批详情查询结果
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
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

    /**
     * 拉取审批详情with重试。
     *
     * @param spNo 审批单号
     * @param rateLimiter ratelimiter
     * @return 详情查询outcome
     *
     * @author cy
     * Copyright (c) CY
     */
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

    /**
     * 拉取审批详情。
     *
     * @param spNo 审批单号
     * @return 审批详情业务视图对象
     * @throws WxErrorException 企业微信 SDK 调用失败时抛出
     *
     * @author cy
     * Copyright (c) CY
     */
    private WxApprovalDetailVO fetchApprovalDetail(String spNo) throws WxErrorException {
        WxCpApprovalDetailResult detail = wxCpService.getOaService().getApprovalDetail(spNo);
        return WxApprovalConverter.from(detail);
    }

    /**
     * 创建模板filter。
     *
     * @param templateId 审批模板 ID
     * @return 企业微信审批信息查询filter
     *
     * @author cy
     * Copyright (c) CY
     */
    private WxCpApprovalInfoQueryFilter createTemplateFilter(String templateId) {
        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException("templateId must not be blank");
        }
        WxCpApprovalInfoQueryFilter filter = new WxCpApprovalInfoQueryFilter();
        filter.setKey(WxCpApprovalInfoQueryFilter.KEY.TEMPLATE_ID);
        filter.setValue(templateId);
        return filter;
    }

    /**
     * 执行 normalizeSpNos 相关逻辑。
     *
     * @param spNos 审批单号集合
     * @return 列表结果
     *
     * @author cy
     * Copyright (c) CY
     */
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

    /**
     * 转换为模板list。
     *
     * @param details 详情列表
     * @return 列表结果
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<WxApprovalTemplateVO> toTemplateList(List<WxApprovalDetailVO> details) {
        return new ArrayList<>(toTemplateMapByDetails(details).values());
    }

    /**
     * 转换为模板mapby详情列表。
     *
     * @param details 详情列表
     * @return 审批模板vo>
     *
     * @author cy
     * Copyright (c) CY
     */
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

    /**
     * 转换为模板mapbytemplates。
     *
     * @param templates templates
     * @return 审批模板vo>
     *
     * @author cy
     * Copyright (c) CY
     */
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

    /**
     * 转换为审批单号list。
     *
     * @param spNos 审批单号集合
     * @return 列表结果
     *
     * @author cy
     * Copyright (c) CY
     */
    private List<String> toSpNoList(String... spNos) {
        return spNos == null ? List.of() : List.of(spNos);
    }

    /**
     * 执行 sleepBackoff 相关逻辑。
     *
     * @param attempt attempt
     *
     * @author cy
     * Copyright (c) CY
     */
    private void sleepBackoff(int attempt) {
        long backoffMillis = calculateExponentialBackoff(attempt);
        if (backoffMillis > 0) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(backoffMillis));
        }
    }

    /**
     * 计算exponential退避。
     *
     * @param attempt attempt
     * @return long
     *
     * @author cy
     * Copyright (c) CY
     */
    private long calculateExponentialBackoff(int attempt) {
        long baseBackoff = Math.max(0L, options.retryBackoffMillis());
        if (baseBackoff == 0L) {
            return 0L;
        }
        long exponentialBackoff = baseBackoff * (1L << (attempt - 1));
        long maxBackoff = baseBackoff * 32;
        return Math.min(exponentialBackoff, maxBackoff);
    }

    /**
     * 执行 unwrap 相关逻辑。
     *
     * @param throwable throwable
     * @return throwable
     *
     * @author cy
     * Copyright (c) CY
     */
    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current != current.getCause()) {
            current = current.getCause();
        }
        return current;
    }

    /**
     * 校验时间范围。
     *
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     *
     * @author cy
     * Copyright (c) CY
     */
    private void validateTimeRange(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime and endTime must not be null");
        }
        if (!startTime.before(endTime)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
    }

    /**
     * 记录说明：详情查询outcome。
     *
     * @param detail 详情
     * @param failure failure
     *
     * @author cy
     * Copyright (c) CY
     */
    private record DetailQueryOutcome(
            WxApprovalDetailVO detail,
            WxApprovalDetailFetchFailure failure
    ) {
    }

    /**
     * 类说明：simpleratelimiter。
     *
     * @author cy
     * Copyright (c) CY
     */
    private static final class SimpleRateLimiter {
        /**
         * 字段说明：日志。
         *
         * @author cy
         * Copyright (c) CY
         */
        private static final Logger log = LoggerFactory.getLogger(SimpleRateLimiter.class);
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
         * 字段说明：总acquires。
         *
         * @author cy
         * Copyright (c) CY
         */
        private long totalAcquires = 0;
        /**
         * 字段说明：总wait时间nanos。
         *
         * @author cy
         * Copyright (c) CY
         */
        private long totalWaitTimeNanos = 0;

        /**
         * 创建 simpleratelimiter实例。
         *
         * @param requestsPerSecond 请求每秒
         *
         * @author cy
         * Copyright (c) CY
         */
        private SimpleRateLimiter(double requestsPerSecond) {
            if (requestsPerSecond <= 0D) {
                this.intervalNanos = 0L;
            } else {
                this.intervalNanos = (long) (1_000_000_000D / requestsPerSecond);
            }
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

    /**
     * 类说明：模板查询runtime异常。
     *
     * @author cy
     * Copyright (c) CY
     */
    private static final class TemplateQueryRuntimeException extends RuntimeException {
        /**
         * 字段说明：原始异常。
         *
         * @author cy
         * Copyright (c) CY
         */
        private final WxErrorException cause;

        /**
         * 创建 模板查询runtime异常实例。
         *
         * @param cause 原始异常
         *
         * @author cy
         * Copyright (c) CY
         */
        private TemplateQueryRuntimeException(WxErrorException cause) {
            super(cause);
            this.cause = cause;
        }
    }

}
