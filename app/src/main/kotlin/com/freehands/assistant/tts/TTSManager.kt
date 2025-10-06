package com.freehands.assistant.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.resume

/**
 * TTSManager handles text-to-speech with queue support
 */
class TTSManager(private val context: Context) {
    
    companion object {
        private const val TAG = "TTSManager"
    }
    
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val speechQueue = ConcurrentLinkedQueue<String>()
    private var isSpeaking = false
    
    /**
     * Initialize TTS engine
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            try {
                tts = TextToSpeech(context) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        // Set language to Russian
                        val result = tts?.setLanguage(Locale("ru", "RU"))
                        
                        when (result) {
                            TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> {
                                Log.w(TAG, "Russian language not supported, trying English")
                                tts?.setLanguage(Locale.ENGLISH)
                            }
                        }
                        
                        // Configure TTS
                        tts?.setPitch(1.0f)
                        tts?.setSpeechRate(1.0f)
                        
                        // Set utterance listener
                        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                Log.d(TAG, "TTS started: $utteranceId")
                            }
                            
                            override fun onDone(utteranceId: String?) {
                                Log.d(TAG, "TTS completed: $utteranceId")
                                isSpeaking = false
                                processQueue()
                            }
                            
                            override fun onError(utteranceId: String?) {
                                Log.e(TAG, "TTS error: $utteranceId")
                                isSpeaking = false
                                processQueue()
                            }
                            
                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?, errorCode: Int) {
                                Log.e(TAG, "TTS error: $utteranceId, code: $errorCode")
                                isSpeaking = false
                                processQueue()
                            }
                        })
                        
                        isInitialized = true
                        Log.i(TAG, "✓ TTS initialized successfully")
                        continuation.resume(true)
                    } else {
                        Log.e(TAG, "TTS initialization failed")
                        continuation.resume(false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing TTS", e)
                continuation.resume(false)
            }
        }
    }
    
    /**
     * Speak text (adds to queue if already speaking)
     */
    fun speak(text: String, priority: Boolean = false) {
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS not initialized")
            return
        }
        
        if (text.isBlank()) {
            Log.w(TAG, "Empty text provided")
            return
        }
        
        if (priority) {
            // Stop current speech and speak immediately
            stopSpeaking()
            speakNow(text)
        } else {
            // Add to queue
            speechQueue.offer(text)
            if (!isSpeaking) {
                processQueue()
            }
        }
    }
    
    /**
     * Speak text immediately (internal)
     */
    private fun speakNow(text: String) {
        try {
            isSpeaking = true
            val utteranceId = UUID.randomUUID().toString()
            
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            Log.d(TAG, "Speaking: $text")
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text", e)
            isSpeaking = false
        }
    }
    
    /**
     * Process speech queue
     */
    private fun processQueue() {
        if (isSpeaking || speechQueue.isEmpty()) {
            return
        }
        
        val text = speechQueue.poll()
        if (text != null) {
            speakNow(text)
        }
    }
    
    /**
     * Stop speaking and clear queue
     */
    fun stopSpeaking() {
        try {
            tts?.stop()
            speechQueue.clear()
            isSpeaking = false
            Log.d(TAG, "Stopped speaking")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
    }
    
    /**
     * Check if currently speaking
     */
    fun isSpeaking(): Boolean = isSpeaking
    
    /**
     * Set speech rate (0.5 - 2.0)
     */
    fun setSpeechRate(rate: Float) {
        try {
            val clampedRate = rate.coerceIn(0.5f, 2.0f)
            tts?.setSpeechRate(clampedRate)
            Log.d(TAG, "Speech rate set to $clampedRate")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting speech rate", e)
        }
    }
    
    /**
     * Set pitch (0.5 - 2.0)
     */
    fun setPitch(pitch: Float) {
        try {
            val clampedPitch = pitch.coerceIn(0.5f, 2.0f)
            tts?.setPitch(clampedPitch)
            Log.d(TAG, "Pitch set to $clampedPitch")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting pitch", e)
        }
    }
    
    /**
     * Set language
     */
    fun setLanguage(locale: Locale): Boolean {
        return try {
            val result = tts?.setLanguage(locale)
            when (result) {
                TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> {
                    Log.w(TAG, "Language not supported: ${locale.language}")
                    false
                }
                else -> {
                    Log.d(TAG, "Language set to ${locale.language}")
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting language", e)
            false
        }
    }
    
    /**
     * Get available languages
     */
    fun getAvailableLanguages(): Set<Locale>? {
        return try {
            tts?.availableLanguages
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available languages", e)
            null
        }
    }
    
    /**
     * Check if TTS is ready
     */
    fun isReady(): Boolean = isInitialized && tts != null
    
    /**
     * Get queue size
     */
    fun getQueueSize(): Int = speechQueue.size
    
    /**
     * Release TTS resources
     */
    fun release() {
        try {
            stopSpeaking()
            tts?.shutdown()
            tts = null
            isInitialized = false
            Log.d(TAG, "TTS released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing TTS", e)
        }
    }
}

/**
 * TTS configuration
 */
data class TTSConfig(
    val language: Locale = Locale("ru", "RU"),
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f
)
