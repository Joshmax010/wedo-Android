package com.example.nutrition.viewmodel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Report operational failures while preserving structured coroutine cancellation. */
internal fun CoroutineScope.launchWithErrorFeedback(
    message: String,
    onError: (String) -> Unit,
    block: suspend CoroutineScope.() -> Unit
): Job = launch {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        onError(message)
    }
}
