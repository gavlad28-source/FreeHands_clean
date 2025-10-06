package com.freehands.assistant.wakeword

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.freehands.assistant.asr.VoskConfig
import com.freehands.assistant.asr.VoskManager
import kotlinx.coroutines.*
import org.json.JSONObject
import org.vosk.Recognizer

/**
 * WakeWordDetector uses Vosk for offline wake word detection
 * Listens for trigger phrases like "привет брат" to activate the assistant
 */
class WakeWordDetector(
    private val context: Context,
    private val voskManager: VoskManager
) {
    companion object {
        private const val TAG = "WakeWordDetector"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        
        // Wake words (can be configured)
        val WAKE_WORDS = listOf(
            "привет брат",
            "окей ассистент",
            "эй помощник",
            "слушай"
        )
    }
    
    private var audioRecord: AudioRecord? = null
    private var recognizer: Recognizer? = null
    private var isListening = false
    private var detectionJob: Job? = null
    
    private var onWakeWordDetected: ((String) -> Unit)? = null
    
    /**
     * Initialize wake word detector
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Create recognizer with wake word grammar
            val grammar = buildWakeWordGrammar()
            recognizer = voskManager.createRecognizerWithGrammar(grammar)
            
            if (recognizer == null) {
                Log.e(TAG, "Failed to create recognizer")
                return@withContext false
            }
            
            // Initialize AudioRecord
            val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL,
                ENCODING,
                bufferSize * 2
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Failed to initialize AudioRecord")
                return@withContext false
            }
            
            Log.i(TAG, "✓ Wake word detector initialized")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing wake word detector", e)
            return@withContext false
        }
    }
    
    /**
     * Build grammar JSON for wake words
     */
    private fun buildWakeWordGrammar(): String {
        val wordsArray = WAKE_WORDS.joinToString("\", \"", "[\"", "\"]")
        return wordsArray
    }
    
    /**
     * Start listening for wake word
     */
    fun startListening(onDetected: (String) -> Unit) {
        if (isListening) {
            Log.w(TAG, "Already listening")
            return
        }
        
        onWakeWordDetected = onDetected
        isListening = true
        
        detectionJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                audioRecord?.startRecording()
                Log.d(TAG, "Started listening for wake word")
                
                val buffer = ShortArray(VoskConfig.BUFFER_SIZE)
                
                while (isActive && isListening) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    
                    if (read > 0) {
                        // Convert short array to byte array for Vosk
                        val byteBuffer = ByteArray(read * 2)
                        for (i in 0 until read) {
                            byteBuffer[i * 2] = (buffer[i].toInt() and 0xFF).toByte()
                            byteBuffer[i * 2 + 1] = ((buffer[i].toInt() shr 8) and 0xFF).toByte()
                        }
                        
                        // Feed audio to recognizer
                        val isFinal = recognizer?.acceptWaveForm(byteBuffer, byteBuffer.size) ?: false
                        
                        if (isFinal) {
                            // Get recognition result
                            val result = recognizer?.result ?: ""
                            processResult(result)
                        } else {
                            // Check partial result for early detection
                            val partial = recognizer?.partialResult ?: ""
                            processPartialResult(partial)
                        }
                    }
                    
                    // Small delay to prevent CPU overuse
                    delay(10)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in wake word detection loop", e)
            } finally {
                audioRecord?.stop()
                Log.d(TAG, "Stopped listening for wake word")
            }
        }
    }
    
    /**
     * Process final recognition result
     */
    private suspend fun processResult(resultJson: String) = withContext(Dispatchers.Main) {
        try {
            val json = JSONObject(resultJson)
            val text = json.optString("text", "").lowercase().trim()
            
            if (text.isNotEmpty()) {
                Log.d(TAG, "Recognized: $text")
                
                // Check if any wake word is detected
                for (wakeWord in WAKE_WORDS) {
                    if (text.contains(wakeWord.lowercase())) {
                        Log.i(TAG, "🎙️ Wake word detected: $wakeWord")
                        onWakeWordDetected?.invoke(wakeWord)
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing result", e)
        }
    }
    
    /**
     * Process partial recognition result for early detection
     */
    private suspend fun processPartialResult(partialJson: String) = withContext(Dispatchers.Main) {
        try {
            val json = JSONObject(partialJson)
            val partial = json.optString("partial", "").lowercase().trim()
            
            if (partial.isNotEmpty()) {
                // Check if any wake word is in partial result
                for (wakeWord in WAKE_WORDS) {
                    if (partial.contains(wakeWord.lowercase())) {
                        Log.d(TAG, "Wake word detected in partial: $wakeWord")
                        // Don't trigger yet, wait for final result to avoid false positives
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore errors in partial results
        }
    }
    
    /**
     * Stop listening for wake word
     */
    fun stopListening() {
        isListening = false
        detectionJob?.cancel()
        audioRecord?.stop()
        Log.d(TAG, "Wake word detection stopped")
    }
    
    /**
     * Check if currently listening
     */
    fun isListening(): Boolean = isListening
    
    /**
     * Add custom wake word
     */
    fun addWakeWord(wakeWord: String) {
        if (!WAKE_WORDS.contains(wakeWord.lowercase())) {
            (WAKE_WORDS as MutableList).add(wakeWord.lowercase())
            Log.d(TAG, "Added wake word: $wakeWord")
        }
    }
    
    /**
     * Get current wake words
     */
    fun getWakeWords(): List<String> = WAKE_WORDS.toList()
    
    /**
     * Release resources
     */
    fun release() {
        stopListening()
        audioRecord?.release()
        audioRecord = null
        recognizer = null
        Log.d(TAG, "Resources released")
    }
}

/**
 * Wake word detection settings
 */
data class WakeWordSettings(
    val enabled: Boolean = true,
    val sensitivity: Float = 0.5f, // 0.0 to 1.0
    val customWakeWords: List<String> = emptyList(),
    val requireExactMatch: Boolean = false
)
