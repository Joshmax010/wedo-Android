package com.example.nutrition.domain.usecase

import com.example.nutrition.domain.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Calculator 单元测试
 * 覆盖全部 7 个计算函数 + 边界场景（空数据、溢出、零值目标、单条/多条记录）
 */
class CalculatorTest {

    // ==================== 辅助工厂方法 ====================

    private fun record(
        name: String = "食物",
        calories: Double = 100.0,
        protein: Double = 10.0,
        fat: Double = 5.0,
        carbs: Double = 20.0,
        micros: List<MealMicro> = emptyList()
    ) = MealRecord(
        name = name,
        calories = calories,
        protein = protein,
        fat = fat,
        carbs = carbs,
        micronutrients = micros
    )

    private fun dayRecords(
        date: String = "2026-06-30",
        breakfast: List<MealRecord> = emptyList(),
        lunch: List<MealRecord> = emptyList(),
        dinner: List<MealRecord> = emptyList(),
        snack: List<MealRecord> = emptyList()
    ) = DayRecords(
        dateStr = date,
        breakfast = breakfast,
        lunch = lunch,
        dinner = dinner,
        snack = snack
    )

    private fun defaultTargets() = NutritionTargets(
        calories = 2000.0,
        protein = 120.0,
        fat = 65.0,
        carbs = 250.0,
        micronutrients = listOf(
            MicronutrientTarget("vitC", "维生素C", "mg", 100.0),
            MicronutrientTarget("calcium", "钙", "mg", 800.0)
        )
    )

    // ==================== aggregateMeal ====================

    @Test
    fun `aggregateMeal - 空列表返回零值`() {
        val result = Calculator.aggregateMeal(emptyList())
        assertEquals(0, result.calories)
        assertEquals(0.0, result.protein, 0.001)
        assertEquals(0.0, result.fat, 0.001)
        assertEquals(0.0, result.carbs, 0.001)
        assertEquals(0, result.count)
        assertTrue(result.micronutrients.isEmpty())
    }

    @Test
    fun `aggregateMeal - 单条记录`() {
        val result = Calculator.aggregateMeal(listOf(
            record(calories = 350.0, protein = 25.5, fat = 12.3, carbs = 45.7)
        ))
        assertEquals(350, result.calories)
        assertEquals(25.5, result.protein, 0.001)
        assertEquals(12.3, result.fat, 0.001)
        assertEquals(45.7, result.carbs, 0.001)
        assertEquals(1, result.count)
    }

    @Test
    fun `aggregateMeal - 多条记录汇总`() {
        val result = Calculator.aggregateMeal(listOf(
            record(calories = 200.0, protein = 15.0, fat = 8.0, carbs = 30.0),
            record(calories = 150.0, protein = 10.0, fat = 6.0, carbs = 25.0),
            record(calories = 50.0, protein = 3.0, fat = 1.0, carbs = 8.0)
        ))
        assertEquals(400, result.calories)
        assertEquals(28.0, result.protein, 0.001)
        assertEquals(15.0, result.fat, 0.001)
        assertEquals(63.0, result.carbs, 0.001)
        assertEquals(3, result.count)
    }

    @Test
    fun `aggregateMeal - 热量取整`() {
        val result = Calculator.aggregateMeal(listOf(
            record(calories = 100.4),
            record(calories = 100.4)
        ))
        // 200.8 → roundToInt → 201
        assertEquals(201, result.calories)
    }

    @Test
    fun `aggregateMeal - 蛋白质保留一位小数`() {
        val result = Calculator.aggregateMeal(listOf(
            record(protein = 10.33),
            record(protein = 10.33)
        ))
        // 20.66 → roundOne → 20.7
        assertEquals(20.7, result.protein, 0.001)
    }

