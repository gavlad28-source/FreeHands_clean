package com.freehands.assistant.data.service

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.freehands.assistant.R
import com.freehands.assistant.utils.AppConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class VoiceRecognitionService : Service() {
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    private val _recognitionState = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val recognitionState = _recognitionState.asStateFlow()
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    
    @Inject
    lateinit var context: Context
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        startForeground(
            AppConstants.Notification.VOICE_RECOGNITION_SERVICE_ID,
            createNotification()
        )
        initializeSpeechRecognizer()
    }
    
    private fun createNotification() = NotificationCompat.Builder(
        this,
        AppConstants.Notification.CHANNEL_ID_VOICE_RECOGNITION
    ).apply {
        setContentTitle(getString(R.string.voice_recognition_active))
        setContentText(getString(R.string.listening_for_commands))
        setSmallIcon(R.drawable.ic_mic)
        setOngoing(true)
        setShowWhen(false)
    }.build()
    
    private fun initializeSpeechRecognizer() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            _recognitionState.value = RecognitionState.Error("Microphone permission not granted")
            return
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(createRecognitionListener())
        }
    }
    
    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: android.os.Bundle?) {
            _recognitionState.value = RecognitionState.Ready
        }
        
        override fun onBeginningOfSpeech() {
            _recognitionState.value = RecognitionState.Listening
        }
        
        override fun onRmsChanged(rmsdB: Float) {
            _recognitionState.value = RecognitionState.AudioLevel(rmsdB)
        }
        
        override fun onBufferReceived(buffer: ByteArray?) {}
        
        override fun onEndOfSpeech() {
            _recognitionState.value = RecognitionState.Processing
        }
        
        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Unknown error occurred"
            }
            _recognitionState.value = RecognitionState.Error(errorMessage)
        }
        
        override fun onResults(results: android.os.Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: return
            _recognitionState.value = RecognitionState.Result(text)
            
            // Automatically start listening again
            startListening()
        }
        
        override fun onPartialResults(partialResults: android.os.Bundle?) {}
        override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
    }
    
    fun startListening() {
        if (isListening) return
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        speechRecognizer?.startListening(intent)
        isListening = true
    }
    
    fun stopListening() {
        if (!isListening) return
        
        speechRecognizer?.stopListening()
        isListening = false
    }
    
    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
    
    sealed class RecognitionState {
        object Idle : RecognitionState()
        object Ready : RecognitionState()
        object Listening : RecognitionState()
        object Processing : RecognitionState()
        data class AudioLevel(val rmsdB: Float) : RecognitionState()
        data class Result(val text: String) : RecognitionState()
        data class Error(val message: String) : RecognitionState()
    }
    
    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        
        fun isMicrophoneAvailable(context: Context): Boolean {
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            )
            
            return try {
                audioRecord.startRecording()
                val isRecording = audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING
                audioRecord.stop()
                audioRecord.release()
                isRecording
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
