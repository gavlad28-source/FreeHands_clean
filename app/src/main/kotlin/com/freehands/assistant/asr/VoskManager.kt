package com.freehands.assistant.asr

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.File
import java.io.IOException

/**
 * VoskManager handles offline speech recognition using Vosk
 */
class VoskManager(private val context: Context) {
    
    companion object {
        private const val TAG = "VoskManager"
        
        // Model configuration
        const val MODEL_NAME = "vosk-model-small-ru-0.22"
        const val SAMPLE_RATE = 16000f
    }
    
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var recognitionListener: RecognitionListener? = null
    
    /**
     * Initialize Vosk model
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing Vosk...")
            
            // Unpack model from assets if needed
            val modelDir = unpackModel()
            if (modelDir == null) {
                Log.e(TAG, "Failed to unpack model")
                return@withContext false
            }
            
            // Load model
            model = Model(modelDir.absolutePath)
            Log.i(TAG, "✓ Vosk model loaded successfully")
            
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Vosk", e)
            return@withContext false
        }
    }
    
    /**
     * Unpack model from assets to internal storage
     */
    private suspend fun unpackModel(): File? = withContext(Dispatchers.IO) {
        try {
            // Check if model already exists in internal storage
            val modelDir = File(context.filesDir, "models/$MODEL_NAME")
            if (modelDir.exists() && modelDir.isDirectory) {
                Log.d(TAG, "Model already unpacked at ${modelDir.absolutePath}")
                return@withContext modelDir
            }
            
            // Check if model exists in assets
            val assetsPath = "models/vosk/$MODEL_NAME"
            val assetFiles = try {
                context.assets.list(assetsPath)
            } catch (e: IOException) {
                Log.e(TAG, "Model not found in assets at $assetsPath")
                Log.e(TAG, "Please download and place the model in app/src/main/assets/$assetsPath/")
                Log.e(TAG, "See README_ASSETS.md for instructions")
                null
            }
            
            if (assetFiles == null || assetFiles.isEmpty()) {
                Log.e(TAG, "Model files not found in assets")
                return@withContext null
            }
            
            // Create model directory
            modelDir.mkdirs()
            
            // Copy model files from assets to internal storage
            Log.d(TAG, "Unpacking model to ${modelDir.absolutePath}...")
            copyAssetFolder(assetsPath, modelDir)
            
            Log.i(TAG, "✓ Model unpacked successfully")
            return@withContext modelDir
        } catch (e: Exception) {
            Log.e(TAG, "Error unpacking model", e)
            return@withContext null
        }
    }
    
    /**
     * Recursively copy folder from assets
     */
    private fun copyAssetFolder(assetPath: String, targetDir: File) {
        try {
            val files = context.assets.list(assetPath) ?: return
            
            if (files.isEmpty()) {
                // It's a file, not a directory
                copyAssetFile(assetPath, File(targetDir.parent, File(assetPath).name))
            } else {
                // It's a directory
                for (file in files) {
                    val newPath = "$assetPath/$file"
                    val newTarget = File(targetDir, file)
                    
                    val subFiles = context.assets.list(newPath)
                    if (subFiles != null && subFiles.isNotEmpty()) {
                        // Directory
                        newTarget.mkdirs()
                        copyAssetFolder(newPath, newTarget)
                    } else {
                        // File
                        copyAssetFile(newPath, newTarget)
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error copying asset folder: $assetPath", e)
        }
    }
    
    /**
     * Copy single file from assets
     */
    private fun copyAssetFile(assetPath: String, targetFile: File) {
        try {
            context.assets.open(assetPath).use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error copying asset file: $assetPath", e)
        }
    }
    
    /**
     * Start speech recognition
     */
    fun startRecognition(listener: RecognitionListener): Boolean {
        return try {
            if (model == null) {
                Log.e(TAG, "Model not initialized")
                return false
            }
            
            recognitionListener = listener
            speechService = SpeechService(model, SAMPLE_RATE)
            speechService?.startListening(listener)
            
            Log.d(TAG, "Speech recognition started")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recognition", e)
            false
        }
    }
    
    /**
     * Stop speech recognition
     */
    fun stopRecognition() {
        try {
            speechService?.stop()
            speechService = null
            Log.d(TAG, "Speech recognition stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recognition", e)
        }
    }
    
    /**
     * Pause speech recognition
     */
    fun pauseRecognition() {
        try {
            speechService?.setPause(true)
            Log.d(TAG, "Speech recognition paused")
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing recognition", e)
        }
    }
    
    /**
     * Resume speech recognition
     */
    fun resumeRecognition() {
        try {
            speechService?.setPause(false)
            Log.d(TAG, "Speech recognition resumed")
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming recognition", e)
        }
    }
    
    /**
     * Create recognizer for continuous recognition
     */
    fun createRecognizer(): Recognizer? {
        return try {
            if (model == null) {
                Log.e(TAG, "Model not initialized")
                return null
            }
            Recognizer(model, SAMPLE_RATE)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating recognizer", e)
            null
        }
    }
    
    /**
     * Create recognizer with specific grammar/keywords
     */
    fun createRecognizerWithGrammar(grammar: String): Recognizer? {
        return try {
            if (model == null) {
                Log.e(TAG, "Model not initialized")
                return null
            }
            Recognizer(model, SAMPLE_RATE, grammar)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating recognizer with grammar", e)
            null
        }
    }
    
    /**
     * Check if model is loaded
     */
    fun isModelLoaded(): Boolean = model != null
    
    /**
     * Check if recognition is active
     */
    fun isRecognitionActive(): Boolean = speechService != null
    
    /**
     * Release resources
     */
    fun release() {
        try {
            stopRecognition()
            model?.close()
            model = null
            Log.d(TAG, "Resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing resources", e)
        }
    }
    
    /**
     * Get model directory path
     */
    fun getModelPath(): String? {
        val modelDir = File(context.filesDir, "models/$MODEL_NAME")
        return if (modelDir.exists()) modelDir.absolutePath else null
    }
}

/**
 * Configuration for Vosk recognition
 */
object VoskConfig {
    const val MODEL_NAME = VoskManager.MODEL_NAME
    const val SAMPLE_RATE = VoskManager.SAMPLE_RATE
    const val BUFFER_SIZE = 4096
    
    // Grammar for wake word detection
    const val WAKE_WORD_GRAMMAR = """
        ["привет брат", "окей ассистент", "эй помощник"]
    """
    
    // Common commands grammar (improves recognition accuracy)
    const val COMMANDS_GRAMMAR = """
        [
            "включи вайфай", "выключи вайфай",
            "включи блютуз", "выключи блютуз",
            "увеличь яркость", "уменьши яркость",
            "включи не беспокоить", "выключи не беспокоить",
            "открой настройки", "открой камеру",
            "открой телефон", "открой сообщения"
        ]
    """
}