    @Test
    fun `aggregateMeal - 微量营养素汇总`() {
        val result = Calculator.aggregateMeal(listOf(
            record(micros = listOf(MealMicro("vitC", 30.0), MealMicro("calcium", 100.0))),
            record(micros = listOf(MealMicro("vitC", 20.0), MealMicro("iron", 5.0)))
        ))
        assertEquals(50.0, result.micronutrients["vitC"]!!, 0.001)
        assertEquals(100.0, result.micronutrients["calcium"]!!, 0.001)
        assertEquals(5.0, result.micronutrients["iron"]!!, 0.001)
    }

    @Test
    fun `aggregateMeal - 部分记录有零值字段`() {
        val result = Calculator.aggregateMeal(listOf(
            MealRecord(name = "水", calories = 0.0),
            record(calories = 500.0, protein = 30.0)
        ))
        assertEquals(500, result.calories)
        assertEquals(30.0, result.protein, 0.001)
        assertEquals(2, result.count)
    }

    // ==================== aggregateDay ====================

    @Test
    fun `aggregateDay - 全天空数据`() {
        val day = dayRecords()
        val result = Calculator.aggregateDay(day)
        assertEquals(0, result.calories)
        assertEquals(0.0, result.protein, 0.001)
        assertEquals(4, result.meals.size)
        for (mealKey in MealKey.entries) {
            assertEquals(0, result.meals[mealKey]!!.count)
        }
    }

    @Test
    fun `aggregateDay - 四餐汇总`() {
        val day = dayRecords(
            breakfast = listOf(record(calories = 400.0, protein = 20.0)),
            lunch = listOf(record(calories = 600.0, protein = 35.0)),
            dinner = listOf(record(calories = 700.0, protein = 40.0)),
            snack = listOf(record(calories = 200.0, protein = 5.0))
        )
        val result = Calculator.aggregateDay(day)
        assertEquals(1900, result.calories)
        assertEquals(100.0, result.protein, 0.001)
        assertEquals(4, result.meals.size)
    }

    @Test
    fun `aggregateDay - 只有早餐有数据`() {
        val day = dayRecords(
            breakfast = listOf(record(calories = 500.0, protein = 25.0))
        )
        val result = Calculator.aggregateDay(day)
        assertEquals(500, result.calories)
        assertEquals(25.0, result.protein, 0.001)
        assertEquals(1, result.meals[MealKey.BREAKFAST]!!.count)
        assertEquals(0, result.meals[MealKey.LUNCH]!!.count)
        assertEquals(0, result.meals[MealKey.DINNER]!!.count)
        assertEquals(0, result.meals[MealKey.SNACK]!!.count)
    }

    @Test
    fun `aggregateDay - 微量营养素跨餐次汇总`() {
        val day = dayRecords(
            breakfast = listOf(record(micros = listOf(MealMicro("vitC", 40.0)))),
            lunch = listOf(record(micros = listOf(MealMicro("vitC", 60.0))))
        )
        val result = Calculator.aggregateDay(day)
        assertEquals(100.0, result.micronutrients["vitC"]!!, 0.001)
    }

    // ==================== calcGap ====================

    @Test
    fun `calcGap - 超量为正`() {
        assertEquals(200.0, Calculator.calcGap(2200.0, 2000.0), 0.001)
    }

    @Test
    fun `calcGap - 缺口为负`() {
        assertEquals(-300.0, Calculator.calcGap(1700.0, 2000.0), 0.001)
    }

    @Test
    fun `calcGap - 刚好达标`() {
        assertEquals(0.0, Calculator.calcGap(2000.0, 2000.0), 0.001)
    }

    @Test
    fun `calcGap - 保留一位小数`() {
        // 1500.54 - 2000 = -499.46 → roundOne → -499.5（远离 .5 边界，验证保留一位小数）
        // 注：roundOne 与 JS Math.round 一致，.5 恰好落在边界时向正无穷取整（如 -499.5 → -499.4）
        assertEquals(-499.5, Calculator.calcGap(1500.54, 2000.0), 0.001)
    }

    // ==================== calcAchievementRate ====================

    @Test
    fun `calcAchievementRate - 完全达标`() {
        assertEquals(100.0, Calculator.calcAchievementRate(2000.0, 2000.0), 0.001)
    }

