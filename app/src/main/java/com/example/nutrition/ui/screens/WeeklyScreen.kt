package com.example.nutrition.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import com.example.nutrition.ui.components.DataLoadError
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrition.NutritionApp
import com.example.nutrition.ui.charts.CaloriesLineChart
import com.example.nutrition.ui.charts.NutrientBarChart
import com.example.nutrition.ui.theme.BgCard
import com.example.nutrition.ui.theme.BgMain
import com.example.nutrition.ui.theme.Error
import com.example.nutrition.ui.theme.Primary
import com.example.nutrition.ui.theme.TextPrimary
import com.example.nutrition.ui.theme.TextSecondary
import com.example.nutrition.ui.theme.TextPlaceholder
import com.example.nutrition.viewmodel.WeeklyViewModel
import kotlin.math.min

/**
 * 周报页 —— 对应小程序 pages/weekly/weekly.wxml
 *
 * 周切换栏 + 折线图 + 柱状图 + 周报摘要 + 达标率 + 微量表格
 */
@Composable
fun WeeklyScreen(
    viewModel: WeeklyViewModel = viewModel(
        factory = WeeklyViewModel.Factory(NutritionApp.instance.repository)
    )
) {
    val context = LocalContext.current

    // 页面状态（单一 UiState）
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            uiState.dataError?.let { DataLoadError(it, viewModel::loadData) }
            // ========== 周切换栏 ==========
            WeekBar(
                weekRange = uiState.weekRange,
                onPrev = { viewModel.prevWeek() },
                onNext = { viewModel.nextWeek() }
            )

            if (!uiState.hasData) {
                // ========== 空状态 ==========
                if (uiState.dataError == null) EmptyState()
            } else {
                // ========== 热量折线图 ==========
                ChartCard {
                    CaloriesLineChart(
                        points = uiState.caloriePoints,
                        targetCalories = uiState.targetCalories
                    )
                }

                // ========== 营养素柱状图 ==========
                ChartCard {
                    NutrientBarChart(bars = uiState.nutrientBars)
                }

                // ========== 周报摘要 ==========
                uiState.summary?.let { summary ->
                    SummaryCard(
                        summary = summary,
                        nutrientRates = uiState.nutrientRates,
                        onCopyReport = {
                            copyToClipboard(context, uiState.reportText)
                            Toast.makeText(context, "周报已复制", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // ========== 微量营养素周览 ==========
                if (uiState.microTable.isNotEmpty()) {
                    MicroTableCard(rows = uiState.microTable)
                }

                // 底部留白
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ==================== 周切换栏 ====================

@Composable
private fun WeekBar(
    weekRange: String,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "‹",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Primary,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onPrev)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
        Text(
            text = weekRange,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Text(
            text = "›",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Primary,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onNext)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

// ==================== 空状态 ====================

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "\uD83D\uDCCA",
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "本周暂无记录数据",
            fontSize = 15.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "去录入页添加饮食记录后即可查看统计",
            fontSize = 13.sp,
            color = TextPlaceholder
        )
    }
}

// ==================== 图表卡片容器 ====================

@Composable
private fun ChartCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

// ==================== 周报摘要卡片 ====================

@Composable
private fun SummaryCard(
    summary: WeeklyViewModel.SummaryGrid,
    nutrientRates: List<WeeklyViewModel.NutrientRateRow>,
    onCopyReport: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题
            Text(
                text = "周报摘要",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 四宫格
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem(
                    value = summary.avgCalories.toString(),
                    label = "日均热量(kcal)"
                )
                SummaryItem(
                    value = summary.avgProtein.toString(),
                    label = "日均蛋白质(g)"
                )
                SummaryItem(
                    value = "${summary.achievementDays}/7",
                    label = "达标天数"
                )
                SummaryItem(
                    value = summary.totalGap.toInt().toString(),
                    label = "热量缺口(kcal)"
                )
            }

            // 营养素达标率
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 0.5.dp
            )
            Text(
                text = "各营养素日均达标率",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            nutrientRates.forEach { rate ->
                NutrientRateRow(rate = rate)
            }

            // 复制按钮
            OutlinedButton(
                onClick = onCopyReport,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("复制周报", color = Primary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SummaryItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextPlaceholder
        )
    }
}

@Composable
private fun NutrientRateRow(rate: WeeklyViewModel.NutrientRateRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rate.name,
            fontSize = 13.sp,
            color = TextPrimary,
            modifier = Modifier.width(56.dp)
        )

        // 进度条轨道
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFF0F0F0))
        ) {
            val fillPercent = min(rate.rate, 100.0).toFloat() / 100f
            val fillColor = if (rate.rate > 100) Error else Primary

            Box(
                modifier = Modifier
                    .fillMaxWidth(fillPercent)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(fillColor)
            )
        }

        Text(
            text = "${rate.rate.toInt()}%",
            fontSize = 12.sp,
            color = if (rate.rate > 100) Error else TextSecondary,
            modifier = Modifier
                .width(48.dp)
                .padding(start = 8.dp)
        )
    }
}

// ==================== 微量营养素表格卡片 ====================

@Composable
private fun MicroTableCard(rows: List<WeeklyViewModel.MicroRow>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "微量营养素周览",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 表头
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFAFAFA))
                    .padding(vertical = 6.dp, horizontal = 4.dp)
            ) {
                TableCell("营养素", Modifier.weight(0.3f), true)
                TableCell("日均", Modifier.weight(0.25f), true)
                TableCell("目标", Modifier.weight(0.25f), true)
                TableCell("达标率", Modifier.weight(0.2f), true)
            }

            // 数据行
            rows.forEach { row ->
                HorizontalDivider(thickness = 0.3.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableCell(row.name, Modifier.weight(0.3f))
                    TableCell("${row.dailyAvg}${row.unit}", Modifier.weight(0.25f))
                    TableCell("${row.target}${row.unit}", Modifier.weight(0.25f))

                    // 达标率（超量标红）
                    val rateColor = if (row.rate > 100) Error else TextSecondary
                    Box(modifier = Modifier.weight(0.2f)) {
                        Text(
                            text = "${row.rate.toInt()}%",
                            fontSize = 12.sp,
                            color = rateColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TableCell(text: String, modifier: Modifier, isHeader: Boolean = false) {
    Box(modifier = modifier) {
        Text(
            text = text,
            fontSize = if (isHeader) 11.sp else 12.sp,
            fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isHeader) TextSecondary else TextPrimary
        )
    }
}

// ==================== 辅助函数 ====================

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("营养周报", text)
    clipboard.setPrimaryClip(clip)
}
