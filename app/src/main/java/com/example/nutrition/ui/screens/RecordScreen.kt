package com.example.nutrition.ui.screens

import android.widget.Toast
import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import com.example.nutrition.ui.theme.nutritionFieldColors
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.nutrition.ui.components.DataLoadError
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
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
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.nutrition.NutritionApp
import com.example.nutrition.domain.model.FoodTemplate
import com.example.nutrition.domain.model.MealKey
import com.example.nutrition.domain.usecase.DateUtils
import com.example.nutrition.ui.components.NutrientPicker
import com.example.nutrition.ui.components.RecordItem
import com.example.nutrition.ui.theme.BgCard
import com.example.nutrition.ui.theme.BgMain
import com.example.nutrition.ui.theme.Error
import com.example.nutrition.ui.theme.Primary
import com.example.nutrition.ui.theme.TextPrimary
import com.example.nutrition.ui.theme.TextSecondary
import com.example.nutrition.ui.theme.TextPlaceholder
import com.example.nutrition.viewmodel.RecordViewModel
import com.example.nutrition.viewmodel.UIEvent
import java.time.LocalDate

/**
 * 录入页 —— 对应小程序 pages/record/record.wxml
 *
 * 餐次Tab + 日期选择 + 表单 + 记录列表 + 编辑/删除
 */
