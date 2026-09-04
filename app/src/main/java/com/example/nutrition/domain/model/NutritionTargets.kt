package com.example.nutrition.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * 营养目标配置 —— 对应小程序 nutrition_targets
 *
 * 全局唯一，含宏量营养素目标 + 微量营养素目标列表
 * 二期新增：bodyProfile（身体档案）和 isAutoCalculated（是否由代谢公式自动算出）
 */
@Serializable
data class NutritionTargets(
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val micronutrients: List<MicronutrientTarget>,
    val updatedAt: String = Instant.now().toString(),
    val bodyProfile: BodyProfile? = null,
    val isAutoCalculated: Boolean = false
)
