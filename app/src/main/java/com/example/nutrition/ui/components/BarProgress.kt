package com.example.nutrition.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrition.ui.theme.Error
import com.example.nutrition.ui.theme.Primary
import com.example.nutrition.ui.theme.TextPlaceholder
import com.example.nutrition.ui.theme.TextPrimary
import com.example.nutrition.ui.theme.TextSecondary
import com.example.nutrition.ui.theme.Warning
import kotlin.math.abs
import kotlin.math.min

/**
 * 条形进度组件 —— 对应小程序 bar-progress 组件
 *
 * 支持当前/目标/标签/单位显示
 * 数值滚动动画 easeOutQuad 500ms
 * 超量时红色 + 条纹滚动动画
 * mini 模式用于微量营养素网格
 *
 * @param current 当前值
 * @param target 目标值
 * @param label 标签名称
 * @param unit 单位
 * @param mini 是否迷你模式
 */
@Composable
fun BarProgress(
    current: Double,
    target: Double,
    label: String = "",
    unit: String = "",
    modifier: Modifier = Modifier,
    mini: Boolean = false,
    color: Color = Primary,
    warningColor: Color = Warning,
    overflowColor: Color = Error,
    warningThreshold: Float = 80f
) {
    val trackColor = com.example.nutrition.ui.theme.BgTag
    // 计算进度
    val rawPercent = if (target > 0) (current / target * 100).toInt() else 0
    val isOverflow = rawPercent > 100
    val isWarning = !isOverflow && rawPercent >= warningThreshold
    val displayPercent = min(rawPercent, 100)

    val barColor = when {
        isOverflow -> overflowColor
        isWarning -> warningColor
        else -> color
    }

    // 数值滚动动画
    val animatedCurrent by animateIntAsState(
        targetValue = (current * 10).toInt(),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "barCurrent"
    )
    val displayCurrent = animatedCurrent / 10.0

    // 剩余量文本
    val remaining = ((target - current) * 10).toInt() / 10.0
    val remainingText = when {
        remaining > 0 -> "剩 $remaining$unit"
        remaining < 0 -> "超 ${abs(remaining)}$unit"
        else -> "已达标"
    }

    // 条纹动画偏移（超量时启用）
    val stripeOffset = if (isOverflow) {
        val transition = rememberInfiniteTransition(label = "stripe")
        val offset by transition.animateFloat(
            initialValue = 0f,
            targetValue = 28f,
            animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
            label = "stripeOffset"
        )
        offset
    } else 0f

    // 尺寸参数
    val trackHeight: Dp = if (mini) 5.dp else 8.dp
    val cornerRadius: Dp = if (mini) 3.dp else 4.dp
    val labelFontSize = if (mini) 11.sp else 14.sp
    val valueFontSize = if (mini) 11.sp else 14.sp
    val targetFontSize = if (mini) 10.sp else 12.sp

    Column(modifier = modifier.fillMaxWidth()) {
        // 标签行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = label,
                fontSize = labelFontSize,
                fontWeight = if (mini) FontWeight.Normal else FontWeight.Medium,
                color = TextPrimary
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatNumber(displayCurrent),
                    fontSize = valueFontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "/",
                    fontSize = targetFontSize,
                    color = TextPlaceholder
                )
                Text(
                    text = "${formatNumber(target)}$unit",
                    fontSize = targetFontSize,
                    color = TextPlaceholder
                )
            }
        }

        // 进度条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(RoundedCornerShape(cornerRadius))
        ) {
            // 轨道背景
            Canvas(modifier = Modifier.fillMaxWidth().height(trackHeight)) {
                drawRect(
                    color = trackColor,
                    size = Size(this.size.width, this.size.height)
                )
            }

            // 填充条
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = displayPercent / 100f)
                    .height(trackHeight)
                    .clip(RoundedCornerShape(cornerRadius))
                    .drawBehind {
                        // 底色
                        drawRect(color = barColor)
                        // 超量时绘制条纹
                        if (isOverflow) {
                            val stripeWidth = 10.dp.toPx()
                            val stripeGap = 10.dp.toPx()
                            val totalStripe = stripeWidth + stripeGap
                            drawRect(color = barColor)
                            // 45度条纹
                            val h = this.size.height
                            val w = this.size.width
                            var x = -stripeOffset - w
                            while (x < w + stripeWidth) {
                                drawLine(
                                    color = Color.White.copy(alpha = 0.25f),
                                    start = Offset(x, 0f),
                                    end = Offset(x + h, h),
                                    strokeWidth = stripeWidth
                                )
                                x += totalStripe
                            }
                        }
                    }
            )
        }

        // 剩余量（非 mini 模式）
        if (!mini) {
            Text(
                text = remainingText,
                fontSize = 11.sp,
                color = if (isOverflow) Error else TextPlaceholder,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

private fun formatNumber(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format("%.1f", value)
    }
}
