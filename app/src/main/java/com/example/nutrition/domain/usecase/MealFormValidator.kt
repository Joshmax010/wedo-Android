package com.example.nutrition.domain.usecase

/**
 * 录入表单校验 —— 对应小程序 saveRecord 中的校验逻辑
 *
 * 与 UI 解耦的纯逻辑，ViewModel 只需传入表单文本即可得到错误提示
 */
object MealFormValidator {

    /** 微量营养素行输入（与 ViewModel 的表单行类型解耦） */
    data class MicroInput(
        val name: String,
        val value: String
    )

    /**
     * 校验表单字段，返回错误信息或 null
     */
    fun validate(
        calories: String,
        protein: String,
        fat: String,
        carbs: String,
        micros: List<MicroInput> = emptyList()
    ): String? {
        // 热量必填
        val cal = calories.toDoubleOrNull()
        if (cal == null || cal < 0) return "热量需为非负数"
        if (cal > 5000) return "热量不应超过 5000"

        // 蛋白质/脂肪/碳水选填，填了需合法
        if (protein.isNotEmpty()) {
            val p = protein.toDoubleOrNull()
            if (p == null || p < 0 || p > 500) return "蛋白质需为 0-500"
        }
        if (fat.isNotEmpty()) {
            val f = fat.toDoubleOrNull()
            if (f == null || f < 0 || f > 500) return "脂肪需为 0-500"
        }
        if (carbs.isNotEmpty()) {
            val c = carbs.toDoubleOrNull()
            if (c == null || c < 0 || c > 500) return "碳水需为 0-500"
        }

        // 微量营养素校验
        for (mn in micros) {
            if (mn.value.isNotEmpty()) {
                val v = mn.value.toDoubleOrNull()
                if (v == null || v < 0) return "${mn.name}需为非负数"
            }
        }

        return null
    }
}
