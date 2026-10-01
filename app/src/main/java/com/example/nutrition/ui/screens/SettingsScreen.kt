package com.example.nutrition.ui.screens

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.nutrition.ui.components.DataLoadError
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutrition.NutritionApp
import com.example.nutrition.domain.model.ActivityLevel
import com.example.nutrition.domain.model.Gender
import com.example.nutrition.domain.usecase.BackupManager
import com.example.nutrition.ui.components.NutrientPicker
import com.example.nutrition.ui.theme.BgCard
import com.example.nutrition.ui.theme.BgMain
import com.example.nutrition.ui.theme.Error
import com.example.nutrition.ui.theme.Primary
import com.example.nutrition.ui.theme.TextPrimary
import com.example.nutrition.ui.theme.TextSecondary
import com.example.nutrition.ui.theme.TextPlaceholder
import com.example.nutrition.ui.theme.Warning
import com.example.nutrition.viewmodel.SettingsViewModel
import com.example.nutrition.viewmodel.UIEvent

/**
 * 设置页 —— 对应小程序 pages/settings/settings.wxml
 *
 * 每日营养目标表单 + 数据管理 + 关于 + 导入弹窗
 */
@Composable
fun SettingsScreen(
    onNavigateToTemplates: () -> Unit = {},
    onNavigateToBodyStats: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            NutritionApp.instance.repository,
            NutritionApp.instance.backupManager
        )
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    // 一次性事件（Toast）
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UIEvent.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
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
                .padding(horizontal = 16.dp)
        ) {
            uiState.targetsError?.let { DataLoadError(it, viewModel::initialize) }
            Spacer(modifier = Modifier.height(8.dp))

            // ========== 身体档案 ==========
            BodyProfileCard(viewModel = viewModel, uiState = uiState)

            // ========== 每日营养目标 ==========
            TargetsCard(viewModel = viewModel, uiState = uiState)

            // ========== 功能 ==========
            FeaturesCard(
                onTemplates = onNavigateToTemplates,
                onBodyStats = onNavigateToBodyStats
            )

            // ========== 数据管理 ==========
            DataManagementCard(viewModel = viewModel)

            // ========== 关于 ==========
            AboutCard(viewModel = viewModel, uiState = uiState)

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ========== 推荐值预览弹窗 ==========
        if (uiState.showCalcDialog) {
            CalculationDialog(viewModel = viewModel, uiState = uiState)
        }

        // ========== 营养素选择器 ==========
        NutrientPicker(
            visible = uiState.showNutrientPicker,
            existingKeys = uiState.micronutrients.map { it.key },
            onPick = { item -> viewModel.addMicronutrient(item) },
            onDismiss = { viewModel.dismissNutrientPicker() }
        )

        // ========== 导入弹窗 ==========
        if (uiState.showImportModal) {
            ImportModal(viewModel = viewModel, uiState = uiState)
        }

        // ========== 清空确认弹窗 ==========
        if (uiState.showClearConfirm) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissClear() },
                title = { Text("确认清空", fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = { Text("将清空全部饮食记录（目标设置保留），此操作不可恢复。", color = TextSecondary) },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmClear() }) {
                        Text("清空", color = Error, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissClear() }) {
                        Text("取消", color = TextSecondary)
                    }
                }
            )
        }
    }
}

// ==================== 营养目标卡片 ====================

@Composable
private fun TargetsCard(viewModel: SettingsViewModel, uiState: SettingsViewModel.UiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle("每日营养目标")

            // 热量
            TargetField(
                label = "热量", unit = "kcal",
                value = uiState.calories,
                placeholder = "如 2000",
                onValueChange = viewModel::onCaloriesInput
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            // 宏量
            TargetField("蛋白质", "g", uiState.protein, "如 120", viewModel::onProteinInput)
            TargetField("脂肪", "g", uiState.fat, "如 65", viewModel::onFatInput)
            TargetField("碳水", "g", uiState.carbs, "如 250", viewModel::onCarbsInput)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            // 微量营养素标题
            Text(
                text = "微量营养素目标",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 微量营养素行
            uiState.micronutrients.forEachIndexed { index, micro ->
                MicroTargetRow(
                    micro = micro,
                    onValueChange = { viewModel.onMicroTargetInput(index, it) },
                    onDelete = { viewModel.deleteMicronutrient(index) }
                )
            }

            // 添加营养素
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { viewModel.openNutrientPicker() }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("+ 添加营养素", fontSize = 14.sp, color = Primary, fontWeight = FontWeight.Medium)
            }

            // 保存按钮
            Button(
                onClick = { viewModel.saveTargets() },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("保存目标", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ==================== 功能卡片 ====================

@Composable
private fun FeaturesCard(
    onTemplates: () -> Unit,
    onBodyStats: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle("功能")

            DataRow(
                title = "食物模板",
                desc = "管理常用食物营养参数，录入时自动补全",
                onClick = onTemplates
            )
            DataRow(
                title = "身体记录",
                desc = "记录体重、体脂等趋势",
                onClick = onBodyStats
            )
        }
    }
}

// ==================== 数据管理卡片 ====================

@Composable
private fun DataManagementCard(viewModel: SettingsViewModel) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle("数据管理")

            DataRow(
                title = "导出数据",
                desc = "复制全部数据到剪贴板",
                onClick = { viewModel.exportData(context) }
            )
            DataRow(
                title = "导入数据",
                desc = "粘贴备份数据恢复记录",
                onClick = { viewModel.showImport() }
            )
            DataRow(
                title = "清空记录",
                desc = "清空全部饮食记录（保留目标）",
                isDanger = true,
                onClick = { viewModel.requestClear() }
            )
        }
    }
}

