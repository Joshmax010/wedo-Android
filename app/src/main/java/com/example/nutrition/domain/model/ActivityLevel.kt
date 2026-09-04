package com.example.nutrition.domain.model

import kotlinx.serialization.SerialName

/**
 * 活动系数枚举 —— 用于 TDEE 计算
 *
 * 系数参考 Mifflin-St Jeor 公式的常用乘数
 */
enum class ActivityLevel(
    val key: String,
    val displayName: String,
    val description: String,
    val coefficient: Double
) {
    @SerialName("sedentary") SEDENTARY("sedentary", "久坐", "几乎不运动", 1.2),
    @SerialName("light") LIGHT("light", "轻度", "每周运动 1-3 天", 1.375),
    @SerialName("moderate") MODERATE("moderate", "中度", "每周运动 3-5 天", 1.55),
    @SerialName("active") ACTIVE("active", "高度", "每周运动 6-7 天", 1.725),
    @SerialName("very_active") VERY_ACTIVE("very_active", "极高", "体力劳动或每天高强度训练", 1.9);

    companion object {
        fun fromKey(key: String): ActivityLevel? {
            return entries.find { it.key == key }
        }
    }
}
