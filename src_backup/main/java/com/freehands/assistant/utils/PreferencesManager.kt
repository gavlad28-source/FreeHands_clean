package com.freehands.assistant.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "FreeHandsPrefs",
        Context.MODE_PRIVATE
    )
    
    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "system") ?: "system"
        set(value) = prefs.edit { putString(KEY_THEME_MODE, value) }
    
    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit { putBoolean(KEY_FIRST_LAUNCH, value) }
    
    var voiceSensitivity: Float
        get() = prefs.getFloat(KEY_VOICE_SENSITIVITY, 0.7f)
        set(value) = prefs.edit { putFloat(KEY_VOICE_SENSITIVITY, value) }
    
    var wakeWordEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_WAKE_WORD_ENABLED, value) }
    
    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_VOICE_SENSITIVITY = "voice_sensitivity"
        private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
    }
}
