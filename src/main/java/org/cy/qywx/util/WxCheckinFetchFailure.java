package org.cy.qywx.util;

import java.util.Date;
import java.util.List;

/**
 * 记录说明：考勤打卡fetchfailure。
 *
 * @param segmentStart 分段开始
 * @param segmentEnd 分段结束
 * @param userIdBatch 成员ID批次
 * @param attempts 次数
 * @param errorType error类型
 * @param errorMessage errormessage
 *
 * @author cy
 * Copyright (c) CY
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
