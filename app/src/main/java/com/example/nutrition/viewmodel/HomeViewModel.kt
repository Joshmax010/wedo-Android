package com.example.nutrition.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nutrition.domain.constants.NutrientConstants
import com.example.nutrition.domain.model.DayRecords
import com.example.nutrition.domain.model.MealKey
import com.example.nutrition.domain.model.NutritionTargets
import com.example.nutrition.domain.repository.LocalStorageRepository
import com.example.nutrition.domain.usecase.Calculator
import com.example.nutrition.domain.usecase.DateUtils
import com.example.nutrition.domain.usecase.MetabolismCalculator
import com.example.nutrition.ui.theme.CarbsColor
import com.example.nutrition.ui.theme.FatColor
import com.example.nutrition.ui.theme.ProteinColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel —— 对应小程序 pages/index/index.js
 *
 * 管理数据聚合与日期切换。页面状态收敛为单一 HomeUiState，
 * 目标/当天记录数据由 Repository Flow 自动驱动重算
 */
class HomeViewModel(
    private val repository: LocalStorageRepository
) : ViewModel() {

    // ==================== 聚合数据类 ====================

    data class MacroData(
        val current: Double,
        val target: Double,
        val label: String,
        val unit: String,
        val color: Color
    )

    data class MicroData(
        val current: Double,
        val target: Double,
        val label: String,
        val unit: String
    )

    data class MealData(
        val key: MealKey,
        val calories: Int,
        val count: Int
    )

    data class StorageWarn(
        val message: String
    )

    // ==================== UI 状态 ====================

    data class UiState(
        val currentDate: String = DateUtils.today(),
        val isToday: Boolean = true,
        val hasData: Boolean = false,
        val ringPercent: Int = 0,
        val ringCenterText: String = "0",
        val targetCalories: Double = 0.0,
        val macros: List<MacroData> = emptyList(),
        val micros: List<MicroData> = emptyList(),
        val meals: List<MealData> = emptyList(),
        val storageWarn: StorageWarn? = null,
        val showGuide: Boolean = false,
        // 二期：代谢计算相关展示
        val tdee: Double? = null,
        val isAutoCalculated: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // 日期或仓库数据变化时自动重算
        viewModelScope.launch {
            _uiState
                .map { it.currentDate }
                .distinctUntilChanged()
                .flatMapLatest { date ->
                    combine(
                        repository.getTargets(),
                        repository.getDayRecords(date)
                    ) { targets, dayData ->
                        date to (targets to dayData)
                    }
                }
                .collect { (date, pair) ->
                    applyHomeData(date, pair.first, pair.second)
                }
        }
    }

    // ==================== 数据加载 ====================

    /**
     * 兼容入口：数据已由 Flow 自动驱动，无需手动加载
     */
    fun loadData() {
        // no-op
    }

    private suspend fun applyHomeData(
        dateStr: String,
        targetsOrNull: NutritionTargets?,
        dayData: DayRecords
    ) {
        val targets = targetsOrNull ?: NutrientConstants.getDefaultTargets()
        val dayAgg = Calculator.aggregateDay(dayData)
        val gap = Calculator.calcGap(dayAgg.calories.toDouble(), targets.calories)
        val capacity = repository.checkStorageCapacity()

        _uiState.update { state ->
            state.copy(
                isToday = DateUtils.isToday(dateStr),
                hasData = dayAgg.calories > 0,
                // 二期：代谢计算展示
                isAutoCalculated = targets.isAutoCalculated,
                tdee = targets.bodyProfile?.let { MetabolismCalculator.calculateTdee(it) },
                // 热量环形进度
                targetCalories = targets.calories,
                ringPercent = if (targets.calories > 0) {
                    (dayAgg.calories / targets.calories * 100).toInt()
                } else {
                    0
                },
                ringCenterText = when {
                    gap > 0 -> "+${gap.toInt()}"
                    gap < 0 -> "${gap.toInt()}"
                    else -> "${dayAgg.calories}"
                },
                // 宏量营养素
                macros = listOf(
                    MacroData(dayAgg.protein, targets.protein, "蛋白质", "g", ProteinColor),
                    MacroData(dayAgg.fat, targets.fat, "脂肪", "g", FatColor),
                    MacroData(dayAgg.carbs, targets.carbs, "碳水", "g", CarbsColor)
                ),
                // 微量营养素
                micros = targets.micronutrients.map { mn ->
                    MicroData(
                        current = dayAgg.micronutrients[mn.key] ?: 0.0,
                        target = mn.target,
                        label = mn.name,
                        unit = mn.unit
                    )
                },
                // 四餐分布
                meals = MealKey.entries.map { mealKey ->
                    val mealAgg = dayAgg.meals[mealKey]
                    MealData(
                        key = mealKey,
                        calories = mealAgg?.calories ?: 0,
                        count = dayData.getMeal(mealKey).size
                    )
                },
                // 存储容量告警
                storageWarn = if (capacity.warn || !capacity.ok) {
                    StorageWarn(capacity.message)
                } else {
                    null
                }
            )
        }
    }

    /**
     * 检测首次使用并显示引导
     */
    fun checkFirstUse() {
        viewModelScope.launch {
            val meta = repository.getMeta().first()
            if (meta == null || !meta.hasSeenGuide) {
                _uiState.update { it.copy(showGuide = true) }
            }
        }
    }

    /**
     * 标记引导已完成
     */
    fun onGuideFinish() {
        _uiState.update { it.copy(showGuide = false) }
        viewModelScope.launch {
            val meta = repository.getMeta().first() ?: NutrientConstants.getDefaultMeta()
            repository.setMeta(meta.copy(hasSeenGuide = true))
        }
    }

    // ==================== 日期切换 ====================

    fun pickDate(dateStr: String) {
        _uiState.update { it.copy(currentDate = dateStr) }
    }

    fun prevDay() {
        _uiState.update { it.copy(currentDate = DateUtils.prevDay(it.currentDate)) }
    }

    fun nextDay() {
        _uiState.update { state ->
            val next = DateUtils.nextDay(state.currentDate)
            // 不超过今天
            if (next <= DateUtils.today()) state.copy(currentDate = next) else state
        }
    }

    // ==================== ViewModelFactory ====================

    class Factory(private val repository: LocalStorageRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
