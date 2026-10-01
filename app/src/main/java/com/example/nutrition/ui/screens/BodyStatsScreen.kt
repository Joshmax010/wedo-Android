package com.example.nutrition.ui.screens

import android.app.DatePickerDialog
import com.example.nutrition.ui.components.ObserveUiEvents
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.nutrition.ui.components.NutritionField
import com.example.nutrition.ui.navigation.LocalPageChrome
import com.example.nutrition.ui.navigation.formRevealModifier
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import com.example.nutrition.ui.theme.nutritionFieldColors
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.nutrition.ui.components.DataLoadError
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.nutrition.NutritionApp
import com.example.nutrition.domain.model.BodyRecord
import com.example.nutrition.domain.usecase.DateUtils
import com.example.nutrition.ui.charts.WeightLineChart
import com.example.nutrition.ui.theme.BgCard
import com.example.nutrition.ui.theme.BgMain
import com.example.nutrition.ui.theme.Error
import com.example.nutrition.ui.theme.Primary
import com.example.nutrition.ui.theme.TextPlaceholder
import com.example.nutrition.ui.theme.TextPrimary
import com.example.nutrition.ui.theme.TextSecondary
import com.example.nutrition.viewmodel.BodyStatsViewModel
import java.time.LocalDate

/**
 * 身体记录页
 *
 * 体重趋势图表 + 录入表单 + 历史记录
 */
@Composable
fun BodyStatsScreen(
    viewModel: BodyStatsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { BodyStatsViewModel(NutritionApp.instance.repository) }
        }
    )
) {
    val context = LocalContext.current

    // 页面状态（单一 UiState）
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chrome = LocalPageChrome.current
    var editRequest by remember { mutableIntStateOf(0) }
    SideEffect { chrome.editing = uiState.editing }
    DisposableEffect(chrome) { onDispose { chrome.editing = false } }

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    // 一次性事件（Toast）
    ObserveUiEvents(viewModel.events, blocked = uiState.deleteDate != null)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            com.example.nutrition.ui.navigation.PageTitle("身体记录", "记录身体变化")
            uiState.dataError?.let { DataLoadError(it, viewModel::initialize) }
            uiState.dateError?.let { message -> DataLoadError(message) { viewModel.selectDate(uiState.selectedDate) } }
            // 图表
            if (uiState.records.size >= 2) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        WeightLineChart(records = uiState.records)
                    }
                }
            }

            // 表单
            BodyRecordFormCard(viewModel = viewModel, uiState = uiState, modifier = formRevealModifier(editRequest))

            // 历史记录
            if (uiState.records.isNotEmpty()) {
                Text(
                    text = "历史记录",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.records.reversed().forEach { record ->
                        BodyRecordItem(
                            record = record,
                            onEdit = { viewModel.editRecord(record); editRequest++ },
                            onDelete = { viewModel.requestDelete(record.dateStr) }
                        )
                    }
                }
            }

            Box(modifier = Modifier.height(24.dp))
        }

        uiState.deleteDate?.let {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDelete() },
                title = { Text("确认删除", fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = { Text("确定删除这条身体记录吗？", color = TextSecondary) },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmDelete() }) {
                        Text("删除", color = Error, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDelete() }) {
                        Text("取消", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun BodyRecordFormCard(
    viewModel: BodyStatsViewModel,
    uiState: BodyStatsViewModel.UiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (uiState.editing) "编辑身体记录" else "记录身体数据",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (uiState.editing) {
                TextButton(onClick = viewModel::cancelEdit) { Text("取消编辑", color = Primary) }
            }

            // 日期选择
            val contextForPicker = LocalContext.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        val parsed = DateUtils.parseDate(uiState.selectedDate)
                        DatePickerDialog(
                            contextForPicker,
                            { _, year, month, dayOfMonth ->
                                val selected = LocalDate.of(year, month + 1, dayOfMonth)
                                viewModel.selectDate(DateUtils.formatDate(selected))
                            },
                            parsed.year,
                            parsed.monthValue - 1,
                            parsed.dayOfMonth
                        ).show()
                    }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("日期", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                Text(
                    text = uiState.selectedDate,
                    fontSize = 14.sp,
                    color = Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            NutritionField("体重", "kg", uiState.weight, viewModel::onWeightInput, true)
            NutritionField("体脂率", "%", uiState.bodyFat, viewModel::onBodyFatInput)
            NutritionField("肌肉量", "kg", uiState.muscle, viewModel::onMuscleInput)
            NutritionField("备注", "", uiState.note, viewModel::onNoteInput, keyboardType = KeyboardType.Text)

            Button(
                onClick = { viewModel.saveRecord() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(if (uiState.editing) "更新记录" else "保存记录", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


@Composable
private fun BodyRecordItem(
    record: BodyRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.dateStr,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "体重 ${String.format("%.1f", record.weightKg)} kg" +
                        (record.bodyFatPercent?.let { " · 体脂 ${String.format("%.1f", it)}%" } ?: "") +
                        (record.muscleKg?.let { " · 肌肉 ${String.format("%.1f", it)} kg" } ?: ""),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (!record.note.isNullOrEmpty()) {
                    Text(
                        text = record.note,
                        fontSize = 12.sp,
                        color = TextPlaceholder,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onDelete)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "删除",
                    fontSize = 13.sp,
                    color = Error,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
