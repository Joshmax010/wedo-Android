package com.example.nutrition.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrition.ui.theme.CarbsColor
import com.example.nutrition.ui.theme.FatColor
import com.example.nutrition.ui.theme.ProteinColor
import com.example.nutrition.ui.theme.TextPrimary
import com.example.nutrition.ui.theme.TextSecondary
import com.example.nutrition.viewmodel.WeeklyViewModel
import kotlin.math.max

/**
 * 宏量营养素分组柱状图 —— 对应小程序 chart.js getNutrientBarOption
 *
 * 7 天 × 3 组柱状（蛋白质/脂肪/碳水），底部图例
 */
@Composable
fun NutrientBarChart(
    bars: List<WeeklyViewModel.NutrientBar>,
    modifier: Modifier = Modifier
) {
    val gridColor = com.example.nutrition.ui.theme.Divider
    val labelColor = TextSecondary
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "宏量营养素每日摄入",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            if (bars.isEmpty()) return@Canvas

            val leftPad = 36.dp.toPx()
            val rightPad = 12.dp.toPx()
            val topPad = 8.dp.toPx()
            val bottomPad = 28.dp.toPx()

            val chartW = size.width - leftPad - rightPad
            val chartH = size.height - topPad - bottomPad

            // 计算 Y 轴最大值
            val maxVal = maxOf(
                bars.maxOf { max(max(it.protein, it.fat), it.carbs) },
                10.0
            )
            val yScale = chartH / (maxVal * 1.15).toFloat()

            // Y 轴刻度
            val ySteps = 4
            for (i in 0..ySteps) {
                val yVal = (maxVal * 1.15 / ySteps * i).toInt()
                val y = topPad + chartH - yVal * yScale
                drawLine(
                    color = gridColor,
                    start = Offset(leftPad, y),
                    end = Offset(size.width - rightPad, y),
                    strokeWidth = 1f
                )
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        yVal.toString(),
                        leftPad - 4.dp.toPx(),
                        y + 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = labelColor.toArgb()
                            textSize = 9.sp.toPx()
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                    )
                }
            }

            // 柱状图绘制
            val groupCount = bars.size
            val groupWidth = chartW / groupCount
            val barWidth = groupWidth * 0.22f
            val barGap = barWidth * 0.25f

            val colors = listOf(ProteinColor, FatColor, CarbsColor)

            bars.forEachIndexed { groupIndex, bar ->
                val groupCenterX = leftPad + groupWidth * groupIndex + groupWidth / 2

                val values = listOf(bar.protein, bar.fat, bar.carbs)
                val totalBarsWidth = 3 * barWidth + 2 * barGap
                var startX = groupCenterX - totalBarsWidth / 2

                values.forEachIndexed { barIndex, value ->
                    val barH = (value.toFloat() * yScale).coerceAtLeast(0f)
                    val x = startX + (barWidth + barGap) * barIndex
                    val y = topPad + chartH - barH

                    drawRoundRect(
                        color = colors[barIndex],
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }

                // X 轴标签
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        bar.label,
                        groupCenterX,
                        topPad + chartH + 16.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = labelColor.toArgb()
                            textSize = 9.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }

        // 图例
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = ProteinColor, label = "蛋白质")
            LegendItem(color = FatColor, label = "脂肪", modifier = Modifier.padding(start = 16.dp))
            LegendItem(color = CarbsColor, label = "碳水", modifier = Modifier.padding(start = 16.dp))
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp, 8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary
        )
    }
}
