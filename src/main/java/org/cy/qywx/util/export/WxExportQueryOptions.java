package org.cy.qywx.util.export;

/**
 * 记录说明：企业微信异步导出查询配置选项。
 *
 * @param pollIntervalMillis 轮询间隔毫秒（>0 才休眠）
 * @param pollTimeoutMillis 轮询超时毫秒
 * @param maxPollAttempts 最大轮询次数
 *
 * @author cy
 * Copyright (c) CY
 */
public record WxExportQueryOptions(
        long pollIntervalMillis,
        long pollTimeoutMillis,
        int maxPollAttempts
) {
    /**
     * 创建默认查询配置。
     *
     * @return 企业微信异步导出查询配置选项
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxExportQueryOptions defaults() {
        return new WxExportQueryOptions(2000L, 60000L, 30);
    }
}
