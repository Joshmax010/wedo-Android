package com.example.nutrition.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity: 身体记录
 *
 * 以日期字符串为主键，记录体重/体脂/肌肉量等
 */
@Entity(tableName = "body_records")
data class BodyRecordEntity(
    @PrimaryKey
    val dateStr: String,
    val weightKg: Double,
    val bodyFatPercent: Double? = null,
    val muscleKg: Double? = null,
    val note: String? = null,
    val createdAt: String
)
