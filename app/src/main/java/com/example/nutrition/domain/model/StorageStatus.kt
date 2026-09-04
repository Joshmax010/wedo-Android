package com.example.nutrition.domain.model

/**
 * 存储状态 —— 对应小程序 storage.getStorageStatus() 返回值
 *
 * 显示在设置页"关于"区域
 */
data class StorageStatus(
    val currentSize: Long,
    val limitSize: Long,
    val keys: Int
)

/**
 * 存储容量检查结果 —— 对应小程序 storage.checkStorageCapacity() 返回值
 *
 * 80% 阈值告警
 */
data class CapacityStatus(
    val ok: Boolean,
    val warn: Boolean,
    val currentSize: Long,
    val limitSize: Long,
    val message: String
)
