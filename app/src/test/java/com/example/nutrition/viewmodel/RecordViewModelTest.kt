package com.example.nutrition.viewmodel

import androidx.lifecycle.ViewModelStore
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
}
