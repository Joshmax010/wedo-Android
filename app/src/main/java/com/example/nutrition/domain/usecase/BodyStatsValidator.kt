package com.example.nutrition.domain.usecase

/**
 * 身体数据表单校验 —— 体重 / 体脂率 / 肌肉量
 */
object BodyStatsValidator {

    /**
     * 校验身体数据表单，返回错误信息或 null
     * @param weight 体重文本（kg，必填）
     * @param bodyFat 体脂率文本（%，选填）
     * @param muscle 肌肉量文本（kg，选填）
     */
    fun validate(
        weight: String,
        bodyFat: String,
        muscle: String
    ): String? {
        val weightValue = weight.toDoubleOrNull()
        if (weightValue == null || weightValue <= 0 || weightValue > 500) {
            return "请输入有效体重（0-500kg）"
        }

        val bodyFatValue = bodyFat.toDoubleOrNull()
        if (bodyFatValue != null && (bodyFatValue < 0 || bodyFatValue > 100)) {
            return "体脂率需在 0-100% 之间"
        }

        val muscleValue = muscle.toDoubleOrNull()
        if (muscleValue != null && (muscleValue < 0 || muscleValue > 500)) {
            return "肌肉量需在 0-500kg 之间"
        }

        return null
    }
}
