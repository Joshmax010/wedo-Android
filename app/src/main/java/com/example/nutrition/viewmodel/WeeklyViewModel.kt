package com.example.nutrition.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nutrition.domain.constants.NutrientConstants
import com.example.nutrition.domain.repository.LocalStorageRepository
import com.example.nutrition.domain.usecase.Calculator
import com.example.nutrition.domain.usecase.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * 周报页 ViewModel —— 对应小程序 pages/weekly/weekly.js
 *
 * 管理周数据聚合、图表数据、周报摘要、复制周报。
 * 聚合计算下沉到 Calculator，页面状态收敛为单一 UiState
 */
class WeeklyViewModel(
    private val repository: LocalStorageRepository
) : ViewModel() {

    // ==================== 图表数据类 ====================

    /** 热量折线图数据点 */
    data class CaloriePoint(
        val label: String,   // MM-dd
        val calories: Int,
        val dayOfWeek: String // 周一/周二...
    )

    /** 营养素柱状图数据点 */
    data class NutrientBar(
        val label: String,   // MM-dd
        val protein: Double,
        val fat: Double,
        val carbs: Double
    )

    /** 周报摘要四宫格 */
    data class SummaryGrid(
        val avgCalories: Int,
        val avgProtein: Double,
        val achievementDays: Int,
        val totalGap: Double
    )

    /** 营养素达标率行 */
    data class NutrientRateRow(
        val name: String,
        val rate: Double     // 百分比，可 >100
    )

    /** 微量营养素表格行 */
    data class MicroRow(
        val name: String,
        val unit: String,
        val dailyAvg: Double,
        val target: Double,
        val rate: Double     // 百分比
    )

    // ==================== UI 状态 ====================

    data class UiState(
        val weekRange: String = "",
        val hasData: Boolean = false,
        val caloriePoints: List<CaloriePoint> = emptyList(),
        val nutrientBars: List<NutrientBar> = emptyList(),
        val targetCalories: Double = 0.0,
        val summary: SummaryGrid? = null,
        val nutrientRates: List<NutrientRateRow> = emptyList(),
        val microTable: List<MicroRow> = emptyList(),
        val reportText: String = ""
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** 当前周基准日期（该周周一） */
    private var baseDate =
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    // ==================== 数据加载 ====================

    fun loadData() {
        viewModelScope.launch {
            val weekDates = getWeekDates(baseDate)
            val weekRange = DateUtils.formatWeekRange(baseDate)

            // 读取目标配置
            val targets = repository.getTargets().first()
                ?: NutrientConstants.getDefaultTargets()

            // 读取全部记录并计算周报摘要
            val allRecords = repository.getAllRecords().first()
            val weekSummary = Calculator.calcWeekSummary(allRecords, weekDates, targets)

            _uiState.update {
                it.copy(
                    weekRange = weekRange,
                    targetCalories = targets.calories,
                    hasData = weekSummary.avgCalories > 0 ||
                        weekSummary.dailyData.any { day -> day.calories > 0 },
                    // 热量折线图数据
                    caloriePoints = weekDates.mapIndexed { index, dateStr ->
                        val day = weekSummary.dailyData[index]
                        CaloriePoint(
                            label = dateStr.substring(5), // MM-dd
                            calories = day.calories,
                            dayOfWeek = DAY_NAMES[index]
                        )
                    },
                    // 营养素柱状图数据
                    nutrientBars = weekDates.mapIndexed { index, dateStr ->
                        val day = weekSummary.dailyData[index]
                        NutrientBar(
                            label = dateStr.substring(5),
                            protein = day.protein,
                            fat = day.fat,
                            carbs = day.carbs
                        )
                    },
                    // 摘要四宫格
                    summary = SummaryGrid(
                        avgCalories = weekSummary.avgCalories,
                        avgProtein = weekSummary.avgProtein,
                        achievementDays = weekSummary.achievementDays,
                        totalGap = weekSummary.totalGap
                    ),
                    // 营养素达标率
                    nutrientRates = weekSummary.nutrientRates.map { rate ->
                        NutrientRateRow(name = rate.name, rate = rate.rate)
                    },
                    // 微量营养素表格
                    microTable = weekSummary.microTable.map { row ->
                        MicroRow(
                            name = row.name,
                            unit = row.unit,
                            dailyAvg = row.dailyAvg,
                            target = row.target,
                            rate = row.rate
                        )
                    },
                    // 周报文本
                    reportText = Calculator.generateWeekReportText(weekSummary, weekRange)
                )
            }
        }
    }

    // ==================== 周切换 ====================

    fun prevWeek() {
        baseDate = baseDate.minusWeeks(1)
        loadData()
    }

    fun nextWeek() {
        val nextMonday = baseDate.plusWeeks(1)
        val currentMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        if (nextMonday <= currentMonday) {
            baseDate = nextMonday
            loadData()
        }
    }

    // ==================== 辅助方法 ====================

    private fun getWeekDates(monday: LocalDate): List<String> {
        return (0..6).map {
            DateUtils.formatDate(monday.plusDays(it.toLong()))
        }
    }

    companion object {
        private val DAY_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    }

    // ==================== ViewModelFactory ====================

    class Factory(private val repository: LocalStorageRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WeeklyViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return WeeklyViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
