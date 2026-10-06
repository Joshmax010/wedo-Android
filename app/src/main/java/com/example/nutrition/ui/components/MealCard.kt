package com.example.nutrition.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrition.domain.model.MealKey
import com.example.nutrition.ui.theme.BgCard
import com.example.nutrition.ui.theme.Primary
import com.example.nutrition.ui.theme.TextPlaceholder
import com.example.nutrition.ui.theme.TextPrimary

/**
 * 餐次卡片组件 —— 对应小程序 meal-card 组件
 *
 * 显示餐次名称/emoji 图标/热量/记录数/占日百分比
 * 点击时 HapticFeedback 轻触反馈 + 缩放动画
 *
 * @param mealKey 餐次枚举
 * @param calories 该餐次已摄入热量
 * @param count 该餐次记录数
 * @param dailyTarget 每日总目标热量（用于计算占比）
 * @param onClick 点击回调
 */
@Composable
fun MealCard(
    mealKey: MealKey,
    calories: Int = 0,
    count: Int = 0,
    dailyTarget: Double = 0.0,
    modifier: Modifier = Modifier,
    onClick: (MealKey) -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 点击缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "mealCardScale"
    )

    // 图标缩放动画
    val iconScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "iconScale"
    )

    // 热量占比
    val percent = if (dailyTarget > 0 && calories > 0) {
        (calories / dailyTarget * 100).toInt()
    } else {
        0
    }

    // 餐次图标背景色
    val iconBgColor = when (mealKey) {
        MealKey.BREAKFAST -> Color(0xFFFF9800).copy(alpha = 0.15f)
        MealKey.LUNCH     -> Color(0xFF4CAF50).copy(alpha = 0.15f)
        MealKey.DINNER    -> Color(0xFF2196F3).copy(alpha = 0.15f)
        MealKey.SNACK     -> Color(0xFF9C27B0).copy(alpha = 0.15f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 0.dp,
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .background(BgCard)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick(mealKey)
            }
            .scale(scale)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：图标 + 信息
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 餐次 emoji 图标
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .scale(iconScale)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mealKey.emoji,
                    fontSize = 18.sp
                )
            }

            // 餐次信息
            Column {
                Text(
                    text = mealKey.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = if (count > 0) "$count 条记录" else "未记录",
                    fontSize = 11.sp,
                    color = TextPlaceholder
                )
            }
        }

        // 右侧：热量 + 占比
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            if (calories > 0) {
                Text(
                    text = "$calories",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Text(
                    text = "kcal",
                    fontSize = 10.sp,
                    color = TextPlaceholder,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                if (percent > 0) {
                    Text(
                        text = "${percent}%",
                        fontSize = 10.sp,
                        color = TextPlaceholder,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )
                }
            } else {
                Text(
                    text = "-",
                    fontSize = 14.sp,
                    color = TextPlaceholder
                )
            }
        }
    }
}
