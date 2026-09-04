package com.example.nutrition.domain.usecase

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * 日期处理工具 —— 移植自小程序 utils/date.js
 *
 * 所有函数均为纯函数，使用 java.time.LocalDate
 * 周一为一周起点（与小程序一致）
 */
object DateUtils {

    private val ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE          // YYYY-MM-DD
    private val SHORT_FORMAT = DateTimeFormatter.ofPattern("MM.dd")    // MM.dd

    // ==================== 格式化 ====================

    /**
     * 格式化日期为 YYYY-MM-DD
     * 对应 JS: formatDate(date)
     */
    fun formatDate(date: LocalDate): String {
        return date.format(ISO_FORMAT)
    }

    /**
     * 解析 YYYY-MM-DD 字符串为 LocalDate
     */
    fun parseDate(dateStr: String): LocalDate {
        return LocalDate.parse(dateStr, ISO_FORMAT)
    }

    /**
     * 格式化日期为 MM.dd（用于周报周期显示）
     * 对应 JS: formatShortDate(date)
     */
    fun formatShortDate(date: LocalDate): String {
        return date.format(SHORT_FORMAT)
    }

    // ==================== 日期判断 ====================

    /**
     * 判断是否今天
     * 对应 JS: isToday(dateStr)
     */
    fun isToday(dateStr: String): Boolean {
        return dateStr == formatDate(LocalDate.now())
    }

    // ==================== 日期偏移 ====================

    /**
     * 在某个日期上加减天数
     * 对应 JS: addDays(date, days)
     */
    fun addDays(date: LocalDate, days: Int): LocalDate {
        return date.plusDays(days.toLong())
    }

    /**
     * 在日期字符串上加减天数，返回字符串
     */
    fun addDays(dateStr: String, days: Int): String {
        return formatDate(addDays(parseDate(dateStr), days))
    }

    /**
     * 获取前一天的日期字符串
     * 对应 JS: prevDay(dateStr)
     */
    fun prevDay(dateStr: String): String {
        return addDays(dateStr, -1)
    }

    /**
     * 获取后一天的日期字符串
     * 对应 JS: nextDay(dateStr)
     */
    fun nextDay(dateStr: String): String {
        return addDays(dateStr, 1)
    }

    // ==================== 周操作 ====================

    /**
     * 获取某天所在周的起止日期（周一为一周起点）
     * 对应 JS: getWeekRange(date)
     *
     * @return Pair<周一, 周日>
     */
    fun getWeekRange(date: LocalDate): Pair<LocalDate, LocalDate> {
        val start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val end = start.plusDays(6)
        return Pair(start, end)
    }

    /**
     * 获取某天所在周的起止日期（字符串入参版本）
     */
    fun getWeekRange(dateStr: String): Pair<LocalDate, LocalDate> {
        return getWeekRange(parseDate(dateStr))
    }

    /**
     * 格式化周范围为 "MM.dd - MM.dd"
     * 对应 JS: formatWeekRange(date)
     */
    fun formatWeekRange(date: LocalDate): String {
        val (start, end) = getWeekRange(date)
        return "${formatShortDate(start)} - ${formatShortDate(end)}"
    }

    /**
     * 格式化周范围（字符串入参版本）
     */
    fun formatWeekRange(dateStr: String): String {
        return formatWeekRange(parseDate(dateStr))
    }

    /**
     * 获取上一周的起止日期
     * 对应 JS: prevWeek(date)
     */
    fun prevWeek(date: LocalDate): Pair<LocalDate, LocalDate> {
        val (start, _) = getWeekRange(date)
        return getWeekRange(start.minusDays(1))
    }

    /**
     * 获取下一周的起止日期
     * 对应 JS: nextWeek(date)
     */
    fun nextWeek(date: LocalDate): Pair<LocalDate, LocalDate> {
        val (_, end) = getWeekRange(date)
        return getWeekRange(end.plusDays(1))
    }

    /**
     * 获取某天所在周的全部 7 天日期字符串数组（周一到周日）
     * 对应 JS: getWeekDates(date)
     */
    fun getWeekDates(date: LocalDate): List<String> {
        val (start, _) = getWeekRange(date)
        return (0..6).map { formatDate(start.plusDays(it.toLong())) }
    }

    /**
     * 获取某天所在周的全部 7 天日期字符串数组（字符串入参版本）
     */
    fun getWeekDates(dateStr: String): List<String> {
        return getWeekDates(parseDate(dateStr))
    }

    // ==================== 便捷方法 ====================

    /**
     * 获取今天的日期字符串 YYYY-MM-DD
     */
    fun today(): String {
        return formatDate(LocalDate.now())
    }
}
