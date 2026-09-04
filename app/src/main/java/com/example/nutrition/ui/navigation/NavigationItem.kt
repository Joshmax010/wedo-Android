package com.example.nutrition.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 底部导航 Tab 枚举 —— 对应小程序 app.json tabBar.list
 *
 * 4 个固定 Tab：总览 / 录入 / 周报 / 设置
 * route 持有类型安全路由对象（替代原来的字符串路由）
 */
enum class NavigationItem(
    val route: AppRoute,
    val displayName: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(
        route = HomeRoute,
        displayName = "总览",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    RECORD(
        route = RecordRoute(),
        displayName = "录入",
        selectedIcon = Icons.Filled.AddCircle,
        unselectedIcon = Icons.Outlined.AddCircle
    ),
    WEEKLY(
        route = WeeklyRoute,
        displayName = "周报",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart
    ),
    SETTINGS(
        route = SettingsRoute,
        displayName = "设置",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}
