package com.freehands.assistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.freehands.assistant.utils.PreferencesManager
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class FreeHandsApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Setup Crashlytics
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        
        // Setup logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }
        
        // Create notification channels
        createNotificationChannels()
    }
    
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
            .build()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Voice Command Channel
            val voiceCommandChannel = NotificationChannel(
                CHANNEL_ID_VOICE_COMMAND,
                "Voice Commands",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for voice command processing"
                enableVibration(true)
            }
            
            // Background Service Channel
            val serviceChannel = NotificationChannel(
                CHANNEL_ID_SERVICE,
                "Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for background services"
            }
            
            notificationManager.createNotificationChannels(listOf(voiceCommandChannel, serviceChannel))
        }
    }
    
    companion object {
        const val CHANNEL_ID_VOICE_COMMAND = "voice_command_channel"
        const val CHANNEL_ID_SERVICE = "service_channel"
        const val NOTIFICATION_ID_FOREGROUND_SERVICE = 1001
    }
}

/**
 * A tree which logs important information for crash reporting.
 */
class CrashReportingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == android.util.Log.ERROR || priority == android.util.Log.WARN) {
            FirebaseCrashlytics.getInstance().log("$tag: $message")
            t?.let { FirebaseCrashlytics.getInstance().recordException(it) }
        }
    }
}
