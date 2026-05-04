package org.cy.qywx.util;

/**
 * 考勤查询参数（线程池由 Spring 注入）。
 *
 * @author CY
 * Copyright (c) CY
 *
 * @param segmentDays         单段最大天数（API 上限 30）
 * @param userBatchSize       单批最大 userId 数（API 上限 100）
 * @param maxRetryAttempts    单批次最大重试次数
 * @param retryBackoffMillis  重试初始退避毫秒
 * @param requestsPerSecond   全局限流每秒请求数（&lt;=0 表示不限流）
 */
public record WxCheckinQueryOptions(
        int segmentDays,
        int userBatchSize,
        int maxRetryAttempts,
        long retryBackoffMillis,
        double requestsPerSecond
) {
    /**
     * 构造默认参数：30 天 / 段、100 人 / 批、3 次重试、500ms 退避、不限流。
     *
     * @return 默认 options
     */
    public static WxCheckinQueryOptions defaults() {
        return new WxCheckinQueryOptions(30, 100, 3, 500L, 0D);
    }
}
