package com.example.nutrition.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.example.nutrition.ui.theme.TextPrimary
import com.example.nutrition.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.sign

/** All scrolling pages share navigation visibility; content keeps consuming the scroll. */
@Stable
class PageChrome(private val threshold: Float) {
    data class Notice(val id: Int, val message: String, val celebration: Boolean)
    var notice by mutableStateOf<Notice?>(null)
    private var feedbackId = 0
    fun showFeedback(message: String, celebration: Boolean) {
        visible = true
        notice = Notice(++feedbackId, message, celebration)
    }

    var visible by mutableStateOf(true)
    var collapsed by mutableStateOf(false)
    var titleCollapseFraction by mutableFloatStateOf(0f)
        private set
    var editing by mutableStateOf(false)
    var keyboardOpen by mutableStateOf(false)
    var topBarBottom by mutableFloatStateOf(0f)
    private var distance = 0f

    // Use consumed content scroll, never finger deltas or clipped layout bounds.
    fun updateTitleScroll(offsetPx: Float, collapseDistancePx: Float) {
        titleCollapseFraction = (offsetPx / collapseDistancePx.coerceAtLeast(1f)).coerceIn(0f, 1f)
        collapsed = titleCollapseFraction >= 1f
        if (offsetPx <= 0f) visible = true
    }

    val scrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.UserInput && !editing && !keyboardOpen) {
                if (available.y.sign != distance.sign) distance = 0f
                distance += available.y
                if (abs(distance) >= threshold) {
                    visible = distance > 0
                    distance = 0f
                }
            }
            return Offset.Zero
        }
    }

    fun reset() {
        visible = true
        collapsed = false
        titleCollapseFraction = 0f
        editing = false
        distance = 0f
        notice = null
    }
}

val LocalPageChrome = staticCompositionLocalOf<PageChrome> { error("Page requires MainScreen") }
// Insets belong inside each scroll container, so hiding an overlay never resizes its viewport.
val LocalPageContentPadding = staticCompositionLocalOf { PaddingValues() }
val LocalRootPage = staticCompositionLocalOf { false }
// Initial page spacing and fixed toolbar clearance serve different purposes.
val LocalPageTopBarHeight = staticCompositionLocalOf { 48.dp }

@Composable
fun TrackRootTitleScroll(scrollOffset: () -> Float) {
    val chrome = LocalPageChrome.current
    val currentOffset by rememberUpdatedState(scrollOffset)
    val collapseDistance = with(LocalDensity.current) { MaterialTheme.typography.headlineLarge.lineHeight.toPx() }
    LaunchedEffect(chrome, collapseDistance) {
        snapshotFlow { currentOffset() }.collect { chrome.updateTitleScroll(it, collapseDistance) }
    }
}

@Composable
fun PageTitle(
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    trackScroll: Boolean = true,
    action: (@Composable () -> Unit)? = null
) {
    val chrome = LocalPageChrome.current
    val rootPage = LocalRootPage.current
    val titleHeight = with(LocalDensity.current) { MaterialTheme.typography.headlineLarge.lineHeight.toDp() }
    Column(
        modifier = modifier.fillMaxWidth().onGloballyPositioned {
            if (trackScroll && !rootPage) {
                val bounds = it.boundsInRoot()
                chrome.collapsed = bounds.bottom <= chrome.topBarBottom
                if (bounds.top >= chrome.topBarBottom) chrome.visible = true
            }
        }.padding(top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (rootPage) {
            // MainScreen draws a single title that stays left-aligned while shrinking.
            Spacer(Modifier.height(titleHeight))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.headlineLarge, color = TextPrimary, modifier = Modifier.weight(1f))
                action?.invoke()
            }
        }
        if (subtitle.isNotEmpty()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

/** Reveal the form heading rather than its entire, potentially taller-than-screen body. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun formRevealModifier(request: Int): Modifier {
    val requester = remember { BringIntoViewRequester() }
    val density = LocalDensity.current
    val headingHeight = with(density) { 120.dp.toPx() }
    val topInset = with(density) { LocalPageTopBarHeight.current.toPx() }
    LaunchedEffect(request, topInset) {
        if (request > 0) {
            withFrameNanos { }
            // Include the toolbar clearance: it overlays the scroll viewport.
            requester.bringIntoView(Rect(0f, -topInset, 1f, headingHeight))
        }
    }
    return Modifier.bringIntoViewRequester(requester)
}
