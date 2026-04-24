package org.cy.qywx.util;

public record WxApprovalQueryOptions(
        int segmentDays,
        int pageSize,
        int maxRetryAttempts,
        long retryBackoffMillis,
        double requestsPerSecond
) {
    public static WxApprovalQueryOptions defaults() {
        return new WxApprovalQueryOptions(29, 100, 3, 300L, 0D);
    }
}
