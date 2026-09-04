package com.example.nutrition.domain.model

import kotlinx.serialization.SerialName

/**
 * 性别枚举 —— 用于代谢计算
 */
enum class Gender(
    val key: String,
    val displayName: String
) {
    @SerialName("male") MALE("male", "男"),
    @SerialName("female") FEMALE("female", "女");

    companion object {
        fun fromKey(key: String): Gender? {
            return entries.find { it.key == key }
        }
    }
}
