package com.example.nutrition.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.example.nutrition.ui.components.DataLoadError
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrition.NutritionApp
import com.example.nutrition.domain.model.MealKey
import com.example.nutrition.domain.usecase.DateUtils
import com.example.nutrition.ui.components.BarProgress
import com.example.nutrition.ui.components.GuideOverlay
import com.example.nutrition.ui.components.MealCard
import com.example.nutrition.ui.components.RingProgress
import com.example.nutrition.ui.theme.BgCard
import com.example.nutrition.ui.theme.BgMain
import com.example.nutrition.ui.theme.BgTag
import com.example.nutrition.ui.theme.Error
import com.example.nutrition.ui.theme.Primary
import com.example.nutrition.ui.theme.TextInverse
import com.example.nutrition.ui.theme.TextPlaceholder
import com.example.nutrition.ui.theme.TextPrimary
import com.example.nutrition.ui.theme.TextSecondary
import com.example.nutrition.ui.theme.Warning
import com.example.nutrition.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate

/**
 * 首页总览 —— 对应小程序 pages/index/index.wxml
 *
 * 日期栏 + 存储告警 + 空状态/仪表盘 + 环形图 + 宏量/微量营养素 + 四餐卡片 + FAB
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(NutritionApp.instance.repository)
    ),
    onNavigateToRecord: (MealKey?) -> Unit = {}
) {
    // 页面状态（单一 UiState）
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 首次加载
    LaunchedEffect(Unit) {
        viewModel.loadData()
        viewModel.checkFirstUse()
    }

    val context = LocalContext.current

    // 列表滚动状态：向下滑动时隐藏 FAB，向上滑动或回到顶部时显示
    val listState = rememberLazyListState()
    var previousIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }
    var fabVisible by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                if (index == 0 && offset == 0) {
                    fabVisible = true
                } else if (index > previousIndex || (index == previousIndex && offset > previousScrollOffset)) {
                    fabVisible = false
                } else {
                    fabVisible = true
                }
                previousIndex = index
                previousScrollOffset = offset
            }
    }

    // 日期选择辅助
    fun showHomeDatePicker() {
        val parsed = DateUtils.parseDate(uiState.currentDate)
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selected = LocalDate.of(year, month + 1, dayOfMonth)
                viewModel.pickDate(DateUtils.formatDate(selected))
            },
            parsed.year,
            parsed.monthValue - 1,
            parsed.dayOfMonth
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    Box(modifier = Modifier.fillMaxSize().background(BgMain)) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            uiState.dataError?.let { message ->
                item { DataLoadError(message, viewModel::loadData) }
            }
            uiState.metadataError?.let { message ->
                item { DataLoadError(message, viewModel::checkFirstUse) }
            }
            // 存储容量告警横幅
            uiState.storageWarn?.let { warn ->
                item {
                    StorageWarnBanner(warn.message)
                }
            }

            // 日期栏
            item {
                DateBar(
                    dateStr = uiState.currentDate,
                    isToday = uiState.isToday,
                    onPrev = { viewModel.prevDay() },
                    onNext = { viewModel.nextDay() },
                    onDateClick = { showHomeDatePicker() }
                )
            }

            if (!uiState.hasData && uiState.dataError == null) {
                // 空状态
                item {
                    Spacer(modifier = Modifier.height(60.dp))
                    EmptyState(onRecord = { onNavigateToRecord(null) })
                }
            } else if (uiState.hasData) {
                // 热量环形进度卡片
                item {
                    RingCard(
                        percent = uiState.ringPercent.toFloat(),
                        centerText = uiState.ringCenterText,
                        targetCalories = uiState.targetCalories
                    )
                }

                // TDEE 参考（二期新增）
                if (uiState.tdee != null) {
                    item {
                        AutoCalculatedInfo(tdee = uiState.tdee!!)
                    }
                }

                // 宏量营养素卡片
                item {
                    DashboardCard(title = "宏量营养素") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            uiState.macros.forEach { macro ->
                                BarProgress(
                                    current = macro.current,
                                    target = macro.target,
                                    label = macro.label,
                                    unit = macro.unit,
                                    color = macro.color
                                )
                            }
                        }
                    }
                }

                // 微量营养素网格
                if (uiState.micros.isNotEmpty()) {
                    item {
                        DashboardCard(title = "微量营养素") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                uiState.micros.chunked(2).forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        row.forEach { micro ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                BarProgress(
                                                    current = micro.current,
                                                    target = micro.target,
                                                    label = micro.label,
                                                    unit = micro.unit,
                                                    color = Color(0xFFAB47BC),
                                                    mini = true
                                                )
                                            }
                                        }
                                        // 填充空位保证对齐
                                        repeat(2 - row.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 四餐分布卡片
                item {
                    DashboardCard(title = "四餐分布") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.meals.forEach { meal ->
                                MealCard(
                                    mealKey = meal.key,
                                    calories = meal.calories,
                                    count = meal.count,
                                    dailyTarget = uiState.targetCalories,
                                    onClick = { key ->
                                        onNavigateToRecord(key)
                                    }
                                )
                            }
                        }
                    }
                }

                // 底部间距（给 FAB 让位）
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // 悬浮录入按钮，滑动时自动隐藏
        AnimatedVisibility(
            visible = fabVisible,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToRecord(null) },
                modifier = Modifier.padding(16.dp),
                containerColor = Primary,
                contentColor = TextInverse,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("录入", fontWeight = FontWeight.SemiBold) }
            )
        }

        // 首次使用引导
        GuideOverlay(
            visible = uiState.showGuide,
            onFinish = { viewModel.onGuideFinish() }
        )
    }
}

// ==================== 子组件 ====================

@Composable
private fun StorageWarnBanner(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Warning.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = message,
            fontSize = 12.sp,
            color = Warning
        )
    }
}

@Composable
private fun DateBar(
    dateStr: String,
    isToday: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDateClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 前一天
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(BgCard)
                .clickable(onClick = onPrev),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "‹", fontSize = 22.sp, color = TextPrimary)
        }

        // 日期 + 今天标签
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onDateClick)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = dateStr,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            if (isToday) {
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Primary.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = "今天", fontSize = 10.sp, color = Primary)
                }
            }
        }

        // 后一天
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(BgCard)
                .clickable(onClick = onNext),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "›", fontSize = 22.sp, color = TextPrimary)
        }
    }
}

@Composable
private fun EmptyState(onRecord: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "\uD83C\uDF7D\uFE0F", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "今天还没有记录",
            fontSize = 16.sp,
            color = TextPlaceholder
        )
        Text(
            text = "点击下方按钮开始录入第一餐",
            fontSize = 12.sp,
            color = TextPlaceholder
        )
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Primary)
                .clickable(onClick = onRecord)
                .padding(horizontal = 32.dp, vertical = 10.dp)
        ) {
            Text(
                text = "+ 录入",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextInverse
            )
        }
    }
}

@Composable
private fun RingCard(
    percent: Float,
    centerText: String,
    targetCalories: Double
) {
    DashboardCard {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            RingProgress(
                percent = percent,
                centerText = centerText,
                subText = "kcal 缺口",
                size = 140.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RingLabel(value = "${percent.toInt()}%", label = "已摄入")
                RingLabel(value = "${targetCalories.toInt()}", label = "对比目标")
            }
        }
    }
}

@Composable
private fun RingLabel(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextPlaceholder
        )
    }
}

@Composable
private fun DashboardCard(
    title: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            content()
        }
    }
}

// ==================== 二期：TDEE 参考 ====================

@Composable
private fun AutoCalculatedInfo(tdee: Double) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Primary.copy(alpha = 0.08f))
            .padding(12.dp)
    ) {
        Text(
            text = "参考 TDEE：${tdee.toInt()} kcal · 基于身体档案估算",
            fontSize = 12.sp,
            color = Primary
        )
    }
}
