package com.example.nutrition.domain.model

import kotlinx.serialization.Serializable

/**
 * 身体档案 —— 用于 BMR/TDEE 计算
 *
 * @param gender 性别
 * @param age 年龄（岁）
 * @param heightCm 身高（厘米）
 * @param weightKg 体重（公斤）
 * @param activityLevel 活动系数
 */
@Serializable
data class BodyProfile(
    val gender: Gender,
    val age: Int,
    val heightCm: Int,
    val weightKg: Double,
    val activityLevel: ActivityLevel
)
