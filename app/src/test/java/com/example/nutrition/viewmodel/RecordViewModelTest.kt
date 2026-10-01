package com.example.nutrition.viewmodel

import androidx.lifecycle.ViewModelStore
import com.example.nutrition.domain.model.DayRecords
import com.example.nutrition.domain.model.FoodTemplate
import com.example.nutrition.domain.model.MealKey
import com.example.nutrition.domain.model.MealRecord
import com.example.nutrition.domain.model.Resource
import com.example.nutrition.domain.repository.LocalStorageRepository
import com.example.nutrition.domain.usecase.BackupManagerTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val store = ViewModelStore()
    @After fun cleanUp() { store.clear() }

    @Test
    fun autocomplete_reusesOneSubscription_andRespondsToTemplateEdits() = runTest {
        val milk = FoodTemplate(id = "milk", name = "牛奶", calories = 60.0)
        val templates = MutableStateFlow(listOf(milk))
        var subscriptions = 0
        val repository = object : LocalStorageRepository by BackupManagerTest.FakeRepository() {
            override fun getAllFoodTemplates() = flow {
                subscriptions++
                emitAll(templates)
            }
        }
        val viewModel = RecordViewModel(repository)
        store.put("record", viewModel)
        runCurrent()
        viewModel.onNameInput("牛")
        runCurrent()
        assertEquals(listOf(milk), viewModel.uiState.value.templateSuggestions)
        val updatedMilk = milk.copy(calories = 80.0)
        templates.value = listOf(updatedMilk)
        runCurrent()
        assertEquals(listOf(updatedMilk), viewModel.uiState.value.templateSuggestions)
        assertEquals(1, subscriptions)
    }

    @Test
    fun restoringPage_andSelectingSameMeal_preserveDraft() = runTest {
        val viewModel = RecordViewModel(BackupManagerTest.FakeRepository())
        store.put("record", viewModel)
        viewModel.initWithMeal(MealKey.BREAKFAST)
        viewModel.onNameInput("未保存的早餐")
        viewModel.onCaloriesInput("300")
        viewModel.initWithMeal(MealKey.BREAKFAST)
        viewModel.refreshMealByTime()
        viewModel.switchMeal(MealKey.BREAKFAST)
        runCurrent()
        assertEquals(MealKey.BREAKFAST, viewModel.uiState.value.currentMeal)
        assertEquals("未保存的早餐", viewModel.uiState.value.foodName)
        assertEquals("300", viewModel.uiState.value.calories)
    }

    @Test
    fun delayedSave_doesNotResetNewDraft() = runTest {
        val saved = CompletableDeferred<Unit>()
        var written: MealRecord? = null
        val repository = object : LocalStorageRepository by BackupManagerTest.FakeRepository() {
            override suspend fun addRecord(dateStr: String, mealKey: MealKey, record: MealRecord): Resource<Unit> {
                written = record
                saved.await()
                return Resource.success()
            }
        }
        val viewModel = RecordViewModel(repository)
        store.put("record", viewModel)
        viewModel.onNameInput("第一餐")
        viewModel.onCaloriesInput("300")
        viewModel.saveRecord()
        runCurrent()
        viewModel.onNameInput("下一餐")
        viewModel.onCaloriesInput("400")
        saved.complete(Unit)
        runCurrent()
        assertEquals("第一餐", written?.name)
        assertEquals("下一餐", viewModel.uiState.value.foodName)
        assertEquals("400", viewModel.uiState.value.calories)
        assertNull(viewModel.uiState.value.pendingTemplate)
    }
    @Test
    fun changingDate_clearsPreviousRecords_beforeDelayedReadCompletes() = runTest {
        val loaded = CompletableDeferred<Unit>()
        val repository = object : LocalStorageRepository by BackupManagerTest.FakeRepository() {
            override fun getDayRecords(dateStr: String) = flow {
                if (dateStr == "2026-09-28") loaded.await()
                emit(DayRecords(dateStr, breakfast = if (dateStr == "2026-09-28") emptyList() else listOf(MealRecord(calories = 300.0))))
            }
        }
        val viewModel = RecordViewModel(repository)
        store.put("record", viewModel)
        viewModel.initWithMeal(MealKey.BREAKFAST)
        viewModel.pickDate("2026-09-27")
        runCurrent()
        assertEquals(1, viewModel.uiState.value.recordList.size)
        viewModel.pickDate("2026-09-28")
        assertTrue(viewModel.uiState.value.recordList.isEmpty())
        assertTrue(viewModel.uiState.value.isLoadingRecords)
        runCurrent()
        loaded.complete(Unit)
        runCurrent()
        assertFalse(viewModel.uiState.value.isLoadingRecords)
        assertTrue(viewModel.uiState.value.recordList.isEmpty())
    }

    @Test
    fun readFailure_isVisible_andChangingDateCanRecover() = runTest {
        val repository = object : LocalStorageRepository by BackupManagerTest.FakeRepository() {
            override fun getDayRecords(dateStr: String) = flow {
                if (dateStr == "2026-09-28") error("test read failure")
                emit(DayRecords(dateStr))
            }
        }
        val viewModel = RecordViewModel(repository)
        store.put("record", viewModel)
        runCurrent()
        viewModel.pickDate("2026-09-28")
        runCurrent()
        assertNotNull(viewModel.uiState.value.dataError)
        assertFalse(viewModel.uiState.value.isLoadingRecords)
        viewModel.pickDate("2026-09-29")
        runCurrent()
        assertNull(viewModel.uiState.value.dataError)
        assertFalse(viewModel.uiState.value.isLoadingRecords)
    }

}
