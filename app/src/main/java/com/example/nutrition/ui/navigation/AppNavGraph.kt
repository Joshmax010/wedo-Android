package com.example.nutrition.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.nutrition.NutritionApp
import com.example.nutrition.viewmodel.SettingsViewModel
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
import com.example.nutrition.ui.screens.SettingsSection
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
                onNavigateToRecord = { mealKey, date ->
                    navController.navigate(RecordRoute(mealKey = mealKey?.key, date = date)) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        // Explicit date arguments must not be replaced by a restored route's arguments.
                        restoreState = false
                    }
                }
            )
        }
        composable<RecordRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<RecordRoute>()
            RecordScreen(initialMeal = route.mealKey?.let(MealKey::fromKey), initialDate = route.date)
        }
        composable<WeeklyRoute> {
            WeeklyScreen()
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onNavigateToTemplates = { navController.navigate(TemplatesRoute) },
                onNavigateToBodyStats = { navController.navigate(BodyStatsRoute) },
                onNavigateToTargets = { navController.navigate(NutritionSettingsRoute) },
                onNavigateToData = { navController.navigate(DataSettingsRoute) },
                onNavigateToAbout = { navController.navigate(AboutRoute) }
            )
        }
        composable<TemplatesRoute> {
            FoodTemplateScreen()
        }
        composable<BodyStatsRoute> {
            BodyStatsScreen()
        }
        composable<NutritionSettingsRoute> { entry ->
            SettingsScreen(section = SettingsSection.TARGETS, viewModel = settingsViewModel(navController, entry))
        }
        composable<DataSettingsRoute> { entry ->
            SettingsScreen(section = SettingsSection.DATA, viewModel = settingsViewModel(navController, entry))
        }
        composable<AboutRoute> { entry ->
            SettingsScreen(section = SettingsSection.ABOUT, viewModel = settingsViewModel(navController, entry))
        }
    }
}

/** Subpages share the parent settings draft and its existing save/import logic. */
@Composable
private fun settingsViewModel(navController: NavHostController, entry: NavBackStackEntry): SettingsViewModel {
    val parent = remember(entry) { navController.getBackStackEntry(SettingsRoute) }
    return viewModel(viewModelStoreOwner = parent, factory = viewModelFactory {
        initializer { SettingsViewModel(NutritionApp.instance.repository, NutritionApp.instance.backupManager) }
    })
}
