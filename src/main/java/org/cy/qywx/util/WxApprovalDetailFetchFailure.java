package org.cy.qywx.util;

/**
 * 记录说明：审批详情fetchfailure。
 *
 * @param spNo 审批单号
 * @param attempts 次数
 * @param errorType error类型
 * @param errorMessage errormessage
 *
 * @author cy
 * Copyright (c) CY
 */
public record WxApprovalDetailFetchFailure(
        String spNo,
        int attempts,
        String errorType,
        String errorMessage
) {
}
