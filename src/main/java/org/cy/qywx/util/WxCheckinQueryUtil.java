package org.cy.qywx.util;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinDayData;
import me.chanjar.weixin.cp.bean.oa.WxCpCropCheckinOption;
import org.cy.qywx.vo.WxCheckinDayDataVO;
import org.cy.qywx.vo.WxCheckinGroupVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * 企业微信打卡查询工具，覆盖考勤组、原始打卡记录、日报、月报、排班，
 * 并提供「迟到 / 早退 / 缺卡 / 旷工 / 地点异常 / 设备异常」业务语义查询及一站式
 * {@link org.cy.qywx.vo.WxAttendanceReportVO} 报表。
 *
 * <p>该工具复用主 {@link WxCpService}（与 {@code WxApprovalQueryUtil} 一致），
 * 通过 {@code wxCpService.getOaService()} 调用 WxJava 暴露的考勤接口；
 * 内部对企业微信「单次 ≤100 userId、≤30 天」硬限制做透明分批 / 分段。</p>
 *
 * @author CY
 * Copyright (c) CY
 */
public class WxCheckinQueryUtil {

    private static final Logger log = LoggerFactory.getLogger(WxCheckinQueryUtil.class);

    /** 打卡类型常量：上下班打卡。 */
    public static final int CHECKIN_TYPE_NORMAL = 1;
    /** 打卡类型常量：外出打卡。 */
    public static final int CHECKIN_TYPE_OUTSIDE = 2;
    /** 打卡类型常量：全部。 */
    public static final int CHECKIN_TYPE_ALL = 3;

    private final WxCpService wxCpService;
    private final Executor executor;
    private final WxCheckinQueryOptions options;

    /**
     * 构造方法。
     *
     * @param wxCpService 主 WxCpService
     * @param executor    异步执行器（由 Spring 注入 qywxCheckinExecutor）
     * @param options     查询参数
     */
    public WxCheckinQueryUtil(WxCpService wxCpService, Executor executor, WxCheckinQueryOptions options) {
        this.wxCpService = wxCpService;
        this.executor = executor == null ? Runnable::run : executor;
        this.options = options == null ? WxCheckinQueryOptions.defaults() : options;
        log.info("WxCheckinQueryUtil initialized: segmentDays={}, userBatchSize={}, "
                        + "maxRetryAttempts={}, retryBackoffMillis={}, requestsPerSecond={}",
                this.options.segmentDays(), this.options.userBatchSize(),
                this.options.maxRetryAttempts(), this.options.retryBackoffMillis(),
                this.options.requestsPerSecond());
    }

    // -------------------- getCheckinGroups --------------------

    /**
     * 拉取企业全部考勤组配置。
     *
     * @return 考勤组列表，可能为空
     * @throws WxErrorException 调用企业微信失败
     */
    public List<WxCheckinGroupVO> getCheckinGroups() throws WxErrorException {
        List<WxCpCropCheckinOption> source = wxCpService.getOaService().getCropCheckinOption();
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        List<WxCheckinGroupVO> out = new ArrayList<>(source.size());
        for (WxCpCropCheckinOption o : source) {
            WxCheckinGroupVO vo = WxCheckinConverter.fromGroup(o);
            if (vo != null) {
                out.add(vo);
            }
        }
        return out;
    }

    // -------------------- getCheckinDayData --------------------

