package com.freehands.assistant.domain.usecase

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import com.freehands.assistant.R
import com.freehands.assistant.data.model.VoiceCommand
import com.freehands.assistant.utils.AppConstants
import com.freehands.assistant.utils.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles processing of voice commands and executes corresponding actions.
 */
@Singleton
class VoiceCommandProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val audioManager: AudioManager
) {
    
    /**
     * Processes the given voice command and returns the result.
     */
    fun processCommand(command: String): Flow<CommandResult> = flow {
        emit(CommandResult.Processing)
        
        try {
            val normalizedCommand = command.lowercase().trim()
            val result = when {
                // Greeting
                normalizedCommand.contains("hello") || 
                normalizedCommand.contains("hi") || 
                normalizedCommand.contains("hey") -> {
                    handleGreeting()
                }
                
                // Settings commands
                normalizedCommand.contains(AppConstants.VoiceCommand.SETTINGS_BRIGHTNESS) -> {
                    handleBrightnessCommand(normalizedCommand)
                }
                
                normalizedCommand.contains(AppConstants.VoiceCommand.SETTINGS_VOLUME) -> {
                    handleVolumeCommand(normalizedCommand)
                }
                
                normalizedCommand.contains(AppConstants.VoiceCommand.SETTINGS_WIFI) -> {
                    handleWifiCommand(normalizedCommand)
                }
                
                normalizedCommand.contains(AppConstants.VoiceCommand.SETTINGS_BLUETOOTH) -> {
                    handleBluetoothCommand(normalizedCommand)
                }
                
                // Media commands
                normalizedCommand.contains(AppConstants.VoiceCommand.MEDIA_PLAY) -> {
                    handleMediaPlayCommand()
                }
                
                normalizedCommand.contains(AppConstants.VoiceCommand.MEDIA_PAUSE) -> {
                    handleMediaPauseCommand()
                }
                
                // Navigation commands
                normalizedCommand.startsWith(AppConstants.VoiceCommand.ACTION_NAVIGATE) -> {
                    handleNavigationCommand(normalizedCommand)
                }
                
                // Default case - command not recognized
                else -> {
                    CommandResult.Error("I'm sorry, I didn't understand that command.")
                }
            }
            
            emit(result)
        } catch (e: Exception) {
            emit(CommandResult.Error("Error processing command: ${e.message}"))
        }
    }
    
    // Region: Command Handlers
    
    private fun handleGreeting(): CommandResult {
        val userName = preferencesManager.userName ?: "there"
        return CommandResult.Success("Hello $userName! How can I assist you today?")
    }
    
    private fun handleBrightnessCommand(command: String): CommandResult {
        return try {
            val brightnessValue = extractNumberFromCommand(command)
            if (brightnessValue in 0..100) {
                // This requires WRITE_SETTINGS permission
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    brightnessValue
                )
                CommandResult.Success("Brightness set to $brightnessValue%")
            } else {
                CommandResult.Error("Please specify a brightness value between 0 and 100")
            }
        } catch (e: Exception) {
            CommandResult.Error("Failed to set brightness: ${e.message}")
        }
    }
    
    private fun handleVolumeCommand(command: String): CommandResult {
        return try {
            val volumeLevel = extractNumberFromCommand(command)
            if (volumeLevel in 0..audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    volumeLevel,
                    AudioManager.FLAG_SHOW_UI
                )
                CommandResult.Success("Volume set to $volumeLevel")
            } else {
                CommandResult.Error("Please specify a volume between 0 and ${audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)}")
            }
        } catch (e: Exception) {
            CommandResult.Error("Failed to adjust volume: ${e.message}")
        }
    }
    
    private fun handleWifiCommand(command: String): CommandResult {
        return try {
            val isEnable = !command.contains("off") && !command.contains("disable")
            // This requires CHANGE_WIFI_STATE permission
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            wifiManager.isWifiEnabled = isEnable
            
            val status = if (isEnable) "enabled" else "disabled"
            CommandResult.Success("WiFi has been $status")
        } catch (e: Exception) {
            CommandResult.Error("Failed to toggle WiFi: ${e.message}")
        }
    }
    
    private fun handleBluetoothCommand(command: String): CommandResult {
        return try {
            val isEnable = !command.contains("off") && !command.contains("disable")
            // This requires BLUETOOTH_ADMIN permission
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            if (isEnable) {
                bluetoothAdapter?.enable()
            } else {
                bluetoothAdapter?.disable()
            }
            
            val status = if (isEnable) "enabled" else "disabled"
            CommandResult.Success("Bluetooth has been $status")
        } catch (e: Exception) {
            CommandResult.Error("Failed to toggle Bluetooth: ${e.message}")
        }
    }
    
    private fun handleMediaPlayCommand(): CommandResult {
        // Simulate media play action
        return CommandResult.Success("Playing media")
    }
    
    private fun handleMediaPauseCommand(): CommandResult {
        // Simulate media pause action
        return CommandResult.Success("Media paused")
    }
    
    private fun handleNavigationCommand(command: String): CommandResult {
        val destination = command.replace("navigate to", "").trim()
        if (destination.isBlank()) {
            return CommandResult.Error("Please specify a destination")
        }
        
        // In a real app, you would integrate with a maps/navigation API here
        return CommandResult.Success("Navigating to $destination")
    }
    
    // Region: Helper Methods
    
    private fun extractNumberFromCommand(command: String): Int {
        val regex = "\\d+".toRegex()
        return regex.find(command)?.value?.toInt() ?: throw NumberFormatException("No number found in command")
    }
    
    // Region: Data Classes
    
    sealed class CommandResult {
        object Processing : CommandResult()
        data class Success(val message: String) : CommandResult()
        data class Error(val message: String) : CommandResult()
    }
}