    @Test
    fun `calcAchievementRate - 50 百分比`() {
        assertEquals(50.0, Calculator.calcAchievementRate(1000.0, 2000.0), 0.001)
    }

    @Test
    fun `calcAchievementRate - 超量 150 百分比`() {
        assertEquals(150.0, Calculator.calcAchievementRate(3000.0, 2000.0), 0.001)
    }

    @Test
    fun `calcAchievementRate - 目标为零返回 0`() {
        assertEquals(0.0, Calculator.calcAchievementRate(100.0, 0.0), 0.001)
    }

    @Test
    fun `calcAchievementRate - 摄入为零`() {
        assertEquals(0.0, Calculator.calcAchievementRate(0.0, 2000.0), 0.001)
    }

    @Test
    fun `calcAchievementRate - 保留一位小数`() {
        // 1000 / 3000 * 100 = 33.333... → 33.3
        assertEquals(33.3, Calculator.calcAchievementRate(1000.0, 3000.0), 0.001)
    }

    // ==================== aggregateWeek ====================

    @Test
    fun `aggregateWeek - 空数据返回 7 个零值天`() {
        val weekDates = listOf(
            "2026-06-29", "2026-06-30", "2026-07-01", "2026-07-02",
            "2026-07-03", "2026-07-04", "2026-07-05"
        )
        val result = Calculator.aggregateWeek(emptyMap(), weekDates)
        assertEquals(7, result.size)
        for (day in result) {
            assertEquals(0, day.calories)
        }
    }

    @Test
    fun `aggregateWeek - 部分天有数据`() {
        val records = mapOf(
            "2026-06-29" to dayRecords(
                date = "2026-06-29",
                breakfast = listOf(record(calories = 500.0))
            ),
            "2026-07-01" to dayRecords(
                date = "2026-07-01",
                lunch = listOf(record(calories = 800.0))
            )
        )
        val weekDates = listOf(
            "2026-06-29", "2026-06-30", "2026-07-01", "2026-07-02",
            "2026-07-03", "2026-07-04", "2026-07-05"
        )
        val result = Calculator.aggregateWeek(records, weekDates)
        assertEquals(7, result.size)
        assertEquals(500, result[0].calories)
        assertEquals(0, result[1].calories)
        assertEquals(800, result[2].calories)
    }

    // ==================== calcWeekSummary ====================

    @Test
    fun `calcWeekSummary - 全部空数据`() {
        val weekDates = listOf(
            "2026-06-29", "2026-06-30", "2026-07-01", "2026-07-02",
            "2026-07-03", "2026-07-04", "2026-07-05"
        )
        val summary = Calculator.calcWeekSummary(emptyMap(), weekDates, defaultTargets())
        assertEquals(0, summary.avgCalories)
        assertEquals(0.0, summary.avgProtein, 0.001)
        assertEquals(0, summary.achievementDays)
        assertEquals(7, summary.dailyData.size)
        assertEquals(4, summary.nutrientRates.size)
        assertEquals(2, summary.microTable.size)
    }

    @Test
    fun `calcWeekSummary - 每天达标热量`() {
        val weekDates = listOf(
            "2026-06-29", "2026-06-30", "2026-07-01", "2026-07-02",
            "2026-07-03", "2026-07-04", "2026-07-05"
        )
        val records = weekDates.associateWith { date ->
            dayRecords(
                date = date,
                breakfast = listOf(record(calories = 2000.0, protein = 120.0, fat = 65.0, carbs = 250.0))
            )
        }
        val summary = Calculator.calcWeekSummary(records, weekDates, defaultTargets())
        assertEquals(2000, summary.avgCalories)
        assertEquals(120.0, summary.avgProtein, 0.001)
        assertEquals(65.0, summary.avgFat, 0.001)
        assertEquals(250.0, summary.avgCarbs, 0.001)
        assertEquals(7, summary.achievementDays)
        assertEquals(0.0, summary.totalGap, 0.001)

        // 各营养素达标率应为 100%
        for (nr in summary.nutrientRates) {
            assertEquals(100.0, nr.rate, 0.001)
        }
    }

