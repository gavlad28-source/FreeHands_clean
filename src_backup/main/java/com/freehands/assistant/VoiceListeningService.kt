package com.freehands.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.freehands.assistant.utils.VoiceRecognitionManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class VoiceListeningService : Service() {

    @Inject
    lateinit var voiceRecognitionManager: VoiceRecognitionManager
    
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var originalAudioMode = AudioManager.MODE_NORMAL
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "VoiceAssistantChannel"
        private const val WAKE_LOCK_TAG = "FreeHands:VoiceListeningService"
        
        fun startService(context: Context) {
            val intent = Intent(context, VoiceListeningService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, VoiceListeningService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Timber.d("VoiceListeningService created")
        
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        
        // Set up wake lock to keep CPU running
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "$WAKE_LOCK_TAG:WakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire()
        }
        
        // Set up audio manager for proper audio routing
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        originalAudioMode = audioManager?.mode ?: AudioManager.MODE_NORMAL
        
        // Start in foreground with a notification
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("VoiceListeningService started")
        
        // Start voice recognition
        try {
            voiceRecognitionManager.startListening()
        } catch (e: Exception) {
            Timber.e(e, "Failed to start voice recognition")
            stopSelf()
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        Timber.d("VoiceListeningService destroyed")
        
        // Stop voice recognition
        try {
            voiceRecognitionManager.stopListening()
        } catch (e: Exception) {
            Timber.e(e, "Error stopping voice recognition")
        }
        
        // Release wake lock
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        
        // Restore original audio mode
        audioManager?.mode = originalAudioMode
        
        super.onDestroy()
    }
    
    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FreeHands Assistant")
            .setContentText("Listening for voice commands...")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Voice Assistant Service",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Keeps the voice assistant running in the background"
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}
