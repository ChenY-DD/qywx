package org.cy.qywx.util;

/**
 * 记录说明：考勤打卡查询配置选项。
 *
 * @param segmentDays 分段天数
 * @param userBatchSize 成员批次大小
 * @param maxRetryAttempts 最大重试次数
 * @param retryBackoffMillis 重试退避毫秒
 * @param requestsPerSecond 请求每秒
 *
 * @author cy
 * Copyright (c) CY
 */
public record WxCheckinQueryOptions(
        int segmentDays,
        int userBatchSize,
        int maxRetryAttempts,
        long retryBackoffMillis,
        double requestsPerSecond
) {
    /**
     * 创建默认查询配置。
     *
     * @return 考勤打卡查询配置选项
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxCheckinQueryOptions defaults() {
        return new WxCheckinQueryOptions(30, 100, 3, 500L, 0D);
    }
}
