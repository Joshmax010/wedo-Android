package com.example.nutrition.viewmodel

import androidx.lifecycle.ViewModelStore
import com.example.nutrition.domain.model.FoodTemplate
import com.example.nutrition.domain.repository.LocalStorageRepository
import com.example.nutrition.domain.usecase.BackupManagerTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
}
