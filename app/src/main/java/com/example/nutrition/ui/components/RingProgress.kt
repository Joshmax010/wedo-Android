package com.example.nutrition.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrition.ui.theme.Error
import com.example.nutrition.ui.theme.Primary
import com.example.nutrition.ui.theme.TextPlaceholder
import com.example.nutrition.ui.theme.Warning
import kotlin.math.min

/**
 * 环形进度组件 —— 对应小程序 ring-progress 组件
 *
 * Canvas + drawArc 绘制轨道弧 + 进度弧
 * 三色阈值：>100% 红色、>=80% 橙色、正常绿色
 * 超量时绘制半透明溢出标记弧
 * easeOutCubic 缓动动画 600ms
 *
 * @param percent 进度百分比（0~可超过100）
 * @param centerText 中心主文字（如缺口数值）
 * @param subText 中心副文字（如 "kcal 缺口"）
 * @param size 组件尺寸
 * @param strokeWidth 环宽
 * @param animated 是否开启动画
 */
@Composable
fun RingProgress(
    percent: Float,
    centerText: String = "",
    subText: String = "",
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 12.dp,
    animated: Boolean = true,
    primaryColor: Color = Primary,
    warningColor: Color = Warning,
    overflowColor: Color = Error,
    trackColor: Color = Color(0xFFE0E0E0),
    warningThreshold: Float = 80f
) {
    // 动画目标值
    val animatedPercent by animateFloatAsState(
        targetValue = percent,
        animationSpec = if (animated) {
            tween(durationMillis = 600, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 0)
        },
        label = "ringProgress"
    )

    // 根据百分比计算当前颜色
    val currentColor = when {
        animatedPercent > 100f -> overflowColor
        animatedPercent >= warningThreshold -> warningColor
        else -> primaryColor
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Canvas 绘制
        Canvas(modifier = Modifier.size(size)) {
            val canvasSize = this.size.minDimension
            val stroke = strokeWidth.toPx()
            val radius = (canvasSize - stroke) / 2f
            val topLeft = Offset(
                (this.size.width - canvasSize) / 2f + stroke / 2f,
                (this.size.height - canvasSize) / 2f + stroke / 2f
            )
            val arcSize = Size(radius * 2, radius * 2)

            // 绘制轨道（完整圆环）
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )

            // 绘制进度弧（最多到 100%）
            val clampedPercent = min(animatedPercent, 100f).coerceAtLeast(0f)
            if (clampedPercent > 0f) {
                val sweepAngle = 360f * (clampedPercent / 100f)
                drawArc(
                    color = currentColor,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            // 超量时绘制半透明溢出标记弧（外层加粗弧）
            if (animatedPercent > 100f) {
                val overflowRatio = min((animatedPercent - 100f) / 50f, 1f)
                val overflowSweep = 360f * overflowRatio
                drawArc(
                    color = overflowColor.copy(alpha = 0.3f),
                    startAngle = -90f,
                    sweepAngle = overflowSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke + 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // 中心文字
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerText,
                color = currentColor,
                fontSize = (size.value * 0.2f).sp,
                fontWeight = FontWeight.Bold
            )
            if (subText.isNotEmpty()) {
                Text(
                    text = subText,
                    color = TextPlaceholder,
                    fontSize = (size.value * 0.09f).sp
                )
            }
        }
    }
}
