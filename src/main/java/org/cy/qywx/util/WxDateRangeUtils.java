package org.cy.qywx.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 日期范围工具类，提供当天、近三天、当月、当年以及自定义时间范围的快速构造。
 */
public final class WxDateRangeUtils {

    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    private WxDateRangeUtils() {
    }

    /**
     * 获取“当天 00:00:00 到当前时刻”的时间范围。
     *
     * @return 当天时间范围
     */
    public static WxDateRange today() {
        LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);
        return custom(now.toLocalDate().atStartOfDay(), now);
    }

    /**
     * 获取“当前时刻向前 3 天”的时间范围。
     *
     * @return 近三天时间范围
     */
    public static WxDateRange last3Days() {
        return lastDays(3);
    }

    /**
     * 获取“当前时刻向前 N 天”的时间范围。
     *
     * @param days 天数
     * @return 近 N 天时间范围
     */
    public static WxDateRange lastDays(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("days must be greater than 0");
        }
        LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);
        return custom(now.minusDays(days), now);
    }

    /**
     * 获取“本月 1 号 00:00:00 到当前时刻”的时间范围。
     *
     * @return 当月时间范围
     */
    public static WxDateRange currentMonth() {
        LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);
        LocalDate firstDay = now.toLocalDate().withDayOfMonth(1);
        return custom(firstDay.atStartOfDay(), now);
    }

    /**
     * 获取“今年 1 月 1 日 00:00:00 到当前时刻”的时间范围。
     *
     * @return 当年时间范围
     */
    public static WxDateRange currentYear() {
        LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);
        LocalDate firstDay = now.toLocalDate().withDayOfYear(1);
        return custom(firstDay.atStartOfDay(), now);
    }

    /**
     * 使用 {@link Date} 自定义时间范围。
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 自定义时间范围
     */
    public static WxDateRange custom(Date startTime, Date endTime) {
        return new WxDateRange(startTime, endTime);
    }

    /**
     * 使用 {@link LocalDateTime} 自定义时间范围。
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 自定义时间范围
     */
    public static WxDateRange custom(LocalDateTime startTime, LocalDateTime endTime) {
        return custom(toDate(startTime), toDate(endTime));
    }

    /**
     * 使用 {@link LocalDate} 自定义时间范围，结束时间自动扩展到结束日期当天的最后时刻。
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 自定义时间范围
     */
    public static WxDateRange custom(LocalDate startDate, LocalDate endDate) {
        return custom(
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX)
        );
    }

    private static Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(DEFAULT_ZONE).toInstant());
    }
}
