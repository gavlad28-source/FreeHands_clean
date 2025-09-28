package com.freehands.assistant.data.model

/**
 * Represents the type of command that can be executed by the voice assistant.
 */
enum class CommandType {
    // System commands
    UNKNOWN,
    HELP,
    
    // Communication
    CALL,
    MESSAGE,
    EMAIL,
    
    // Navigation
    NAVIGATE,
    
    // Media
    PLAY_MEDIA,
    PAUSE_MEDIA,
    NEXT_TRACK,
    PREVIOUS_TRACK,
    
    // System controls
    ENABLE_FEATURE,
    DISABLE_FEATURE,
    
    // Custom user-defined commands
    CUSTOM;
    
    companion object {
        fun fromString(value: String): CommandType {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                UNKNOWN
            }
        }
    }
}
