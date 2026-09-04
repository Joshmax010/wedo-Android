package com.example.nutrition.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

/**
 * 食物模板 —— 用于录入时快速填充营养数据
 *
 * @param id 唯一标识
 * @param name 食物名称
 * @param calories 每份热量（kcal）
 * @param protein 蛋白质（g）
 * @param fat 脂肪（g）
 * @param carbs 碳水（g）
 * @param micronutrients 微量营养素
 * @param isPreset 是否系统预设（不可删除，可重置）
 * @param source 数据来源说明（如“每100g”“ USDA”等）
 * @param createdAt 创建时间
 */
@Serializable
data class FoodTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val calories: Double,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val carbs: Double = 0.0,
    val micronutrients: List<MealMicro> = emptyList(),
    val tags: List<String> = emptyList(),
    val isPreset: Boolean = false,
    val source: String = "",
    val createdAt: String = Instant.now().toString()
)
