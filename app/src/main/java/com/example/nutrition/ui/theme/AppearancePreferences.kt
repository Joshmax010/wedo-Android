package com.example.nutrition.ui.theme

import android.content.Context
import androidx.core.content.edit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"), LIGHT("浅色"), DARK("深色")
}

/** Device appearance is separate from portable nutrition backups. */
class AppearancePreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    var mode by mutableStateOf(
        ThemeMode.entries.firstOrNull { it.name == preferences.getString("theme", null) } ?: ThemeMode.SYSTEM
    )
        private set

    fun select(mode: ThemeMode) {
        preferences.edit { putString("theme", mode.name) }
        this.mode = mode
    }
    /** Local, persistent deduplication; imports never invoke these methods. */
    fun claimCompletion(date: String, wholeDay: Boolean): Boolean {
        val key = if (wholeDay) "lastCelebratedDate" else "lastMacroFeedbackDate"
        if (preferences.getString(key, null) == date) return false
        preferences.edit {
            putString(key, date)
            if (wholeDay) putString("lastMacroFeedbackDate", date)
        }
        return true
    }
}

val LocalAppearance = staticCompositionLocalOf<AppearancePreferences> {
    error("Appearance requires NutritionTheme")
}