    /**
     * 拉取指定 userId 在指定时间范围内的打卡日报。
     *
     * @param start   开始时间
     * @param end     结束时间
     * @param userIds 用户 ID 集合（内部自动去重 / 分批）
     * @return 日报查询结果（含成功 + 失败批次）
     */
    public WxCheckinDayDataResult getCheckinDayData(Date start, Date end, Collection<String> userIds) {
        List<String> normalized = normaliseUserIds(userIds);
        if (normalized.isEmpty()) {
            return WxCheckinDayDataResult.empty();
        }
        validateRange(start, end);

        long t0 = System.currentTimeMillis();
        List<WxDateRange> segments = segmentDates(start, end, options.segmentDays());
        List<List<String>> batches = partitionUsers(normalized, options.userBatchSize());
        List<FanoutTask<List<WxCpCheckinDayData>>> tasks = new ArrayList<>();
        for (WxDateRange seg : segments) {
            for (List<String> batch : batches) {
                tasks.add(new FanoutTask<>(seg, batch,
                        () -> wxCpService.getOaService().getCheckinDayData(seg.startTime(), seg.endTime(), batch)));
            }
        }

        SimpleRateLimiter limiter = new SimpleRateLimiter(options.requestsPerSecond());
        List<CompletableFuture<FanoutOutcome<List<WxCpCheckinDayData>>>> futures = new ArrayList<>(tasks.size());
        for (FanoutTask<List<WxCpCheckinDayData>> task : tasks) {
            futures.add(CompletableFuture.supplyAsync(() -> withRetry(task, limiter), executor));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();

        List<WxCheckinDayDataVO> success = new ArrayList<>();
        List<WxCheckinFetchFailure> failures = new ArrayList<>();
        for (CompletableFuture<FanoutOutcome<List<WxCpCheckinDayData>>> f : futures) {
            FanoutOutcome<List<WxCpCheckinDayData>> o = f.join();
            if (o.failure() != null) {
                failures.add(o.failure());
                continue;
            }
            if (o.success() != null) {
                for (WxCpCheckinDayData d : o.success()) {
                    WxCheckinDayDataVO vo = WxCheckinConverter.fromDayData(d);
                    if (vo != null) {
                        success.add(vo);
                    }
                }
            }
        }

        log.info("Checkin day data query: tasks={}, success={}, failures={}, durationMs={}",
                tasks.size(), success.size(), failures.size(), System.currentTimeMillis() - t0);
        return new WxCheckinDayDataResult(List.copyOf(success), List.copyOf(failures));
    }

    /**
     * {@link WxDateRange} 重载。
     *
     * @param range   时间范围
     * @param userIds 用户 ID 集合
     * @return 日报查询结果
     */
    public WxCheckinDayDataResult getCheckinDayData(WxDateRange range, Collection<String> userIds) {
        return getCheckinDayData(range.startTime(), range.endTime(), userIds);
    }

    // -------------------- fan-out 引擎 --------------------

    private static List<String> normaliseUserIds(Collection<String> in) {
        if (in == null || in.isEmpty()) {
            return List.of();
        }
        return in.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    private static void validateRange(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("start and end must not be null");
        }
        if (start.after(end)) {
            throw new IllegalArgumentException("start must not be after end");
        }
    }

    private static List<WxDateRange> segmentDates(Date start, Date end, int days) {
        int safeDays = Math.max(1, days);
        long stepMillis = safeDays * 86_400_000L;
        List<WxDateRange> out = new ArrayList<>();
        Date cursor = start;
        while (!cursor.after(end)) {
            Date segEnd = new Date(Math.min(cursor.getTime() + stepMillis, end.getTime()));
            out.add(new WxDateRange(cursor, segEnd));
            if (segEnd.equals(end)) {
                break;
            }
            cursor = new Date(segEnd.getTime() + 1000L);
        }
        return out;
    }

    private static List<List<String>> partitionUsers(List<String> userIds, int batchSize) {
        int safe = Math.max(1, batchSize);
        List<List<String>> out = new ArrayList<>();
        for (int i = 0; i < userIds.size(); i += safe) {
            out.add(List.copyOf(userIds.subList(i, Math.min(i + safe, userIds.size()))));
        }
        return out;
    }

    private <T> FanoutOutcome<T> withRetry(FanoutTask<T> task, SimpleRateLimiter limiter) {
        int max = Math.max(1, options.maxRetryAttempts());
        Throwable last = null;
        for (int attempt = 1; attempt <= max; attempt++) {
            try {
                limiter.acquire();
                T result = task.callable().call();
                return new FanoutOutcome<>(result, null);
            } catch (Throwable t) {
                last = unwrap(t);
                log.warn("Checkin batch failed: range=[{},{}], users={}, attempt={}/{}, error={}",
                        task.range().startTime(), task.range().endTime(), task.userIds().size(),
                        attempt, max, last.getMessage());
                if (attempt < max) {
                    long backoff = exponentialBackoff(attempt);
                    if (backoff > 0L) {
                        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(backoff));
                    }
                }
            }
        }
        String type = last == null ? "UNKNOWN" : last.getClass().getSimpleName();
        String msg = last == null ? "unknown error" : String.valueOf(last.getMessage());
        return new FanoutOutcome<>(null, new WxCheckinFetchFailure(
                task.range().startTime(), task.range().endTime(), task.userIds(), max, type, msg));
    }

    private long exponentialBackoff(int attempt) {
        long base = Math.max(0L, options.retryBackoffMillis());
        if (base == 0L) {
            return 0L;
        }
        long backoff = base * (1L << (attempt - 1));
        return Math.min(backoff, base * 32);
    }

    private static Throwable unwrap(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c != c.getCause()) {
            c = c.getCause();
        }
        return c;
    }

    private record FanoutTask<T>(WxDateRange range, List<String> userIds, Callable<T> callable) {
    }

    private record FanoutOutcome<T>(T success, WxCheckinFetchFailure failure) {
    }

    // -------------------- 内部工具 --------------------

    /**
     * 简易令牌桶限流器（与 WxApprovalQueryUtil / WxHrRosterQueryUtil 保持一致的内嵌实现）。
     *
     * @author CY
     * Copyright (c) CY
     */
    static final class SimpleRateLimiter {
        private final long intervalNanos;
        private long nextAllowedNanos;

        SimpleRateLimiter(double requestsPerSecond) {
            this.intervalNanos = requestsPerSecond <= 0D ? 0L : (long) (1_000_000_000D / requestsPerSecond);
            this.nextAllowedNanos = System.nanoTime();
        }

        synchronized void acquire() {
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
