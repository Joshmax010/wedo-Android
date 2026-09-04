package com.example.nutrition.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity: 某天饮食记录
 *
 * 主键为日期字符串 YYYY-MM-DD，每餐以 JSON 字符串存储 List<MealRecord>
 */
@Entity(tableName = "day_records")
data class DayRecordEntity(
    @PrimaryKey
    val dateStr: String,
    val breakfastJson: String = "[]",
    val lunchJson: String = "[]",
    val dinnerJson: String = "[]",
    val snackJson: String = "[]"
)