    @Test
    fun `calcWeekSummary - 达标天数边界判定`() {
        val weekDates = listOf(
            "2026-06-29", "2026-06-30", "2026-07-01", "2026-07-02",
            "2026-07-03", "2026-07-04", "2026-07-05"
        )
        // 目标 2000，90% = 1800，110% = 2200
        val records = mapOf(
            // 达标（1800~2200 内）
            "2026-06-29" to dayRecords(date = "2026-06-29", breakfast = listOf(record(calories = 1800.0))),
            "2026-06-30" to dayRecords(date = "2026-06-30", breakfast = listOf(record(calories = 2000.0))),
            "2026-07-01" to dayRecords(date = "2026-07-01", breakfast = listOf(record(calories = 2200.0))),
            // 不达标
            "2026-07-02" to dayRecords(date = "2026-07-02", breakfast = listOf(record(calories = 1700.0))),
            "2026-07-03" to dayRecords(date = "2026-07-03", breakfast = listOf(record(calories = 2300.0))),
            "2026-07-04" to dayRecords(date = "2026-07-04", breakfast = listOf(record(calories = 500.0))),
            "2026-07-05" to dayRecords(date = "2026-07-05", breakfast = listOf(record(calories = 3000.0)))
        )
        val summary = Calculator.calcWeekSummary(records, weekDates, defaultTargets())
        assertEquals(3, summary.achievementDays)
    }

    @Test
    fun `calcWeekSummary - 热量缺口计算正确`() {
        val weekDates = listOf(
            "2026-06-29", "2026-06-30", "2026-07-01", "2026-07-02",
            "2026-07-03", "2026-07-04", "2026-07-05"
        )
        // 每天 1500，目标 2000，每天缺口 -500，总缺口 -3500
        val records = weekDates.associateWith { date ->
            dayRecords(date = date, breakfast = listOf(record(calories = 1500.0)))
        }
        val summary = Calculator.calcWeekSummary(records, weekDates, defaultTargets())
        assertEquals(-3500.0, summary.totalGap, 0.001)
    }

    @Test
    fun `calcWeekSummary - 日均计算正确`() {
        val weekDates = listOf(
            "2026-06-29", "2026-06-30", "2026-07-01", "2026-07-02",
            "2026-07-03", "2026-07-04", "2026-07-05"
        )
        // 总热量 14000 / 7 = 2000
        val records = weekDates.associateWith { date ->
            dayRecords(date = date, breakfast = listOf(record(calories = 2000.0, protein = 80.0)))
        }
        val summary = Calculator.calcWeekSummary(records, weekDates, defaultTargets())
        assertEquals(2000, summary.avgCalories)
        // 总蛋白 560 / 7 = 80.0
        assertEquals(80.0, summary.avgProtein, 0.001)
    }

    @Test
    fun `calcWeekSummary - 微量营养素表格正确`() {
        val weekDates = listOf(
            "2026-06-29", "2026-06-30", "2026-07-01", "2026-07-02",
            "2026-07-03", "2026-07-04", "2026-07-05"
        )
        // 每天 vitC 摄入 70mg，目标 100mg
        val records = weekDates.associateWith { date ->
            dayRecords(
                date = date,
                breakfast = listOf(record(micros = listOf(MealMicro("vitC", 70.0))))
            )
        }
        val summary = Calculator.calcWeekSummary(records, weekDates, defaultTargets())
        val vitCRow = summary.microTable.find { it.key == "vitC" }!!
        assertEquals(70.0, vitCRow.dailyAvg, 0.001)
        assertEquals(100.0, vitCRow.target, 0.001)
        assertEquals(70.0, vitCRow.rate, 0.001)
    }

