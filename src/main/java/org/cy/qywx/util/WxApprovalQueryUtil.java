package org.cy.qywx.util;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalDetailResult;
import me.chanjar.weixin.cp.bean.oa.WxCpApprovalInfo;
import org.cy.qywx.vo.WxApprovalDetailVO;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

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

    public List<String> getApprovalSpNos(Date startTime, Date endTime) throws WxErrorException {
        validateTimeRange(startTime, endTime);

        Set<String> spNos = new LinkedHashSet<>();
        Date segmentStartTime = startTime;
        long segmentMillis = Math.max(1, options.segmentDays()) * 24L * 60 * 60 * 1000;

        while (!segmentStartTime.after(endTime)) {
            Date segmentEndTime = new Date(Math.min(
                    segmentStartTime.getTime() + segmentMillis,
                    endTime.getTime()
            ));

            Integer cursor = 0;
            while (true) {
                WxCpApprovalInfo approvalInfo = wxCpService.getOaService()
                        .getApprovalInfo(segmentStartTime, segmentEndTime, cursor, Math.max(1, options.pageSize()), null);

                if (approvalInfo.getSpNoList() != null && !approvalInfo.getSpNoList().isEmpty()) {
                    spNos.addAll(approvalInfo.getSpNoList());
                }

                Integer nextCursor = approvalInfo.getNextCursor();
                if (nextCursor == null || nextCursor.equals(cursor)) {
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

    public List<WxApprovalDetailVO> getApprovalDetails(Date startTime, Date endTime) throws WxErrorException {
        return queryApprovalDetails(startTime, endTime).details();
    }

    public Map<String, List<WxApprovalDetailVO>> getApprovalDetailsGroupByTemplateId(Date startTime, Date endTime)
            throws WxErrorException {
        List<WxApprovalDetailVO> details = getApprovalDetails(startTime, endTime);
        return details.stream()
                .collect(Collectors.groupingBy(
                        detail -> detail.getTemplateId() == null ? "UNKNOWN_TEMPLATE" : detail.getTemplateId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    public WxApprovalDetailQueryResult queryApprovalDetails(Date startTime, Date endTime) throws WxErrorException {
        List<String> spNos = getApprovalSpNos(startTime, endTime);
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
                WxCpApprovalDetailResult detail = wxCpService.getOaService().getApprovalDetail(spNo);
                return new DetailQueryOutcome(WxApprovalConverter.from(detail), null);
            } catch (Throwable throwable) {
                lastError = unwrap(throwable);
                if (attempt < maxAttempts) {
                    sleepBackoff(attempt);
                }
            }
        }

        String errorType = lastError == null ? "UNKNOWN" : lastError.getClass().getSimpleName();
        String errorMessage = lastError == null ? "unknown error" : String.valueOf(lastError.getMessage());
        WxApprovalDetailFetchFailure failure = new WxApprovalDetailFetchFailure(spNo, maxAttempts, errorType, errorMessage);
        return new DetailQueryOutcome(null, failure);
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
}
