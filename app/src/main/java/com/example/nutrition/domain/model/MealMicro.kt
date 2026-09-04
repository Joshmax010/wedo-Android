package com.example.nutrition.domain.model

import kotlinx.serialization.Serializable

/**
 * 微量营养素条目 —— 对应小程序 record.micronutrients[] 中的单项
 *
 * 例：{ key: "calcium", value: 200 } 表示钙 200mg
 */
@Serializable
data class MealMicro(
    val key: String,
    val value: Double
)
