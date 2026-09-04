package com.example.nutrition.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * DataStore 偏好存储管理
 *
 * 管理应用偏好设置（非结构化数据），与 Room 数据库分离
 * 对应小程序 app.js 中的 globalData 运行时状态
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nutrition_prefs")

class DataStoreManager(private val context: Context) {

    // ==================== Keys ====================

    private object Keys {
        val IS_FIRST_USE = booleanPreferencesKey("is_first_use")
        val HAS_SEEN_GUIDE = booleanPreferencesKey("has_seen_guide")
        val STORAGE_WARN_THRESHOLD = floatPreferencesKey("storage_warn_threshold")
        val SELECTED_DATE = stringPreferencesKey("selected_date")
    }

    // ==================== 首次使用 ====================

    /** 是否首次使用（未看过引导页） */
    val isFirstUseFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.IS_FIRST_USE] ?: true
    }

    suspend fun isFirstUse(): Boolean {
        return context.dataStore.data.first()[Keys.IS_FIRST_USE] ?: true
    }

    suspend fun setFirstUse(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_FIRST_USE] = value
        }
    }

    // ==================== 引导页 ====================

    /** 是否已看过引导页 */
    val hasSeenGuideFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HAS_SEEN_GUIDE] ?: false
    }

    suspend fun hasSeenGuide(): Boolean {
        return context.dataStore.data.first()[Keys.HAS_SEEN_GUIDE] ?: false
    }

    suspend fun setHasSeenGuide(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAS_SEEN_GUIDE] = value
        }
    }

    // ==================== 存储容量告警阈值 ====================

    /** 存储容量告警阈值（默认 0.8 = 80%） */
    val storageWarnThresholdFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.STORAGE_WARN_THRESHOLD] ?: DEFAULT_WARN_THRESHOLD
    }

    suspend fun getStorageWarnThreshold(): Float {
        return context.dataStore.data.first()[Keys.STORAGE_WARN_THRESHOLD] ?: DEFAULT_WARN_THRESHOLD
    }

    suspend fun setStorageWarnThreshold(value: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.STORAGE_WARN_THRESHOLD] = value
        }
    }

    // ==================== 跨页日期传递 ====================

    /** 临时保存选中日期（用于首页 → 录入页传值） */
    suspend fun getSelectedDate(): String? {
        return context.dataStore.data.first()[Keys.SELECTED_DATE]
    }

    suspend fun setSelectedDate(dateStr: String?) {
        context.dataStore.edit { prefs ->
            if (dateStr != null) {
                prefs[Keys.SELECTED_DATE] = dateStr
            } else {
                prefs.remove(Keys.SELECTED_DATE)
            }
        }
    }

    // ==================== 清除 ====================

    /** 清除所有偏好设置 */
    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    companion object {
        const val DEFAULT_WARN_THRESHOLD = 0.8f
    }
}
