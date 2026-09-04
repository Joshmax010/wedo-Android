package com.example.nutrition.domain.usecase

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * DateUtils 单元测试
 * 覆盖全部 11 个日期工具函数 + 边界场景（跨月、跨年、周一/周日边界）
 */
class DateUtilsTest {

    // ==================== formatDate ====================

    @Test
    fun `formatDate - 普通日期`() {
        val date = LocalDate.of(2026, 6, 30)
        assertEquals("2026-06-30", DateUtils.formatDate(date))
    }

    @Test
    fun `formatDate - 个位数月日补零`() {
        val date = LocalDate.of(2026, 1, 5)
        assertEquals("2026-01-05", DateUtils.formatDate(date))
    }

    @Test
    fun `formatDate - 跨年到 12 月 31 日`() {
        val date = LocalDate.of(2025, 12, 31)
        assertEquals("2025-12-31", DateUtils.formatDate(date))
    }

    // ==================== parseDate ====================

    @Test
    fun `parseDate - 正常解析`() {
        val date = DateUtils.parseDate("2026-03-15")
        assertEquals(LocalDate.of(2026, 3, 15), date)
    }

    @Test
    fun `parseDate - formatDate 往返一致`() {
        val original = LocalDate.of(2026, 11, 28)
        val str = DateUtils.formatDate(original)
        val parsed = DateUtils.parseDate(str)
        assertEquals(original, parsed)
    }

    // ==================== formatShortDate ====================

    @Test
    fun `formatShortDate - 普通日期`() {
        val date = LocalDate.of(2026, 6, 30)
        assertEquals("06.30", DateUtils.formatShortDate(date))
    }

    @Test
    fun `formatShortDate - 1 月 1 日`() {
        val date = LocalDate.of(2026, 1, 1)
        assertEquals("01.01", DateUtils.formatShortDate(date))
    }

    // ==================== isToday ====================

    @Test
    fun `isToday - 今天返回 true`() {
        val today = DateUtils.formatDate(LocalDate.now())
        assertTrue(DateUtils.isToday(today))
    }

    @Test
    fun `isToday - 昨天返回 false`() {
        val yesterday = DateUtils.formatDate(LocalDate.now().minusDays(1))
        assertFalse(DateUtils.isToday(yesterday))
    }

    @Test
    fun `isToday - 明天返回 false`() {
        val tomorrow = DateUtils.formatDate(LocalDate.now().plusDays(1))
        assertFalse(DateUtils.isToday(tomorrow))
    }

    // ==================== addDays ====================

    @Test
    fun `addDays - 正数向后`() {
        val result = DateUtils.addDays(LocalDate.of(2026, 6, 28), 3)
        assertEquals(LocalDate.of(2026, 7, 1), result)
    }

    @Test
    fun `addDays - 负数向前`() {
        val result = DateUtils.addDays(LocalDate.of(2026, 7, 2), -3)
        assertEquals(LocalDate.of(2026, 6, 29), result)
    }

    @Test
    fun `addDays - 跨月`() {
        val result = DateUtils.addDays(LocalDate.of(2026, 1, 30), 2)
        assertEquals(LocalDate.of(2026, 2, 1), result)
    }

    @Test
    fun `addDays - 跨年`() {
        val result = DateUtils.addDays(LocalDate.of(2025, 12, 31), 1)
        assertEquals(LocalDate.of(2026, 1, 1), result)
    }

    @Test
    fun `addDays - 字符串版本`() {
        assertEquals("2026-07-01", DateUtils.addDays("2026-06-28", 3))
    }

    @Test
    fun `addDays - 零天不变`() {
        val date = LocalDate.of(2026, 6, 15)
        assertEquals(date, DateUtils.addDays(date, 0))
    }

    // ==================== prevDay / nextDay ====================

    @Test
    fun `prevDay - 普通日期`() {
        assertEquals("2026-06-29", DateUtils.prevDay("2026-06-30"))
    }

