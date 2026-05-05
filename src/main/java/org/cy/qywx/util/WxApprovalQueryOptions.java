package org.cy.qywx.util;

/**
 * 记录说明：审批查询配置选项。
 *
 * @param segmentDays 分段天数
 * @param pageSize 分页大小
 * @param maxRetryAttempts 最大重试次数
 * @param retryBackoffMillis 重试退避毫秒
 * @param requestsPerSecond 请求每秒
 *
 * @author cy
 * Copyright (c) CY
 */
public record WxApprovalQueryOptions(
        int segmentDays,
        int pageSize,
        int maxRetryAttempts,
        long retryBackoffMillis,
        double requestsPerSecond
) {
    /**
     * 创建默认查询配置。
     *
     * @return 审批查询配置选项
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxApprovalQueryOptions defaults() {
        return new WxApprovalQueryOptions(29, 100, 5, 1000L, 10D);
    }
}
