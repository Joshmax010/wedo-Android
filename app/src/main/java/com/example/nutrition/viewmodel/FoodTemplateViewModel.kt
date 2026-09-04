package com.example.nutrition.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nutrition.domain.model.FoodTemplate
import com.example.nutrition.domain.model.Resource
import com.example.nutrition.domain.repository.LocalStorageRepository
import com.example.nutrition.domain.usecase.UnitConverter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 食物模板管理页 ViewModel
 *
 * 管理模板列表、搜索、新增/删除自定义模板。
 * 页面状态收敛为单一 UiState，模板列表由 Repository Flow 自动驱动
 */
class FoodTemplateViewModel(
    private val repository: LocalStorageRepository
) : ViewModel() {

    // ==================== UI 状态 ====================

    data class UiState(
        val templates: List<FoodTemplate> = emptyList(),
        val searchQuery: String = "",
        val isLoading: Boolean = true,
        // 新增/编辑表单
        val showAddDialog: Boolean = false,
        val templateName: String = "",
        val templateCalories: String = "",
        val templateProtein: String = "",
        val templateFat: String = "",
        val templateCarbs: String = "",
        val templateTags: Set<String> = emptySet(),
        val newTagInput: String = "",
        // 筛选标签（选中任意一个即显示）
        val selectedFilterTags: Set<String> = emptySet(),
        // 全部标签筛选面板
        val showFilterDialog: Boolean = false,
        // 编辑状态（null 表示新增）
        val editingId: String? = null,
        // 删除确认
        val deleteId: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 一次性事件（Toast 等）
    private val _events = Channel<UIEvent>(Channel.BUFFERED)
    val events: Flow<UIEvent> = _events.receiveAsFlow()

    private fun sendEvent(event: UIEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    init {
        repository.getAllFoodTemplates()
            .onEach { list ->
                _uiState.update { it.copy(templates = list, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    // ==================== 兼容入口 ====================

    fun initialize() {
        // 模板列表已由 Flow 自动驱动，无需手动加载
    }

    // ==================== 搜索与筛选 ====================

    fun onSearchQueryChange(value: String) {
        _uiState.update { it.copy(searchQuery = value) }
    }

    fun filteredTemplates(): List<FoodTemplate> {
        val state = _uiState.value
        val query = state.searchQuery.trim()
        var result = state.templates
        if (query.isNotEmpty()) {
            result = result.filter { it.name.contains(query, ignoreCase = true) }
        }
        if (state.selectedFilterTags.isNotEmpty()) {
            result = result.filter { template ->
                template.tags.any { it in state.selectedFilterTags }
            }
        }
        return result
    }

    /** 当前可用的全部标签（来自所有模板 + 当前编辑中的标签） */
    fun availableTags(): List<String> {
        val state = _uiState.value
        val set = mutableSetOf<String>()
        state.templates.forEach { set.addAll(it.tags) }
        set.addAll(state.templateTags)
        return set.sorted()
    }

    // ==================== 表单输入 ====================

    fun onNameInput(value: String) { _uiState.update { it.copy(templateName = value) } }
    fun onCaloriesInput(value: String) { _uiState.update { it.copy(templateCalories = value) } }
    fun onProteinInput(value: String) { _uiState.update { it.copy(templateProtein = value) } }
    fun onFatInput(value: String) { _uiState.update { it.copy(templateFat = value) } }
    fun onCarbsInput(value: String) { _uiState.update { it.copy(templateCarbs = value) } }
    fun onNewTagInput(value: String) { _uiState.update { it.copy(newTagInput = value) } }

    fun toggleTemplateTag(tag: String) {
        _uiState.update { state ->
            state.copy(
                templateTags = if (tag in state.templateTags) {
                    state.templateTags - tag
                } else {
                    state.templateTags + tag
                }
            )
        }
    }

    fun removeTemplateTag(tag: String) {
        _uiState.update { it.copy(templateTags = it.templateTags - tag) }
    }

    fun addNewTag() {
        _uiState.update { state ->
            val tag = state.newTagInput.trim()
            if (tag.isEmpty()) {
                state
            } else {
                state.copy(templateTags = state.templateTags + tag, newTagInput = "")
            }
        }
    }

    fun toggleFilterTag(tag: String) {
        _uiState.update { state ->
            state.copy(
                selectedFilterTags = if (tag in state.selectedFilterTags) {
                    state.selectedFilterTags - tag
                } else {
                    state.selectedFilterTags + tag
                }
            )
        }
    }

    fun clearFilterTags() {
        _uiState.update { it.copy(selectedFilterTags = emptySet()) }
    }

    fun openFilterDialog() {
        _uiState.update { it.copy(showFilterDialog = true) }
    }

    fun closeFilterDialog() {
        _uiState.update { it.copy(showFilterDialog = false) }
    }

    // ==================== 新增/编辑弹窗 ====================

    fun openAddDialog() {
        resetForm()
        _uiState.update { it.copy(showAddDialog = true) }
    }

    fun openEditDialog(template: FoodTemplate) {
        _uiState.update {
            it.copy(
                editingId = template.id,
                templateName = template.name,
                templateCalories = UnitConverter.formatForInput(template.calories),
                templateProtein = UnitConverter.formatForInput(template.protein),
                templateFat = UnitConverter.formatForInput(template.fat),
                templateCarbs = UnitConverter.formatForInput(template.carbs),
                templateTags = template.tags.toSet(),
                newTagInput = "",
                showAddDialog = true
            )
        }
    }

    fun closeAddDialog() {
        _uiState.update {
            it.copy(showAddDialog = false, editingId = null, templateTags = emptySet(), newTagInput = "")
        }
    }

    // ==================== 保存模板 ====================

    fun saveTemplate() {
        val state = _uiState.value
        val name = state.templateName.trim()
        if (name.isEmpty()) {
            sendEvent(UIEvent.ShowToast("请输入食物名称"))
            return
        }
        val calories = state.templateCalories.toDoubleOrNull()
        if (calories == null || calories < 0) {
            sendEvent(UIEvent.ShowToast("热量需为非负数"))
            return
        }
        if (calories > 10000) {
            sendEvent(UIEvent.ShowToast("热量不应超过 10000"))
            return
        }

        val pro = state.templateProtein.toDoubleOrNull() ?: 0.0
        val fat = state.templateFat.toDoubleOrNull() ?: 0.0
        val carbs = state.templateCarbs.toDoubleOrNull() ?: 0.0

        val isEdit = state.editingId != null
        val isPreset = state.templates.find { it.id == state.editingId }?.isPreset ?: false
        val template = FoodTemplate(
            id = state.editingId ?: UUID.randomUUID().toString(),
            name = name,
            calories = calories,
            protein = pro,
            fat = fat,
            carbs = carbs,
            tags = state.templateTags.toList(),
            isPreset = isPreset
        )

        viewModelScope.launch {
            when (val result = repository.saveFoodTemplate(template)) {
                is Resource.Error -> sendEvent(UIEvent.ShowToast(result.message))
                is Resource.Success -> {
                    sendEvent(UIEvent.ShowToast(if (isEdit) "已更新" else "已保存"))
                    closeAddDialog()
                }
            }
        }
    }

    // ==================== 删除模板 ====================

    fun requestDelete(id: String) {
        _uiState.update { it.copy(deleteId = id) }
    }

    fun confirmDelete() {
        val id = _uiState.value.deleteId ?: return
        _uiState.update { it.copy(deleteId = null) }
        viewModelScope.launch {
            when (val result = repository.deleteFoodTemplate(id)) {
                is Resource.Error -> sendEvent(UIEvent.ShowToast(result.message))
                is Resource.Success -> sendEvent(UIEvent.ShowToast("已删除"))
            }
        }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(deleteId = null) }
    }

    // ==================== 重置表单 ====================

    private fun resetForm() {
        _uiState.update {
            it.copy(
                templateName = "",
                templateCalories = "",
                templateProtein = "",
                templateFat = "",
                templateCarbs = "",
                templateTags = emptySet(),
                newTagInput = "",
                editingId = null
            )
        }
    }

    // ==================== ViewModelFactory ====================

    class Factory(private val repository: LocalStorageRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FoodTemplateViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FoodTemplateViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