    @Test
    fun `prevDay - 跨月 1 号到上月末`() {
        assertEquals("2026-05-31", DateUtils.prevDay("2026-06-01"))
    }

    @Test
    fun `prevDay - 跨年`() {
        assertEquals("2025-12-31", DateUtils.prevDay("2026-01-01"))
    }

    @Test
    fun `nextDay - 普通日期`() {
        assertEquals("2026-07-01", DateUtils.nextDay("2026-06-30"))
    }

    @Test
    fun `nextDay - 跨月`() {
        assertEquals("2026-02-01", DateUtils.nextDay("2026-01-31"))
    }

    @Test
    fun `nextDay - 跨年`() {
        assertEquals("2026-01-01", DateUtils.nextDay("2025-12-31"))
    }

    // ==================== getWeekRange ====================

    @Test
    fun `getWeekRange - 周一当天返回本周一到周日`() {
        // 2026-06-29 是周一
        val monday = LocalDate.of(2026, 6, 29)
        val (start, end) = DateUtils.getWeekRange(monday)
        assertEquals(monday, start)
        assertEquals(LocalDate.of(2026, 7, 5), end)
    }

    @Test
    fun `getWeekRange - 周三返回本周一到周日`() {
        // 2026-07-01 是周三
        val wed = LocalDate.of(2026, 7, 1)
        val (start, end) = DateUtils.getWeekRange(wed)
        assertEquals(LocalDate.of(2026, 6, 29), start)  // 周一
        assertEquals(LocalDate.of(2026, 7, 5), end)      // 周日
    }

    @Test
    fun `getWeekRange - 周日返回本周一到当天`() {
        // 2026-07-05 是周日
        val sunday = LocalDate.of(2026, 7, 5)
        val (start, end) = DateUtils.getWeekRange(sunday)
        assertEquals(LocalDate.of(2026, 6, 29), start)
        assertEquals(sunday, end)
    }

    @Test
    fun `getWeekRange - 跨月边界`() {
        // 2026-08-03 是周一，周日 08-09 跨到同一月内，测试 07-27 周一到 08-02 周日
        // 2026-07-29 是周三 → 本周一 07-27，周日 08-02
        val wed = LocalDate.of(2026, 7, 29)
        val (start, end) = DateUtils.getWeekRange(wed)
        assertEquals(LocalDate.of(2026, 7, 27), start)
        assertEquals(LocalDate.of(2026, 8, 2), end)
    }

    @Test
    fun `getWeekRange - 字符串版本`() {
        val (start, end) = DateUtils.getWeekRange("2026-07-01")
        assertEquals(LocalDate.of(2026, 6, 29), start)
        assertEquals(LocalDate.of(2026, 7, 5), end)
    }

    // ==================== formatWeekRange ====================

    @Test
    fun `formatWeekRange - 正常格式化`() {
        val date = LocalDate.of(2026, 7, 1)
        // 本周一 06-29，周日 07-05
        assertEquals("06.29 - 07.05", DateUtils.formatWeekRange(date))
    }

    @Test
    fun `formatWeekRange - 字符串版本`() {
        assertEquals("06.29 - 07.05", DateUtils.formatWeekRange("2026-07-01"))
    }

    // ==================== prevWeek / nextWeek ====================

    @Test
    fun `prevWeek - 上一周范围`() {
        val date = LocalDate.of(2026, 7, 1) // 本周: 06-29 ~ 07-05
        val (start, end) = DateUtils.prevWeek(date)
        assertEquals(LocalDate.of(2026, 6, 22), start)
        assertEquals(LocalDate.of(2026, 6, 28), end)
    }

    @Test
    fun `nextWeek - 下一周范围`() {
        val date = LocalDate.of(2026, 7, 1) // 本周: 06-29 ~ 07-05
        val (start, end) = DateUtils.nextWeek(date)
        assertEquals(LocalDate.of(2026, 7, 6), start)
        assertEquals(LocalDate.of(2026, 7, 12), end)
    }

