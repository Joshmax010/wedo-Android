package com.example.nutrition.viewmodel

import androidx.lifecycle.ViewModelStore
import com.example.nutrition.domain.constants.NutrientConstants
import com.example.nutrition.domain.model.NutritionTargets
import com.example.nutrition.domain.model.Resource
import com.example.nutrition.domain.repository.LocalStorageRepository
import com.example.nutrition.domain.usecase.BackupManager
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
class SettingsViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val store = ViewModelStore()
    @After fun cleanUp() { store.clear() }

    @Test
    fun returningToPage_preservesDraft_withoutReloadingTargets() = runTest {
        var reads = 0
        val repository = object : LocalStorageRepository by BackupManagerTest.FakeRepository() {
            override fun getTargets() = flow<NutritionTargets?> {
                reads++
                emit(NutrientConstants.getDefaultTargets())
            }
        }
        val viewModel = SettingsViewModel(repository, BackupManager(repository))
        store.put("settings", viewModel)
        viewModel.initialize()
        runCurrent()
        viewModel.onCaloriesInput("3000")
        viewModel.initialize()
        runCurrent()
        assertEquals("3000", viewModel.uiState.value.calories)
        assertEquals(1, reads)
    }

    @Test
    fun delayedInitialRead_doesNotOverwriteUserInput() = runTest {
        val loaded = CompletableDeferred<Unit>()
        val repository = object : LocalStorageRepository by BackupManagerTest.FakeRepository() {
            override fun getTargets() = flow<NutritionTargets?> {
                loaded.await()
                emit(NutrientConstants.getDefaultTargets())
            }
        }
        val viewModel = SettingsViewModel(repository, BackupManager(repository))
        store.put("settings", viewModel)
        viewModel.initialize()
        runCurrent()
        viewModel.onCaloriesInput("3000")
        loaded.complete(Unit)
        runCurrent()
        assertEquals("3000", viewModel.uiState.value.calories)
        assertEquals(NutrientConstants.getDefaultTargets().protein.toInt().toString(), viewModel.uiState.value.protein)
        assertEquals(NutrientConstants.getDefaultTargets().micronutrients.size, viewModel.uiState.value.micronutrients.size)
        assertNull(viewModel.uiState.value.targetsError)
    }

    @Test
    fun failedRead_blocksSaving_andCanBeRetried() = runTest {
        var failRead = true
        var writes = 0
        val repository = object : LocalStorageRepository by BackupManagerTest.FakeRepository() {
            override fun getTargets() = flow<NutritionTargets?> {
                if (failRead) error("test read failure")
                emit(NutrientConstants.getDefaultTargets())
            }
            override suspend fun setTargets(targets: NutritionTargets): Resource<Unit> {
                writes++
                return Resource.success()
            }
        }
        val viewModel = SettingsViewModel(repository, BackupManager(repository))
        store.put("settings", viewModel)
        viewModel.initialize()
        runCurrent()
        assertNotNull(viewModel.uiState.value.targetsError)
        viewModel.onCaloriesInput("2000")
        viewModel.onProteinInput("100")
        viewModel.onFatInput("60")
        viewModel.onCarbsInput("250")
        viewModel.saveTargets()
        runCurrent()
        assertEquals(0, writes)
        failRead = false
        viewModel.loadTargets()
        runCurrent()
        assertNull(viewModel.uiState.value.targetsError)
        assertEquals("2000", viewModel.uiState.value.calories) // Retry also retains the draft entered after failure.
    }
}
