package com.example.nutrition.ui

import com.example.nutrition.ui.components.FeedbackOverlay
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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
    CompositionLocalProvider(LocalPageChrome provides chrome) {
        Scaffold(
            modifier = Modifier.nestedScroll(chrome.scrollConnection),
            containerColor = BgMain,
            contentColor = TextPrimary,
            topBar = {
                AnimatedVisibility(
                    visible = showChrome,
                    enter = expandVertically() + slideInVertically { -it },
                    exit = shrinkVertically() + slideOutVertically { -it }
                ) {
                    androidx.compose.material3.Surface(color = BgMain.copy(alpha = 0.94f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 12.dp)
                                .onGloballyPositioned { chrome.topBarBottom = it.boundsInRoot().bottom },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (rootPage == null) {
                                TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.width(84.dp)) {
                                    Text("‹ 返回", color = Primary)
                                }
                            } else {
                                Text("wedo", color = Primary, fontWeight = FontWeight.Bold, modifier = Modifier.width(84.dp))
                            }
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text(chrome.notice?.message ?: if (chrome.collapsed) title else "", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            }
                            Box(Modifier.width(84.dp))
                        }
                    }
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = showChrome,
                    enter = expandVertically() + slideInVertically { it },
                    exit = shrinkVertically() + slideOutVertically { it }
                ) {
                    NavigationBar(containerColor = BgMain.copy(alpha = 0.94f), tonalElevation = 0.dp) {
                        NavigationItem.entries.forEach { item ->
                            val selected = rootPage == item || rootPage == null && item == NavigationItem.SETTINGS
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected || rootPage == null) {
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
        ) { padding ->
            Box(Modifier.padding(padding)) {
                AppNavGraph(navController)
                FeedbackOverlay(chrome)
            }
        }
    }
}
