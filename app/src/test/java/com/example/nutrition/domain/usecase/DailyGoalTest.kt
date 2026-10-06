package com.example.nutrition.domain.usecase

import com.example.nutrition.domain.model.DayRecords
import com.example.nutrition.domain.model.MealRecord
import com.example.nutrition.domain.model.NutritionTargets
import org.junit.Assert.*
import org.junit.Test

class DailyGoalTest {
    private val targets = NutritionTargets(2000.0, 120.0, 60.0, 250.0, emptyList())
    private fun day(calories: Double = 2000.0, protein: Double = 120.0, fat: Double = 60.0, carbs: Double = 250.0) =
        DayRecords("2026-10-01", breakfast = listOf(MealRecord(name = "餐食", calories = calories, protein = protein, fat = fat, carbs = carbs)))

    @Test fun `calorie lower boundary qualifies with all macros`() {
        assertTrue(DailyGoal.evaluate(day(1800.0), targets).dayComplete)
    }
    @Test fun `calorie upper boundary qualifies with all macros`() {
        assertTrue(DailyGoal.evaluate(day(2200.0), targets).dayComplete)
    }
    @Test fun `calories below band qualify only for macro feedback`() {
        assertEquals(DailyGoal.Completion(true, false), DailyGoal.evaluate(day(1799.0), targets))
    }
    @Test fun `calories above band do not qualify for daily celebration`() {
        assertEquals(DailyGoal.Completion(true, false), DailyGoal.evaluate(day(2201.0), targets))
    }
    @Test fun `each macro is required independently`() {
        for (incomplete in listOf(day(protein = 119.0), day(fat = 59.0), day(carbs = 249.0))) {
            assertEquals(DailyGoal.Completion(false, false), DailyGoal.evaluate(incomplete, targets))
        }
    }
    @Test fun `empty day never qualifies`() {
        assertEquals(DailyGoal.Completion(false, false), DailyGoal.evaluate(DayRecords("2026-10-01"), targets))
    }
    @Test fun `invalid target does not create spurious completion`() {
        for (invalid in listOf(targets.copy(calories = 0.0), targets.copy(protein = 0.0), targets.copy(fat = -1.0), targets.copy(carbs = Double.NaN))) {
            assertEquals(DailyGoal.Completion(false, false), DailyGoal.evaluate(day(), invalid))
        }
    }
    @Test fun `totals across meals use the same aggregation as the overview`() {
        val meal = MealRecord(name = "半份", calories = 1000.0, protein = 60.0, fat = 30.0, carbs = 125.0)
        assertTrue(DailyGoal.evaluate(DayRecords("2026-10-01", breakfast = listOf(meal), dinner = listOf(meal)), targets).dayComplete)
    }
}
