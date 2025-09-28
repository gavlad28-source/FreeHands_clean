package com.freehands.assistant.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.freehands.assistant.R
import com.freehands.assistant.data.local.dao.VoiceCommandDao
import com.freehands.assistant.data.local.entity.VoiceCommandEntity
import com.freehands.assistant.data.model.CommandType
import com.freehands.assistant.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Processes voice commands and executes the corresponding actions.
 * This class handles the interpretation of natural language commands and maps them to specific actions.
 */
@Singleton
class VoiceCommandProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val commandDao: VoiceCommandDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val tag = "VoiceCommandProcessor"
    
    /**
     * Processes a recognized voice command.
     * @param command The recognized speech text to process
     * @param callback Callback to handle the result of command processing
     */
    suspend fun processCommand(
        command: String,
        callback: (Boolean, String) -> Unit
    ) = withContext(ioDispatcher) {
        try {
            Log.d(tag, "Processing command: $command")
            
            // Normalize the command for processing
            val normalizedCommand = command.trim().lowercase(Locale.getDefault())
            
            // Check for exact matches in custom commands first
            val customCommand = commandDao.getCommandByPhrase(normalizedCommand)
            if (customCommand != null) {
                executeCommand(customCommand, callback)
                return@withContext
            }
            
            // If no exact match, try to interpret the command
            val interpretedCommand = interpretCommand(normalizedCommand)
            if (interpretedCommand != null) {
                executeCommand(interpretedCommand, callback)
            } else {
                // If we couldn't interpret the command, save it for later analysis
                saveUnknownCommand(command)
                callback(false, context.getString(R.string.command_not_recognized))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error processing command", e)
            callback(false, "Error: ${e.message}")
        }
    }
    
    /**
     * Interprets a natural language command and maps it to a known command type.
     */
    private suspend fun interpretCommand(command: String): VoiceCommandEntity? {
        // Basic command interpretation logic
        // This can be enhanced with more sophisticated NLP in the future
        return when {
            command.contains("call") || command.contains("phone") -> {
                VoiceCommandEntity(
                    id = 0,
                    phrase = command,
                    action = CommandType.CALL,
                    target = extractContact(command),
                    needsConfirmation = true
                )
            }
            command.contains("message") || command.contains("text") || command.contains("sms") -> {
                VoiceCommandEntity(
                    id = 0,
                    phrase = command,
                    action = CommandType.MESSAGE,
                    target = extractContact(command),
                    data = extractMessage(command),
                    needsConfirmation = true
                )
            }
            command.contains("navigate") || command.contains("directions") || command.contains("map") -> {
                VoiceCommandEntity(
                    id = 0,
                    phrase = command,
                    action = CommandType.NAVIGATE,
                    target = extractLocation(command),
                    needsConfirmation = true
                )
            }
            command.contains("play") || command.contains("music") || command.contains("song") -> {
                VoiceCommandEntity(
                    id = 0,
                    phrase = command,
                    action = CommandType.PLAY_MEDIA,
                    target = extractMediaQuery(command),
                    needsConfirmation = false
                )
            }
            command.contains("turn on") || command.contains("turn off") -> {
                val setting = if (command.contains("wifi")) "wifi"
                    else if (command.contains("bluetooth")) "bluetooth"
                    else if (command.contains("airplane")) "airplane"
                    else ""
                
                if (setting.isNotEmpty()) {
                    VoiceCommandEntity(
                        id = 0,
                        phrase = command,
                        action = if (command.contains("on")) CommandType.ENABLE_FEATURE 
                               else CommandType.DISABLE_FEATURE,
                        target = setting,
                        needsConfirmation = true
                    )
                } else {
                    null
                }
            }
            command.contains("what can you do") || command.contains("help") -> {
                VoiceCommandEntity(
                    id = 0,
                    phrase = command,
                    action = CommandType.HELP,
                    needsConfirmation = false
                )
            }
            else -> null
        }
    }
    
    /**
     * Executes a recognized command.
     */
    private suspend fun executeCommand(
        command: VoiceCommandEntity,
        callback: (Boolean, String) -> Unit
    ) {
        try {
            Log.d(tag, "Executing command: ${command.action} - ${command.target}")
            
            // Check if command needs confirmation
            if (command.needsConfirmation) {
                // In a real app, this would show a confirmation dialog
                // For now, we'll just log it
                Log.i(tag, "Confirmation required for: ${command.phrase}")
            }
            
            // Execute the command based on its type
            when (command.action) {
                CommandType.CALL -> handleCallCommand(command)
                CommandType.MESSAGE -> handleMessageCommand(command)
                CommandType.NAVIGATE -> handleNavigationCommand(command)
                CommandType.PLAY_MEDIA -> handleMediaCommand(command)
                CommandType.ENABLE_FEATURE -> handleFeatureCommand(command, enable = true)
                CommandType.DISABLE_FEATURE -> handleFeatureCommand(command, enable = false)
                CommandType.HELP -> handleHelpCommand()
                else -> {
                    Log.w(tag, "Unhandled command type: ${command.action}")
                    callback(false, "Command not yet implemented")
                    return
                }
            }
            
            // If we got here, the command was executed successfully
            callback(true, "Command executed: ${command.action}")
            
            // Save the successful command for future reference
            saveCommand(command)
            
        } catch (e: SecurityException) {
            Log.e(tag, "Permission denied for command: ${command.action}", e)
            callback(false, "Permission denied: ${e.message}")
        } catch (e: Exception) {
            Log.e(tag, "Error executing command: ${command.action}", e)
            callback(false, "Error: ${e.message}")
        }
    }
    
    // --- Command Handlers ---
    
    private fun handleCallCommand(command: VoiceCommandEntity) {
        val contact = command.target ?: throw IllegalArgumentException("No contact specified")
        val phoneNumber = lookupContact(contact) ?: contact
        
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    private fun handleMessageCommand(command: VoiceCommandEntity) {
        val contact = command.target ?: throw IllegalArgumentException("No contact specified")
        val message = command.data ?: throw IllegalArgumentException("No message specified")
        val phoneNumber = lookupContact(contact) ?: contact
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms:$phoneNumber")
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    private fun handleNavigationCommand(command: VoiceCommandEntity) {
        val location = command.target ?: throw IllegalArgumentException("No location specified")
        val uri = Uri.parse("google.navigation:q=$location")
        
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to web URL if Google Maps app is not installed
            val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$location")
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }
    
    private fun handleMediaCommand(command: VoiceCommandEntity) {
        val query = command.target ?: throw IllegalArgumentException("No media query specified")
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("vnd.youtube:$query")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to web search
            val webUri = Uri.parse("https://www.youtube.com/results?search_query=$query")
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }
    
    private fun handleFeatureCommand(command: VoiceCommandEntity, enable: Boolean) {
        val setting = command.target?.lowercase() ?: throw IllegalArgumentException("No feature specified")
        
        when (setting) {
            "wifi" -> {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
                wifiManager.isWifiEnabled = enable
            }
            "bluetooth" -> {
                val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                if (enable) bluetoothAdapter?.enable() else bluetoothAdapter?.disable()
            }
            "airplane" -> {
                Settings.Global.putInt(
                    context.contentResolver,
                    Settings.Global.AIRPLANE_MODE_ON,
                    if (enable) 1 else 0
                )
                // Post an intent to reload
                val intent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).apply {
                    putExtra("state", enable)
                }
                context.sendBroadcast(intent)
            }
            else -> throw IllegalArgumentException("Unsupported feature: $setting")
        }
    }
    
    private fun handleHelpCommand() {
        // In a real app, this would show a help screen or speak the available commands
        val availableCommands = listOf(
            "Call [contact]",
            "Message [contact] [message]",
            "Navigate to [location]",
            "Play [song/artist/playlist]",
            "Turn on/off [wifi/bluetooth/airplane]",
            "What can you do?"
        )
        
        val message = "You can say things like: ${availableCommands.joinToString(", ")}"
        Log.i(tag, message)
        
        // In a real app, you would use TextToSpeech here to speak the message
        // or update the UI to show the available commands
    }
    
    // --- Utility Methods ---
    
    private fun extractContact(command: String): String? {
        // Simple extraction - in a real app, use more sophisticated NLP
        return command.split(" ").getOrNull(1)
    }
    
    private fun extractMessage(command: String): String? {
        // Simple extraction - in a real app, use more sophisticated NLP
        val parts = command.split(" ")
        return if (parts.size > 2) parts.drop(2).joinToString(" ") else null
    }
    
    private fun extractLocation(command: String): String? {
        // Simple extraction - in a real app, use more sophisticated NLP
        return command.substringAfter("to ").trim()
    }
    
    private fun extractMediaQuery(command: String): String? {
        // Simple extraction - in a real app, use more sophisticated NLP
        return command.substringAfter("play ").trim()
    }
    
    private fun lookupContact(name: String): String? {
        // In a real app, query the contacts provider to get the phone number
        // This is a simplified version
        return null
    }
    
    private suspend fun saveCommand(command: VoiceCommandEntity) {
        try {
            commandDao.insertCommand(command)
        } catch (e: Exception) {
            Log.e(tag, "Error saving command to database", e)
        }
    }
    
    private suspend fun saveUnknownCommand(command: String) {
        try {
            val unknownCommand = VoiceCommandEntity(
                id = 0,
                phrase = command,
                action = CommandType.UNKNOWN,
                isCustom = false,
                needsConfirmation = false,
                lastUsed = System.currentTimeMillis()
            )
            commandDao.insertCommand(unknownCommand)
        } catch (e: Exception) {
            Log.e(tag, "Error saving unknown command", e)
        }
    }
}
