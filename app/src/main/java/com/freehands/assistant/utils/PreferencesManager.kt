package com.freehands.assistant.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A secure preferences manager that uses EncryptedSharedPreferences for storing sensitive data.
 * All data is encrypted before being stored on the device.
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // App Settings
    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var isNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    // User Preferences
    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    // Voice Settings
    var voiceLanguage: String
        get() = prefs.getString(KEY_VOICE_LANGUAGE, "en-US") ?: "en-US"
        set(value) = prefs.edit().putString(KEY_VOICE_LANGUAGE, value).apply()

    var voiceSpeed: Float
        get() = prefs.getFloat(KEY_VOICE_SPEED, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_VOICE_SPEED, value).apply()

    var voicePitch: Float
        get() = prefs.getFloat(KEY_VOICE_PITCH, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_VOICE_PITCH, value).apply()

    // App State
    var lastOpenedTimestamp: Long
        get() = prefs.getLong(KEY_LAST_OPENED, 0)
        set(value) = prefs.edit().putLong(KEY_LAST_OPENED, value).apply()

    var appLaunchCount: Int
        get() = prefs.getInt(KEY_APP_LAUNCH_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_APP_LAUNCH_COUNT, value).apply()

    // Feature Toggles
    var isWakeWordEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, value).apply()

    var isAutoStartEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_START_ENABLED, value).apply()

    // Security
    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var isPinRequired: Boolean
        get() = prefs.getBoolean(KEY_PIN_REQUIRED, false)
        set(value) = prefs.edit().putBoolean(KEY_PIN_REQUIRED, value).apply()

    // Clear all preferences
    fun clear() {
        prefs.edit().clear().apply()
    }

    // Remove a specific preference
    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    // Check if a key exists
    fun contains(key: String): Boolean {
        return prefs.contains(key)
    }

    // Get all preferences as a map
    fun getAll(): Map<String, *> {
        return prefs.all
    }

    companion object {
        // App Settings
        private const val KEY_FIRST_LAUNCH = "pref_first_launch"
        private const val KEY_DARK_MODE = "pref_dark_mode"
        private const val KEY_NOTIFICATIONS_ENABLED = "pref_notifications_enabled"

        // User Preferences
        private const val KEY_USER_NAME = "pref_user_name"
        private const val KEY_USER_EMAIL = "pref_user_email"

        // Voice Settings
        private const val KEY_VOICE_LANGUAGE = "pref_voice_language"
        private const val KEY_VOICE_SPEED = "pref_voice_speed"
        private const val KEY_VOICE_PITCH = "pref_voice_pitch"

        // App State
        private const val KEY_LAST_OPENED = "pref_last_opened"
        private const val KEY_APP_LAUNCH_COUNT = "pref_app_launch_count"

        // Feature Toggles
        private const val KEY_WAKE_WORD_ENABLED = "pref_wake_word_enabled"
        private const val KEY_AUTO_START_ENABLED = "pref_auto_start_enabled"

        // Security
        private const val KEY_BIOMETRIC_ENABLED = "pref_biometric_enabled"
        private const val KEY_PIN_REQUIRED = "pref_pin_required"
    }
}