// ==================== 关于卡片 ====================

@Composable
private fun AboutCard(viewModel: SettingsViewModel, uiState: SettingsViewModel.UiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle("关于")

            AboutRow("版本", viewModel.version)
            AboutRow("数据存储", "本地保存，不上传")

            uiState.storageInfo?.let { info ->
                AboutRow("存储占用", "${info.currentKB} / ${info.limitKB} KB (${info.ratio}%)")
            }

            // 免责声明
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = "免责声明",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "本工具仅提供数据记录与汇总功能，用于对比目标进度，不构成任何专业建议。如有特殊需求，请咨询专业人士。",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 备份提示
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Warning.copy(alpha = 0.08f))
                    .padding(12.dp)
                    .padding(top = 12.dp)
            ) {
                Text(
                    text = "建议定期使用「导出数据」功能备份数据，卸载应用或清理缓存可能导致数据丢失。",
                    fontSize = 12.sp,
                    color = Warning
                )
            }
        }
    }
}

// ==================== 导入弹窗 ====================

@Composable
private fun ImportModal(viewModel: SettingsViewModel, uiState: SettingsViewModel.UiState) {
    AlertDialog(
        onDismissRequest = { viewModel.hideImport() },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("导入数据", fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    text = "×",
                    fontSize = 22.sp,
                    color = TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { viewModel.hideImport() }
                        .padding(4.dp)
                )
            }
        },
        text = {
            Column {
                if (!uiState.showPreview) {
                    // 文本输入
                    OutlinedTextField(
                        value = uiState.importText,
                        onValueChange = { viewModel.onImportTextInput(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        placeholder = {
                            Text("在此粘贴导出的 JSON 数据", color = TextPlaceholder)
                        },
                        textStyle = TextStyle(fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = TextPlaceholder.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // 错误提示
                    if (uiState.importError.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("⚠", fontSize = 14.sp, color = Error)
                            Text(
                                uiState.importError,
                                fontSize = 13.sp,
                                color = Error
                            )
                        }
                    }
                } else {
                    // 预览
                    uiState.importPreview?.let { preview ->
                        ImportPreviewContent(preview = preview)
                    }
                }
            }
        },
        confirmButton = {
            if (!uiState.showPreview) {
                Button(
                    onClick = { viewModel.doPreview() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("预览", fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = { viewModel.confirmImport() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("确认导入", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            if (!uiState.showPreview) {
                OutlinedButton(
                    onClick = { viewModel.hideImport() },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("取消", color = TextSecondary)
                }
            } else {
                OutlinedButton(
                    onClick = { viewModel.backToEdit() },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("返回", color = TextSecondary)
                }
            }
        }
    )
}

// ==================== 导入预览内容 ====================

@Composable
private fun ImportPreviewContent(preview: BackupManager.BackupPreview) {
    Column {
        Text(
            text = "数据概览",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 天数 + 记录数
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PreviewStat(preview.dayCount.toString(), "天数")
            PreviewStat(preview.recordCount.toString(), "记录数")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (preview.earliestDate.isNotEmpty()) {
            PreviewDetail("日期范围", "${preview.earliestDate} ~ ${preview.latestDate}")
        }
        if (preview.hasTargets) {
            preview.targetCalories?.let { PreviewDetail("热量目标", "$it kcal") }
            preview.targetProtein?.let { PreviewDetail("蛋白质目标", "$it g") }
        }
        if (preview.templateCount > 0) {
            PreviewDetail("自定义模板", "${preview.templateCount} 个")
        }
        if (preview.bodyRecordCount > 0) {
            PreviewDetail("身体记录", "${preview.bodyRecordCount} 条")
        }
        PreviewDetail("数据版本", "v${preview.schemaVersion}")
        if (preview.exportedAt.isNotEmpty()) {
            PreviewDetail("导出时间", preview.exportedAt)
        }

        // 警告
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Error.copy(alpha = 0.08f))
                .padding(10.dp)
                .padding(top = 12.dp)
        ) {
            Text(
                text = "确认导入将覆盖当前数据，此操作不可撤销",
                fontSize = 12.sp,
                color = Error
            )
        }
    }
}

// ==================== 通用组件 ====================

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun TargetField(
    label: String,
    unit: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(0.35f), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(" $unit", fontSize = 12.sp, color = TextPlaceholder)
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(0.65f),
            placeholder = { Text(placeholder, fontSize = 13.sp, color = TextPlaceholder) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = TextStyle(fontSize = 14.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = TextPlaceholder.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
private fun MicroTargetRow(
    micro: SettingsViewModel.MicroTargetRow,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(0.35f), verticalAlignment = Alignment.CenterVertically) {
            Text(micro.name, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(" ${micro.unit}", fontSize = 12.sp, color = TextPlaceholder)
        }
        Row(
            modifier = Modifier.weight(0.65f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = micro.target,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("目标值", fontSize = 13.sp, color = TextPlaceholder) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = TextPlaceholder.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(8.dp)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onDelete)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("×", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Error)
            }
        }
    }
}

@Composable
private fun DataRow(
    title: String,
    desc: String,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDanger) Error else TextPrimary
            )
            Text(text = desc, fontSize = 12.sp, color = TextPlaceholder)
        }
        Text(
            text = "›",
            fontSize = 20.sp,
            color = TextPlaceholder
        )
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = TextSecondary)
        Text(value, fontSize = 14.sp, color = TextPrimary)
    }
}

@Composable
private fun PreviewStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Primary)
        Text(label, fontSize = 12.sp, color = TextPlaceholder)
    }
}

