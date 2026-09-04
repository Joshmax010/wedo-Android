package com.example.nutrition.domain.model

import kotlinx.serialization.Serializable

import java.time.LocalTime

/**
 * 餐次枚举 —— 对应小程序 MEALS 定义
 *
 * 四个固定时段：早餐 / 午餐 / 晚餐 / 加餐
 */
enum class MealKey(
    val key: String,
    val displayName: String,
    val emoji: String
) {
    BREAKFAST("breakfast", "早餐", "🌅"),
    LUNCH("lunch", "午餐", "☀"),
    DINNER("dinner", "晚餐", "🌙"),
    SNACK("snack", "加餐", "🍪");

    companion object {
        /** 通过 key 字符串查找枚举值，找不到返回 null */
        fun fromKey(key: String): MealKey? {
            return entries.find { it.key == key }
        }

        /** 所有 key 字符串列表 */
        val allKeys: List<String>
            get() = entries.map { it.key }

        /** 根据当前系统时间返回合适的默认餐次 */
        fun fromCurrentTime(): MealKey {
            val hour = LocalTime.now().hour
            return when (hour) {
                in 5..9 -> BREAKFAST
                in 10..13 -> LUNCH
                in 14..16 -> SNACK
                in 17..20 -> DINNER
                else -> SNACK
            }
        }
    }
}
