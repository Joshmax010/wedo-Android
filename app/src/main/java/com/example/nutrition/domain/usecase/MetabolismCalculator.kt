package com.example.nutrition.domain.usecase

import com.example.nutrition.domain.model.ActivityLevel
import com.example.nutrition.domain.model.BodyProfile
import com.example.nutrition.domain.model.Gender
import com.example.nutrition.domain.model.NutritionTargets
import kotlin.math.roundToInt

/**
 * 代谢计算器 —— 基于 Mifflin-St Jeor 公式计算 BMR/TDEE
 *
 * 纯函数，输入身体档案，输出推荐营养目标
 */
object MetabolismCalculator {

    /** 计算结果 */
    data class Result(
        val bmr: Double,
        val tdee: Double,
        val targets: NutritionTargets
    )

    /**
     * 根据身体档案计算推荐营养目标
     *
     * @param bodyProfile 身体档案
     * @param micronutrients 微量营养素目标（沿用现有设置）
     * @return 包含 BMR、TDEE 与推荐目标的结果
     */
    fun calculate(
        bodyProfile: BodyProfile,
        micronutrients: List<com.example.nutrition.domain.model.MicronutrientTarget>
    ): Result {
        val bmr = calculateBmr(bodyProfile)
        val tdee = bmr * bodyProfile.activityLevel.coefficient

        val targets = buildTargets(tdee.roundToInt().toDouble(), bodyProfile, micronutrients)

        return Result(
            bmr = roundOne(bmr),
            tdee = roundOne(tdee),
            targets = targets
        )
    }

    /**
     * 仅计算 BMR（基础代谢率）
     */
    fun calculateBmr(bodyProfile: BodyProfile): Double {
        val weight = bodyProfile.weightKg
        val height = bodyProfile.heightCm.toDouble()
        val age = bodyProfile.age

        return when (bodyProfile.gender) {
            Gender.MALE -> 10.0 * weight + 6.25 * height - 5.0 * age + 5.0
            Gender.FEMALE -> 10.0 * weight + 6.25 * height - 5.0 * age - 161.0
        }
    }

    /**
     * 仅计算 TDEE（每日总能量消耗）
     */
    fun calculateTdee(bodyProfile: BodyProfile): Double {
        val bmr = calculateBmr(bodyProfile)
        return bmr * bodyProfile.activityLevel.coefficient
    }

    /**
     * 根据 TDEE 和身体档案构建推荐营养目标
     *
     * 宏量拆分策略：
     * - 蛋白质：按体重 1.8 g/kg（上限 2.4 g/kg），用于一般健身/减脂人群
     * - 脂肪：占 TDEE 28%
     * - 碳水：用剩余热量补足
     */
    private fun buildTargets(
        calories: Double,
        bodyProfile: BodyProfile,
        micronutrients: List<com.example.nutrition.domain.model.MicronutrientTarget>
    ): NutritionTargets {
        val protein = (bodyProfile.weightKg * 1.8).coerceIn(50.0, 240.0)
        val fat = (calories * 0.28 / 9.0).coerceIn(30.0, 120.0)
        val carbs = (calories - protein * 4.0 - fat * 9.0) / 4.0

        return NutritionTargets(
            calories = calories,
            protein = roundOne(protein),
            fat = roundOne(fat),
            carbs = roundOne(carbs.coerceAtLeast(30.0)),
            micronutrients = micronutrients,
            updatedAt = java.time.Instant.now().toString(),
            bodyProfile = bodyProfile,
            isAutoCalculated = true
        )
    }

    /** 四舍五入到 1 位小数 */
    private fun roundOne(value: Double): Double {
        return (value * 10).roundToInt() / 10.0
    }
}
