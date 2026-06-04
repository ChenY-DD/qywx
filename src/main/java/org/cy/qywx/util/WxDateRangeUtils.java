package org.cy.qywx.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 类说明：日期范围utils。
 *
 * @author cy
 * Copyright (c) CY
 */
public final class WxDateRangeUtils {

    /**
     * 字段说明：默认时区。
     *
     * @author cy
     * Copyright (c) CY
     */
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    /**
     * 创建 日期范围utils实例。
     *
     * @author cy
     * Copyright (c) CY
     */
    private WxDateRangeUtils() {
    }

    /**
     * 转换为天。
     *
     * @return 日期范围
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxDateRange today() {
        LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);
        return custom(now.toLocalDate().atStartOfDay(), now);
    }

    /**
     * 最近 3 天（默认对齐到自然日，等价于 {@link #lastDays(int)} 传入 3）。
     *
     * @return 日期范围
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxDateRange last3Days() {
        return lastDays(3);
    }

    /**
     * 最近 N 个自然日（含今天共 days 天；默认对齐到自然日：开始 00:00:00、结束当天 23:59:59.999）。
     *
     * @param days 向前追溯的天数（必须大于 0）
     * @return 日期范围
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxDateRange lastDays(int days) {
        return lastDays(days, true);
    }

    /**
     * 最近 N 天。
     *
     * @param days            向前追溯的天数（必须大于 0）
     * @param alignToWholeDay true：取最近 days 个完整自然日（开始 [今天-(days-1)] 00:00:00、结束今天 23:59:59.999）；
     *                        false：精确到当前时分秒的滚动窗口 [now - days 天, now]
     * @return 日期范围
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxDateRange lastDays(int days, boolean alignToWholeDay) {
        if (days <= 0) {
            throw new IllegalArgumentException("days must be greater than 0");
        }
        LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);
        if (alignToWholeDay) {
            return custom(now.minusDays(days - 1).toLocalDate(), now.toLocalDate());
        }
        return custom(now.minusDays(days), now);
    }

    /**
     * 最近 N 个自然日，但不含今天（截止到昨天：开始 [今天-days] 00:00:00、结束昨天 23:59:59.999，共 days 天）。
     *
     * @param days 向前追溯的天数（必须大于 0）
     * @return 日期范围
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxDateRange lastDaysBeforeToday(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("days must be greater than 0");
        }
        LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);
        return custom(now.minusDays(days).toLocalDate(), now.minusDays(1).toLocalDate());
    }

    /**
     * 执行 currentMonth 相关逻辑。
     *
     * @return 日期范围
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxDateRange currentMonth() {
        LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);
        LocalDate firstDay = now.toLocalDate().withDayOfMonth(1);
        return custom(firstDay.atStartOfDay(), now);
    }

    /**
     * 执行 currentYear 相关逻辑。
     *
     * @return 日期范围
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxDateRange currentYear() {
        LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);
        LocalDate firstDay = now.toLocalDate().withDayOfYear(1);
        return custom(firstDay.atStartOfDay(), now);
    }

    /**
     * 执行 custom 相关逻辑。
     *
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     * @return 日期范围
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxDateRange custom(Date startTime, Date endTime) {
        return new WxDateRange(startTime, endTime);
    }

    /**
     * 执行 custom 相关逻辑。
     *
     * @param startTime 查询开始时间
     * @param endTime 查询结束时间
     * @return 日期范围
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxDateRange custom(LocalDateTime startTime, LocalDateTime endTime) {
        return custom(toDate(startTime), toDate(endTime));
    }

    /**
     * 执行 custom 相关逻辑。
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 日期范围
     *
     * @author cy
     * Copyright (c) CY
     */
    public static WxDateRange custom(LocalDate startDate, LocalDate endDate) {
        return custom(
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX)
        );
    }

    /**
     * 转换为日期。
     *
     * @param dateTime 日期时间
     * @return 日期
     *
     * @author cy
     * Copyright (c) CY
     */
    private static Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(DEFAULT_ZONE).toInstant());
    }
}
