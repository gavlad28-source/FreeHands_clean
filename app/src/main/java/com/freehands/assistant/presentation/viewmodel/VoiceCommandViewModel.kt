package com.freehands.assistant.presentation.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.freehands.assistant.data.service.VoiceRecognitionService
import com.freehands.assistant.domain.model.VoiceCommand
import com.freehands.assistant.domain.usecase.ProcessVoiceCommandUseCase
import com.freehands.assistant.utils.AppConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceCommandViewModel @Inject constructor(
    application: Application,
    private val processVoiceCommandUseCase: ProcessVoiceCommandUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<VoiceCommandUiState>(VoiceCommandUiState.Idle)
    val uiState: StateFlow<VoiceCommandUiState> = _uiState

    private var voiceRecognitionService: VoiceRecognitionService? = null
    private var isListening = false

    init {
        startVoiceRecognition()
    }

    private fun startVoiceRecognition() {
        val intent = Intent(getApplication(), VoiceRecognitionService::class.java)
        getApplication<Application>().startService(intent)
        
        viewModelScope.launch {
            // In a real app, you would bind to the service and observe its state
            // For simplicity, we'll simulate the recognition state changes
            _uiState.value = VoiceCommandUiState.Ready
        }
    }

    fun startListening() {
        if (isListening) return
        
        viewModelScope.launch {
            _uiState.value = VoiceCommandUiState.Listening
            isListening = true
            
            // In a real app, you would start listening through the bound service
            // For now, we'll simulate a voice command after a short delay
            kotlinx.coroutines.delay(2000)
            processVoiceCommand("set brightness to 50 percent")
        }
    }

    fun stopListening() {
        isListening = false
        _uiState.value = VoiceCommandUiState.Processing
    }

    fun processVoiceCommand(voiceInput: String) {
        viewModelScope.launch {
            _uiState.value = VoiceCommandUiState.Processing
            
            try {
                val command = VoiceCommand.fromText(voiceInput)
                processVoiceCommandUseCase(command).collectLatest { result ->
                    when (result) {
                        is ProcessVoiceCommandUseCase.CommandResult.Success -> {
                            _uiState.value = VoiceCommandUiState.Success(result.message)
                        }
                        is ProcessVoiceCommandUseCase.CommandResult.Error -> {
                            _uiState.value = VoiceCommandUiState.Error(result.message)
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = VoiceCommandUiState.Error("Error processing command: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up resources
        voiceRecognitionService = null
    }

    sealed class VoiceCommandUiState {
        object Idle : VoiceCommandUiState()
        object Ready : VoiceCommandUiState()
        object Listening : VoiceCommandUiState()
        object Processing : VoiceCommandUiState()
        data class Success(val message: String) : VoiceCommandUiState()
        data class Error(val message: String) : VoiceCommandUiState()
    }
}
