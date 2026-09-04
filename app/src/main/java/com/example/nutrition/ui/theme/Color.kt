package com.example.nutrition.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 颜色常量 —— 对应小程序 app.wxss CSS 变量
 *
 * 主色系、营养素色系、功能色、文字色、背景色
 */

// ==================== 主色系 ====================

val Primary         = Color(0xFF4CAF50)  // --color-primary
val PrimaryLight    = Color(0xFF81C784)  // --color-primary-light
val PrimaryDark     = Color(0xFF388E3C)  // --color-primary-dark

// ==================== 营养素色 ====================

val ProteinColor    = Color(0xFF42A5F5)  // 蛋白质蓝
val FatColor        = Color(0xFFFFA726)  // 脂肪橙
val CarbsColor      = Color(0xFF66BB6A)  // 碳水绿

// ==================== 功能色 ====================

val Warning         = Color(0xFFFF9800)  // --color-warning
val Error           = Color(0xFFF44336)  // --color-danger
val Info            = Color(0xFF2196F3)  // --color-info

// ==================== 背景色 ====================

val BgMain          = Color(0xFFF5F5F5)  // --color-bg
val BgCard          = Color(0xFFFFFFFF)  // --color-bg-card
val BgTag           = Color(0xFFF0F0F0)  // --color-bg-tag

// ==================== 文字色 ====================

val TextPrimary     = Color(0xFF333333)  // --color-text-primary
val TextSecondary   = Color(0xFF666666)  // --color-text-secondary
val TextPlaceholder = Color(0xFF999999)  // --color-text-placeholder
val TextInverse     = Color(0xFFFFFFFF)  // --color-text-inverse

// ==================== 边框/分割线 ====================

val Border          = Color(0xFFE0E0E0)  // --color-border
val Divider         = Color(0xFFF0F0F0)  // --color-divider

// ==================== TabBar 色 ====================

val TabSelected     = Color(0xFF4CAF50)  // selectedColor
val TabUnselected   = Color(0xFF999999)  // color（未选中灰色）
