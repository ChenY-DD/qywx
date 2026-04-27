package org.cy.qywx.util;

public record WxApprovalQueryOptions(
        int segmentDays,
        int pageSize,
        int maxRetryAttempts,
        long retryBackoffMillis,
        double requestsPerSecond
) {
    public static WxApprovalQueryOptions defaults() {
        return new WxApprovalQueryOptions(29, 100, 5, 1000L, 10D);
    }
}
