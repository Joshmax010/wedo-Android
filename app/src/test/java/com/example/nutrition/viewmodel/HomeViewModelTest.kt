package com.example.nutrition.viewmodel

import androidx.lifecycle.ViewModelStore
import com.example.nutrition.domain.model.DayRecords
import com.example.nutrition.domain.model.MealRecord
import com.example.nutrition.domain.constants.NutrientConstants
import com.example.nutrition.domain.usecase.BackupManagerTest
import com.example.nutrition.domain.usecase.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val store = ViewModelStore()
    @After fun cleanUp() { store.clear() }

    @Test fun zeroCalorieRecordStillDisplaysDashboard() = runTest {
        val repository = BackupManagerTest.FakeRepository()
        val today = DateUtils.today()
        repository.setDayRecords(today, DayRecords(today, snack = listOf(MealRecord(calories = 0.0))))
        val viewModel = HomeViewModel(repository)
        store.put("home", viewModel)
        runCurrent()
        assertTrue(viewModel.uiState.value.hasData)
        assertEquals(1, viewModel.uiState.value.meals.sumOf { it.count })
    }

    @Test fun exactCalorieTargetDisplaysZeroGap() = runTest {
        val repository = BackupManagerTest.FakeRepository()
        val today = DateUtils.today()
        repository.setTargets(NutrientConstants.getDefaultTargets().copy(calories = 2000.0))
        repository.setDayRecords(today, DayRecords(today, lunch = listOf(MealRecord(calories = 2000.0))))
        val viewModel = HomeViewModel(repository)
        store.put("home", viewModel)
        runCurrent()
        assertEquals("0", viewModel.uiState.value.ringCenterText)
        assertEquals("已达目标", viewModel.uiState.value.ringGapLabel)
        assertEquals(100, viewModel.uiState.value.ringPercent)
    }

    @Test fun belowTargetShowsPositiveAmountWithRemainingLabel() = runTest {
        val repository = BackupManagerTest.FakeRepository()
        val today = DateUtils.today()
        repository.setTargets(NutrientConstants.getDefaultTargets().copy(calories = 2000.0))
        repository.setDayRecords(today, DayRecords(today, lunch = listOf(MealRecord(calories = 133.0))))
        val viewModel = HomeViewModel(repository)
        store.put("home", viewModel)
        runCurrent()
        assertEquals("1867", viewModel.uiState.value.ringCenterText)
        assertEquals("还差", viewModel.uiState.value.ringGapLabel)
    }

    @Test fun smallExcessUsesActualAmountEvenWhenPercentRoundsToOneHundred() = runTest {
        val repository = BackupManagerTest.FakeRepository()
        val today = DateUtils.today()
        repository.setTargets(NutrientConstants.getDefaultTargets().copy(calories = 2000.0))
        repository.setDayRecords(today, DayRecords(today, lunch = listOf(MealRecord(calories = 2001.0))))
        val viewModel = HomeViewModel(repository)
        store.put("home", viewModel)
        runCurrent()
        assertEquals(100, viewModel.uiState.value.ringPercent)
        assertEquals("1", viewModel.uiState.value.ringCenterText)
        assertEquals("超出", viewModel.uiState.value.ringGapLabel)
    }
}
