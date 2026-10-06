package com.example.nutrition.domain.usecase

import com.example.nutrition.domain.model.DayRecords
import com.example.nutrition.domain.repository.LocalStorageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class BackupReadFailureTest {
    @Test
    fun export_readFailure_returnsNoPartialBackup() = runTest {
        val repository = object : LocalStorageRepository by BackupManagerTest.FakeRepository() {
            override fun getAllRecords(): Flow<Map<String, DayRecords>> = flow {
                throw IllegalStateException("Simulated storage read failure")
            }
        }
        val result = BackupManager(repository).exportData()
        assertFalse(result.success)
        assertEquals("", result.json)
        assertEquals("读取备份数据失败，请重试", result.error)
    }

    @Test
    fun export_cancellation_isPropagated() = runTest {
        val cancellation = CancellationException("Cancelled export")
        val repository = object : LocalStorageRepository by BackupManagerTest.FakeRepository() {
            override fun getAllRecords(): Flow<Map<String, DayRecords>> = flow { throw cancellation }
        }
        try {
            BackupManager(repository).exportData()
            fail("Cancellation must not become an export error")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }
}
