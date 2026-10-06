package com.example.nutrition.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelOperationsTest {
    @Test
    fun operationFailure_reportsFeedback() = runTest {
        var feedback: String? = null
        val job = launchWithErrorFeedback("读取失败", { feedback = it }) {
            error("test failure")
        }
        job.join()
        assertEquals("读取失败", feedback)
    }

    @Test
    fun cancellation_doesNotReportAnOperationFailure() = runTest {
        var feedback: String? = null
        val job = launchWithErrorFeedback("读取失败", { feedback = it }) {
            awaitCancellation()
        }
        runCurrent()
        job.cancel()
        job.join()
        assertTrue(job.isCancelled)
        assertNull(feedback)
    }
}
