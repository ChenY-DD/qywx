package org.cy.qywx.util;

import java.util.Date;

/**
 * 日期时间范围对象，用于统一封装审批查询的开始和结束时间。
 */
public record WxDateRange(Date startTime, Date endTime) {

    /**
     * 创建日期范围，并校验开始时间必须早于结束时间。
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     */
    public WxDateRange {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime and endTime must not be null");
        }
        if (!startTime.before(endTime)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
    }
}
