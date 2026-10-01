package com.example.nutrition.viewmodel

import androidx.lifecycle.ViewModelStore
import com.example.nutrition.domain.model.BodyRecord
import com.example.nutrition.domain.repository.LocalStorageRepository
import com.example.nutrition.domain.usecase.BackupManagerTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BodyStatsViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val store = ViewModelStore()
    @After fun cleanUp() { store.clear() }

    @Test
    fun delayedDateRead_doesNotOverwriteUserInput() = runTest {
        val loaded = CompletableDeferred<Unit>()
        val repository = object : LocalStorageRepository by BackupManagerTest.FakeRepository() {
            override fun getBodyRecord(dateStr: String) = flow<BodyRecord?> {
                loaded.await()
                emit(BodyRecord(dateStr, weightKg = 70.0, bodyFatPercent = 20.0, note = "原有备注"))
            }
        }
        val viewModel = BodyStatsViewModel(repository)
        store.put("body", viewModel)
        viewModel.selectDate("2026-09-28")
        runCurrent()
        viewModel.onWeightInput("72")
        loaded.complete(Unit)
        runCurrent()
        assertEquals("72", viewModel.uiState.value.weight)
        assertEquals("20", viewModel.uiState.value.bodyFat)
        assertEquals("原有备注", viewModel.uiState.value.note)
        assertNull(viewModel.uiState.value.dateError)
    }

    @Test
    fun changingDate_cancelsPreviousRead_andKeepsLatestResult() = runTest {
        val oldRead = CompletableDeferred<Unit>()
        val repository = object : LocalStorageRepository by BackupManagerTest.FakeRepository() {
            override fun getBodyRecord(dateStr: String) = flow<BodyRecord?> {
                if (dateStr == "2026-09-28") oldRead.await()
                emit(BodyRecord(dateStr, weightKg = if (dateStr == "2026-09-28") 70.0 else 72.0))
            }
        }
        val viewModel = BodyStatsViewModel(repository)
        store.put("body", viewModel)
        viewModel.selectDate("2026-09-28")
        runCurrent()
        viewModel.selectDate("2026-09-29")
        runCurrent()
        oldRead.complete(Unit)
        runCurrent()
        assertEquals("2026-09-29", viewModel.uiState.value.selectedDate)
        assertEquals("72", viewModel.uiState.value.weight)
    }
}
