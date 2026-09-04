package com.example.nutrition.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nutrition.domain.constants.NutrientConstants
import com.example.nutrition.domain.model.ActivityLevel
import com.example.nutrition.domain.model.BodyProfile
import com.example.nutrition.domain.model.Gender
import com.example.nutrition.domain.model.MicronutrientTarget
import com.example.nutrition.domain.model.NutritionTargets
import com.example.nutrition.domain.model.Resource
import com.example.nutrition.domain.repository.LocalStorageRepository
import com.example.nutrition.domain.usecase.BackupManager
import com.example.nutrition.domain.usecase.MetabolismCalculator
import com.example.nutrition.domain.usecase.UnitConverter
import com.example.nutrition.ui.components.NutrientConstantItem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * 设置页 ViewModel —— 对应小程序 pages/settings/settings.js
 *
 * 管理目标设置表单、数据导出/导入、清空数据、存储状态。
 * 页面状态收敛为单一 UiState，写操作结果经 Resource 反馈，
 * 一次性提示经 Channel 发送
 */
class SettingsViewModel(
    private val repository: LocalStorageRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    // ==================== 微量营养素目标行 ====================

    data class MicroTargetRow(
        val key: String,
        val name: String,
        val unit: String,
        val target: String   // 用户输入文本
    )

    // ==================== 存储状态 ====================

    data class StorageInfo(
        val currentKB: String,
        val limitKB: String,
        val ratio: Int
    )

    // ==================== UI 状态 ====================

    data class UiState(
        // 宏量目标表单
        val calories: String = "",
        val protein: String = "",
        val fat: String = "",
        val carbs: String = "",
        // 身体档案表单
        val gender: Gender? = null,
        val age: String = "",
        val height: String = "",
        val weight: String = "",
        val activityLevel: ActivityLevel? = null,
        // 计算结果预览
        val showCalcDialog: Boolean = false,
        val calcResult: MetabolismCalculator.Result? = null,
        // 微量目标列表
        val micronutrients: List<MicroTargetRow> = emptyList(),
        // 营养素选择器
        val showNutrientPicker: Boolean = false,
        // 导入弹窗
        val showImportModal: Boolean = false,
        val importText: String = "",
        val importPreview: BackupManager.BackupPreview? = null,
        val showPreview: Boolean = false,
        val importError: String = "",
        // 存储状态
        val storageInfo: StorageInfo? = null,
        // 清空确认
        val showClearConfirm: Boolean = false
    )

    /** 版本信息（静态） */
    val version: String = NutrientConstants.APP_VERSION

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 一次性事件（Toast 等）
    private val _events = Channel<UIEvent>(Channel.BUFFERED)
    val events: Flow<UIEvent> = _events.receiveAsFlow()

    private fun sendEvent(event: UIEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    // 当前是否由自动计算产生（用于保存时标记）
    private var isAutoCalculatedFlag: Boolean = false

    // ==================== 初始化 ====================

    fun initialize() {
        loadTargets()
        loadStorageStatus()
    }

    // ==================== 目标值加载 ====================

    fun loadTargets() {
        viewModelScope.launch {
            val targets = repository.getTargets().first()
                ?: NutrientConstants.getDefaultTargets()

            _uiState.update { state ->
                state.copy(
                    calories = UnitConverter.formatForInput(targets.calories),
                    protein = UnitConverter.formatForInput(targets.protein),
                    fat = UnitConverter.formatForInput(targets.fat),
                    carbs = UnitConverter.formatForInput(targets.carbs),
                    micronutrients = targets.micronutrients.map { mn ->
                        MicroTargetRow(
                            key = mn.key,
                            name = mn.name,
                            unit = mn.unit,
                            target = UnitConverter.formatForInput(mn.target)
                        )
                    }
                )
            }

            // 加载身体档案
            val profile = targets.bodyProfile
            _uiState.update {
                it.copy(
                    gender = profile?.gender,
                    age = profile?.age?.toString() ?: "",
                    height = profile?.heightCm?.toString() ?: "",
                    weight = profile?.weightKg?.toString() ?: "",
                    activityLevel = profile?.activityLevel
                )
            }
            isAutoCalculatedFlag = targets.isAutoCalculated
        }
    }

    private fun loadStorageStatus() {
        viewModelScope.launch {
            val status = repository.getStorageStatus()
            val ratio = if (status.limitSize > 0) {
                (status.currentSize.toDouble() / status.limitSize * 100).toInt()
            } else 0
            _uiState.update {
                it.copy(
                    storageInfo = StorageInfo(
                        currentKB = status.currentSize.toString(),
                        limitKB = status.limitSize.toString(),
                        ratio = ratio
                    )
                )
            }
        }
    }

    // ==================== 表单输入 ====================

    fun onCaloriesInput(value: String) { _uiState.update { it.copy(calories = value) } }
    fun onProteinInput(value: String) { _uiState.update { it.copy(protein = value) } }
    fun onFatInput(value: String) { _uiState.update { it.copy(fat = value) } }
    fun onCarbsInput(value: String) { _uiState.update { it.copy(carbs = value) } }

    fun onGenderInput(value: Gender) { _uiState.update { it.copy(gender = value) } }
    fun onAgeInput(value: String) { _uiState.update { it.copy(age = value) } }
    fun onHeightInput(value: String) { _uiState.update { it.copy(height = value) } }
    fun onWeightInput(value: String) { _uiState.update { it.copy(weight = value) } }
    fun onActivityLevelInput(value: ActivityLevel) {
        _uiState.update { it.copy(activityLevel = value) }
    }

    // ==================== 身体档案计算 ====================

    /**
     * 根据当前身体档案表单计算推荐目标
     */
    fun calculateRecommendations() {
        val state = _uiState.value
        val profile = buildBodyProfileOrNull(state)
        if (profile == null) {
            sendEvent(UIEvent.ShowToast("请完整填写身体档案信息"))
            return
        }
        val micros = state.micronutrients.map { mn ->
            MicronutrientTarget(
                key = mn.key,
                name = mn.name,
                unit = mn.unit,
                target = mn.target.toDoubleOrNull() ?: 0.0
            )
        }
        _uiState.update {
            it.copy(
                calcResult = MetabolismCalculator.calculate(profile, micros),
                showCalcDialog = true
            )
        }
    }

    /**
     * 将计算出的推荐目标填充到表单
     */
    fun applyCalculatedTargets() {
        val result = _uiState.value.calcResult ?: return
        val targets = result.targets
        isAutoCalculatedFlag = true

        _uiState.update {
            it.copy(
                calories = UnitConverter.formatForInput(targets.calories),
                protein = UnitConverter.formatForInput(targets.protein),
                fat = UnitConverter.formatForInput(targets.fat),
                carbs = UnitConverter.formatForInput(targets.carbs),
                calcResult = null,
                showCalcDialog = false
            )
        }
        sendEvent(UIEvent.ShowToast("已填充推荐值，请保存"))
    }

    fun dismissCalcDialog() {
        _uiState.update { it.copy(showCalcDialog = false, calcResult = null) }
    }

    private fun buildBodyProfileOrNull(state: UiState): BodyProfile? {
        val selectedGender = state.gender ?: return null
        val ageValue = state.age.toIntOrNull() ?: return null
        val heightValue = state.height.toIntOrNull() ?: return null
        val weightValue = state.weight.toDoubleOrNull() ?: return null
        val activity = state.activityLevel ?: return null

        if (ageValue <= 0 || ageValue > 120 ||
            heightValue <= 0 || heightValue > 300 ||
            weightValue <= 0 || weightValue > 500
        ) {
            return null
        }

        return BodyProfile(
            gender = selectedGender,
            age = ageValue,
            heightCm = heightValue,
            weightKg = weightValue,
            activityLevel = activity
        )
    }

    // ==================== 微量营养素增删 ====================

    fun openNutrientPicker() {
        _uiState.update { it.copy(showNutrientPicker = true) }
    }

    fun dismissNutrientPicker() {
        _uiState.update { it.copy(showNutrientPicker = false) }
    }

    fun addMicronutrient(item: NutrientConstantItem) {
        _uiState.update { state ->
            if (state.micronutrients.any { it.key == item.key }) return@update state
            state.copy(
                micronutrients = state.micronutrients + MicroTargetRow(
                    key = item.key,
                    name = item.name,
                    unit = item.unit,
                    target = UnitConverter.formatForInput(item.target)
                ),
                showNutrientPicker = false
            )
        }
    }

    fun onMicroTargetInput(index: Int, value: String) {
        _uiState.update { state ->
            if (index !in state.micronutrients.indices) return@update state
            val list = state.micronutrients.toMutableList()
            list[index] = list[index].copy(target = value)
            state.copy(micronutrients = list)
        }
    }

    fun deleteMicronutrient(index: Int) {
        _uiState.update { state ->
            if (index !in state.micronutrients.indices) return@update state
            state.copy(micronutrients = state.micronutrients.filterIndexed { i, _ -> i != index })
        }
    }

    // ==================== 保存目标 ====================

    fun saveTargets() {
        val state = _uiState.value

        // 校验宏量
        val cal = state.calories.toDoubleOrNull()
        if (cal == null || cal <= 0) {
            sendEvent(UIEvent.ShowToast("热量目标需为正数")); return
        }
        if (cal > 10000) {
            sendEvent(UIEvent.ShowToast("热量目标不应超过 10000")); return
        }

        val pro = state.protein.toDoubleOrNull()
        if (pro == null || pro <= 0) {
            sendEvent(UIEvent.ShowToast("蛋白质目标需为正数")); return
        }

        val f = state.fat.toDoubleOrNull()
        if (f == null || f <= 0) {
            sendEvent(UIEvent.ShowToast("脂肪目标需为正数")); return
        }

        val c = state.carbs.toDoubleOrNull()
        if (c == null || c <= 0) {
            sendEvent(UIEvent.ShowToast("碳水目标需为正数")); return
        }

        if (pro > 500 || f > 500 || c > 500) {
            sendEvent(UIEvent.ShowToast("营养素目标不应超过 500g")); return
        }

        // 校验微量
        val micros = mutableListOf<MicronutrientTarget>()
        for (mn in state.micronutrients) {
            val target = mn.target.toDoubleOrNull()
            if (target == null || target <= 0) {
                sendEvent(UIEvent.ShowToast("${mn.name}目标需为正数")); return
            }
            micros.add(
                MicronutrientTarget(
                    key = mn.key, name = mn.name, unit = mn.unit, target = target
                )
            )
        }

        val bodyProfile = buildBodyProfileOrNull(state)
        val targets = NutritionTargets(
            calories = cal,
            protein = pro,
            fat = f,
            carbs = c,
            micronutrients = micros,
            updatedAt = Instant.now().toString(),
            bodyProfile = bodyProfile,
            isAutoCalculated = isAutoCalculatedFlag && bodyProfile != null
        )

        viewModelScope.launch {
            when (val result = repository.setTargets(targets)) {
                is Resource.Error -> sendEvent(UIEvent.ShowToast(result.message))
                is Resource.Success -> sendEvent(UIEvent.ShowToast("已保存"))
            }
        }
    }

    // ==================== 数据导出 ====================

    fun exportData(context: Context) {
        viewModelScope.launch {
            val result = backupManager.exportData()
            if (!result.success) {
                sendEvent(UIEvent.ShowToast(result.error.ifEmpty { "导出失败" }))
                return@launch
            }
            try {
                val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("备份数据", result.json))
                sendEvent(UIEvent.ShowToast("已复制到剪贴板"))
            } catch (e: Exception) {
                sendEvent(UIEvent.ShowToast("复制失败"))
            }
        }
    }

    // ==================== 数据导入 ====================

    fun showImport() {
        _uiState.update {
            it.copy(
                showImportModal = true,
                importText = "",
                importPreview = null,
                showPreview = false,
                importError = ""
            )
        }
    }

    fun hideImport() {
        _uiState.update {
            it.copy(
                showImportModal = false,
                importPreview = null,
                showPreview = false,
                importError = ""
            )
        }
    }

    fun onImportTextInput(value: String) {
        _uiState.update { state ->
            state.copy(
                importText = value,
                showPreview = if (state.showPreview || state.importError.isNotEmpty()) false else state.showPreview,
                importPreview = if (state.showPreview || state.importError.isNotEmpty()) null else state.importPreview,
                importError = if (state.showPreview || state.importError.isNotEmpty()) "" else state.importError
            )
        }
    }

    fun doPreview() {
        val state = _uiState.value
        if (state.importText.isBlank()) {
            sendEvent(UIEvent.ShowToast("请粘贴备份数据"))
            return
        }
        val result = backupManager.previewData(state.importText)
        if (!result.valid) {
            _uiState.update {
                it.copy(showPreview = false, importPreview = null, importError = result.error)
            }
            return
        }
        _uiState.update {
            it.copy(showPreview = true, importPreview = result.preview, importError = "")
        }
    }

    fun backToEdit() {
        _uiState.update { it.copy(showPreview = false, importPreview = null) }
    }

    fun confirmImport() {
        val importText = _uiState.value.importText
        if (importText.isBlank()) return
        viewModelScope.launch {
            val result = backupManager.importData(importText)
            if (result.success) {
                sendEvent(UIEvent.ShowToast(result.summary))
                hideImport()
                loadTargets()
                loadStorageStatus()
            } else {
                _uiState.update { it.copy(importError = result.error) }
            }
        }
    }

    // ==================== 清空数据 ====================

    fun requestClear() {
        _uiState.update { it.copy(showClearConfirm = true) }
    }

    fun confirmClear() {
        _uiState.update { it.copy(showClearConfirm = false) }
        viewModelScope.launch {
            val result = repository.clearRecords()
            sendEvent(
                UIEvent.ShowToast(
                    if (result is Resource.Success) "已清空"
                    else (result as? Resource.Error)?.message ?: "清空失败"
                )
            )
            loadStorageStatus()
        }
    }

    fun dismissClear() {
        _uiState.update { it.copy(showClearConfirm = false) }
    }

    // ==================== ViewModelFactory ====================

    class Factory(
        private val repository: LocalStorageRepository,
        private val backupManager: BackupManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(repository, backupManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
