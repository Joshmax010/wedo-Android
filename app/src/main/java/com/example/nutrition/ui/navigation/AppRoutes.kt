package com.example.nutrition.ui.navigation

import kotlinx.serialization.Serializable

/**
 * 应用路由定义 —— 基于 Compose Navigation 类型安全 API（navigation 2.8+）
 *
 * 所有路由均为 @Serializable 对象/数据类，替代原来的字符串路由；
 * 参数（如餐次）通过构造属性直接传递，无需全局 AppState 中转
 */

/** 所有路由的公共标记接口 */
interface AppRoute

/** 底部导航 - 总览 */
@Serializable
object HomeRoute : AppRoute

/**
 * 底部导航 - 录入
 * @param mealKey 从首页带入的餐次 key（breakfast/lunch/dinner/snack），为 null 时按当前时间推断
 */
@Serializable
data class RecordRoute(val mealKey: String? = null) : AppRoute

/** 底部导航 - 周报 */
@Serializable
object WeeklyRoute : AppRoute

/** 底部导航 - 设置 */
@Serializable
object SettingsRoute : AppRoute

/** 设置内二级页 - 食物模板 */
@Serializable
object TemplatesRoute : AppRoute

/** 设置内二级页 - 身体数据 */
@Serializable
object BodyStatsRoute : AppRoute
