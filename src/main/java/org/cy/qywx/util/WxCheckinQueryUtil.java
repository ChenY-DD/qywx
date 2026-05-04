package org.cy.qywx.util;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinData;
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinDayData;
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinMonthData;
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinSchedule;
import me.chanjar.weixin.cp.bean.oa.WxCpCropCheckinOption;
import org.cy.qywx.vo.WxAttendanceReportVO;
import org.cy.qywx.vo.WxCheckinDayDataVO;
import org.cy.qywx.vo.WxCheckinExceptionItemVO;
import org.cy.qywx.vo.WxCheckinGroupVO;
import org.cy.qywx.vo.WxCheckinMonthDataVO;
import org.cy.qywx.vo.WxCheckinRecordVO;
import org.cy.qywx.vo.WxCheckinScheduleListItemVO;
import org.cy.qywx.vo.enums.WxCheckinExceptionTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * 企业微信打卡查询工具，覆盖考勤组、原始打卡记录、日报、月报、排班，
 * 并提供「迟到 / 早退 / 缺卡 / 旷工 / 地点异常 / 设备异常」业务语义查询及一站式
 * {@link WxAttendanceReportVO} 报表。
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

    // -------------------- getCheckinRecords --------------------

    /**
     * 拉取原始打卡记录。
     *
     * @param type    打卡类型（{@link #CHECKIN_TYPE_NORMAL} / {@link #CHECKIN_TYPE_OUTSIDE} / {@link #CHECKIN_TYPE_ALL}）
     * @param start   开始时间
     * @param end     结束时间
     * @param userIds 用户 ID 集合
     * @return 打卡记录查询结果
     */
    public WxCheckinRecordResult getCheckinRecords(int type, Date start, Date end, Collection<String> userIds) {
        List<String> normalized = normaliseUserIds(userIds);
        if (normalized.isEmpty()) {
            return WxCheckinRecordResult.empty();
        }
        validateRange(start, end);

        long t0 = System.currentTimeMillis();
        List<WxDateRange> segments = segmentDates(start, end, options.segmentDays());
        List<List<String>> batches = partitionUsers(normalized, options.userBatchSize());
        List<FanoutTask<List<WxCpCheckinData>>> tasks = new ArrayList<>();
        for (WxDateRange seg : segments) {
            for (List<String> batch : batches) {
                tasks.add(new FanoutTask<>(seg, batch,
                        () -> wxCpService.getOaService()
                                .getCheckinData(type, seg.startTime(), seg.endTime(), batch)));
            }
        }
        SimpleRateLimiter limiter = new SimpleRateLimiter(options.requestsPerSecond());
        List<CompletableFuture<FanoutOutcome<List<WxCpCheckinData>>>> futures = new ArrayList<>(tasks.size());
        for (FanoutTask<List<WxCpCheckinData>> task : tasks) {
            futures.add(CompletableFuture.supplyAsync(() -> withRetry(task, limiter), executor));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();

        List<WxCheckinRecordVO> success = new ArrayList<>();
        List<WxCheckinFetchFailure> failures = new ArrayList<>();
        for (CompletableFuture<FanoutOutcome<List<WxCpCheckinData>>> f : futures) {
            FanoutOutcome<List<WxCpCheckinData>> o = f.join();
            if (o.failure() != null) {
                failures.add(o.failure());
                continue;
            }
            if (o.success() != null) {
                for (WxCpCheckinData d : o.success()) {
                    WxCheckinRecordVO vo = WxCheckinConverter.fromRecord(d);
                    if (vo != null) {
                        success.add(vo);
                    }
                }
            }
        }
        log.info("Checkin records query: type={}, tasks={}, success={}, failures={}, durationMs={}",
                type, tasks.size(), success.size(), failures.size(), System.currentTimeMillis() - t0);
        return new WxCheckinRecordResult(List.copyOf(success), List.copyOf(failures));
    }

    /**
     * {@link #CHECKIN_TYPE_ALL} 默认重载。
     *
     * @param start   开始时间
     * @param end     结束时间
     * @param userIds 用户 ID 集合
     * @return 打卡记录查询结果
     */
    public WxCheckinRecordResult getCheckinRecords(Date start, Date end, Collection<String> userIds) {
        return getCheckinRecords(CHECKIN_TYPE_ALL, start, end, userIds);
    }

    /**
     * {@link WxDateRange} 重载，type=ALL。
     *
     * @param range   时间范围
     * @param userIds 用户 ID 集合
     * @return 打卡记录查询结果
     */
    public WxCheckinRecordResult getCheckinRecords(WxDateRange range, Collection<String> userIds) {
        return getCheckinRecords(CHECKIN_TYPE_ALL, range.startTime(), range.endTime(), userIds);
    }

    /**
     * {@link WxDateRange} + 指定 type 重载。
     *
     * @param type    打卡类型
     * @param range   时间范围
     * @param userIds 用户 ID 集合
     * @return 打卡记录查询结果
     */
    public WxCheckinRecordResult getCheckinRecords(int type, WxDateRange range, Collection<String> userIds) {
        return getCheckinRecords(type, range.startTime(), range.endTime(), userIds);
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
                        () -> wxCpService.getOaService()
                                .getCheckinDayData(seg.startTime(), seg.endTime(), batch)));
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

    // -------------------- getCheckinMonthData --------------------

    /**
     * 拉取打卡月报。
     *
     * @param start   开始时间
     * @param end     结束时间
     * @param userIds 用户 ID 集合
     * @return 月报查询结果
     */
    public WxCheckinMonthDataResult getCheckinMonthData(Date start, Date end, Collection<String> userIds) {
        List<String> normalized = normaliseUserIds(userIds);
        if (normalized.isEmpty()) {
            return WxCheckinMonthDataResult.empty();
        }
        validateRange(start, end);

        long t0 = System.currentTimeMillis();
        List<WxDateRange> segments = segmentDates(start, end, options.segmentDays());
        List<List<String>> batches = partitionUsers(normalized, options.userBatchSize());
        List<FanoutTask<List<WxCpCheckinMonthData>>> tasks = new ArrayList<>();
        for (WxDateRange seg : segments) {
            for (List<String> batch : batches) {
                tasks.add(new FanoutTask<>(seg, batch,
                        () -> wxCpService.getOaService()
                                .getCheckinMonthData(seg.startTime(), seg.endTime(), batch)));
            }
        }
        SimpleRateLimiter limiter = new SimpleRateLimiter(options.requestsPerSecond());
        List<CompletableFuture<FanoutOutcome<List<WxCpCheckinMonthData>>>> futures = new ArrayList<>(tasks.size());
        for (FanoutTask<List<WxCpCheckinMonthData>> task : tasks) {
            futures.add(CompletableFuture.supplyAsync(() -> withRetry(task, limiter), executor));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();

        List<WxCheckinMonthDataVO> success = new ArrayList<>();
        List<WxCheckinFetchFailure> failures = new ArrayList<>();
        for (CompletableFuture<FanoutOutcome<List<WxCpCheckinMonthData>>> f : futures) {
            FanoutOutcome<List<WxCpCheckinMonthData>> o = f.join();
            if (o.failure() != null) {
                failures.add(o.failure());
                continue;
            }
            if (o.success() != null) {
                for (WxCpCheckinMonthData d : o.success()) {
                    WxCheckinMonthDataVO vo = WxCheckinConverter.fromMonthData(d);
                    if (vo != null) {
                        success.add(vo);
                    }
                }
            }
        }
        log.info("Checkin month data query: tasks={}, success={}, failures={}, durationMs={}",
                tasks.size(), success.size(), failures.size(), System.currentTimeMillis() - t0);
        return new WxCheckinMonthDataResult(List.copyOf(success), List.copyOf(failures));
    }

    /**
     * {@link WxDateRange} 重载。
     *
     * @param range   时间范围
     * @param userIds 用户 ID 集合
     * @return 月报查询结果
     */
    public WxCheckinMonthDataResult getCheckinMonthData(WxDateRange range, Collection<String> userIds) {
        return getCheckinMonthData(range.startTime(), range.endTime(), userIds);
    }

    // -------------------- getScheduleList --------------------

    /**
     * 拉取排班信息。失败的批次仅 WARN 级别日志，不收集到返回值。
     *
     * @param start   开始时间
     * @param end     结束时间
     * @param userIds 用户 ID 集合
     * @return 排班列表
     */
    public List<WxCheckinScheduleListItemVO> getScheduleList(Date start, Date end, Collection<String> userIds) {
        List<String> normalized = normaliseUserIds(userIds);
        if (normalized.isEmpty()) {
            return List.of();
        }
        validateRange(start, end);

        List<WxDateRange> segments = segmentDates(start, end, options.segmentDays());
        List<List<String>> batches = partitionUsers(normalized, options.userBatchSize());
        List<FanoutTask<List<WxCpCheckinSchedule>>> tasks = new ArrayList<>();
        for (WxDateRange seg : segments) {
            for (List<String> batch : batches) {
                tasks.add(new FanoutTask<>(seg, batch,
                        () -> wxCpService.getOaService()
                                .getCheckinScheduleList(seg.startTime(), seg.endTime(), batch)));
            }
        }
        SimpleRateLimiter limiter = new SimpleRateLimiter(options.requestsPerSecond());
        List<CompletableFuture<FanoutOutcome<List<WxCpCheckinSchedule>>>> futures = new ArrayList<>(tasks.size());
        for (FanoutTask<List<WxCpCheckinSchedule>> task : tasks) {
            futures.add(CompletableFuture.supplyAsync(() -> withRetry(task, limiter), executor));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();

        List<WxCheckinScheduleListItemVO> out = new ArrayList<>();
        for (CompletableFuture<FanoutOutcome<List<WxCpCheckinSchedule>>> f : futures) {
            FanoutOutcome<List<WxCpCheckinSchedule>> o = f.join();
            if (o.failure() != null) {
                log.warn("Schedule list batch failed: range=[{},{}], users={}, error={}",
                        o.failure().segmentStart(), o.failure().segmentEnd(),
                        o.failure().userIdBatch().size(), o.failure().errorMessage());
                continue;
            }
            if (o.success() != null) {
                for (WxCpCheckinSchedule s : o.success()) {
                    WxCheckinScheduleListItemVO vo = WxCheckinConverter.fromScheduleItem(s);
                    if (vo != null) {
                        out.add(vo);
                    }
                }
            }
        }
        return List.copyOf(out);
    }

    /**
     * {@link WxDateRange} 重载。
     *
     * @param range   时间范围
     * @param userIds 用户 ID 集合
     * @return 排班列表
     */
    public List<WxCheckinScheduleListItemVO> getScheduleList(WxDateRange range, Collection<String> userIds) {
        return getScheduleList(range.startTime(), range.endTime(), userIds);
    }

    // -------------------- 业务语义方法 --------------------

    /**
     * 时段内迟到记录（按 {@code (userId, date)} 粒度展开）。
     *
     * @param start   开始时间
     * @param end     结束时间
     * @param userIds 用户 ID 集合
     * @return 迟到记录列表
     */
    public List<WxCheckinExceptionItemVO> getLatePersons(Date start, Date end, Collection<String> userIds) {
        return collectExceptions(start, end, userIds, WxCheckinExceptionTypeEnum.LATE);
    }

    /**
     * {@link WxDateRange} 重载。
     *
     * @param range   时间范围
     * @param userIds 用户 ID 集合
     * @return 迟到记录列表
     */
    public List<WxCheckinExceptionItemVO> getLatePersons(WxDateRange range, Collection<String> userIds) {
        return getLatePersons(range.startTime(), range.endTime(), userIds);
    }

    /**
     * 时段内早退记录。
     *
     * @param start   开始时间
     * @param end     结束时间
     * @param userIds 用户 ID 集合
     * @return 早退记录列表
     */
    public List<WxCheckinExceptionItemVO> getEarlyLeavePersons(Date start, Date end, Collection<String> userIds) {
        return collectExceptions(start, end, userIds, WxCheckinExceptionTypeEnum.EARLY_LEAVE);
    }

    /**
     * {@link WxDateRange} 重载。
     *
     * @param range   时间范围
     * @param userIds 用户 ID 集合
     * @return 早退记录列表
     */
    public List<WxCheckinExceptionItemVO> getEarlyLeavePersons(WxDateRange range, Collection<String> userIds) {
        return getEarlyLeavePersons(range.startTime(), range.endTime(), userIds);
    }

    /**
     * 时段内缺卡记录。
     *
     * @param start   开始时间
     * @param end     结束时间
     * @param userIds 用户 ID 集合
     * @return 缺卡记录列表
     */
    public List<WxCheckinExceptionItemVO> getMissingCardPersons(Date start, Date end, Collection<String> userIds) {
        return collectExceptions(start, end, userIds, WxCheckinExceptionTypeEnum.MISSING_CARD);
    }

    /**
     * {@link WxDateRange} 重载。
     *
     * @param range   时间范围
     * @param userIds 用户 ID 集合
     * @return 缺卡记录列表
     */
    public List<WxCheckinExceptionItemVO> getMissingCardPersons(WxDateRange range, Collection<String> userIds) {
        return getMissingCardPersons(range.startTime(), range.endTime(), userIds);
    }

    /**
     * 时段内旷工记录。
     *
     * @param start   开始时间
     * @param end     结束时间
     * @param userIds 用户 ID 集合
     * @return 旷工记录列表
     */
    public List<WxCheckinExceptionItemVO> getAbsentPersons(Date start, Date end, Collection<String> userIds) {
        return collectExceptions(start, end, userIds, WxCheckinExceptionTypeEnum.ABSENT);
    }

    /**
     * {@link WxDateRange} 重载。
     *
     * @param range   时间范围
     * @param userIds 用户 ID 集合
     * @return 旷工记录列表
     */
    public List<WxCheckinExceptionItemVO> getAbsentPersons(WxDateRange range, Collection<String> userIds) {
        return getAbsentPersons(range.startTime(), range.endTime(), userIds);
    }

    /**
     * 时段内地点异常记录。
     *
     * @param start   开始时间
     * @param end     结束时间
     * @param userIds 用户 ID 集合
     * @return 地点异常记录列表
     */
    public List<WxCheckinExceptionItemVO> getLocationExceptions(Date start, Date end, Collection<String> userIds) {
        return collectExceptions(start, end, userIds, WxCheckinExceptionTypeEnum.LOCATION_EXCEPTION);
    }

    /**
     * {@link WxDateRange} 重载。
     *
     * @param range   时间范围
     * @param userIds 用户 ID 集合
     * @return 地点异常记录列表
     */
    public List<WxCheckinExceptionItemVO> getLocationExceptions(WxDateRange range, Collection<String> userIds) {
        return getLocationExceptions(range.startTime(), range.endTime(), userIds);
    }

    /**
     * 时段内设备异常记录。
     *
     * @param start   开始时间
     * @param end     结束时间
     * @param userIds 用户 ID 集合
     * @return 设备异常记录列表
     */
    public List<WxCheckinExceptionItemVO> getDeviceExceptions(Date start, Date end, Collection<String> userIds) {
        return collectExceptions(start, end, userIds, WxCheckinExceptionTypeEnum.DEVICE_EXCEPTION);
    }

    /**
     * {@link WxDateRange} 重载。
     *
     * @param range   时间范围
     * @param userIds 用户 ID 集合
     * @return 设备异常记录列表
     */
    public List<WxCheckinExceptionItemVO> getDeviceExceptions(WxDateRange range, Collection<String> userIds) {
        return getDeviceExceptions(range.startTime(), range.endTime(), userIds);
    }

    private List<WxCheckinExceptionItemVO> collectExceptions(Date start, Date end,
                                                             Collection<String> userIds,
                                                             WxCheckinExceptionTypeEnum filter) {
        WxCheckinDayDataResult dayResult = getCheckinDayData(start, end, userIds);
        List<WxCheckinExceptionItemVO> out = new ArrayList<>();
        for (WxCheckinDayDataVO day : dayResult.dayDataList()) {
            out.addAll(WxCheckinConverter.explodeExceptions(day, filter));
        }
        return List.copyOf(out);
    }

    // -------------------- getAttendanceReport --------------------

    /**
     * 一次性聚合考勤报表。底层只调一次 {@code getcheckin_daydata}，按异常类型分桶。
     *
     * @param start   开始时间
     * @param end     结束时间
     * @param userIds 用户 ID 集合
     * @return 聚合报表
     */
    public WxAttendanceReportVO getAttendanceReport(Date start, Date end, Collection<String> userIds) {
        List<String> normalized = normaliseUserIds(userIds);
        WxAttendanceReportVO report = new WxAttendanceReportVO();
        report.setRange(new WxDateRange(start, end));
        report.setTotalUsers(normalized.size());

        if (normalized.isEmpty()) {
            report.setLate(List.of());
            report.setEarlyLeave(List.of());
            report.setMissingCard(List.of());
            report.setAbsent(List.of());
            report.setLocationException(List.of());
            report.setDeviceException(List.of());
            report.setFailures(List.of());
            report.setReportedUsers(0);
            return report;
        }

        WxCheckinDayDataResult dayResult = getCheckinDayData(start, end, normalized);

        List<WxCheckinExceptionItemVO> late = new ArrayList<>();
        List<WxCheckinExceptionItemVO> earlyLeave = new ArrayList<>();
        List<WxCheckinExceptionItemVO> missingCard = new ArrayList<>();
        List<WxCheckinExceptionItemVO> absent = new ArrayList<>();
        List<WxCheckinExceptionItemVO> locationEx = new ArrayList<>();
        List<WxCheckinExceptionItemVO> deviceEx = new ArrayList<>();
        Set<String> reportedUsers = new HashSet<>();

        for (WxCheckinDayDataVO day : dayResult.dayDataList()) {
            if (day.getUserId() != null) {
                reportedUsers.add(day.getUserId());
            }
            for (WxCheckinExceptionItemVO item : WxCheckinConverter.explodeExceptions(day, null)) {
                switch (item.getExceptionType()) {
                    case LATE -> late.add(item);
                    case EARLY_LEAVE -> earlyLeave.add(item);
                    case MISSING_CARD -> missingCard.add(item);
                    case ABSENT -> absent.add(item);
                    case LOCATION_EXCEPTION -> locationEx.add(item);
                    case DEVICE_EXCEPTION -> deviceEx.add(item);
                }
            }
        }

        report.setLate(List.copyOf(late));
        report.setEarlyLeave(List.copyOf(earlyLeave));
        report.setMissingCard(List.copyOf(missingCard));
        report.setAbsent(List.copyOf(absent));
        report.setLocationException(List.copyOf(locationEx));
        report.setDeviceException(List.copyOf(deviceEx));
        report.setFailures(dayResult.failures());
        report.setReportedUsers(reportedUsers.size());
        return report;
    }

    /**
     * {@link WxDateRange} 重载。
     *
     * @param range   时间范围
     * @param userIds 用户 ID 集合
     * @return 聚合报表
     */
    public WxAttendanceReportVO getAttendanceReport(WxDateRange range, Collection<String> userIds) {
        return getAttendanceReport(range.startTime(), range.endTime(), userIds);
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
