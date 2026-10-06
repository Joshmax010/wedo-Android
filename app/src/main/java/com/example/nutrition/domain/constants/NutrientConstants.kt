package com.example.nutrition.domain.constants

import com.example.nutrition.domain.model.MicronutrientTarget
import com.example.nutrition.domain.model.AppMeta
import com.example.nutrition.domain.model.NutritionTargets
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 营养素常量定义 —— 移植自小程序 config/nutrients.js
 *
 * 全局唯一数据源，定义宏量/微量营养素默认值、餐次、工厂方法
 */
object NutrientConstants {

    const val APP_NAME = "nutrition-tracker"
    const val SCHEMA_VERSION = 3
    const val APP_VERSION = "1.4.5"

    // ==================== 宏量营养素默认值 ====================

    data class MacroDef(val key: String, val name: String, val unit: String, val target: Double)

    val CALORIES = MacroDef("calories", "热量", "kcal", 2000.0)
    val PROTEIN = MacroDef("protein", "蛋白质", "g", 120.0)
    val FAT = MacroDef("fat", "脂肪", "g", 65.0)
    val CARBS = MacroDef("carbs", "碳水", "g", 250.0)

    /** 全部宏量营养素（含热量） */
    val MACROS = listOf(CALORIES, PROTEIN, FAT, CARBS)

    // ==================== 微量营养素默认值 ====================

    data class MicroDef(val key: String, val name: String, val unit: String, val target: Double)

    val MICROS = listOf(
        MicroDef("vitA",    "维生素A", "μg", 800.0),
        MicroDef("vitB1",   "维生素B1", "mg", 1.4),
        MicroDef("vitC",    "维生素C", "mg", 100.0),
        MicroDef("vitD",    "维生素D", "μg", 10.0),
        MicroDef("vitE",    "维生素E", "mg", 14.0),
        MicroDef("calcium", "钙",      "mg", 800.0),
        MicroDef("iron",    "铁",      "mg", 12.0),
        MicroDef("zinc",    "锌",      "mg", 10.0)
    )

    // ==================== 餐次定义 ====================

    data class MealDef(val key: String, val name: String)

    val MEALS = listOf(
        MealDef("breakfast", "早餐"),
        MealDef("lunch",     "午餐"),
        MealDef("dinner",    "晚餐"),
        MealDef("snack",     "加餐")
    )

    // ==================== 工厂方法 ====================

    /**
     * 生成默认营养目标配置对象
     * 对应小程序 getDefaultTargets()
     */
    fun getDefaultTargets(): NutritionTargets {
        return NutritionTargets(
            calories = CALORIES.target,
            protein = PROTEIN.target,
            fat = FAT.target,
            carbs = CARBS.target,
            micronutrients = MICROS.map { micro ->
                MicronutrientTarget(
                    key = micro.key,
                    name = micro.name,
                    unit = micro.unit,
                    target = micro.target
                )
            },
            updatedAt = Instant.now().toString(),
            bodyProfile = null,
            isAutoCalculated = false
        )
    }

    /**
     * 生成默认元信息对象
     * 对应小程序 getDefaultMeta()
     */
    fun getDefaultMeta(): AppMeta {
        val today = LocalDate.now()
        val dateStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        return AppMeta(
            version = APP_VERSION,
            firstUseDate = dateStr,
            lastExportDate = null,
            schemaVersion = SCHEMA_VERSION,
            hasSeenGuide = false
        )
    }

    // ==================== 辅助方法 ====================

    /**
     * 根据 key 查找微量营养素中文名
     * 用于 RecordItem 组件显示中文名（如 calcium → 钙）
     */
    fun getMicroName(key: String): String {
        return MICROS.find { it.key == key }?.name ?: key
    }

    /**
     * 根据 key 查找微量营养素单位
     */
    fun getMicroUnit(key: String): String {
        return MICROS.find { it.key == key }?.unit ?: ""
    }

    /**
     * 根据 key 查找宏量营养素定义
     */
    fun getMacro(key: String): MacroDef? {
        return MACROS.find { it.key == key }
    }
}
