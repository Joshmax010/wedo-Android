package com.example.nutrition.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.example.nutrition.ui.theme.TextPrimary
import com.example.nutrition.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.sign

/** All scrolling pages share navigation visibility; content keeps consuming the scroll. */
@Stable
class PageChrome(private val threshold: Float) {
    var visible by mutableStateOf(true)
    var collapsed by mutableStateOf(false)
    var editing by mutableStateOf(false)
    var keyboardOpen by mutableStateOf(false)
    var topBarBottom by mutableFloatStateOf(0f)
    private var distance = 0f

    val scrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.Drag && !editing && !keyboardOpen) {
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
        collapsed = true
        editing = false
        distance = 0f
    }
}

val LocalPageChrome = staticCompositionLocalOf<PageChrome> { error("Page requires MainScreen") }

@Composable
fun PageTitle(
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    val chrome = LocalPageChrome.current
    Column(
        modifier = modifier.fillMaxWidth().onGloballyPositioned {
            chrome.collapsed = it.boundsInRoot().bottom <= chrome.topBarBottom
        }.padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.headlineLarge, color = TextPrimary, modifier = Modifier.weight(1f))
            action?.invoke()
        }
        if (subtitle.isNotEmpty()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}
