package org.cy.qywx.util;

/**
 * 花名册查询失败记录。
 *
 * @param userId 失败的成员 userId
 * @param attempts 已重试次数
 * @param errorType 错误类型（异常类名）
 * @param errorMessage 错误信息
 */
public record WxHrRosterFetchFailure(
        String userId,
        int attempts,
        String errorType,
        String errorMessage
) {
}
