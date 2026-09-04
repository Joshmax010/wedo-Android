package com.example.nutrition.domain.model

import kotlinx.serialization.Serializable

/**
 * 微量营养素目标 —— 对应小程序 nutrition_targets.micronutrients[] 中的单项
 *
 * 例：{ key: "vitC", name: "维生素C", unit: "mg", target: 100 }
 */
@Serializable
data class MicronutrientTarget(
    val key: String,
    val name: String,
    val unit: String,
    val target: Double
)
