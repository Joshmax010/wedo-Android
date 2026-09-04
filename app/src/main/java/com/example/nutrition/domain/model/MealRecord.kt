package com.example.nutrition.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

/**
 * 单条饮食记录 —— 对应小程序 record 对象
 *
 * 每条记录属于某个日期的某个餐次（breakfast/lunch/dinner/snack）
 */
@Serializable
data class MealRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val calories: Double,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val carbs: Double = 0.0,
    val micronutrients: List<MealMicro> = emptyList(),
    val weightGrams: Double = 100.0,
    val createdAt: String = Instant.now().toString(),
    val updatedAt: String? = null
)
