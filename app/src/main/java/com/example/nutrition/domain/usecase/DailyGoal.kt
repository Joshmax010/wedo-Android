package com.example.nutrition.domain.usecase

import com.example.nutrition.domain.model.DayRecords
import com.example.nutrition.domain.model.NutritionTargets

/** Feedback eligibility; weekly statistics keep their existing calorie-only rule. */
object DailyGoal {
    data class Completion(val macrosComplete: Boolean, val dayComplete: Boolean)

    fun evaluate(day: DayRecords, targets: NutritionTargets): Completion {
        if (day.isEmpty() || listOf(targets.calories, targets.protein, targets.fat, targets.carbs)
                .any { !it.isFinite() || it <= 0.0 }) return Completion(false, false)
        val total = Calculator.aggregateDay(day)
        val macros = total.protein >= targets.protein && total.fat >= targets.fat && total.carbs >= targets.carbs
        val calories = total.calories / targets.calories * 100.0
        return Completion(macros, macros && calories in 90.0..110.0)
    }
}
