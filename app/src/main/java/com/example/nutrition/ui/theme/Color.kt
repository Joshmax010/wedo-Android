package com.example.nutrition.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Nutrient identity colors also appear in ViewModel display data.
val ProteinColor = Color(0xFF7D8DC1)
val FatColor = Color(0xFFC69A54)
val CarbsColor = Color(0xFF739D82)

// Existing UI names resolve to the active Material theme, including dark mode.
val Primary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
val Warning: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.tertiary
val Info: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.secondary
val Error: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.error
val BgMain: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.background
val BgCard: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surface
val BgTag: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceVariant
val TextPrimary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface
val TextSecondary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
val TextPlaceholder: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
val TextInverse: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onPrimary
val Border: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outline
val Divider: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outlineVariant
val TabSelected: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
val TabUnselected: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
