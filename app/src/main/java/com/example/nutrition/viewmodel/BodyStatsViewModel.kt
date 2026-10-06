package com.example.nutrition.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrition.domain.model.BodyRecord
import com.example.nutrition.domain.model.Resource
import com.example.nutrition.domain.repository.LocalStorageRepository
import com.example.nutrition.domain.usecase.BodyStatsValidator
import com.example.nutrition.domain.usecase.DateUtils
import com.example.nutrition.domain.usecase.UnitConverter
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 身体记录页 ViewModel
 *
 * 页面状态收敛为单一 UiState，记录列表由 Repository Flow 自动驱动，
 * 校验逻辑下沉到 BodyStatsValidator，写操作结果经 Resource 反馈
 */
class BodyStatsViewModel(
    private val repository: LocalStorageRepository
) : ViewModel() {

    // ==================== UI 状态 ====================

    data class UiState(
        val records: List<BodyRecord> = emptyList(),
        val dataError: String? = null,
        val dateError: String? = null,
        val selectedDate: String = DateUtils.today(),
        val editing: Boolean = false,
        val showDatePicker: Boolean = false,
        val weight: String = "",
        val bodyFat: String = "",
        val muscle: String = "",
        val note: String = "",
        val deleteDate: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 一次性事件（Toast 等）
    private val _events = Channel<UIEvent>(Channel.BUFFERED)
    val events: Flow<UIEvent> = _events.receiveAsFlow()

    private fun sendEvent(event: UIEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    private var dataJob: Job? = null
    private var dateJob: Job? = null
    private val editedDateFields = mutableSetOf<String>()

    init { initialize() }

    fun initialize() {
        if (dataJob?.isActive == true) return
        dataJob = viewModelScope.launchWithErrorFeedback("读取身体记录失败，请重试", { message ->
            _uiState.update { state -> state.copy(dataError = message) }
        }) {
            repository.getAllBodyRecords().collect { list ->
                _uiState.update { state -> state.copy(records = list.sortedBy { it.dateStr }, dataError = null) }
            }
        }
    }

    // ==================== 表单输入 ====================

    fun onWeightInput(value: String) {
        editedDateFields += "weight"
        _uiState.update { it.copy(weight = value) }
    }
    fun onBodyFatInput(value: String) {
        editedDateFields += "bodyFat"
        _uiState.update { it.copy(bodyFat = value) }
    }
    fun onMuscleInput(value: String) {
        editedDateFields += "muscle"
        _uiState.update { it.copy(muscle = value) }
    }
    fun onNoteInput(value: String) {
        editedDateFields += "note"
        _uiState.update { it.copy(note = value) }
    }

    // ==================== 日期选择 ====================

    fun setShowDatePicker(show: Boolean) {
        _uiState.update { it.copy(showDatePicker = show) }
    }

    fun selectDate(dateStr: String) {
        dateJob?.cancel()
        val changedDate = _uiState.value.selectedDate != dateStr
        if (changedDate) editedDateFields.clear()
        _uiState.update { state ->
            state.copy(
                selectedDate = dateStr, showDatePicker = false,
                editing = if (changedDate) false else state.editing,
                weight = if (changedDate) "" else state.weight,
                bodyFat = if (changedDate) "" else state.bodyFat,
                muscle = if (changedDate) "" else state.muscle,
                note = if (changedDate) "" else state.note
            )
        }
        dateJob = viewModelScope.launchWithErrorFeedback("读取该日身体记录失败，请重试", { message ->
            _uiState.update { it.copy(dateError = message) }
        }) {
            val existing = repository.getBodyRecord(dateStr).first()
            _uiState.update { it.copy(dateError = null) }
            if (_uiState.value.selectedDate != dateStr) return@launchWithErrorFeedback
            _uiState.update { state ->
                state.copy(
                    editing = existing != null,
                    weight = if ("weight" in editedDateFields) state.weight else existing?.let { r -> UnitConverter.formatForInput(r.weightKg) } ?: "",
                    bodyFat = if ("bodyFat" in editedDateFields) state.bodyFat else existing?.bodyFatPercent?.let { v -> UnitConverter.formatForInput(v) } ?: "",
                    muscle = if ("muscle" in editedDateFields) state.muscle else existing?.muscleKg?.let { v -> UnitConverter.formatForInput(v) } ?: "",
                    note = if ("note" in editedDateFields) state.note else existing?.note ?: ""
                )
            }
        }
    }

    // ==================== 保存记录 ====================

    /** The displayed history row is already a loaded snapshot; cancel older date reads. */
    fun editRecord(record: BodyRecord) {
        dateJob?.cancel()
        editedDateFields.clear()
        _uiState.update {
            it.copy(
                selectedDate = record.dateStr, editing = true, dateError = null, showDatePicker = false,
                weight = UnitConverter.formatForInput(record.weightKg),
                bodyFat = record.bodyFatPercent?.let(UnitConverter::formatForInput) ?: "",
                muscle = record.muscleKg?.let(UnitConverter::formatForInput) ?: "",
                note = record.note ?: ""
            )
        }
    }

    fun cancelEdit() {
        dateJob?.cancel()
        editedDateFields.clear()
        _uiState.update {
            it.copy(selectedDate = DateUtils.today(), editing = false, dateError = null,
                weight = "", bodyFat = "", muscle = "", note = "")
        }
    }

    fun saveRecord() {
        if (dateJob?.isActive == true || _uiState.value.dateError != null) {
            sendEvent(UIEvent.ShowToast("请先完成该日记录的读取，失败时点击重试"))
            return
        }
        val state = _uiState.value
        val error = BodyStatsValidator.validate(
            weight = state.weight,
            bodyFat = state.bodyFat,
            muscle = state.muscle
        )
        if (error != null) {
            sendEvent(UIEvent.ShowToast(error))
            return
        }

        val record = BodyRecord(
            dateStr = state.selectedDate,
            weightKg = state.weight.toDouble(),
            bodyFatPercent = state.bodyFat.toDoubleOrNull(),
            muscleKg = state.muscle.toDoubleOrNull(),
            note = state.note.trim().takeIf { it.isNotEmpty() }
        )

        viewModelScope.launchWithErrorFeedback("操作失败，请重试", { sendEvent(UIEvent.ShowToast(it)) }) {
            when (val result = repository.saveBodyRecord(record)) {
                is Resource.Error -> sendEvent(UIEvent.ShowToast(result.message))
                is Resource.Success -> sendEvent(UIEvent.SaveSuccess())
            }
        }
    }

    // ==================== 删除记录 ====================

    fun requestDelete(dateStr: String) {
        _uiState.update { it.copy(deleteDate = dateStr) }
    }

    fun confirmDelete() {
        val date = _uiState.value.deleteDate ?: return
        _uiState.update { it.copy(deleteDate = null) }
        viewModelScope.launchWithErrorFeedback("操作失败，请重试", { sendEvent(UIEvent.ShowToast(it)) }) {
            when (val result = repository.deleteBodyRecord(date)) {
                is Resource.Error -> sendEvent(UIEvent.ShowToast(result.message))
                is Resource.Success -> sendEvent(UIEvent.ShowToast("已删除"))
            }
        }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(deleteDate = null) }
    }
}
