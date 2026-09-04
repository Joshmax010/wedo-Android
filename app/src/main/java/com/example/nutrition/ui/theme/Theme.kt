package com.example.nutrition.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Material3 浅色主题 —— 对应小程序 app.wxss 色彩体系
 */

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = TextInverse,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryDark,
    secondary = ProteinColor,
    onSecondary = TextInverse,
    tertiary = FatColor,
    onTertiary = TextInverse,
    error = Error,
    onError = TextInverse,
    background = BgMain,
    onBackground = TextPrimary,
    surface = BgCard,
    onSurface = TextPrimary,
    surfaceVariant = BgTag,
    onSurfaceVariant = TextSecondary,
    outline = Border,
    outlineVariant = Divider
)

@Composable
fun NutritionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        content = content
    )
}
