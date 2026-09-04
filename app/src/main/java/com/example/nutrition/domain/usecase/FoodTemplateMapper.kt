package com.example.nutrition.domain.usecase

import com.example.nutrition.domain.model.FoodTemplate
import com.example.nutrition.domain.model.MealRecord

/**
 * 记录 → 模板 映射 —— 将一条饮食记录折算为每 100g 基准的食物模板
 *
 * 供"保存为模板"功能使用，替代原先写在 ViewModel 中的换算逻辑
 */
object FoodTemplateMapper {

    /** 将一条饮食记录折算为每 100g 的模板 */
    fun fromMealRecord(record: MealRecord): FoodTemplate {
        val w = record.weightGrams.coerceAtLeast(0.01)
        val factor = 100.0 / w
        return FoodTemplate(
            name = record.name,
            calories = record.calories * factor,
            protein = record.protein * factor,
            fat = record.fat * factor,
            carbs = record.carbs * factor,
            micronutrients = record.micronutrients.map { micro ->
                micro.copy(value = micro.value * factor)
            },
            isPreset = false
        )
    }
}
