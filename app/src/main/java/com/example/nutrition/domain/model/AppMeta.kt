package com.example.nutrition.domain.model

import kotlinx.serialization.Serializable

/**
 * 应用元信息 —— 对应小程序 nutrition_meta
 *
 * 含版本号、首次使用日期、上次导出时间、引导状态
 */
@Serializable
data class AppMeta(
    val version: String = "1.0.0",
    val firstUseDate: String,
    val lastExportDate: String? = null,
    val schemaVersion: Int = 1,
    val hasSeenGuide: Boolean = false
)
