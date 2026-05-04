package org.cy.qywx.util;

import java.util.Date;
import java.util.List;

/**
 * 单次「日期段 × 用户批」查询失败的记录。一个批次失败不影响其他批次合并。
 *
 * @author CY
 * Copyright (c) CY
 *
 * @param segmentStart 失败的日期段开始时间
 * @param segmentEnd   失败的日期段结束时间
 * @param userIdBatch  失败批次涉及的 userId
 * @param attempts     实际重试次数
 * @param errorType    异常类型简称
 * @param errorMessage 异常消息
 */
public record WxCheckinFetchFailure(
        Date segmentStart,
        Date segmentEnd,
        List<String> userIdBatch,
        int attempts,
        String errorType,
        String errorMessage
) {
}
