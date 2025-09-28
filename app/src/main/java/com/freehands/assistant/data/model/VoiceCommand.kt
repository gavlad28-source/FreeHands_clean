package com.freehands.assistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.freehands.assistant.utils.AppConstants
import java.util.UUID

/**
 * Represents a voice command recognized by the app.
 *
 * @property id Unique identifier for the command
 * @property originalText The original text of the command as recognized by the speech recognizer
 * @property normalizedText The normalized/processed text of the command
 * @property timestamp When the command was received (in milliseconds since epoch)
 * @property confidence The confidence score of the speech recognition (0.0 to 1.0)
 * @property isProcessed Whether the command has been processed
 * @property processingResult The result of processing the command
 * @property commandType The type of command (e.g., "settings", "media", "navigation")
 * @property metadata Additional metadata about the command (JSON string)
 */
@Entity(tableName = "voice_commands")
data class VoiceCommand(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val originalText: String,
    val normalizedText: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 0f,
    val isProcessed: Boolean = false,
    val processingResult: String = "",
    val commandType: String = "",
    val metadata: String = ""
) {
    /**
     * Extracts the action from the command text.
     * For example, in "set brightness to 50%", the action is "set".
     */
    fun extractAction(): String {
        return normalizedText.split(" ").firstOrNull() ?: ""
    }
    
    /**
     * Extracts the target from the command text.
     * For example, in "set brightness to 50%", the target is "brightness".
     */
    fun extractTarget(): String {
        val parts = normalizedText.split(" ")
        return if (parts.size > 1) parts[1] else ""
    }
    
    /**
     * Extracts the value from the command text.
     * For example, in "set brightness to 50%", the value is "50%".
     */
    fun extractValue(): String {
        val parts = normalizedText.split(" ")
        return if (parts.size > 3) parts.drop(3).joinToString(" ") else ""
    }
    
    /**
     * Determines if this is a valid command that can be processed.
     */
    fun isValid(): Boolean {
        return originalText.isNotBlank() && confidence >= AppConstants.VoiceRecognition.CONFIDENCE_THRESHOLD
    }
    
    companion object {
        /**
         * Creates a VoiceCommand from recognized speech results.
         */
        fun fromSpeechResults(
            results: List<String>,
            confidenceScores: FloatArray? = null
        ): List<VoiceCommand> {
            return results.mapIndexed { index, text ->
                val confidence = confidenceScores?.getOrNull(index) ?: 1.0f
                VoiceCommand(
                    originalText = text,
                    normalizedText = text.lowercase().trim(),
                    confidence = confidence
                )
            }
        }
    }
}
