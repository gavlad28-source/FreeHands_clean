package com.freehands.assistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.freehands.assistant.data.model.CommandType

/**
 * Represents a voice command that can be recognized and executed by the assistant.
 * 
 * @property id Unique identifier for the command
 * @property phrase The spoken phrase that triggers this command
 * @property action The type of action to perform when this command is recognized
 * @property target The target of the action (e.g., contact name, URL, app package name)
 * @property data Additional data needed for the action (e.g., message body)
 * @property isCustom Whether this is a user-defined custom command
 * @property needsConfirmation Whether the user should confirm this command before execution
 * @property lastUsed Timestamp when this command was last used
 * @property useCount How many times this command has been used
 */
@Entity(tableName = "voice_commands")
data class VoiceCommandEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // The spoken phrase that triggers this command
    val phrase: String,
    
    // The type of action to perform
    val action: CommandType,
    
    // The target of the action (e.g., contact name, URL, app package name)
    val target: String? = null,
    
    // Additional data needed for the action (e.g., message body)
    val data: String? = null,
    
    // Whether this is a user-defined custom command
    val isCustom: Boolean = false,
    
    // Whether the user should confirm this command before execution
    val needsConfirmation: Boolean = true,
    
    // Timestamp when this command was last used
    val lastUsed: Long = System.currentTimeMillis(),
    
    // How many times this command has been used
    val useCount: Int = 0
) {
    /**
     * Creates a copy of this command with an incremented use count and updated last used timestamp.
     */
    fun withIncrementedUse(): VoiceCommandEntity {
        return copy(
            useCount = useCount + 1,
            lastUsed = System.currentTimeMillis()
        )
    }
    
    /**
     * Creates a copy of this command with the given confirmation requirement.
     */
    fun withConfirmation(needsConfirmation: Boolean): VoiceCommandEntity {
        return copy(needsConfirmation = needsConfirmation)
    }
}
