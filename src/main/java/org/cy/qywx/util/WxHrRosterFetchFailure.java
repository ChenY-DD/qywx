package org.cy.qywx.util;

/**
 * 记录说明：HR花名册fetchfailure。
 *
 * @param userId 成员 userId
 * @param attempts 次数
 * @param errorType error类型
 * @param errorMessage errormessage
 *
 * @author cy
 * Copyright (c) CY
 */
public record WxHrRosterFetchFailure(
        String userId,
        int attempts,
        String errorType,
        String errorMessage
) {
}
