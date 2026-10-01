package com.example.nutrition.ui.screens

import com.example.nutrition.ui.components.ObserveUiEvents
import com.example.nutrition.ui.components.NutritionField
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.nutrition.NutritionApp
import com.example.nutrition.domain.model.FoodTemplate
import com.example.nutrition.ui.theme.BgCard
import com.example.nutrition.ui.theme.BgMain
import com.example.nutrition.ui.theme.Error
import com.example.nutrition.ui.theme.Primary
import com.example.nutrition.ui.theme.TextPlaceholder
import com.example.nutrition.ui.theme.TextPrimary
import com.example.nutrition.ui.theme.TextSecondary
import com.example.nutrition.viewmodel.FoodTemplateViewModel

/**
 * 食物模板管理页
 *
 * 展示预设和自定义模板，支持新增/删除/搜索
 */
@Composable
fun FoodTemplateScreen(
    viewModel: FoodTemplateViewModel = viewModel(
        factory = viewModelFactory {
            initializer { FoodTemplateViewModel(NutritionApp.instance.repository) }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    ObserveUiEvents(viewModel.events, blocked = uiState.showAddDialog || uiState.showFilterDialog || uiState.deleteId != null)

    Box(Modifier.fillMaxSize().background(BgMain)) {
        val filtered = viewModel.filteredTemplates()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
        ) {
            item {
                Column {
                    com.example.nutrition.ui.navigation.PageTitle("食物模板", "熟悉的食物，更快地录入", modifier = Modifier.padding(horizontal = 16.dp)) {
                        androidx.compose.material3.IconButton(onClick = { viewModel.openAddDialog() }) {
                            Icon(Icons.Default.Add, contentDescription = "新增食物模板", tint = Primary)
                        }
                    }
                    uiState.dataError?.let { DataLoadError(it, viewModel::initialize) }
                    SearchBar(query = uiState.searchQuery, onQueryChange = viewModel::onSearchQueryChange)
                    QuickTagFilterBar(
                        availableTags = viewModel.availableTags(), selectedTags = uiState.selectedFilterTags,
                        onTagToggle = viewModel::toggleFilterTag, onOpenAll = viewModel::openFilterDialog
                    )
                    SelectedTagRow(
                        selectedTags = uiState.selectedFilterTags,
                        onTagRemove = viewModel::toggleFilterTag, onClear = viewModel::clearFilterTags
                    )
                }
            }
            if (filtered.isEmpty() && uiState.dataError == null) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        Text("暂无匹配模板", color = TextPlaceholder)
                    }
                }
            }
            items(filtered, key = { it.id }) { template ->
                Box(Modifier.padding(horizontal = 16.dp)) {
                    TemplateItem(template = template,
                        onEdit = { viewModel.openEditDialog(template) },
                        onDelete = viewModel::requestDelete)
                }
            }
        }
    }

    if (uiState.showAddDialog) {
        AddTemplateDialog(viewModel = viewModel, uiState = uiState)
    }

    if (uiState.showFilterDialog) {
        FilterDialog(viewModel = viewModel, uiState = uiState)
    }

    uiState.deleteId?.let {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDelete() },
            title = { Text("确认删除", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("删除后不可恢复，确定删除吗？", color = TextSecondary) },
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickTagFilterBar(
    availableTags: List<String>,
    selectedTags: Set<String>,
    onTagToggle: (String) -> Unit,
    onOpenAll: () -> Unit
) {
    val quickTags = rememberQuickTags(availableTags)
    if (quickTags.isEmpty()) return

    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            quickTags.forEach { tag ->
                val isSelected = tag in selectedTags
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Primary else BgMain)
                        .clickable { onTagToggle(tag) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tag,
                        fontSize = 12.sp,
                        color = if (isSelected) com.example.nutrition.ui.theme.TextInverse else TextPrimary,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(BgMain)
                .clickable { onOpenAll() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "全部 ▾",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun rememberQuickTags(availableTags: List<String>): List<String> {
    val curated = listOf("主食", "肉类", "海鲜", "豆制品", "蔬菜", "水果", "坚果", "零食", "饮品", "调味品")
    return curated.filter { it in availableTags }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectedTagRow(
    selectedTags: Set<String>,
    onTagRemove: (String) -> Unit,
    onClear: () -> Unit
) {
    if (selectedTags.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard)
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectedTags.forEach { tag ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Primary)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
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
                        modifier = Modifier.clickable { onTagRemove(tag) }
                    )
                }
            }
        }
        Text(
            text = "清除",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.clickable { onClear() }
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索模板，如 米饭、鸡胸肉", color = TextPlaceholder) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextPlaceholder
                )
            },
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp),
            colors = nutritionFieldColors(),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateItem(
    template: FoodTemplate,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = template.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    if (template.isPreset) {
                        Text(
                            text = "  预设",
                            fontSize = 11.sp,
                            color = Primary,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
                Text(
                    text = "${template.calories.toInt()} kcal · 蛋 ${formatNum(template.protein)}g · 脂 ${formatNum(template.fat)}g · 碳 ${formatNum(template.carbs)}g",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (template.tags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        template.tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(BgMain)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
                if (template.source.isNotEmpty()) {
                    Text(
                        text = template.source,
                        fontSize = 11.sp,
                        color = TextPlaceholder,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onEdit(template.id) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "编辑",
                        fontSize = 13.sp,
                        color = Primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (!template.isPreset) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onDelete(template.id) }
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterDialog(viewModel: FoodTemplateViewModel, uiState: FoodTemplateViewModel.UiState) {
    AlertDialog(
        onDismissRequest = { viewModel.closeFilterDialog() },
        title = {
            Text("按标签筛选", fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            val available = viewModel.availableTags()
            Column {
                if (available.isEmpty()) {
                    Text("暂无标签", color = TextSecondary)
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        available.forEach { tag ->
                            val isSelected = tag in uiState.selectedFilterTags
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Primary else BgMain)
                                    .clickable { viewModel.toggleFilterTag(tag) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 13.sp,
                                    color = if (isSelected) com.example.nutrition.ui.theme.TextInverse else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.closeFilterDialog() }) {
                Text("确定", color = Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.clearFilterTags(); viewModel.closeFilterDialog() }) {
                Text("清除", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun AddTemplateDialog(viewModel: FoodTemplateViewModel, uiState: FoodTemplateViewModel.UiState) {
    AlertDialog(
        onDismissRequest = { viewModel.closeAddDialog() },
        title = {
            Text(
                text = if (uiState.editingId != null) "编辑模板" else "新增自定义模板",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column {
                NutritionField(label = "名称", value = uiState.templateName, onValueChange = viewModel::onNameInput, keyboardType = KeyboardType.Text, placeholder = "")
                NutritionField(label = "热量", value = uiState.templateCalories, onValueChange = viewModel::onCaloriesInput, keyboardType = KeyboardType.Decimal, placeholder = "")
                NutritionField(label = "蛋白质", value = uiState.templateProtein, onValueChange = viewModel::onProteinInput, keyboardType = KeyboardType.Decimal, placeholder = "")
                NutritionField(label = "脂肪", value = uiState.templateFat, onValueChange = viewModel::onFatInput, keyboardType = KeyboardType.Decimal, placeholder = "")
                NutritionField(label = "碳水", value = uiState.templateCarbs, onValueChange = viewModel::onCarbsInput, keyboardType = KeyboardType.Decimal, placeholder = "")

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                TemplateTagEditor(viewModel = viewModel, uiState = uiState)
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.saveTemplate() },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(
                    text = if (uiState.editingId != null) "更新" else "保存",
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { viewModel.closeAddDialog() },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateTagEditor(viewModel: FoodTemplateViewModel, uiState: FoodTemplateViewModel.UiState) {
    Column {
        Text(
            text = "标签",
            fontSize = 14.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // 已选标签
        if (uiState.templateTags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                uiState.templateTags.forEach { tag ->
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
                            modifier = Modifier.clickable { viewModel.removeTemplateTag(tag) }
                        )
                    }
                }
            }
        }

        // 添加新标签
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            OutlinedTextField(
                value = uiState.newTagInput,
                onValueChange = viewModel::onNewTagInput,
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入新标签，如 高蛋白", fontSize = 13.sp, color = TextPlaceholder) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp),
                colors = nutritionFieldColors(),
                shape = RoundedCornerShape(8.dp)
            )
            OutlinedButton(
                onClick = { viewModel.addNewTag() },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("添加", color = Primary, fontSize = 13.sp)
            }
        }

        // 已有标签候选
        val available = viewModel.availableTags().filter { it !in uiState.templateTags }
        if (available.isNotEmpty()) {
            Text(
                text = "点击添加已有标签：",
                fontSize = 12.sp,
                color = TextPlaceholder,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                available.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(BgMain)
                            .clickable { viewModel.toggleTemplateTag(tag) }
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


private fun formatNum(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format("%.1f", value)
    }
}
