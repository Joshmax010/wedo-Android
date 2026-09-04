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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutrition.domain.model.BodyRecord
import com.example.nutrition.ui.theme.Primary
import com.example.nutrition.ui.theme.TextPrimary
import com.example.nutrition.ui.theme.TextSecondary
import kotlin.math.max
import kotlin.math.min

/**
 * 体重趋势折线图
 */
@Composable
fun WeightLineChart(
    records: List<BodyRecord>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "体重趋势（kg）",
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
            if (records.size < 2) return@Canvas

            val leftPad = 40.dp.toPx()
            val rightPad = 16.dp.toPx()
            val topPad = 16.dp.toPx()
            val bottomPad = 28.dp.toPx()

            val chartW = size.width - leftPad - rightPad
            val chartH = size.height - topPad - bottomPad

            val weights = records.map { it.weightKg }
            val minWeight = weights.minOrNull() ?: 0.0
            val maxWeight = weights.maxOrNull() ?: 0.0
            val padding = max((maxWeight - minWeight) * 0.15, 2.0)
            val lower = max(0.0, minWeight - padding)
            val upper = max(lower + 1.0, maxWeight + padding)
            val range = upper - lower

            val ySteps = 4
            for (i in 0..ySteps) {
                val yVal = lower + range * i / ySteps
                val y = topPad + chartH - (chartH * i / ySteps).toFloat()
                drawLine(
                    color = Color(0xFFF0F0F0),
                    start = Offset(leftPad, y),
                    end = Offset(size.width - rightPad, y),
                    strokeWidth = 1f
                )
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        String.format("%.1f", yVal),
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

            val xStep = chartW / (records.size - 1)
            val dataPoints = records.mapIndexed { index, record ->
                val x = leftPad + xStep * index
                val ratio = (record.weightKg - lower) / range
                val y = topPad + chartH - (chartH * ratio).toFloat()
                Offset(x, y) to record
            }

            if (dataPoints.size >= 2) {
                val fillPath = Path().apply {
                    moveTo(dataPoints[0].first.x, topPad + chartH)
                    dataPoints.forEach { lineTo(it.first.x, it.first.y) }
                    lineTo(dataPoints.last().first.x, topPad + chartH)
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

                val linePath = Path().apply {
                    moveTo(dataPoints[0].first.x, dataPoints[0].first.y)
                    for (i in 1 until dataPoints.size) {
                        lineTo(dataPoints[i].first.x, dataPoints[i].first.y)
                    }
                }
                drawPath(
                    path = linePath,
                    color = Primary,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }

            dataPoints.forEach { (point, record) ->
                drawCircle(color = Primary, radius = 3.dp.toPx(), center = point)
                drawCircle(color = Color.White, radius = 1.5.dp.toPx(), center = point)

                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        String.format("%.1f", record.weightKg),
                        point.x,
                        point.y - 6.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.rgb(76, 175, 80)
                            textSize = 8.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                    drawText(
                        record.dateStr.substring(5),
                        point.x,
                        topPad + chartH + 16.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 9.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}
