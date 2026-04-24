package org.cy.qywx.util;

public record WxApprovalDetailFetchFailure(
        String spNo,
        int attempts,
        String errorType,
        String errorMessage
) {
}
