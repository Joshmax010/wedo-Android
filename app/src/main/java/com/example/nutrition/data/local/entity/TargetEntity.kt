package com.example.nutrition.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity: 营养目标配置
 *
 * 全局唯一行（id = 1），映射 NutritionTargets 领域模型
 */
@Entity(tableName = "targets")
data class TargetEntity(
    @PrimaryKey
    val id: Int = 1,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    /** JSON 序列化的 List<MicronutrientTarget> */
    val micronutrientsJson: String,
    val updatedAt: String,
    /** JSON 序列化的 BodyProfile（可为空） */
    val bodyProfileJson: String? = null,
    /** 是否由代谢公式自动算出 */
    val isAutoCalculated: Boolean = false
)
