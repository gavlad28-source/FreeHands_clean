package com.freehands.assistant.commands

import android.Manifest
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * CommandExecutor handles execution of system commands with proper permissions and confirmations
 */
class CommandExecutor(private val context: Context) {
    
    companion object {
        private const val TAG = "CommandExecutor"
    }
    
    /**
     * Command execution result
     */
    sealed class CommandResult {
        object Success : CommandResult()
        data class RequiresConfirmation(val commandType: CommandType, val details: String) : CommandResult()
        data class RequiresPermission(val permission: String) : CommandResult()
        data class Error(val message: String) : CommandResult()
    }
    
    /**
     * Types of commands
     */
    enum class CommandType {
        WIFI_TOGGLE,
        BLUETOOTH_TOGGLE,
        BRIGHTNESS_CHANGE,
        DND_TOGGLE,
        APP_LAUNCH,
        APP_CLOSE,
        PHONE_CALL,
        SEND_SMS,
        OPEN_SETTINGS,
        VOLUME_CHANGE
    }
    
    /**
     * Execute command based on voice input
     */
    suspend fun executeCommand(command: String, confirmed: Boolean = false): CommandResult = withContext(Dispatchers.Main) {
        try {
            val normalizedCommand = command.lowercase().trim()
            
            return@withContext when {
                // Wi-Fi commands
                normalizedCommand.contains("включи wi-fi") || normalizedCommand.contains("включи вайфай") -> {
                    toggleWifi(true, confirmed)
                }
                normalizedCommand.contains("выключи wi-fi") || normalizedCommand.contains("выключи вайфай") -> {
                    toggleWifi(false, confirmed)
                }
                
                // Bluetooth commands
                normalizedCommand.contains("включи bluetooth") || normalizedCommand.contains("включи блютуз") -> {
                    toggleBluetooth(true, confirmed)
                }
                normalizedCommand.contains("выключи bluetooth") || normalizedCommand.contains("выключи блютуз") -> {
                    toggleBluetooth(false, confirmed)
                }
                
                // Brightness commands
                normalizedCommand.contains("увеличь яркость") -> {
                    changeBrightness(increase = true, confirmed)
                }
                normalizedCommand.contains("уменьши яркость") -> {
                    changeBrightness(increase = false, confirmed)
                }
                normalizedCommand.contains("максимальная яркость") -> {
                    setBrightness(255, confirmed)
                }
                normalizedCommand.contains("минимальная яркость") -> {
                    setBrightness(10, confirmed)
                }
                
                // Do Not Disturb
                normalizedCommand.contains("включи не беспокоить") || normalizedCommand.contains("включи dnd") -> {
                    toggleDoNotDisturb(true, confirmed)
                }
                normalizedCommand.contains("выключи не беспокоить") || normalizedCommand.contains("выключи dnd") -> {
                    toggleDoNotDisturb(false, confirmed)
                }
                
                // App launch
                normalizedCommand.startsWith("открой ") || normalizedCommand.startsWith("запусти ") -> {
                    val appName = normalizedCommand.removePrefix("открой ").removePrefix("запусти ").trim()
                    launchApp(appName, confirmed)
                }
                
                // Phone call
                normalizedCommand.startsWith("позвони ") -> {
                    val number = extractPhoneNumber(normalizedCommand)
                    if (number != null) {
                        makePhoneCall(number, confirmed)
                    } else {
                        CommandResult.Error("Не удалось распознать номер телефона")
                    }
                }
                
                // SMS
                normalizedCommand.startsWith("отправь сообщение") -> {
                    CommandResult.RequiresConfirmation(CommandType.SEND_SMS, "SMS требует дополнительной настройки")
                }
                
                // Settings
                normalizedCommand.contains("открой настройки") -> {
                    openSettings()
                }
                
                else -> {
                    CommandResult.Error("Команда не распознана: $command")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing command", e)
            CommandResult.Error("Ошибка выполнения: ${e.message}")
        }
    }
    
    /**
     * Toggle Wi-Fi on/off
     */
    private fun toggleWifi(enable: Boolean, confirmed: Boolean): CommandResult {
        if (!confirmed) {
            return CommandResult.RequiresConfirmation(
                CommandType.WIFI_TOGGLE,
                if (enable) "Включить Wi-Fi?" else "Выключить Wi-Fi?"
            )
        }
        
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ doesn't allow direct Wi-Fi toggle
                // Open Wi-Fi settings instead
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                CommandResult.Success
            } else {
                @Suppress("DEPRECATION")
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = enable
                CommandResult.Success
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling WiFi", e)
            CommandResult.Error("Не удалось изменить Wi-Fi: ${e.message}")
        }
    }
    
    /**
     * Toggle Bluetooth on/off
     */
    private fun toggleBluetooth(enable: Boolean, confirmed: Boolean): CommandResult {
        if (!confirmed) {
            return CommandResult.RequiresConfirmation(
                CommandType.BLUETOOTH_TOGGLE,
                if (enable) "Включить Bluetooth?" else "Выключить Bluetooth?"
            )
        }
        
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Check permission for Android 12+
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) 
                    != PackageManager.PERMISSION_GRANTED) {
                    return CommandResult.RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ - open Bluetooth settings
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                CommandResult.Success
            } else {
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val bluetoothAdapter = bluetoothManager.adapter
                
                if (bluetoothAdapter == null) {
                    return CommandResult.Error("Bluetooth не поддерживается на этом устройстве")
                }
                
                @Suppress("DEPRECATION")
                if (enable) {
                    bluetoothAdapter.enable()
                } else {
                    bluetoothAdapter.disable()
                }
                CommandResult.Success
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Bluetooth permission denied", e)
            CommandResult.RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling Bluetooth", e)
            CommandResult.Error("Не удалось изменить Bluetooth: ${e.message}")
        }
    }
    
    /**
     * Change screen brightness
     */
    private fun changeBrightness(increase: Boolean, confirmed: Boolean): CommandResult {
        if (!confirmed) {
            return CommandResult.RequiresConfirmation(
                CommandType.BRIGHTNESS_CHANGE,
                if (increase) "Увеличить яркость?" else "Уменьшить яркость?"
            )
        }
        
        return try {
            // Check if we can modify system settings
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(context)) {
                    // Open settings to grant permission
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    return CommandResult.Error("Требуется разрешение на изменение системных настроек")
                }
            }
            
            // Get current brightness
            val currentBrightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                128
            )
            
            // Calculate new brightness (0-255)
            val newBrightness = if (increase) {
                (currentBrightness + 50).coerceIn(10, 255)
            } else {
                (currentBrightness - 50).coerceIn(10, 255)
            }
            
            // Set new brightness
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                newBrightness
            )
            
            CommandResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error changing brightness", e)
            CommandResult.Error("Не удалось изменить яркость: ${e.message}")
        }
    }
    
    /**
     * Set specific brightness level
     */
    private fun setBrightness(level: Int, confirmed: Boolean): CommandResult {
        return changeBrightness(increase = level > 128, confirmed)
    }
    
    /**
     * Toggle Do Not Disturb mode
     */
    private fun toggleDoNotDisturb(enable: Boolean, confirmed: Boolean): CommandResult {
        if (!confirmed) {
            return CommandResult.RequiresConfirmation(
                CommandType.DND_TOGGLE,
                if (enable) "Включить режим 'Не беспокоить'?" else "Выключить режим 'Не беспокоить'?"
            )
        }
        
        return try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Check if we have DND permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!notificationManager.isNotificationPolicyAccessGranted) {
                    // Open settings to grant permission
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    return CommandResult.Error("Требуется разрешение на управление режимом 'Не беспокоить'")
                }
            }
            
            // Set DND mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val mode = if (enable) {
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY
                } else {
                    NotificationManager.INTERRUPTION_FILTER_ALL
                }
                notificationManager.setInterruptionFilter(mode)
            }
            
            CommandResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling DND", e)
            CommandResult.Error("Не удалось изменить режим 'Не беспокоить': ${e.message}")
        }
    }
    
    /**
     * Launch app by name
     */
    private fun launchApp(appName: String, confirmed: Boolean): CommandResult {
        if (!confirmed) {
            return CommandResult.RequiresConfirmation(
                CommandType.APP_LAUNCH,
                "Открыть приложение $appName?"
            )
        }
        
        return try {
            // Map common app names to package names
            val packageName = getPackageNameForApp(appName)
            
            if (packageName != null) {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    CommandResult.Success
                } else {
                    CommandResult.Error("Не удалось запустить приложение $appName")
                }
            } else {
                CommandResult.Error("Приложение '$appName' не найдено")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app", e)
            CommandResult.Error("Ошибка запуска приложения: ${e.message}")
        }
    }
    
    /**
     * Make phone call
     */
    private fun makePhoneCall(phoneNumber: String, confirmed: Boolean): CommandResult {
        if (!confirmed) {
            return CommandResult.RequiresConfirmation(
                CommandType.PHONE_CALL,
                "Позвонить на номер $phoneNumber?"
            )
        }
        
        return try {
            // Check permission
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) 
                != PackageManager.PERMISSION_GRANTED) {
                return CommandResult.RequiresPermission(Manifest.permission.CALL_PHONE)
            }
            
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            CommandResult.Success
        } catch (e: SecurityException) {
            Log.e(TAG, "Call permission denied", e)
            CommandResult.RequiresPermission(Manifest.permission.CALL_PHONE)
        } catch (e: Exception) {
            Log.e(TAG, "Error making call", e)
            CommandResult.Error("Ошибка звонка: ${e.message}")
        }
    }
    
    /**
     * Open system settings
     */
    private fun openSettings(): CommandResult {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            CommandResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error opening settings", e)
            CommandResult.Error("Не удалось открыть настройки: ${e.message}")
        }
    }
    
    /**
     * Extract phone number from command
     */
    private fun extractPhoneNumber(command: String): String? {
        // Simple regex to extract phone number
        val regex = Regex("""\+?\d[\d\s-]{7,}\d""")
        return regex.find(command)?.value?.replace("\\s".toRegex(), "")
    }
    
    /**
     * Map app names to package names
     */
    private fun getPackageNameForApp(appName: String): String? {
        val appMap = mapOf(
            "chrome" to "com.android.chrome",
            "хром" to "com.android.chrome",
            "youtube" to "com.google.android.youtube",
            "ютуб" to "com.google.android.youtube",
            "gmail" to "com.google.android.gm",
            "почта" to "com.google.android.gm",
            "whatsapp" to "com.whatsapp",
            "ватсап" to "com.whatsapp",
            "telegram" to "org.telegram.messenger",
            "телеграм" to "org.telegram.messenger",
            "camera" to "com.android.camera2",
            "камера" to "com.android.camera2",
            "phone" to "com.android.dialer",
            "телефон" to "com.android.dialer",
            "contacts" to "com.android.contacts",
            "контакты" to "com.android.contacts",
            "messages" to "com.android.messaging",
            "сообщения" to "com.android.messaging",
            "calculator" to "com.android.calculator2",
            "калькулятор" to "com.android.calculator2",
            "clock" to "com.android.deskclock",
            "часы" to "com.android.deskclock",
            "calendar" to "com.google.android.calendar",
            "календарь" to "com.google.android.calendar"
        )
        
        return appMap[appName.lowercase()]
    }
}
