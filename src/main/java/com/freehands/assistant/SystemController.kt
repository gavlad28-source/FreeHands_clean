package com.freehands.assistant

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.freehands.assistant.utils.PermissionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles system-level operations like volume control, screen state, and device settings.
 */
@Singleton
class SystemController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionManager: PermissionManager
) {
    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    
    private val powerManager: PowerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    
    // Volume control methods
    fun setVolume(streamType: Int, volume: Int, flags: Int = 0) {
        audioManager.setStreamVolume(streamType, volume, flags)
    }
    
    fun getVolume(streamType: Int): Int {
        return audioManager.getStreamVolume(streamType)
    }
    
    fun getMaxVolume(streamType: Int): Int {
        return audioManager.getStreamMaxVolume(streamType)
    }
    
    fun setRingerMode(mode: Int) {
        audioManager.ringerMode = mode
    }
    
    fun getRingerMode(): Int {
        return audioManager.ringerMode
    }
    
    // Screen control methods
    fun isScreenOn(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            powerManager.isInteractive
        } else {
            @Suppress("DEPRECATION")
            powerManager.isScreenOn
        }
    }
    
    fun isDeviceIdleMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isDeviceIdleMode
        } else {
            false
        }
    }
    
    // Notification control methods
    fun areNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationManager.areNotificationsEnabled()
        } else {
            true // Default to true for older versions
        }
    }
    
    fun openNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            @Suppress("DEPRECATION")
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
            }
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
    
    // System settings methods
    fun openWifiSettings() {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    fun openBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    // Permission-related methods
    fun hasPermission(permission: String): Boolean {
        return permissionManager.hasPermissions(context, permission)
    }
    
    fun hasAllRequiredPermissions(): Boolean {
        return permissionManager.hasAllRequiredPermissions(context)
    }
    
    fun getMissingPermissions(): List<String> {
        return permissionManager.getMissingPermissions(context)
    }
    
    // System services control
    fun startAccessibilityService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(Intent(context, VoiceAccessibilityService::class.java))
        } else {
            context.startService(Intent(context, VoiceAccessibilityService::class.java))
        }
    }
    
    fun stopAccessibilityService() {
        context.stopService(Intent(context, VoiceAccessibilityService::class.java))
    }
    
    fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        
        return enabledServices?.contains("${context.packageName}/${VoiceAccessibilityService::class.java.name}") == true
    }
    
    companion object {
        // Stream types
        const val STREAM_VOICE_CALL = AudioManager.STREAM_VOICE_CALL
        const val STREAM_SYSTEM = AudioManager.STREAM_SYSTEM
        const val STREAM_RING = AudioManager.STREAM_RING
        const val STREAM_MUSIC = AudioManager.STREAM_MUSIC
        const val STREAM_ALARM = AudioManager.STREAM_ALARM
        const val STREAM_NOTIFICATION = AudioManager.STREAM_NOTIFICATION
        
        // Ringer modes
        const val RINGER_MODE_SILENT = AudioManager.RINGER_MODE_SILENT
        const val RINGER_MODE_VIBRATE = AudioManager.RINGER_MODE_VIBRATE
        const val RINGER_MODE_NORMAL = AudioManager.RINGER_MODE_NORMAL
    }
}