    @Test
    fun `calcWeekSummary - 营养素达标率列表包含四项`() {
        val weekDates = listOf(
            "2026-06-29", "2026-06-30", "2026-07-01", "2026-07-02",
            "2026-07-03", "2026-07-04", "2026-07-05"
        )
        val summary = Calculator.calcWeekSummary(emptyMap(), weekDates, defaultTargets())
        assertEquals(4, summary.nutrientRates.size)
        assertEquals("热量", summary.nutrientRates[0].name)
        assertEquals("蛋白质", summary.nutrientRates[1].name)
        assertEquals("脂肪", summary.nutrientRates[2].name)
        assertEquals("碳水", summary.nutrientRates[3].name)
    }

    // ==================== generateWeekReportText ====================

    @Test
    fun `generateWeekReportText - 包含全部关键字段`() {
        val summary = Calculator.WeekSummary(
            avgCalories = 1850,
            avgProtein = 95.5,
            avgFat = 60.0,
            avgCarbs = 220.3,
            achievementDays = 5,
            totalGap = -1050.0,
            nutrientRates = listOf(
                Calculator.NutrientRate("热量", 92.5),
                Calculator.NutrientRate("蛋白质", 79.6),
                Calculator.NutrientRate("脂肪", 92.3),
                Calculator.NutrientRate("碳水", 88.1)
            ),
            microTable = listOf(
                Calculator.MicroTableRow("vitC", "维生素C", "mg", 85.0, 100.0, 85.0),
                Calculator.MicroTableRow("calcium", "钙", "mg", 600.0, 800.0, 75.0)
            ),
            dailyData = emptyList()
        )
        val text = Calculator.generateWeekReportText(summary, "06.29 - 07.05")

        assertTrue(text.contains("【营养周报 06.29 - 07.05】"))
        assertTrue(text.contains("1850 kcal"))
        assertTrue(text.contains("95.5 g"))
        assertTrue(text.contains("60.0 g"))
        assertTrue(text.contains("220.3 g"))
        assertTrue(text.contains("5/7 天"))
        assertTrue(text.contains("-1050.0 kcal"))
        assertTrue(text.contains("热量：92.5%"))
        assertTrue(text.contains("蛋白质：79.6%"))
        assertTrue(text.contains("维生素C：85.0mg"))
        assertTrue(text.contains("钙：600.0mg"))
        assertTrue(text.contains("由营养记录生成"))
    }

    @Test
    fun `generateWeekReportText - 空微量营养素也不报错`() {
        val summary = Calculator.WeekSummary(
            avgCalories = 0,
            avgProtein = 0.0,
            avgFat = 0.0,
            avgCarbs = 0.0,
            achievementDays = 0,
            totalGap = 0.0,
            nutrientRates = listOf(
                Calculator.NutrientRate("热量", 0.0),
                Calculator.NutrientRate("蛋白质", 0.0),
                Calculator.NutrientRate("脂肪", 0.0),
                Calculator.NutrientRate("碳水", 0.0)
            ),
            microTable = emptyList(),
            dailyData = emptyList()
        )
        val text = Calculator.generateWeekReportText(summary, "01.01 - 01.07")
        assertTrue(text.contains("【营养周报 01.01 - 01.07】"))
        assertTrue(text.contains("0 kcal"))
    }

    // ==================== 大数值 / 浮点精度边界 ====================

    @Test
    fun `aggregateMeal - 大数值累加不溢出`() {
        val records = (1..100).map {
            record(calories = 9999.0, protein = 999.9, fat = 999.9, carbs = 999.9)
        }
        val result = Calculator.aggregateMeal(records)
        // 100 * 9999 = 999900
        assertEquals(999900, result.calories)
    }

    @Test
    fun `calcAchievementRate - 超大值`() {
        // 10000 / 2000 * 100 = 500.0%
        assertEquals(500.0, Calculator.calcAchievementRate(10000.0, 2000.0), 0.001)
    }

    @Test
    fun `calcGap - 零减零`() {
        assertEquals(0.0, Calculator.calcGap(0.0, 0.0), 0.001)
    }
}
