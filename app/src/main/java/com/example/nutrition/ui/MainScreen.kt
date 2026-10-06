package com.example.nutrition.ui

import com.example.nutrition.ui.components.FeedbackOverlay
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.nutrition.ui.navigation.*
import com.example.nutrition.ui.theme.*

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val context = LocalContext.current
    val density = LocalDensity.current
    val chrome = remember(density) { PageChrome(with(density) { 8.dp.toPx() }) }
    val keyboardOpen = WindowInsets.ime.getBottom(density) > 0
    SideEffect { chrome.keyboardOpen = keyboardOpen }
    LaunchedEffect(backStackEntry?.id) { chrome.reset() }

    val rootPage = when {
        destination?.hasRoute<HomeRoute>() == true -> NavigationItem.HOME
        destination?.hasRoute<RecordRoute>() == true -> NavigationItem.RECORD
        destination?.hasRoute<WeeklyRoute>() == true -> NavigationItem.WEEKLY
        destination?.hasRoute<SettingsRoute>() == true -> NavigationItem.SETTINGS
        else -> null
    }
    val title = rootPage?.displayName ?: when {
        destination?.hasRoute<TemplatesRoute>() == true -> "食物模板"
        destination?.hasRoute<BodyStatsRoute>() == true -> "身体记录"
        destination?.hasRoute<NutritionSettingsRoute>() == true -> "档案与目标"
        destination?.hasRoute<DataSettingsRoute>() == true -> "数据管理"
        destination?.hasRoute<AboutRoute>() == true -> "关于"
        else -> "设置"
    }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    BackHandler(enabled = destination?.hasRoute<HomeRoute>() == true) {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < 2000L) {
            (context as? android.app.Activity)?.finishAffinity()
        } else {
            lastBackPressTime = now
            Toast.makeText(context, "再按一次退出", Toast.LENGTH_SHORT).show()
        }
    }
    val showChrome = chrome.visible || chrome.editing || keyboardOpen || chrome.notice != null
    val bottomBarVisibility by animateFloatAsState(
        targetValue = if (showChrome) 1f else 0f,
        animationSpec = tween(180), label = "bottomBarVisibility"
    )
    val topBarHeight = maxOf(48.dp, with(density) { MaterialTheme.typography.headlineLarge.lineHeight.toDp() } + 8.dp)
    val topBarColor = BgMain
    val expandedTitleOffset = with(density) { 8.dp.toPx() }
    val pagePadding = PaddingValues(
        top = if (rootPage != null) 8.dp else topBarHeight,
        bottom = if (rootPage != null && !keyboardOpen) 96.dp else 16.dp
    )
    CompositionLocalProvider(
        LocalPageChrome provides chrome,
        LocalPageContentPadding provides pagePadding,
        LocalRootPage provides (rootPage != null),
        LocalPageTopBarHeight provides topBarHeight
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding().nestedScroll(chrome.scrollConnection),
            containerColor = BgMain,
            contentColor = TextPrimary,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).clipToBounds()
                .onGloballyPositioned { chrome.topBarBottom = it.positionInRoot().y + with(density) { topBarHeight.toPx() } }) {
                AppNavGraph(navController)
                Box(Modifier.padding(top = topBarHeight)) { FeedbackOverlay(chrome) }
                // Only draw/transform the header; scrolling never resizes the viewport.
                Box(
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().drawBehind {
                        val opacity = if (rootPage == null) 0.94f else 1f
                        drawRect(topBarColor.copy(alpha = opacity))
                    }
                ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(topBarHeight).padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (rootPage == null) {
                                TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.width(84.dp)) {
                                    Text("‹ 返回", color = Primary)
                                }
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    Text(chrome.notice?.message ?: if (chrome.collapsed) title else "", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                }
                                Box(Modifier.width(84.dp))
                            } else {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).semantics { heading() }.graphicsLayer {
                                        val fraction = chrome.titleCollapseFraction
                                        val scale = 1f - fraction * (1f - 20f / 32f)
                                        scaleX = scale
                                        scaleY = scale
                                        transformOrigin = TransformOrigin(0f, 0.5f)
                                        translationY = expandedTitleOffset * (1f - fraction)
                                    }
                                )
                                TextButton(
                                    onClick = {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Joshmax010/wedo-Android")))
                                        } catch (_: ActivityNotFoundException) {
                                            Toast.makeText(context, "未找到可打开链接的应用", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "打开 WeDo 的 GitHub 项目" }
                                ) {
                                    Text("wedo", style = MaterialTheme.typography.headlineMedium, color = Primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                }
                // Secondary settings pages use their back button and never reserve a tab-bar area.
                if (rootPage != null && !keyboardOpen) {
                    NavigationBar(
                        containerColor = BgMain.copy(alpha = 0.94f), tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        modifier = Modifier.align(Alignment.BottomCenter).graphicsLayer {
                            translationY = (1f - bottomBarVisibility) * size.height
                            alpha = bottomBarVisibility
                        }
                    ) {
                        NavigationItem.entries.forEach { item ->
                            val selected = rootPage == item
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    chrome.visible = true
                                },
                                icon = { Icon(if (selected) item.selectedIcon else item.unselectedIcon, contentDescription = item.displayName) },
                                label = { Text(item.displayName) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TabSelected, selectedTextColor = TabSelected,
                                    unselectedIconColor = TabUnselected, unselectedTextColor = TabUnselected,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
