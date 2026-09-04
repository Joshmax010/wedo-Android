package com.example.nutrition.domain.usecase

import com.example.nutrition.domain.model.DayRecords
import com.example.nutrition.domain.model.MealKey
import com.example.nutrition.domain.model.MealRecord
import com.example.nutrition.domain.model.NutritionTargets
import kotlin.math.roundToInt

/**
 * 营养数据计算 —— 移植自小程序 utils/calc.js
 *
 * 全部纯函数，入参出参为数据类
 * 浮点数规则：热量取整，蛋白质/脂肪/碳水保留 1 位小数
 */
object Calculator {

    // ==================== 结果数据类 ====================

    /** 单餐聚合结果 */
    data class MealAggregate(
        val calories: Int,
        val protein: Double,
        val fat: Double,
        val carbs: Double,
        val count: Int,
        val micronutrients: Map<String, Double>
    )

    /** 全天聚合结果 */
    data class DayAggregate(
        val calories: Int,
        val protein: Double,
        val fat: Double,
        val carbs: Double,
        val micronutrients: Map<String, Double>,
        val meals: Map<MealKey, MealAggregate>
    )

    /** 营养素达标率 */
    data class NutrientRate(
        val name: String,
        val rate: Double
    )

    /** 微量营养素周览表格行 */
    data class MicroTableRow(
        val key: String,
        val name: String,
        val unit: String,
        val dailyAvg: Double,
        val target: Double,
        val rate: Double
    )

    /** 周报摘要 */
    data class WeekSummary(
        val avgCalories: Int,
        val avgProtein: Double,
        val avgFat: Double,
        val avgCarbs: Double,
        val achievementDays: Int,
        val totalGap: Double,
        val nutrientRates: List<NutrientRate>,
        val microTable: List<MicroTableRow>,
        val dailyData: List<DayAggregate>
    )

    // ==================== 计算函数 ====================

    /**
     * 聚合单个餐次的记录，汇总热量与营养素
     * 对应 JS: aggregateMeal(mealRecords)
     */
    fun aggregateMeal(mealRecords: List<MealRecord>): MealAggregate {
        if (mealRecords.isEmpty()) {
            return MealAggregate(0, 0.0, 0.0, 0.0, 0, emptyMap())
        }

        var calories = 0.0
        var protein = 0.0
        var fat = 0.0
        var carbs = 0.0
        val micronutrients = mutableMapOf<String, Double>()

        for (r in mealRecords) {
            calories += r.calories
            protein += r.protein
            fat += r.fat
            carbs += r.carbs

            for (mn in r.micronutrients) {
                micronutrients[mn.key] = (micronutrients[mn.key] ?: 0.0) + mn.value
            }
        }

        return MealAggregate(
            calories = calories.roundToInt(),
            protein = roundOne(protein),
            fat = roundOne(fat),
            carbs = roundOne(carbs),
            count = mealRecords.size,
            micronutrients = micronutrients.mapValues { roundOne(it.value) }
        )
    }

    /**
     * 聚合整天的四餐记录
     * 对应 JS: aggregateDay(dayData)
     */
    fun aggregateDay(dayData: DayRecords): DayAggregate {
        val meals = mutableMapOf<MealKey, MealAggregate>()
        var totalCalories = 0.0
        var totalProtein = 0.0
        var totalFat = 0.0
        var totalCarbs = 0.0
        val totalMicros = mutableMapOf<String, Double>()

        for (mealKey in MealKey.entries) {
            val mealAgg = aggregateMeal(dayData.getMeal(mealKey))
            meals[mealKey] = mealAgg
            totalCalories += mealAgg.calories
            totalProtein += mealAgg.protein
            totalFat += mealAgg.fat
            totalCarbs += mealAgg.carbs

            for ((key, value) in mealAgg.micronutrients) {
                totalMicros[key] = (totalMicros[key] ?: 0.0) + value
            }
        }

        return DayAggregate(
            calories = totalCalories.roundToInt(),
            protein = roundOne(totalProtein),
            fat = roundOne(totalFat),
            carbs = roundOne(totalCarbs),
            micronutrients = totalMicros.mapValues { roundOne(it.value) },
            meals = meals
        )
    }

    /**
     * 计算缺口（正=超量，负=缺口）
     * 对应 JS: calcGap(consumed, target)
     */
    fun calcGap(consumed: Double, target: Double): Double {
        return roundOne(consumed - target)
    }

    /**
     * 计算达标率（百分比）
     * 对应 JS: calcAchievementRate(consumed, target)
     */
    fun calcAchievementRate(consumed: Double, target: Double): Double {
        if (target == 0.0) return 0.0
        return roundOne(consumed / target * 100.0)
    }

