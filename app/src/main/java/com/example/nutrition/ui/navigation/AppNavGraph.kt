package com.example.nutrition.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.nutrition.domain.model.MealKey
import com.example.nutrition.ui.screens.BodyStatsScreen
import com.example.nutrition.ui.screens.FoodTemplateScreen
import com.example.nutrition.ui.screens.HomeScreen
import com.example.nutrition.ui.screens.RecordScreen
import com.example.nutrition.ui.screens.SettingsScreen
import com.example.nutrition.ui.screens.WeeklyScreen

/**
 * 应用路由图 —— 4 个 Tab 对应的 composable 路由（类型安全 API）
 *
 * HomeScreen 通过 RecordRoute(mealKey) 直接携带餐次参数跳转录入页，
 * 替代原来的 AppState.pendingMeal 全局中转
 */
@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onNavigateToRecord = { mealKey ->
                    navController.navigate(RecordRoute(mealKey = mealKey?.key)) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable<RecordRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<RecordRoute>()
            RecordScreen(initialMeal = route.mealKey?.let(MealKey::fromKey))
        }
        composable<WeeklyRoute> {
            WeeklyScreen()
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onNavigateToTemplates = { navController.navigate(TemplatesRoute) },
                onNavigateToBodyStats = { navController.navigate(BodyStatsRoute) }
            )
        }
        composable<TemplatesRoute> {
            FoodTemplateScreen()
        }
        composable<BodyStatsRoute> {
            BodyStatsScreen()
        }
    }
}
