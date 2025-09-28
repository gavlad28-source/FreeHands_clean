package com.freehands.assistant.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.freehands.assistant.R
import com.freehands.assistant.presentation.MainActivity
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
class VoiceAssistantService : Service() {
    
    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    
    @Inject
    lateinit var context: Context
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    
    private val _recognitionState = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val recognitionState = _recognitionState.asStateFlow()
    
    inner class LocalBinder : Binder() {
        fun getService(): VoiceAssistantService = this@VoiceAssistantService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
        initializeSpeechRecognizer()
    }
    
    private fun createNotification(): Notification {
        createNotificationChannel()
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.voice_assistant_running))
            .setContentText(getString(R.string.tap_to_open_app))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.voice_assistant_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.voice_assistant_channel_description)
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun initializeSpeechRecognizer() {
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
            // Restart listening after an error
            startListening()
        }
        
        override fun onResults(results: android.os.Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: return
            _recognitionState.value = RecognitionState.Result(text)
            
            // Process the command
            processVoiceCommand(text)
            
            // Restart listening
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
            // Enable offline mode
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            // Disable network timeout
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000)
        }
        
        speechRecognizer?.startListening(intent)
        isListening = true
    }
    
    fun stopListening() {
        if (!isListening) return
        
        speechRecognizer?.stopListening()
        isListening = false
    }
    
    private fun processVoiceCommand(text: String) {
        serviceScope.launch {
            // Process the command in the background
            // This is where you would integrate with your command processing logic
            // For now, we'll just update the state with the recognized text
            _recognitionState.value = RecognitionState.Processing
            // Simulate processing delay
            kotlinx.coroutines.delay(1000)
            _recognitionState.value = RecognitionState.Result("Processed: $text")
        }
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
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "VoiceAssistantChannel"
        
        fun isMicrophoneAvailable(context: Context): Boolean {
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioRecord.getMinBufferSize(
                    16000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
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
