package com.example.nutrition.ui.theme

import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF507755), onPrimary = Color.White,
    primaryContainer = Color(0xFFE7EEE5), onPrimaryContainer = Color(0xFF355139),
    secondary = Color(0xFF6575AB), tertiary = Color(0xFF946D2F),
    error = Color(0xFFB36155), onError = Color.White,
    background = Color.White, onBackground = Color(0xFF202124),
    surface = Color(0xFFF5F5F7), onSurface = Color(0xFF202124),
    surfaceVariant = Color(0xFFEBEBEE), onSurfaceVariant = Color(0xFF68686C),
    outline = Color(0xFFB5B5BA), outlineVariant = Color(0xFFE5E5E8)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FBEA0), onPrimary = Color(0xFF141414),
    primaryContainer = Color(0xFF303E31), onPrimaryContainer = Color(0xFFD6E6D5),
    secondary = Color(0xFFA4AFE0), tertiary = Color(0xFFD0AB73),
    error = Color(0xFFDE9789), onError = Color(0xFF141414),
    background = Color(0xFF141414), onBackground = Color.White,
    surface = Color(0xFF202020), onSurface = Color.White,
    surfaceVariant = Color(0xFF2B2B2D), onSurfaceVariant = Color(0xFFB8B8BC),
    outline = Color(0xFF737378), outlineVariant = Color(0xFF38383A)
)

@Composable
fun NutritionTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val appearance = remember(context.applicationContext) { AppearancePreferences(context) }
    val dark = when (appearance.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors
    DisposableEffect(context, dark) {
        (context as? ComponentActivity)?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.WHITE, 0xFF141414.toInt()) { dark }
        )
        onDispose { }
    }
    CompositionLocalProvider(LocalAppearance provides appearance) {
        MaterialTheme(
            colorScheme = colors,
            typography = AppTypography,
            shapes = Shapes(medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(20.dp))
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.onBackground, content = content)
        }
    }
}

/** Filled, quiet fields; a focused field keeps a clear accent outline. */
@Composable
fun nutritionFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    focusedContainerColor = BgTag, unfocusedContainerColor = BgTag,
    focusedBorderColor = Primary, unfocusedBorderColor = Color.Transparent,
    focusedLabelColor = Primary, unfocusedLabelColor = TextSecondary,
    focusedPlaceholderColor = TextPlaceholder, unfocusedPlaceholderColor = TextPlaceholder,
    cursorColor = Primary
)
