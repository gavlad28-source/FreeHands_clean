package com.freehands.assistant

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.freehands.assistant.utils.VoiceCommandProcessor
import com.freehands.assistant.utils.VoiceRecognitionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val voiceRecognitionManager: VoiceRecognitionManager,
    private val commandProcessor: VoiceCommandProcessor
) : BaseViewModel() {

    private val _voiceState = MutableLiveData<VoiceState>(VoiceState.Idle)
    val voiceState: LiveData<VoiceState> = _voiceState

    private val _recognizedText = MutableLiveData<String>()
    val recognizedText: LiveData<String> = _recognizedText

    private val _commandResult = MutableLiveData<CommandResult>()
    val commandResult: LiveData<CommandResult> = _commandResult

    init {
        setupVoiceRecognition()
    }

    private fun setupVoiceRecognition() {
        voiceRecognitionManager.setOnRecognitionListener(
            onStartOfSpeech = {
                _voiceState.value = VoiceState.Listening
            },
            onEndOfSpeech = {
                _voiceState.value = VoiceState.Processing
            },
            onResults = { results ->
                val text = results.getStringArrayList(0)?.get(0) ?: return@setOnRecognitionListener
                _recognizedText.value = text
                processVoiceCommand(text)
            },
            onError = { error ->
                _voiceState.value = VoiceState.Error(error)
            }
        )
    }

    fun startListening() {
        if (voiceRecognitionManager.isListening()) return
        
        launchCoroutine {
            try {
                voiceRecognitionManager.startListening()
                _voiceState.value = VoiceState.Ready
            } catch (e: Exception) {
                _voiceState.value = VoiceState.Error(e.message ?: "Failed to start listening")
            }
        }
    }

    fun stopListening() {
        if (!voiceRecognitionManager.isListening()) return
        
        launchCoroutine {
            try {
                voiceRecognitionManager.stopListening()
                _voiceState.value = VoiceState.Idle
            } catch (e: Exception) {
                _voiceState.value = VoiceState.Error(e.message ?: "Error stopping voice recognition")
            }
        }
    }

    private fun processVoiceCommand(command: String) {
        launchCoroutine {
            try {
                val result = commandProcessor.processCommand(command)
                _commandResult.value = result
                
                when (result) {
                    is CommandResult.Success -> {
                        _voiceState.value = VoiceState.CommandProcessed(result.message)
                    }
                    is CommandResult.Error -> {
                        _voiceState.value = VoiceState.Error(result.message)
                    }
                    is CommandResult.ActionRequired -> {
                        _voiceState.value = VoiceState.ActionRequired(result.action, result.message)
                    }
                }
            } catch (e: Exception) {
                _voiceState.value = VoiceState.Error("Failed to process command")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceRecognitionManager.destroy()
    }
}

sealed class VoiceState {
    object Idle : VoiceState()
    object Ready : VoiceState()
    object Listening : VoiceState()
    object Processing : VoiceState()
    data class CommandProcessed(val message: String) : VoiceState()
    data class ActionRequired(val action: String, val message: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

sealed class CommandResult {
    data class Success(val message: String) : CommandResult()
    data class Error(val message: String) : CommandResult()
    data class ActionRequired(val action: String, val message: String) : CommandResult()
}
