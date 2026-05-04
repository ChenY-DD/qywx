package org.cy.qywx.util;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.oa.WxCpCropCheckinOption;
import org.cy.qywx.vo.WxCheckinGroupVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
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
