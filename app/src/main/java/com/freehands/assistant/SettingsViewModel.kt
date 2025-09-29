package com.freehands.assistant

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.freehands.assistant.utils.PreferencesManager
import com.freehands.assistant.utils.ThemeHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val themeHelper: ThemeHelper
) : BaseViewModel() {

    private val _themeState = MutableLiveData<String>(preferencesManager.themeMode)
    val themeState: LiveData<String> = _themeState

    private val _wakeWordState = MutableLiveData<Boolean>(preferencesManager.wakeWordEnabled)
    val wakeWordState: LiveData<Boolean> = _wakeWordState

    private val _voiceSensitivity = MutableLiveData<Float>(preferencesManager.voiceSensitivity)
    val voiceSensitivity: LiveData<Float> = _voiceSensitivity

    fun updateTheme(theme: String) {
        preferencesManager.themeMode = theme
        themeHelper.setThemeMode(theme)
        _themeState.value = theme
    }

    fun toggleWakeWord(enabled: Boolean) {
        preferencesManager.wakeWordEnabled = enabled
        _wakeWordState.value = enabled
    }

    fun updateVoiceSensitivity(sensitivity: Float) {
        preferencesManager.voiceSensitivity = sensitivity
        _voiceSensitivity.value = sensitivity
    }

    fun getCurrentSettings(): SettingsState {
        return SettingsState(
            themeMode = preferencesManager.themeMode,
            wakeWordEnabled = preferencesManager.wakeWordEnabled,
            voiceSensitivity = preferencesManager.voiceSensitivity
        )
    }
}

data class SettingsState(
    val themeMode: String,
    val wakeWordEnabled: Boolean,
    val voiceSensitivity: Float
)