    @Test
    fun `prevWeek - 跨月边界`() {
        // 2026-08-05 周三 → 本周 08-03~08-09，上周 07-27~08-02
        val date = LocalDate.of(2026, 8, 5)
        val (start, end) = DateUtils.prevWeek(date)
        assertEquals(LocalDate.of(2026, 7, 27), start)
        assertEquals(LocalDate.of(2026, 8, 2), end)
    }

    // ==================== getWeekDates ====================

    @Test
    fun `getWeekDates - 返回 7 天日期`() {
        val date = LocalDate.of(2026, 7, 1) // 周三
        val dates = DateUtils.getWeekDates(date)
        assertEquals(7, dates.size)
        assertEquals("2026-06-29", dates[0]) // 周一
        assertEquals("2026-06-30", dates[1])
        assertEquals("2026-07-01", dates[2])
        assertEquals("2026-07-02", dates[3])
        assertEquals("2026-07-03", dates[4])
        assertEquals("2026-07-04", dates[5])
        assertEquals("2026-07-05", dates[6]) // 周日
    }

    @Test
    fun `getWeekDates - 周一输入也正确`() {
        val monday = LocalDate.of(2026, 6, 29)
        val dates = DateUtils.getWeekDates(monday)
        assertEquals(7, dates.size)
        assertEquals("2026-06-29", dates[0])
        assertEquals("2026-07-05", dates[6])
    }

    @Test
    fun `getWeekDates - 字符串版本`() {
        val dates = DateUtils.getWeekDates("2026-07-01")
        assertEquals(7, dates.size)
        assertEquals("2026-06-29", dates[0])
    }

    @Test
    fun `getWeekDates - 跨月的一周`() {
        // 2026-07-29 周三 → 周一 07-27 ~ 周日 08-02
        val dates = DateUtils.getWeekDates("2026-07-29")
        assertEquals(7, dates.size)
        assertEquals("2026-07-27", dates[0])
        assertEquals("2026-07-31", dates[4])
        assertEquals("2026-08-01", dates[5])
        assertEquals("2026-08-02", dates[6])
    }

    // ==================== today ====================

    @Test
    fun `today - 返回今天日期字符串`() {
        val expected = DateUtils.formatDate(LocalDate.now())
        assertEquals(expected, DateUtils.today())
    }

    @Test
    fun `today - 格式正确 YYYY-MM-DD`() {
        val today = DateUtils.today()
        assertTrue(today.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    // ==================== 边界一致性 ====================

    @Test
    fun `prevDay 和 nextDay 互为逆运算`() {
        val date = "2026-06-30"
        assertEquals(date, DateUtils.nextDay(DateUtils.prevDay(date)))
        assertEquals(date, DateUtils.prevDay(DateUtils.nextDay(date)))
    }

    @Test
    fun `prevWeek 和 nextWeek 互为逆运算`() {
        val date = LocalDate.of(2026, 7, 1)
        val (prevStart, _) = DateUtils.prevWeek(date)
        val (_, nextEnd) = DateUtils.nextWeek(date)
        // prevWeek 的 start 比本周 start 早 7 天，nextWeek 的 end 比本周 end 晚 7 天
        val (thisStart, thisEnd) = DateUtils.getWeekRange(date)
        assertEquals(7, thisStart.toEpochDay() - prevStart.toEpochDay())
        assertEquals(7, nextEnd.toEpochDay() - thisEnd.toEpochDay())
    }

    @Test
    fun `getWeekDates 首尾与 getWeekRange 一致`() {
        val date = LocalDate.of(2026, 8, 5)
        val (start, end) = DateUtils.getWeekRange(date)
        val dates = DateUtils.getWeekDates(date)
        assertEquals(DateUtils.formatDate(start), dates.first())
        assertEquals(DateUtils.formatDate(end), dates.last())
    }
}
