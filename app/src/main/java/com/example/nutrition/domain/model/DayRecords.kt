package com.example.nutrition.domain.model

import kotlinx.serialization.Serializable

/**
 * 某天饮食记录 —— 对应小程序 nutrition_records["YYYY-MM-DD"]
 *
 * 含四个餐次的记录列表
 */
@Serializable
data class DayRecords(
    val dateStr: String,
    val breakfast: List<MealRecord> = emptyList(),
    val lunch: List<MealRecord> = emptyList(),
    val dinner: List<MealRecord> = emptyList(),
    val snack: List<MealRecord> = emptyList()
) {
    /** 获取指定餐次的记录列表 */
    fun getMeal(mealKey: MealKey): List<MealRecord> = when (mealKey) {
        MealKey.BREAKFAST -> breakfast
        MealKey.LUNCH -> lunch
        MealKey.DINNER -> dinner
        MealKey.SNACK -> snack
    }

    /** 获取指定餐次 key 字符串的记录列表 */
    fun getMeal(key: String): List<MealRecord> {
        val mealKey = MealKey.fromKey(key) ?: return emptyList()
        return getMeal(mealKey)
    }

    /** 当天是否有任何记录 */
    fun isEmpty(): Boolean =
        breakfast.isEmpty() && lunch.isEmpty() && dinner.isEmpty() && snack.isEmpty()

    /** 当天总记录数 */
    fun totalCount(): Int =
        breakfast.size + lunch.size + dinner.size + snack.size

    /** 返回替换指定餐次的新 DayRecords */
    fun withMeal(mealKey: MealKey, records: List<MealRecord>): DayRecords = when (mealKey) {
        MealKey.BREAKFAST -> copy(breakfast = records)
        MealKey.LUNCH -> copy(lunch = records)
        MealKey.DINNER -> copy(dinner = records)
        MealKey.SNACK -> copy(snack = records)
    }
}
