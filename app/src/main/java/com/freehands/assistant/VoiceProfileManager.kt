package com.freehands.assistant

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.freehands.assistant.utils.AudioFeatureExtractor
import com.freehands.assistant.utils.VoiceFeatureStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages voice profiles for user identification and verification.
 */
@Singleton
class VoiceProfileManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioFeatureExtractor: AudioFeatureExtractor,
    private val voiceFeatureStorage: VoiceFeatureStorage
) {
    private val TAG = "VoiceProfileManager"
    
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    
    // Voice profile configuration
    private val sampleRate = 16000 // 16kHz sample rate
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2
    
    /**
     * Creates a new voice profile for the current user.
     * @param userId Unique identifier for the user
     * @param callback Callback to receive the result
     */
    fun createVoiceProfile(userId: String, callback: (Result<Unit>) -> Unit) {
        if (isRecording) {
            stopRecording()
        }
        
        val audioData = mutableListOf<Short>()
        
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )
        
        try {
            audioRecord?.startRecording()
            isRecording = true
            
            recordingJob = coroutineScope.launch {
                val buffer = ShortArray(bufferSize / 2) // 16-bit samples
                
                // Record for 5 seconds or until stopped
                withTimeoutOrNull(5000) {
                    while (isActive && isRecording) {
                        val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (bytesRead > 0) {
                            audioData.addAll(buffer.take(bytesRead).toList())
                        }
                        delay(50) // Small delay to prevent tight loop
                    }
                }
                
                // Process the recorded audio
                processRecordedAudio(userId, audioData.toShortArray(), callback)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating voice profile", e)
            callback(Result.failure(e))
        }
    }
    
    /**
     * Verifies if the recorded voice matches the stored profile.
     * @param userId User ID to verify against
     * @param callback Callback with verification result and confidence score
     */
    fun verifyVoiceProfile(userId: String, callback: (Result<Pair<Boolean, Float>>) -> Unit) {
        if (isRecording) {
            stopRecording()
        }
        
        val audioData = mutableListOf<Short>()
        
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )
        
        try {
            audioRecord?.startRecording()
            isRecording = true
            
            recordingJob = coroutineScope.launch {
                val buffer = ShortArray(bufferSize / 2)
                
                // Record for 3 seconds or until stopped
                withTimeoutOrNull(3000) {
                    while (isActive && isRecording) {
                        val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (bytesRead > 0) {
                            audioData.addAll(buffer.take(bytesRead).toList())
                        }
                        delay(50)
                    }
                }
                
                // Verify the recorded audio
                verifyRecordedAudio(userId, audioData.toShortArray(), callback)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying voice profile", e)
            callback(Result.failure(e))
        }
    }
    
    /**
     * Stops the current recording session.
     */
    fun stopRecording() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio recording", e)
        } finally {
            audioRecord = null
        }
    }
    
    /**
     * Deletes a voice profile.
     * @param userId ID of the user whose profile should be deleted
     */
    fun deleteVoiceProfile(userId: String): Boolean {
        return try {
            voiceFeatureStorage.deleteProfile(userId)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting voice profile", e)
            false
        }
    }
    
    /**
     * Checks if a voice profile exists for the given user.
     */
    fun hasVoiceProfile(userId: String): Boolean {
        return voiceFeatureStorage.profileExists(userId)
    }
    
    private fun processRecordedAudio(
        userId: String,
        audioData: ShortArray,
        callback: (Result<Unit>) -> Unit
    ) {
        if (audioData.isEmpty()) {
            callback(Result.failure(IllegalStateException("No audio data recorded")))
            return
        }
        
        coroutineScope.launch(Dispatchers.Default) {
            try {
                // Extract features from the audio
                val features = audioFeatureExtractor.extractFeatures(audioData, sampleRate)
                
                // Save the features
                voiceFeatureStorage.saveProfile(userId, features)
                
                // Save a sample for debugging
                saveAudioSample(userId, audioData)
                
                withContext(Dispatchers.Main) {
                    callback(Result.success(Unit))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing audio", e)
                withContext(Dispatchers.Main) {
                    callback(Result.failure(e))
                }
            }
        }
    }
    
    private fun verifyRecordedAudio(
        userId: String,
        audioData: ShortArray,
        callback: (Result<Pair<Boolean, Float>>) -> Unit
    ) {
        if (audioData.isEmpty()) {
            callback(Result.failure(IllegalStateException("No audio data recorded")))
            return
        }
        
        coroutineScope.launch(Dispatchers.Default) {
            try {
                // Load the stored profile
                val storedFeatures = voiceFeatureStorage.loadProfile(userId)
                
                // Extract features from the new recording
                val newFeatures = audioFeatureExtractor.extractFeatures(audioData, sampleRate)
                
                // Calculate similarity score
                val similarity = audioFeatureExtractor.calculateSimilarity(storedFeatures, newFeatures)
                
                // Consider it a match if similarity is above threshold
                val isMatch = similarity >= SIMILARITY_THRESHOLD
                
                withContext(Dispatchers.Main) {
                    callback(Result.success(Pair(isMatch, similarity)))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error verifying audio", e)
                withContext(Dispatchers.Main) {
                    callback(Result.failure(e))
                }
            }
        }
    }
    
    private fun saveAudioSample(userId: String, audioData: ShortArray) {
        try {
            val samplesDir = File(context.filesDir, "voice_samples")
            if (!samplesDir.exists()) {
                samplesDir.mkdirs()
            }
            
            val sampleFile = File(samplesDir, "${userId}_${System.currentTimeMillis()}.pcm")
            FileOutputStream(sampleFile).use { stream ->
                val buffer = ByteArray(audioData.size * 2) // 16-bit samples
                for (i in audioData.indices) {
                    buffer[i * 2] = (audioData[i].toInt() and 0xFF).toByte()
                    buffer[i * 2 + 1] = (audioData[i].toInt() shr 8).toByte()
                }
                stream.write(buffer)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error saving audio sample", e)
        }
    }
    
    companion object {
        private const val SIMILARITY_THRESHOLD = 0.8f // Threshold for voice matching
    }
}
