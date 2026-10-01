package com.example.nutrition.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.nutrition.ui.navigation.AppNavGraph
import com.example.nutrition.ui.navigation.HomeRoute
import com.example.nutrition.ui.navigation.NavigationItem
import com.example.nutrition.ui.navigation.RecordRoute
import com.example.nutrition.ui.navigation.SettingsRoute
import com.example.nutrition.ui.navigation.WeeklyRoute
import com.example.nutrition.ui.theme.TabSelected
import com.example.nutrition.ui.theme.TabUnselected

/**
 * 主界面骨架 —— Scaffold + 底部 NavigationBar + NavHost + 双击返回退出
 *
 * 对应小程序 app.json tabBar 的 4 个 Tab
 */
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current

    // 双击返回退出
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    BackHandler(enabled = true) {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < 2000L) {
            // 第二次按下，退出应用
            (context as? android.app.Activity)?.finishAffinity()
        } else {
            lastBackPressTime = now
            Toast.makeText(context, "再按一次退出", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = com.example.nutrition.ui.theme.BgMain
            ) {
                NavigationItem.entries.forEach { item ->
                    val selected = when (item) {
                        NavigationItem.HOME -> currentDestination?.hasRoute<HomeRoute>() == true
                        NavigationItem.RECORD -> currentDestination?.hasRoute<RecordRoute>() == true
                        NavigationItem.WEEKLY -> currentDestination?.hasRoute<WeeklyRoute>() == true
                        NavigationItem.SETTINGS -> currentDestination?.hasRoute<SettingsRoute>() == true
                    }
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.displayName
                            )
                        },
                        label = {
                            Text(text = item.displayName)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TabSelected,
                            selectedTextColor = TabSelected,
                            unselectedIconColor = TabUnselected,
                            unselectedTextColor = TabUnselected,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(innerPadding)) {
            AppNavGraph(navController = navController)
        }
    }
}
