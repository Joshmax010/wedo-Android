package com.example.nutrition.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity: 应用元信息
 *
 * 全局唯一行（id = 1），映射 AppMeta 领域模型
 */
@Entity(tableName = "meta")
data class MetaEntity(
    @PrimaryKey
    val id: Int = 1,
    val version: String,
    val firstUseDate: String,
    val lastExportDate: String?,
    val schemaVersion: Int,
    val hasSeenGuide: Boolean
)
