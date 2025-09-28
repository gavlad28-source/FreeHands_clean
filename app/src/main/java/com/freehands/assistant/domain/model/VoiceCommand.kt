package com.freehands.assistant.domain.model

/**
 * Represents a voice command recognized by the app.
 * @param category The category of the command (e.g., "settings", "apps", "media")
 * @param action The action to perform (e.g., "increase", "open", "play")
 * @param target The target of the action (e.g., "brightness", "whatsapp", "music")
 * @param value Optional value for the command (e.g., "50%", "10 minutes")
 */
data class VoiceCommand(
    val category: String,
    val action: String,
    val target: String,
    val value: String = ""
) {
    companion object {
        fun fromText(text: String): VoiceCommand {
            // This is a simplified parser - you'll want to implement a more sophisticated one
            val words = text.lowercase().split(" ")
            
            // Check for settings commands
            if (words.any { it in listOf("brightness", "volume", "wifi", "bluetooth") }) {
                return parseSettingsCommand(words)
            }
            
            // Check for app commands
            if (words.any { it in listOf("open", "close", "start", "launch") }) {
                return parseAppCommand(words)
            }
            
            // Default to a generic command
            return VoiceCommand(
                category = "general",
                action = "process",
                target = text
            )
        }
        
        private fun parseSettingsCommand(words: List<String>): VoiceCommand {
            // Example: "increase brightness to 50%"
            return when {
                "brightness" in words -> {
                    val action = when {
                        "increase" in words -> "increase"
                        "decrease" in words -> "decrease"
                        "set" in words -> "set"
                        else -> "adjust"
                    }
                    val value = words.find { it.endsWith("%") } ?: ""
                    VoiceCommand("settings", action, "brightness", value)
                }
                "volume" in words -> {
                    val action = when {
                        "increase" in words -> "increase"
                        "decrease" in words -> "decrease"
                        "mute" in words -> "mute"
                        "unmute" in words -> "unmute"
                        else -> "adjust"
                    }
                    VoiceCommand("settings", action, "volume")
                }
                "wifi" in words -> {
                    val action = if ("on" in words || "enable" in words) "enable" else "disable"
                    VoiceCommand("settings", action, "wifi")
                }
                "bluetooth" in words -> {
                    val action = if ("on" in words || "enable" in words) "enable" else "disable"
                    VoiceCommand("settings", action, "bluetooth")
                }
                else -> VoiceCommand("settings", "unknown", "")
            }
        }
        
        private fun parseAppCommand(words: List<String>): VoiceCommand {
            val action = when {
                "open" in words || "start" in words || "launch" in words -> "open"
                "close" in words || "exit" in words -> "close"
                else -> "unknown"
            }
            
            // Find the app name (usually the word after the action)
            val actionIndex = words.indexOfFirst { it in listOf("open", "start", "launch", "close", "exit") }
            val appName = if (actionIndex >= 0 && actionIndex < words.size - 1) {
                words[actionIndex + 1]
            } else ""
            
            return VoiceCommand("apps", action, appName)
        }
    }
}
