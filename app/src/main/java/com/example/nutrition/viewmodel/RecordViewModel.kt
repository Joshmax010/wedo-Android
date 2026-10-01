package com.example.nutrition.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nutrition.domain.constants.NutrientConstants
import com.example.nutrition.domain.model.FoodTemplate
import com.example.nutrition.domain.model.MealKey
import com.example.nutrition.domain.model.MealMicro
import com.example.nutrition.domain.model.MealRecord
import com.example.nutrition.domain.model.Resource
import com.example.nutrition.domain.repository.LocalStorageRepository
import com.example.nutrition.domain.usecase.DateUtils
import com.example.nutrition.domain.usecase.FoodTemplateMapper
import com.example.nutrition.domain.usecase.MealFormValidator
import com.example.nutrition.domain.usecase.UnitConverter
import com.example.nutrition.ui.components.NutrientConstantItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/**
 * 录入页 ViewModel —— 对应小程序 pages/record/record.js
 *
 * 管理录入表单、记录列表、编辑/删除操作。
 * 页面状态收敛为单一 RecordUiState（StateFlow 驱动，数据层 Flow 自动刷新），
 * 一次性事件（Toast）经 Channel 发送，写操作结果经 Resource 反馈
 */
class RecordViewModel(
    private val repository: LocalStorageRepository
) : ViewModel() {

    // ==================== 表单微量营养素行 ====================

    data class FormMicro(
        val key: String,
        val name: String,
        val unit: String,
        val value: String = ""   // 用户输入文本
    )

    // ==================== UI 状态 ====================

    data class UiState(
        val currentMeal: MealKey = MealKey.fromCurrentTime(),
        val currentDate: String = DateUtils.today(),
        // 表单字段
        val foodName: String = "",
        val calories: String = "",
        val kiloJoules: String = "",
        val protein: String = "",
        val fat: String = "",
        val carbs: String = "",
        val formMicronutrients: List<FormMicro> = emptyList(),
        // 重量与每100g基础营养素
        val weight: String = "100",
        val baseCalories: String = "",
        val baseProtein: String = "",
        val baseFat: String = "",
        val baseCarbs: String = "",
        val baseFormMicronutrients: List<FormMicro> = emptyList(),
        // 营养素选择器
        val showNutrientPicker: Boolean = false,
        // 记录列表
        val recordList: List<MealRecord> = emptyList(),
        // 编辑状态
        val editingId: String? = null,
        // 保存为模板弹窗
        val pendingTemplate: FoodTemplate? = null,
        val saveAsTemplateTags: Set<String> = emptySet(),
        val newTemplateTagInput: String = "",
        // 删除确认弹窗
        val deleteConfirm: DeleteTarget? = null,
        // 食物模板
        val templates: List<FoodTemplate> = emptyList(),
        val templateSuggestions: List<FoodTemplate> = emptyList(),
        val dataError: String? = null,
        val templateError: String? = null
    ) {
        /** 当前模板库中已有的所有标签 */
        val allTemplateTags: List<String>
            get() = templates.map { it.tags }.flatten().distinct().sorted()
    }

    // 删除确认弹窗（存储快照防止日期/餐次切换导致误删）
    data class DeleteTarget(val id: String, val dateStr: String, val mealKey: MealKey)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 一次性事件（Toast 等）
    private val _events = Channel<UIEvent>(Channel.BUFFERED)
    val events: Flow<UIEvent> = _events.receiveAsFlow()

    private fun sendEvent(event: UIEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    // 编辑中的记录创建时间（不参与渲染，不进 UiState）
    private var editingCreatedAt: String? = null

    private var recordsJob: Job? = null
    private var templatesJob: Job? = null

    init {
        loadData()
        // Suggestions currently observe templates independently; sharing is optimized separately.
        viewModelScope.launchWithErrorFeedback("读取食物模板失败，请重试", { sendEvent(UIEvent.ShowToast(it)) }) {
            combine(
                _uiState.map { it.foodName }.distinctUntilChanged(),
                repository.getAllFoodTemplates()
            ) { query, templates ->
                val trimmed = query.trim()
                if (trimmed.isEmpty()) emptyList()
                else templates.filter { it.name.contains(trimmed, ignoreCase = true) }.take(5)
            }.collect { suggestions -> _uiState.update { it.copy(templateSuggestions = suggestions) } }
        }
    }

    fun loadData() {
        if (templatesJob?.isActive != true) {
            templatesJob = viewModelScope.launchWithErrorFeedback("读取食物模板失败，请重试", { message ->
                _uiState.update { it.copy(templateError = message) }
            }) {
                repository.getAllFoodTemplates().collect { templates ->
                    _uiState.update { it.copy(templates = templates, templateError = null) }
                }
            }
        }
        if (recordsJob?.isActive != true) {
            recordsJob = viewModelScope.launchWithErrorFeedback("读取饮食记录失败，请重试", { message ->
                _uiState.update { it.copy(dataError = message) }
            }) {
                _uiState.map { it.currentDate to it.currentMeal }.distinctUntilChanged()
                    .flatMapLatest { (date, meal) ->
                        repository.getDayRecords(date).map { it.getMeal(meal) }
                    }.collect { records ->
                        _uiState.update { it.copy(recordList = records, dataError = null) }
                    }
            }
        }
    }

    // ==================== 初始化 ====================

    /**
     * 接收从首页传来的选中餐次（可选），重置表单
     */
    fun initWithMeal(mealKey: MealKey?) {
        resetForm()
        _uiState.update { it.copy(currentMeal = mealKey ?: MealKey.fromCurrentTime()) }
    }

    // ==================== 日期跳转 ====================

    /**
     * 跳转到指定日期
     */
    fun pickDate(dateStr: String) {
        _uiState.update { it.copy(currentDate = dateStr) }
    }

    // ==================== 时间刷新 ====================

    /**
     * 仅根据当前时间刷新默认餐次（不重置表单），用于页面 resume 时
     */
    fun refreshMealByTime() {
        _uiState.update { it.copy(currentMeal = MealKey.fromCurrentTime()) }
    }

    // ==================== 餐次切换 ====================

    fun switchMeal(meal: MealKey) {
        resetForm()
        _uiState.update { it.copy(currentMeal = meal) }
    }

    // ==================== 日期切换 ====================

    fun prevDay() {
        _uiState.update { it.copy(currentDate = DateUtils.prevDay(it.currentDate)) }
    }

    fun nextDay() {
        _uiState.update { state ->
            val next = DateUtils.nextDay(state.currentDate)
            if (next <= DateUtils.today()) state.copy(currentDate = next) else state
        }
    }

    // ==================== 表单输入 ====================

    fun onNameInput(value: String) {
        _uiState.update { it.copy(foodName = value) }
    }

    fun onCaloriesInput(value: String) {
        _uiState.update { state ->
            state.copy(
                calories = value,
                kiloJoules = UnitConverter.caloriesToKiloJoules(value)
            ).withBaseFromActuals()
        }
    }

    fun onKiloJoulesInput(value: String) {
        _uiState.update { state ->
            state.copy(
                kiloJoules = value,
                calories = UnitConverter.kiloJoulesToCalories(value)
            ).withBaseFromActuals()
        }
    }

    fun onProteinInput(value: String) {
        _uiState.update { it.copy(protein = value).withBaseFromActuals() }
    }

    fun onFatInput(value: String) {
        _uiState.update { it.copy(fat = value).withBaseFromActuals() }
    }

    fun onCarbsInput(value: String) {
        _uiState.update { it.copy(carbs = value).withBaseFromActuals() }
    }

    fun onWeightInput(value: String) {
        _uiState.update { it.copy(weight = value).withActualsFromBase() }
    }

    /** 按当前重量，将实际摄入值换算为每100g基准值 */
    private fun UiState.withBaseFromActuals(): UiState {
        val w = weight.toDoubleOrNull() ?: 100.0
        if (w <= 0) return this
        return copy(
            baseCalories = calories.toDoubleOrNull()
                ?.let { UnitConverter.formatForInput(it * 100 / w) } ?: "",
            baseProtein = protein.toDoubleOrNull()
                ?.let { UnitConverter.formatForInput(it * 100 / w) } ?: "",
            baseFat = fat.toDoubleOrNull()
                ?.let { UnitConverter.formatForInput(it * 100 / w) } ?: "",
            baseCarbs = carbs.toDoubleOrNull()
                ?.let { UnitConverter.formatForInput(it * 100 / w) } ?: "",
            baseFormMicronutrients = formMicronutrients.map { actual ->
                val base = if (actual.value.isEmpty()) "" else {
                    actual.value.toDoubleOrNull()
                        ?.let { UnitConverter.formatForInput(it * 100 / w) } ?: ""
                }
                actual.copy(value = base)
            }
        )
    }

    /** 按当前重量，将每100g基准值换算为实际摄入值 */
    private fun UiState.withActualsFromBase(): UiState {
        val w = weight.toDoubleOrNull() ?: 100.0
        if (w <= 0) return this
        val newCalories = baseCalories.toDoubleOrNull()
            ?.let { UnitConverter.formatForInput(it * w / 100) } ?: calories
        return copy(
            calories = newCalories,
            kiloJoules = UnitConverter.caloriesToKiloJoules(newCalories),
            protein = baseProtein.toDoubleOrNull()
                ?.let { UnitConverter.formatForInput(it * w / 100) } ?: protein,
            fat = baseFat.toDoubleOrNull()
                ?.let { UnitConverter.formatForInput(it * w / 100) } ?: fat,
            carbs = baseCarbs.toDoubleOrNull()
                ?.let { UnitConverter.formatForInput(it * w / 100) } ?: carbs,
            formMicronutrients = baseFormMicronutrients.map { base ->
                val actual = if (base.value.isEmpty()) "" else {
                    base.value.toDoubleOrNull()
                        ?.let { UnitConverter.formatForInput(it * w / 100) } ?: ""
                }
                base.copy(value = actual)
            }
        )
    }

    // ==================== 营养素选择器 ====================

    fun toggleNutrientPicker() {
        _uiState.update { it.copy(showNutrientPicker = !it.showNutrientPicker) }
    }

    fun dismissNutrientPicker() {
        _uiState.update { it.copy(showNutrientPicker = false) }
    }

    fun addMicronutrient(item: NutrientConstantItem) {
        _uiState.update { state ->
            // 防止重复添加
            if (state.formMicronutrients.any { it.key == item.key }) return@update state
            val micro = FormMicro(key = item.key, name = item.name, unit = item.unit)
            state.copy(
                formMicronutrients = state.formMicronutrients + micro,
                baseFormMicronutrients = state.baseFormMicronutrients + micro,
                showNutrientPicker = false
            )
        }
    }

    fun onMicroValueInput(index: Int, value: String) {
        _uiState.update { state ->
            if (index !in state.formMicronutrients.indices) return@update state
            val micros = state.formMicronutrients.toMutableList()
            micros[index] = micros[index].copy(value = value)
            state.copy(formMicronutrients = micros).withBaseFromActuals()
        }
    }

    fun deleteMicronutrient(index: Int) {
        _uiState.update { state ->
            if (index !in state.formMicronutrients.indices) return@update state
            val key = state.formMicronutrients[index].key
            state.copy(
                formMicronutrients = state.formMicronutrients.filterIndexed { i, _ -> i != index },
                baseFormMicronutrients = state.baseFormMicronutrients.filter { it.key != key }
            )
        }
    }

    // ==================== 保存为模板弹窗 ====================

    fun onNewTemplateTagInput(value: String) {
        _uiState.update { it.copy(newTemplateTagInput = value) }
    }

    fun addNewTemplateTag() {
        _uiState.update { state ->
            val tag = state.newTemplateTagInput.trim()
            if (tag.isEmpty()) {
                state
            } else {
                state.copy(
                    saveAsTemplateTags = state.saveAsTemplateTags + tag,
                    newTemplateTagInput = ""
                )
            }
        }
    }

    fun toggleSaveAsTemplateTag(tag: String) {
        _uiState.update { state ->
            state.copy(
                saveAsTemplateTags = if (tag in state.saveAsTemplateTags) {
                    state.saveAsTemplateTags - tag
                } else {
                    state.saveAsTemplateTags + tag
                }
            )
        }
    }

    fun removeSaveAsTemplateTag(tag: String) {
        _uiState.update { it.copy(saveAsTemplateTags = it.saveAsTemplateTags - tag) }
    }

    fun confirmSaveAsTemplate() {
        val state = _uiState.value
        val template = state.pendingTemplate ?: return
        val tagged = template.copy(tags = state.saveAsTemplateTags.toList())
        dismissSaveTemplatePrompt()

        viewModelScope.launchWithErrorFeedback("操作失败，请重试", { sendEvent(UIEvent.ShowToast(it)) }) {
            when (val result = repository.saveFoodTemplate(tagged)) {
                is Resource.Success -> sendEvent(UIEvent.ShowToast("已保存到模板"))
                is Resource.Error -> sendEvent(UIEvent.ShowToast(result.message))
            }
        }
    }

    fun dismissSaveTemplatePrompt() {
        resetForm()
        _uiState.update {
            it.copy(pendingTemplate = null, saveAsTemplateTags = emptySet(), newTemplateTagInput = "")
        }
    }

    private fun shouldPromptSaveAsTemplate(name: String): Boolean {
        if (name.isBlank()) return false
        return _uiState.value.templates.none { it.name.equals(name, ignoreCase = true) }
    }

    // ==================== 保存记录 ====================

    fun saveRecord() {
        val state = _uiState.value
        val error = MealFormValidator.validate(
            calories = state.calories,
            protein = state.protein,
            fat = state.fat,
            carbs = state.carbs,
            micros = state.formMicronutrients.map {
                MealFormValidator.MicroInput(name = it.name, value = it.value)
            }
        )
        if (error != null) {
            sendEvent(UIEvent.ShowToast(error))
            return
        }

        val cal = state.calories.toDouble()
        val pro = state.protein.toDoubleOrNull() ?: 0.0
        val f = state.fat.toDoubleOrNull() ?: 0.0
        val c = state.carbs.toDoubleOrNull() ?: 0.0

        // 构造微量营养素列表（只保留有值的）
        val micros = state.formMicronutrients
            .filter { it.value.isNotEmpty() }
            .map { MealMicro(key = it.key, value = it.value.toDouble()) }

        val now = Instant.now().toString()
        val record = MealRecord(
            id = state.editingId ?: UUID.randomUUID().toString(),
            name = state.foodName.trim(),
            calories = cal,
            protein = pro,
            fat = f,
            carbs = c,
            micronutrients = micros,
            weightGrams = state.weight.toDoubleOrNull() ?: 100.0,
            createdAt = editingCreatedAt ?: now
        )

        val isEdit = state.editingId != null
        val editingId = state.editingId
        val currentDate = state.currentDate
        val currentMeal = state.currentMeal

        viewModelScope.launchWithErrorFeedback("操作失败，请重试", { sendEvent(UIEvent.ShowToast(it)) }) {
            val result = if (isEdit) {
                repository.updateRecord(currentDate, currentMeal, editingId!!, record)
            } else {
                repository.addRecord(currentDate, currentMeal, record)
            }

            if (result is Resource.Error) {
                sendEvent(UIEvent.ShowToast(result.message))
                return@launchWithErrorFeedback
            }

            // 新增记录且食物名未存在模板中时，询问是否保存为模板
            if (!isEdit && shouldPromptSaveAsTemplate(record.name)) {
                _uiState.update {
                    it.copy(pendingTemplate = FoodTemplateMapper.fromMealRecord(record))
                }
                sendEvent(UIEvent.ShowToast("已保存"))
            } else {
                resetForm()
                sendEvent(UIEvent.ShowToast("已保存"))
            }
        }
    }

    // ==================== 编辑记录 ====================

    fun editRecord(recordId: String) {
        val record = _uiState.value.recordList.find { it.id == recordId } ?: return

        editingCreatedAt = record.createdAt
        val caloriesText = UnitConverter.formatForInput(record.calories)
        _uiState.update {
            it.copy(
                foodName = record.name,
                weight = UnitConverter.formatForInput(record.weightGrams),
                calories = caloriesText,
                kiloJoules = UnitConverter.caloriesToKiloJoules(caloriesText),
                protein = if (record.protein > 0) UnitConverter.formatForInput(record.protein) else "",
                fat = if (record.fat > 0) UnitConverter.formatForInput(record.fat) else "",
                carbs = if (record.carbs > 0) UnitConverter.formatForInput(record.carbs) else "",
                baseCalories = UnitConverter.formatForInput(record.calories / record.weightGrams * 100),
                baseProtein = UnitConverter.formatForInput(record.protein / record.weightGrams * 100),
                baseFat = UnitConverter.formatForInput(record.fat / record.weightGrams * 100),
                baseCarbs = UnitConverter.formatForInput(record.carbs / record.weightGrams * 100),
                formMicronutrients = record.micronutrients.map { mn ->
                    val config = NutrientConstants.MICROS.find { config -> config.key == mn.key }
                    FormMicro(
                        key = mn.key,
                        name = config?.name ?: mn.key,
                        unit = config?.unit ?: "",
                        value = UnitConverter.formatForInput(mn.value)
                    )
                },
                baseFormMicronutrients = record.micronutrients.map { mn ->
                    val config = NutrientConstants.MICROS.find { config -> config.key == mn.key }
                    FormMicro(
                        key = mn.key,
                        name = config?.name ?: mn.key,
                        unit = config?.unit ?: "",
                        value = UnitConverter.formatForInput(mn.value / record.weightGrams * 100)
                    )
                },
                editingId = record.id,
                templateSuggestions = emptyList()
            )
        }
    }

    fun cancelEdit() {
        resetForm()
    }

    // ==================== 删除记录 ====================

    fun requestDelete(recordId: String) {
        _uiState.update { state ->
            state.copy(
                deleteConfirm = DeleteTarget(
                    id = recordId,
                    dateStr = state.currentDate,
                    mealKey = state.currentMeal
                )
            )
        }
    }

    fun confirmDelete() {
        val target = _uiState.value.deleteConfirm ?: return
        _uiState.update { it.copy(deleteConfirm = null) }

        viewModelScope.launchWithErrorFeedback("操作失败，请重试", { sendEvent(UIEvent.ShowToast(it)) }) {
            val result = repository.deleteRecord(target.dateStr, target.mealKey, target.id)
            when (result) {
                is Resource.Error -> sendEvent(UIEvent.ShowToast(result.message))
                is Resource.Success -> sendEvent(UIEvent.ShowToast("已删除"))
            }
        }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(deleteConfirm = null) }
    }

    // ==================== 模板自动补全 ====================

    fun applyTemplate(template: FoodTemplate) {
        _uiState.update { state ->
            state.copy(
                foodName = template.name,
                baseCalories = UnitConverter.formatForInput(template.calories),
                baseProtein = UnitConverter.formatForInput(template.protein),
                baseFat = UnitConverter.formatForInput(template.fat),
                baseCarbs = UnitConverter.formatForInput(template.carbs),
                baseFormMicronutrients = template.micronutrients.map { micro ->
                    val config = NutrientConstants.MICROS.find { it.key == micro.key }
                    FormMicro(
                        key = micro.key,
                        name = config?.name ?: micro.key,
                        unit = config?.unit ?: "",
                        value = UnitConverter.formatForInput(micro.value)
                    )
                },
                weight = if (state.weight.isEmpty()) "100" else state.weight,
                templateSuggestions = emptyList()
            ).withActualsFromBase()
        }
    }

    fun clearTemplateSuggestions() {
        _uiState.update { it.copy(templateSuggestions = emptyList()) }
    }

    // ==================== 重置表单 ====================

    private fun resetForm() {
        editingCreatedAt = null
        _uiState.update { it.copyEmptyForm() }
    }

    private fun UiState.copyEmptyForm(): UiState = copy(
        foodName = "",
        calories = "",
        kiloJoules = "",
        protein = "",
        fat = "",
        carbs = "",
        formMicronutrients = emptyList(),
        weight = "100",
        baseCalories = "",
        baseProtein = "",
        baseFat = "",
        baseCarbs = "",
        baseFormMicronutrients = emptyList(),
        editingId = null
    )

    // ==================== ViewModelFactory ====================

    class Factory(private val repository: LocalStorageRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RecordViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return RecordViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
