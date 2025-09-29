package com.freehands.assistant

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.freehands.assistant.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : BaseViewModel() {

    private val _uiState = MutableLiveData<MainUiState>(MainUiState.Initial)
    val uiState: LiveData<MainUiState> = _uiState

    private val _permissionsState = MutableLiveData<Set<String>>(emptySet())
    val permissionsState: LiveData<Set<String>> = _permissionsState

    init {
        checkInitialState()
    }

    private fun checkInitialState() {
        if (preferencesManager.isFirstLaunch) {
            _uiState.value = MainUiState.FirstLaunch
            preferencesManager.isFirstLaunch = false
        } else {
            _uiState.value = MainUiState.Ready
        }
    }

    fun updatePermissionsState(permissions: Set<String>) {
        _permissionsState.value = permissions
    }

    fun onStartListening() {
        // Implement voice listening start logic
        _uiState.value = MainUiState.Listening
    }

    fun onStopListening() {
        // Implement voice listening stop logic
        _uiState.value = MainUiState.Ready
    }

    fun onError(error: String) {
        _uiState.value = MainUiState.Error(error)
    }
}

sealed class MainUiState {
    object Initial : MainUiState()
    object FirstLaunch : MainUiState()
    object Ready : MainUiState()
    object Listening : MainUiState()
    data class Error(val message: String) : MainUiState()
}