    /**
     * 聚合一周数据（7 天）
     * 对应 JS: aggregateWeek(allRecords, weekDates)
     */
    fun aggregateWeek(
        allRecords: Map<String, DayRecords>,
        weekDates: List<String>
    ): List<DayAggregate> {
        return weekDates.map { dateStr ->
            val dayData = allRecords[dateStr]
                ?: DayRecords(dateStr = dateStr)
            val agg = aggregateDay(dayData)
            agg
        }
    }

    /**
     * 生成周报摘要
     * 对应 JS: calcWeekSummary(allRecords, weekDates, targets)
     */
    fun calcWeekSummary(
        allRecords: Map<String, DayRecords>,
        weekDates: List<String>,
        targets: NutritionTargets
    ): WeekSummary {
        val dailyData = aggregateWeek(allRecords, weekDates)

        // 总量
        val totalCalories = dailyData.sumOf { it.calories }
        val totalProtein = dailyData.sumOf { it.protein }
        val totalFat = dailyData.sumOf { it.fat }
        val totalCarbs = dailyData.sumOf { it.carbs }

        // 日均
        val avgCalories = (totalCalories / 7.0).roundToInt()
        val avgProtein = roundOne(totalProtein / 7.0)
        val avgFat = roundOne(totalFat / 7.0)
        val avgCarbs = roundOne(totalCarbs / 7.0)

        // 微量营养素日均
        val totalMicros = mutableMapOf<String, Double>()
        for (day in dailyData) {
            for ((key, value) in day.micronutrients) {
                totalMicros[key] = (totalMicros[key] ?: 0.0) + value
            }
        }

        // 达标天数（热量在目标 ±10% 内算达标）
        var achievementDays = 0
        var totalGap = 0.0
        for (day in dailyData) {
            val rate = calcAchievementRate(day.calories.toDouble(), targets.calories)
            if (rate in 90.0..110.0) {
                achievementDays++
            }
            totalGap += calcGap(day.calories.toDouble(), targets.calories)
        }

        // 各营养素达标率
        val nutrientRates = listOf(
            NutrientRate("热量", calcAchievementRate(avgCalories.toDouble(), targets.calories)),
            NutrientRate("蛋白质", calcAchievementRate(avgProtein, targets.protein)),
            NutrientRate("脂肪", calcAchievementRate(avgFat, targets.fat)),
            NutrientRate("碳水", calcAchievementRate(avgCarbs, targets.carbs))
        )

        // 微量营养素周览表格
        val microTable = targets.micronutrients.map { mn ->
            val avgValue = roundOne((totalMicros[mn.key] ?: 0.0) / 7.0)
            MicroTableRow(
                key = mn.key,
                name = mn.name,
                unit = mn.unit,
                dailyAvg = avgValue,
                target = mn.target,
                rate = calcAchievementRate(avgValue, mn.target)
            )
        }

        return WeekSummary(
            avgCalories = avgCalories,
            avgProtein = avgProtein,
            avgFat = avgFat,
            avgCarbs = avgCarbs,
            achievementDays = achievementDays,
            totalGap = roundOne(totalGap),
            nutrientRates = nutrientRates,
            microTable = microTable,
            dailyData = dailyData
        )
    }

    /**
     * 生成周报摘要文本（用于复制到剪贴板）
     * 对应 JS: generateWeekReportText(summary, weekRange)
     */
    fun generateWeekReportText(summary: WeekSummary, weekRange: String): String {
        val sb = StringBuilder()
        sb.appendLine("【营养周报 $weekRange】")
        sb.appendLine()
        sb.appendLine("本周平均热量：${summary.avgCalories} kcal")
        sb.appendLine("本周平均蛋白质：${summary.avgProtein} g")
        sb.appendLine("本周平均脂肪：${summary.avgFat} g")
        sb.appendLine("本周平均碳水：${summary.avgCarbs} g")
        sb.appendLine("热量达标天数：${summary.achievementDays}/7 天")
        sb.appendLine("热量缺口总量：${summary.totalGap} kcal")
        sb.appendLine()
        sb.appendLine("各营养素达标率：")
        for (nr in summary.nutrientRates) {
            sb.appendLine("  ${nr.name}：${nr.rate}%")
        }
        sb.appendLine()
        sb.appendLine("微量营养素日均摄入：")
        for (mt in summary.microTable) {
            sb.appendLine("  ${mt.name}：${mt.dailyAvg}${mt.unit} / 目标 ${mt.target}${mt.unit}（${mt.rate}%）")
        }
        sb.appendLine()
        sb.append("—— 由营养记录生成")
        return sb.toString()
    }

    // ==================== 内部工具 ====================

    /** 四舍五入到 1 位小数 */
    private fun roundOne(value: Double): Double {
        return (value * 10).roundToInt() / 10.0
    }
}