@Composable
private fun PreviewDetail(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Text(value, fontSize = 12.sp, color = TextPrimary)
    }
}

// ==================== 身体档案卡片 ====================

@Composable
private fun BodyProfileCard(viewModel: SettingsViewModel, uiState: SettingsViewModel.UiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle("身体档案")

            // 性别
            Text("性别", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            Row(modifier = Modifier.fillMaxWidth()) {
                Gender.entries.forEach { g ->
                    Row(
                        modifier = Modifier
                            .clickable { viewModel.onGenderInput(g) }
                            .padding(end = 16.dp)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.gender == g,
                            onClick = { viewModel.onGenderInput(g) }
                        )
                        Text(
                            text = g.displayName,
                            fontSize = 14.sp,
                            color = TextPrimary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            // 年龄 / 身高 / 体重
            ProfileNumberField(
                label = "年龄",
                unit = "岁",
                value = uiState.age,
                placeholder = "25",
                onValueChange = viewModel::onAgeInput
            )
            ProfileNumberField(
                label = "身高",
                unit = "cm",
                value = uiState.height,
                placeholder = "170",
                onValueChange = viewModel::onHeightInput
            )
            ProfileNumberField(
                label = "体重",
                unit = "kg",
                value = uiState.weight,
                placeholder = "65",
                onValueChange = viewModel::onWeightInput
            )

            // 活动系数
            Text(
                text = "活动量",
                fontSize = 14.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp)
            )
            ActivityLevel.entries.forEach { level ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onActivityLevelInput(level) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = uiState.activityLevel == level,
                        onClick = { viewModel.onActivityLevelInput(level) }
                    )
                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            text = level.displayName,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = level.description,
                            fontSize = 12.sp,
                            color = TextPlaceholder
                        )
                    }
                }
            }

            // 计算按钮
            Button(
                onClick = { viewModel.calculateRecommendations() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("自动计算推荐值", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ProfileNumberField(
    label: String,
    unit: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(0.35f), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(" $unit", fontSize = 12.sp, color = TextPlaceholder)
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(0.65f),
            placeholder = { Text(placeholder, fontSize = 13.sp, color = TextPlaceholder) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = TextStyle(fontSize = 14.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = TextPlaceholder.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

// ==================== 推荐值预览弹窗 ====================

@Composable
private fun CalculationDialog(viewModel: SettingsViewModel, uiState: SettingsViewModel.UiState) {
    val result = uiState.calcResult ?: return
    val bmr = result.bmr
    val tdee = result.tdee
    val targets = result.targets

    AlertDialog(
        onDismissRequest = { viewModel.dismissCalcDialog() },
        title = { Text("推荐目标", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CalcStatItem("BMR", "${bmr.toInt()} kcal")
                    CalcStatItem("TDEE", "${tdee.toInt()} kcal")
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))
                PreviewDetail("热量", "${targets.calories.toInt()} kcal")
                PreviewDetail("蛋白质", "${targets.protein} g")
                PreviewDetail("脂肪", "${targets.fat} g")
                PreviewDetail("碳水", "${targets.carbs} g")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "注：推荐值基于 Mifflin-St Jeor 公式估算，仅供参考。",
                    fontSize = 12.sp,
                    color = TextPlaceholder
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.applyCalculatedTargets() },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("应用到表单", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { viewModel.dismissCalcDialog() },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun CalcStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Primary)
        Text(label, fontSize = 12.sp, color = TextPlaceholder)
    }
}
