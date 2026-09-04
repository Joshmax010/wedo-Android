package com.example.nutrition.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrition.ui.theme.Primary
import com.example.nutrition.ui.theme.TextPrimary
import com.example.nutrition.ui.theme.TextSecondary
import com.example.nutrition.ui.theme.TextPlaceholder
import com.example.nutrition.ui.theme.Warning
import com.example.nutrition.viewmodel.WeeklyViewModel

/**
 * 每日热量折线图 —— 对应小程序 chart.js getCaloriesLineOption
 *
 * 7 天热量折线 + 目标虚线 + 渐变填充 + 数据点标注
 */
@Composable
fun CaloriesLineChart(
    points: List<WeeklyViewModel.CaloriePoint>,
    targetCalories: Double,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "每日热量摄入",
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
            if (points.isEmpty()) return@Canvas

            val leftPad = 40.dp.toPx()
            val rightPad = 16.dp.toPx()
            val topPad = 16.dp.toPx()
            val bottomPad = 28.dp.toPx()

            val chartW = size.width - leftPad - rightPad
            val chartH = size.height - topPad - bottomPad

            val maxCal = maxOf(
                points.maxOf { it.calories },
                targetCalories.toInt(),
                100
            ).toDouble()
            val yScale = chartH / (maxCal * 1.15)

            // 绘制 Y 轴刻度线
            val ySteps = 4
            for (i in 0..ySteps) {
                val yVal = (maxCal * 1.15 / ySteps * i).toInt()
                val y = topPad + chartH - yVal * yScale.toFloat()
                // 网格线
                drawLine(
                    color = Color(0xFFF0F0F0),
                    start = Offset(leftPad, y),
                    end = Offset(size.width - rightPad, y),
                    strokeWidth = 1f
                )
                // Y 轴标签
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        yVal.toString(),
                        leftPad - 4.dp.toPx(),
                        y + 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 9.sp.toPx()
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                    )
                }
            }

            // 目标虚线
            if (targetCalories > 0) {
                val targetY = topPad + chartH - (targetCalories * yScale).toFloat()
                drawLine(
                    color = Warning,
                    start = Offset(leftPad, targetY),
                    end = Offset(size.width - rightPad, targetY),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(8f, 4f), 0f
                    )
                )
                // 目标标签
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        "目标 ${targetCalories.toInt()}",
                        size.width - rightPad - 2.dp.toPx(),
                        targetY - 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.rgb(255, 152, 0)
                            textSize = 9.sp.toPx()
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                    )
                }
            }

            // X 轴数据点坐标
            val xStep = if (points.size > 1) chartW / (points.size - 1) else chartW
            val dataPoints = points.mapIndexed { index, point ->
                val x = leftPad + xStep * index
                val y = topPad + chartH - (point.calories * yScale).toFloat()
                Offset(x, y)
            }

            // 渐变填充区域
            if (dataPoints.size >= 2) {
                val fillPath = Path().apply {
                    moveTo(dataPoints[0].x, topPad + chartH)
                    dataPoints.forEach { lineTo(it.x, it.y) }
                    lineTo(dataPoints.last().x, topPad + chartH)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.25f),
                            Primary.copy(alpha = 0.02f)
                        )
                    )
                )
            }

            // 折线
            if (dataPoints.size >= 2) {
                val linePath = Path().apply {
                    moveTo(dataPoints[0].x, dataPoints[0].y)
                    for (i in 1 until dataPoints.size) {
                        lineTo(dataPoints[i].x, dataPoints[i].y)
                    }
                }
                drawPath(
                    path = linePath,
                    color = Primary,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2.dp.toPx()
                    )
                )
            }

            // 数据点圆点 + X 轴标签
            dataPoints.forEachIndexed { index, point ->
                // 圆点
                drawCircle(
                    color = Primary,
                    radius = 3.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = Color.White,
                    radius = 1.5.dp.toPx(),
                    center = point
                )

                // X 轴标签
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        points[index].label,
                        point.x,
                        topPad + chartH + 16.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 9.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }

                // 热量值标注（仅在有数据时显示）
                if (points[index].calories > 0) {
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            points[index].calories.toString(),
                            point.x,
                            point.y - 6.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.rgb(76, 175, 80)
                                textSize = 8.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }
            }
        }
    }
}
