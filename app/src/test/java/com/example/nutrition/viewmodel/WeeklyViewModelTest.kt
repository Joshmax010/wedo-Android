package com.example.nutrition.viewmodel

import androidx.lifecycle.ViewModelStore
import com.example.nutrition.domain.model.DayRecords
import com.example.nutrition.domain.model.MealRecord
import com.example.nutrition.domain.repository.LocalStorageRepository
import com.example.nutrition.domain.usecase.BackupManagerTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val store = ViewModelStore()
    @After fun cleanUp() { store.clear() }

    @Test
    fun report_observesRecordEdits_andUsesSelectedWeek() = runTest {
        val records = MutableStateFlow<Map<String, DayRecords>>(emptyMap())
        val repository = object : LocalStorageRepository by BackupManagerTest.FakeRepository() {
            override fun getAllRecords() = records
        }
        val viewModel = WeeklyViewModel(repository)
        store.put("weekly", viewModel)
        viewModel.loadData()
        runCurrent()
        assertFalse(viewModel.uiState.value.hasData)
        val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        records.value = mapOf(monday.toString() to DayRecords(dateStr = monday.toString(), breakfast = listOf(MealRecord(calories = 500.0))))
        runCurrent()
        assertTrue(viewModel.uiState.value.hasData)
        assertEquals(500, viewModel.uiState.value.caloriePoints.first().calories)
        viewModel.prevWeek()
        runCurrent()
        assertFalse(viewModel.uiState.value.hasData)
        val previous = monday.minusWeeks(1)
        records.value = records.value + (previous.toString() to DayRecords(dateStr = previous.toString(), breakfast = listOf(MealRecord(calories = 200.0))))
        runCurrent()
        assertEquals(200, viewModel.uiState.value.caloriePoints.first().calories)
        assertEquals(previous.toString().substring(5), viewModel.uiState.value.caloriePoints.first().label)
    }
}
