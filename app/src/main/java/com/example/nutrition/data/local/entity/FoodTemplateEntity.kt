package com.example.nutrition.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity: 食物模板
 *
 * 每条模板一行，JSON 序列化保存微量营养素列表
 */
@Entity(tableName = "food_templates")
data class FoodTemplateEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    /** JSON 序列化的 List<MealMicro> */
    val micronutrientsJson: String,
    /** JSON 序列化的 List<String> */
    val tagsJson: String = "[]",
    /** 是否系统预设 */
    val isPreset: Boolean = false,
    val source: String = "",
    val createdAt: String
)
