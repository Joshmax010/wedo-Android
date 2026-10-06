package com.example.nutrition.ui.components

import android.os.SystemClock
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.nutrition.domain.usecase.DateUtils
import com.example.nutrition.ui.navigation.LocalPageChrome
import com.example.nutrition.ui.navigation.LocalRootPage
import com.example.nutrition.ui.navigation.PageChrome
import com.example.nutrition.ui.theme.LocalAppearance
import com.example.nutrition.ui.theme.Primary
import com.example.nutrition.ui.theme.ProteinColor
import com.example.nutrition.ui.theme.FatColor
import com.example.nutrition.ui.theme.BgCard
import com.example.nutrition.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

import com.example.nutrition.viewmodel.UIEvent

/** One lifecycle-aware event consumer per visible page. Dialogs take priority over success feedback. */
@Composable
fun ObserveUiEvents(events: Flow<UIEvent>, blocked: Boolean = false) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val chrome = LocalPageChrome.current
    val appearance = LocalAppearance.current
    val haptic = LocalHapticFeedback.current
    var pending by remember(events) { mutableStateOf<UIEvent.SaveSuccess?>(null) }
    var started by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    var lastToastAt by remember { mutableLongStateOf(0L) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> started = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(events, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            events.collect { event ->
                when (event) {
                    is UIEvent.ShowToast -> {
                        chrome.notice = null
                        lastToastAt = SystemClock.uptimeMillis()
                        Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    }
                    is UIEvent.SaveSuccess -> pending = event
                }
            }
        }
    }
    LaunchedEffect(pending, blocked, started, lastToastAt) {
        val success = pending ?: return@LaunchedEffect
        if (blocked || !started) return@LaunchedEffect
        val toastRemaining = 2000L - (SystemClock.uptimeMillis() - lastToastAt)
        if (lastToastAt > 0 && toastRemaining > 0) delay(toastRemaining)
        val today = success.date?.takeIf(DateUtils::isToday)
        val celebrate = today != null && success.dayComplete && appearance.claimCompletion(today, wholeDay = true)
        val macros = !celebrate && today != null && success.macrosComplete && appearance.claimCompletion(today, wholeDay = false)
        chrome.showFeedback(when {
            celebrate -> "✓ 今日目标完成"
            macros -> "✓ 三大营养素达标"
            else -> "✓ 已保存"
        }, celebrate)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        pending = null
    }
}

/** Brief, non-intercepting feedback; root page names stay visible while saving. */
@Composable
fun FeedbackOverlay(chrome: PageChrome) {
    val notice = chrome.notice ?: return
    val progress = remember(notice.id) { Animatable(0f) }
    LaunchedEffect(notice.id) {
        if (notice.celebration) progress.animateTo(1f, tween(1400))
        delay(if (notice.celebration) 800 else 2200)
        if (chrome.notice?.id == notice.id) chrome.notice = null
    }
    if (LocalRootPage.current) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.TopCenter) {
            Text(
                notice.message,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.background(BgCard, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
    if (notice.celebration) {
        val colors = listOf(Primary, ProteinColor, FatColor)
        Canvas(Modifier.fillMaxWidth().height(180.dp)) {
            val fraction = progress.value
            repeat(18) { index ->
                val center = Offset(size.width * (index + 0.5f) / 18f, size.height * fraction + (index % 3) * 12.dp.toPx())
                rotate(index * 29f + fraction * 110f, center) {
                    drawRect(colors[index % colors.size].copy(alpha = 1f - fraction), center, Size(5.dp.toPx(), 9.dp.toPx()))
                }
            }
        }
    }
}
