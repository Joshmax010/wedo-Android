package com.example.nutrition.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * 身体记录 —— 体重/体脂等趋势数据
 *
 * 以日期字符串为主键，同一天多次记录时后者覆盖前者
 *
 * @param dateStr 日期（YYYY-MM-DD）
 * @param weightKg 体重（kg）
 * @param bodyFatPercent 体脂率（%，可选）
 * @param muscleKg 肌肉量（kg，可选）
 * @param note 备注（可选）
 * @param createdAt 创建时间
 */
@Serializable
data class BodyRecord(
    val dateStr: String,
    val weightKg: Double,
    val bodyFatPercent: Double? = null,
    val muscleKg: Double? = null,
    val note: String? = null,
    val createdAt: String = Instant.now().toString()
)
