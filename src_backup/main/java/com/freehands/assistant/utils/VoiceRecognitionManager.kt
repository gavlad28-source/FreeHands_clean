package com.freehands.assistant.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

typealias OnRecognitionListener = (
    onStartOfSpeech: () -> Unit,
    onEndOfSpeech: () -> Unit,
    onResults: (Bundle) -> Unit,
    onError: (String) -> Unit
) -> Unit

@Singleton
class VoiceRecognitionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognitionIntent: Intent? = null
    private var isListening = false
    private var recognitionListener: RecognitionListener? = null

    init {
        initialize()
    }

    private fun initialize() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Timber.e("Speech recognition is not available on this device")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createRecognitionListener())
        }

        recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    fun isListening(): Boolean = isListening

    fun startListening() {
        if (isListening) return
        
        try {
            speechRecognizer?.startListening(recognitionIntent)
            isListening = true
        } catch (e: Exception) {
            Timber.e(e, "Error starting speech recognition")
            throw VoiceRecognitionException("Failed to start voice recognition: ${e.message}")
        }
    }

    fun stopListening() {
        if (!isListening) return
        
        try {
            speechRecognizer?.stopListening()
            isListening = false
        } catch (e: Exception) {
            Timber.e(e, "Error stopping speech recognition")
            throw VoiceRecognitionException("Failed to stop voice recognition: ${e.message}")
        }
    }

    fun setOnRecognitionListener(listener: OnRecognitionListener) {
        recognitionListener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            
            override fun onBeginningOfSpeech() {
                listener({}, {}, {}, {})()
            }
            
            override fun onRmsChanged(rmsdB: Float) {}
            
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {
                listener({}, {}, {}, {})
            }
            
            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No recognition result matched"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error occurred"
                }
                listener({}, {}, {}, { errorMessage })
            }
            
            override fun onResults(results: Bundle?) {
                results?.let { listener({}, {}, it, {}) }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {}
            
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        
        speechRecognizer?.setRecognitionListener(recognitionListener)
    }

    fun destroy() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        recognitionListener = null
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {}
            override fun onResults(results: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}

class VoiceRecognitionException(message: String) : Exception(message)
