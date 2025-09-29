package com.freehands.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Base ViewModel class that provides common functionality for all ViewModels.
 * Handles coroutine scoping and error handling.
 */
abstract class BaseViewModel : ViewModel() {

    protected val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    protected val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error

    protected val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Error in coroutine")
        _error.value = throwable
        _loading.value = false
    }

    protected fun launchCoroutine(block: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch(errorHandler) {
            try {
                _loading.value = true
                block()
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearMessage() {
        _message.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up any resources if needed
    }
}