@Composable
fun RecordScreen(
    initialMeal: MealKey? = null,
    viewModel: RecordViewModel = viewModel(
        factory = viewModelFactory {
            initializer { RecordViewModel(NutritionApp.instance.repository) }
        }
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 页面 resume 时按当前时间刷新默认餐次（首次 resume 由 initialMeal 接管，跳过避免覆盖）
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var isFirstResume = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isFirstResume) {
                    isFirstResume = false
                } else {
                    viewModel.refreshMealByTime()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 首次加载：应用从首页传入的餐次（为 null 时按当前时间推断）
    LaunchedEffect(viewModel, initialMeal) {
        viewModel.initWithMeal(initialMeal)
    }

    // 一次性事件（Toast）
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is UIEvent.ShowToast ->
                        Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
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
        ) {
            com.example.nutrition.ui.navigation.PageTitle("录入", "按餐记录，按克重换算", modifier = Modifier.padding(horizontal = 16.dp))
            uiState.dataError?.let { DataLoadError(it, viewModel::loadData) }
            uiState.templateError?.let { DataLoadError(it, viewModel::loadData) }
            // ========== 餐次 Tab ==========
            MealTabs(
                currentMeal = uiState.currentMeal,
                onMealSelected = { viewModel.switchMeal(it) }
            )

            // ========== 日期选择 ==========
            DateBar(
                dateStr = uiState.currentDate,
                onPrev = { viewModel.prevDay() },
                onNext = { viewModel.nextDay() },
                onDateClick = {
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
            )

            // ========== 录入表单 ==========
            FormCard(
                foodName = uiState.foodName,
                weight = uiState.weight,
                calories = uiState.calories,
                kiloJoules = uiState.kiloJoules,
                protein = uiState.protein,
                fat = uiState.fat,
                carbs = uiState.carbs,
                formMicronutrients = uiState.formMicronutrients,
                editingId = uiState.editingId,
                templateSuggestions = uiState.templateSuggestions,
                onNameInput = viewModel::onNameInput,
                onWeightInput = viewModel::onWeightInput,
                onCaloriesInput = viewModel::onCaloriesInput,
                onKiloJoulesInput = viewModel::onKiloJoulesInput,
                onProteinInput = viewModel::onProteinInput,
                onFatInput = viewModel::onFatInput,
                onCarbsInput = viewModel::onCarbsInput,
                onMicroValueInput = viewModel::onMicroValueInput,
                onDeleteMicro = viewModel::deleteMicronutrient,
                onTogglePicker = { viewModel.toggleNutrientPicker() },
                onSave = { viewModel.saveRecord() },
                onCancelEdit = { viewModel.cancelEdit() },
                onTemplateSelected = { viewModel.applyTemplate(it) }
            )

            // ========== 记录列表标题 ==========
            Text(
                text = "${uiState.currentDate} ${uiState.currentMeal.displayName}记录",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // ========== 记录列表 ==========
            if (uiState.recordList.isEmpty() && uiState.dataError == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.isLoadingRecords) "正在读取记录…" else "暂无记录，开始录入第一餐吧",
                        fontSize = 14.sp,
                        color = TextPlaceholder
                    )
                }
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.recordList.forEach { record ->
                        RecordItem(
                            record = record,
                            isDismissed = uiState.deleteConfirm?.id == record.id,
                            onEdit = { id -> viewModel.editRecord(id) },
                            onDelete = { id -> viewModel.requestDelete(id) }
                        )
                    }
                    // 底部留白
                    Box(modifier = Modifier.height(24.dp))
                }
            }
        }

        // ========== 营养素选择器 ==========
        NutrientPicker(
            visible = uiState.showNutrientPicker,
            existingKeys = uiState.formMicronutrients.map { it.key },
            onPick = { item -> viewModel.addMicronutrient(item) },
            onDismiss = { viewModel.dismissNutrientPicker() }
        )

        // ========== 删除确认弹窗 ==========
        uiState.deleteConfirm?.let {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDelete() },
                title = {
                    Text("确认删除", fontWeight = FontWeight.Bold, color = TextPrimary)
                },
                text = {
                    Text("删除后不可恢复，确定删除这条记录吗？", color = TextSecondary)
                },
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

        // ========== 保存为模板提示弹窗 ==========
        uiState.pendingTemplate?.let { template ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissSaveTemplatePrompt() },
                title = {
                    Text("保存为模板？", fontWeight = FontWeight.Bold, color = TextPrimary)
                },
                text = {
                    Column {
                        Text(
                            "是否将「${template.name}」保存为模板，方便下次快速录入？",
                            color = TextSecondary
                        )
                        SaveAsTemplateTagEditor(viewModel = viewModel, uiState = uiState)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmSaveAsTemplate() }) {
                        Text("保存", color = Primary, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissSaveTemplatePrompt() }) {
                        Text("不了", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SaveAsTemplateTagEditor(viewModel: RecordViewModel, uiState: RecordViewModel.UiState) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            text = "标签（可选）",
            fontSize = 13.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )

        if (uiState.saveAsTemplateTags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                uiState.saveAsTemplateTags.forEach { tag ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Primary)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 12.sp,
                            color = com.example.nutrition.ui.theme.TextInverse
                        )
                        Text(
                            text = "×",
                            fontSize = 14.sp,
                            color = com.example.nutrition.ui.theme.TextInverse,
                            modifier = Modifier.clickable { viewModel.removeSaveAsTemplateTag(tag) }
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            OutlinedTextField(
                value = uiState.newTemplateTagInput,
                onValueChange = viewModel::onNewTemplateTagInput,
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入新标签", fontSize = 13.sp, color = TextPlaceholder) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp),
                colors = nutritionFieldColors(),
                shape = RoundedCornerShape(8.dp)
            )
            OutlinedButton(
                onClick = { viewModel.addNewTemplateTag() },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("添加", color = Primary, fontSize = 13.sp)
            }
        }

        val available = uiState.allTemplateTags.filter { it !in uiState.saveAsTemplateTags }
        if (available.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                available.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(BgMain)
                            .clickable { viewModel.toggleSaveAsTemplateTag(tag) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

// ==================== 餐次 Tab ====================

@Composable
private fun MealTabs(
    currentMeal: MealKey,
    onMealSelected: (MealKey) -> Unit
) {
    val tabIndex = MealKey.entries.indexOf(currentMeal).coerceAtLeast(0)

    ScrollableTabRow(
        selectedTabIndex = tabIndex,
        containerColor = BgCard,
        contentColor = Primary,
        edgePadding = 0.dp,
        divider = {}
    ) {
        MealKey.entries.forEach { meal ->
            Tab(
                selected = meal == currentMeal,
                onClick = { onMealSelected(meal) },
                text = {
                    Text(
                        text = meal.displayName,
                        fontWeight = if (meal == currentMeal) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 15.sp
                    )
                }
            )
        }
    }
}

// ==================== 日期栏 ====================

@Composable
private fun DateBar(
    dateStr: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDateClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
            text = dateStr,
            fontSize = 15.sp,
            color = TextPrimary,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onDateClick)
                .padding(horizontal = 8.dp, vertical = 4.dp)
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

// ==================== 表单卡片 ====================

@Composable
private fun FormCard(
    foodName: String,
    weight: String,
    calories: String,
    kiloJoules: String,
    protein: String,
    fat: String,
    carbs: String,
    formMicronutrients: List<RecordViewModel.FormMicro>,
    editingId: String?,
    templateSuggestions: List<FoodTemplate>,
    onNameInput: (String) -> Unit,
    onWeightInput: (String) -> Unit,
    onCaloriesInput: (String) -> Unit,
    onKiloJoulesInput: (String) -> Unit,
    onProteinInput: (String) -> Unit,
    onFatInput: (String) -> Unit,
    onCarbsInput: (String) -> Unit,
    onMicroValueInput: (Int, String) -> Unit,
    onDeleteMicro: (Int) -> Unit,
    onTogglePicker: () -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
    onTemplateSelected: (FoodTemplate) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题
            Text(
                text = if (editingId != null) "编辑记录" else "新增记录",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 食物名称（带模板自动补全）
            Column {
                FormField(
                    label = "食物名称",
                    value = foodName,
                    placeholder = "选填，如 燕麦牛奶",
                    onValueChange = onNameInput
                )
                if (templateSuggestions.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BgMain)
                    ) {
                        templateSuggestions.forEach { template ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTemplateSelected(template) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = template.name,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${template.calories.toInt()} kcal/100g",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 克重（默认 100g）
            FormField(
                label = "克重",
                unit = "g",
                value = weight,
                placeholder = "100",
                keyboardType = KeyboardType.Decimal,
                onValueChange = onWeightInput
            )

            // 热量：kcal 与 kJ 并列输入
            DualUnitRow(
                label = "热量",
                valueA = calories,
                unitA = "kcal",
                valueB = kiloJoules,
                unitB = "kJ",
                required = true,
                placeholderA = "必填",
                placeholderB = "自动换算",
                onValueAChange = onCaloriesInput,
                onValueBChange = onKiloJoulesInput
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 0.5.dp
            )

            // 蛋白质
            FormField(
                label = "蛋白质",
                unit = "g",
                value = protein,
                placeholder = "选填",
                keyboardType = KeyboardType.Decimal,
                onValueChange = onProteinInput
            )

            // 脂肪
            FormField(
                label = "脂肪",
                unit = "g",
                value = fat,
                placeholder = "选填",
                keyboardType = KeyboardType.Decimal,
                onValueChange = onFatInput
            )

            // 碳水
            FormField(
                label = "碳水",
                unit = "g",
                value = carbs,
                placeholder = "选填",
                keyboardType = KeyboardType.Decimal,
                onValueChange = onCarbsInput
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 0.5.dp
            )

            // 微量营养素标题
            Text(
                text = "微量营养素（选填）",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 微量营养素输入行
            formMicronutrients.forEachIndexed { index, micro ->
                MicroFormRow(
                    micro = micro,
                    onValueChange = { onMicroValueInput(index, it) },
                    onDelete = { onDeleteMicro(index) }
                )
            }

            // 添加营养素按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onTogglePicker)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ 添加营养素",
                    fontSize = 14.sp,
                    color = Primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // 按钮区域
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (editingId != null) {
                    OutlinedButton(
                        onClick = onCancelEdit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("取消编辑", color = TextSecondary)
                    }
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary
                    )
                ) {
                    Text(
                        text = if (editingId != null) "更新" else "保存",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ==================== 双单位并列输入行 ====================

@Composable
private fun DualUnitRow(
    label: String,
    valueA: String,
    unitA: String,
    valueB: String,
    unitB: String,
    required: Boolean = false,
    placeholderA: String = "",
    placeholderB: String = "",
    onValueAChange: (String) -> Unit,
    onValueBChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 标签
        Row(
            modifier = Modifier.weight(0.35f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            if (required) {
                Text(
                    text = " *",
                    fontSize = 14.sp,
                    color = Error
                )
            }
        }

        // 两个并列输入框
        Row(
            modifier = Modifier.weight(0.65f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DualUnitInput(
                value = valueA,
                unit = unitA,
                placeholder = placeholderA,
                onValueChange = onValueAChange,
                modifier = Modifier.weight(1f)
            )
            DualUnitInput(
                value = valueB,
                unit = unitB,
                placeholder = placeholderB,
                onValueChange = onValueBChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DualUnitInput(
    value: String,
    unit: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = {
            Text(placeholder, fontSize = 13.sp, color = TextPlaceholder)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = TextStyle(fontSize = 14.sp),
        trailingIcon = {
            Text(
                text = unit,
                fontSize = 12.sp,
                color = TextPlaceholder,
                modifier = Modifier.padding(end = 12.dp)
            )
        },
        colors = nutritionFieldColors(),
        shape = RoundedCornerShape(8.dp)
    )
}

// ==================== 表单字段 ====================

@Composable
private fun FormField(
    label: String,
    value: String,
    placeholder: String = "",
    unit: String? = null,
    required: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 标签
        Row(
            modifier = Modifier.weight(0.35f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            if (unit != null) {
                Text(
                    text = " $unit",
                    fontSize = 12.sp,
                    color = TextPlaceholder
                )
            }
            if (required) {
                Text(
                    text = " *",
                    fontSize = 14.sp,
                    color = Error
                )
            }
        }

        // 输入框
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(0.65f),
            placeholder = {
                Text(placeholder, fontSize = 13.sp, color = TextPlaceholder)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = TextStyle(fontSize = 14.sp),
            colors = nutritionFieldColors(),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

// ==================== 微量营养素输入行 ====================

@Composable
private fun MicroFormRow(
    micro: RecordViewModel.FormMicro,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 名称 + 单位
        Row(
            modifier = Modifier.weight(0.35f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = micro.name,
                fontSize = 14.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = " ${micro.unit}",
                fontSize = 12.sp,
                color = TextPlaceholder
            )
        }

        // 输入框 + 删除按钮
        Row(
            modifier = Modifier.weight(0.65f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = micro.value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("数值", fontSize = 13.sp, color = TextPlaceholder)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(fontSize = 14.sp),
                colors = nutritionFieldColors(),
                shape = RoundedCornerShape(8.dp)
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onDelete)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "×",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Error
                )
            }
        }
    }
}
