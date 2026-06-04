package org.cy.qywx.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WxDateRangeUtilsTest {

    @Test
    void shouldBuildCustomDateRange() {
        Date startTime = new Date(1_000);
        Date endTime = new Date(2_000);

        WxDateRange range = WxDateRangeUtils.custom(startTime, endTime);

        assertEquals(startTime, range.startTime());
        assertEquals(endTime, range.endTime());
    }

    @Test
    void shouldBuildDateRangeFromLocalDateTime() {
        LocalDateTime start = LocalDateTime.of(2026, 4, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 2, 12, 0);

        WxDateRange range = WxDateRangeUtils.custom(start, end);

        assertEquals(Date.from(start.atZone(ZoneId.systemDefault()).toInstant()), range.startTime());
        assertEquals(Date.from(end.atZone(ZoneId.systemDefault()).toInstant()), range.endTime());
    }

    @Test
    void shouldBuildDateRangeFromLocalDate() {
        WxDateRange range = WxDateRangeUtils.custom(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 3)
        );

        assertTrue(range.startTime().before(range.endTime()));
    }

    @Test
    void shouldBuildPresetRanges() {
        assertTrue(WxDateRangeUtils.today().startTime().before(WxDateRangeUtils.today().endTime()));
        assertTrue(WxDateRangeUtils.last3Days().startTime().before(WxDateRangeUtils.last3Days().endTime()));
        assertTrue(WxDateRangeUtils.currentMonth().startTime().before(WxDateRangeUtils.currentMonth().endTime()));
        assertTrue(WxDateRangeUtils.currentYear().startTime().before(WxDateRangeUtils.currentYear().endTime()));
    }

    @Test
    void lastDaysAlignsToWholeDayByDefault() {
        WxDateRange range = WxDateRangeUtils.lastDays(7);
        assertEquals(LocalTime.MIDNIGHT, toLocalDateTime(range.startTime()).toLocalTime());
        assertEquals(LocalTime.of(23, 59, 59), toLocalDateTime(range.endTime()).toLocalTime().withNano(0));
    }

    @Test
    void lastDaysCoversExactlyNCalendarDays() {
        WxDateRange range = WxDateRangeUtils.lastDays(7);
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        assertEquals(today.minusDays(6), toLocalDateTime(range.startTime()).toLocalDate());
        assertEquals(today, toLocalDateTime(range.endTime()).toLocalDate());
    }

    @Test
    void lastDaysRollingKeepsClockTimeOnBothEnds() {
        WxDateRange range = WxDateRangeUtils.lastDays(7, false);
        LocalTime start = toLocalDateTime(range.startTime()).toLocalTime();
        LocalTime end = toLocalDateTime(range.endTime()).toLocalTime();
        assertEquals(start, end);
    }

    @Test
    void lastDaysBeforeTodayExcludesTodayAndCoversNDays() {
        WxDateRange range = WxDateRangeUtils.lastDaysBeforeToday(7);
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        assertEquals(today.minusDays(7), toLocalDateTime(range.startTime()).toLocalDate());
        assertEquals(today.minusDays(1), toLocalDateTime(range.endTime()).toLocalDate());
        assertEquals(LocalTime.MIDNIGHT, toLocalDateTime(range.startTime()).toLocalTime());
        assertEquals(LocalTime.of(23, 59, 59), toLocalDateTime(range.endTime()).toLocalTime().withNano(0));
    }

    private static LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
