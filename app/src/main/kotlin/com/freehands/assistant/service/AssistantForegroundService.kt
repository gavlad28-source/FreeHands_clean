package com.freehands.assistant.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.freehands.assistant.R
import com.freehands.assistant.asr.VoskManager
import com.freehands.assistant.commands.CommandExecutor
import com.freehands.assistant.tts.TTSManager
import com.freehands.assistant.wakeword.WakeWordDetector
import kotlinx.coroutines.*
import org.json.JSONObject
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener

/**
 * AssistantForegroundService runs in background, listens for wake word,
 * and processes voice commands
 */
class AssistantForegroundService : Service() {
    
    companion object {
        private const val TAG = "AssistantService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "assistant_service_channel"
        
        const val ACTION_START = "com.freehands.assistant.action.START"
        const val ACTION_STOP = "com.freehands.assistant.action.STOP"
        const val ACTION_PAUSE = "com.freehands.assistant.action.PAUSE"
        const val ACTION_RESUME = "com.freehands.assistant.action.RESUME"
        
        fun start(context: Context) {
            val intent = Intent(context, AssistantForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, AssistantForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private lateinit var voskManager: VoskManager
    private lateinit var wakeWordDetector: WakeWordDetector
    private lateinit var commandExecutor: CommandExecutor
    private lateinit var ttsManager: TTSManager
    
    private var wakeLock: PowerManager.WakeLock? = null
    private var isPaused = false
    private var isListeningForCommand = false
    
    private var commandRecognizer: Recognizer? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        // Initialize components
        voskManager = VoskManager(applicationContext)
        commandExecutor = CommandExecutor(applicationContext)
        ttsManager = TTSManager(applicationContext)
        
        // Acquire wake lock to keep service running
        acquireWakeLock()
        
        // Create notification channel
        createNotificationChannel()
        
        // Initialize in background
        serviceScope.launch {
            initializeService()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
                startAssistant()
            }
            ACTION_STOP -> {
                stopAssistant()
                stopSelf()
            }
            ACTION_PAUSE -> {
                pauseAssistant()
            }
            ACTION_RESUME -> {
                resumeAssistant()
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        stopAssistant()
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }
    
    /**
     * Initialize service components
     */
    private suspend fun initializeService() {
        try {
            Log.d(TAG, "Initializing service components...")
            
            // Initialize TTS
            val ttsInitialized = ttsManager.initialize()
            if (!ttsInitialized) {
                Log.e(TAG, "Failed to initialize TTS")
            }
            
            // Initialize Vosk
            val voskInitialized = voskManager.initialize()
            if (!voskInitialized) {
                Log.e(TAG, "Failed to initialize Vosk")
                ttsManager.speak("Ошибка инициализации распознавания речи")
                stopSelf()
                return
            }
            
            // Initialize wake word detector
            wakeWordDetector = WakeWordDetector(applicationContext, voskManager)
            val wakeWordInitialized = wakeWordDetector.initialize()
            if (!wakeWordInitialized) {
                Log.e(TAG, "Failed to initialize wake word detector")
                ttsManager.speak("Ошибка инициализации детектора ключевых слов")
                stopSelf()
                return
            }
            
            Log.i(TAG, "✓ Service initialized successfully")
            ttsManager.speak("Голосовой ассистент готов к работе")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing service", e)
            stopSelf()
        }
    }
    
    /**
     * Start assistant (wake word listening)
     */
    private fun startAssistant() {
        if (isPaused) {
            resumeAssistant()
            return
        }
        
        serviceScope.launch {
            try {
                Log.d(TAG, "Starting assistant...")
                
                // Start listening for wake word
                wakeWordDetector.startListening { wakeWord ->
                    onWakeWordDetected(wakeWord)
                }
                
                updateNotification("Слушаю...")
                Log.i(TAG, "✓ Assistant started")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting assistant", e)
            }
        }
    }
    
    /**
     * Stop assistant
     */
    private fun stopAssistant() {
        try {
            wakeWordDetector.stopListening()
            stopCommandRecognition()
            voskManager.release()
            ttsManager.release()
            updateNotification("Остановлен")
            Log.d(TAG, "Assistant stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping assistant", e)
        }
    }
    
    /**
     * Pause assistant
     */
    private fun pauseAssistant() {
        isPaused = true
        wakeWordDetector.stopListening()
        updateNotification("Приостановлен")
        Log.d(TAG, "Assistant paused")
    }
    
    /**
     * Resume assistant
     */
    private fun resumeAssistant() {
        isPaused = false
        wakeWordDetector.startListening { wakeWord ->
            onWakeWordDetected(wakeWord)
        }
        updateNotification("Слушаю...")
        Log.d(TAG, "Assistant resumed")
    }
    
    /**
     * Called when wake word is detected
     */
    private fun onWakeWordDetected(wakeWord: String) {
        serviceScope.launch {
            try {
                Log.i(TAG, "🎙️ Wake word detected: $wakeWord")
                
                // Stop wake word detection temporarily
                wakeWordDetector.stopListening()
                
                // Play acknowledgment sound or speak
                ttsManager.speak("Слушаю")
                delay(1000) // Wait for TTS to finish
                
                // Start command recognition
                startCommandRecognition()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling wake word", e)
                // Resume wake word detection
                wakeWordDetector.startListening { onWakeWordDetected(it) }
            }
        }
    }
    
    /**
     * Start listening for command after wake word
     */
    private fun startCommandRecognition() {
        try {
            isListeningForCommand = true
            updateNotification("Слушаю команду...")
            
            // Create recognizer for full command recognition
            commandRecognizer = voskManager.createRecognizer()
            
            if (commandRecognizer == null) {
                Log.e(TAG, "Failed to create command recognizer")
                resumeWakeWordDetection()
                return
            }
            
            // Start listening with timeout
            voskManager.startRecognition(object : RecognitionListener {
                override fun onResult(hypothesis: String?) {
                    hypothesis?.let { processCommand(it) }
                }
                
                override fun onFinalResult(hypothesis: String?) {
                    hypothesis?.let { processCommand(it) }
                    stopCommandRecognition()
                    resumeWakeWordDetection()
                }
                
                override fun onPartialResult(hypothesis: String?) {
                    // Update notification with partial result
                    hypothesis?.let {
                        try {
                            val json = JSONObject(it)
                            val partial = json.optString("partial", "")
                            if (partial.isNotEmpty()) {
                                updateNotification("Распознаю: $partial")
                            }
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                }
                
                override fun onError(exception: Exception?) {
                    Log.e(TAG, "Recognition error", exception)
                    stopCommandRecognition()
                    resumeWakeWordDetection()
                }
                
                override fun onTimeout() {
                    Log.d(TAG, "Recognition timeout")
                    ttsManager.speak("Команда не распознана")
                    stopCommandRecognition()
                    resumeWakeWordDetection()
                }
            })
            
            // Set timeout for command recognition (10 seconds)
            serviceScope.launch {
                delay(10000)
                if (isListeningForCommand) {
                    Log.d(TAG, "Command recognition timeout")
                    stopCommandRecognition()
                    ttsManager.speak("Время ожидания команды истекло")
                    resumeWakeWordDetection()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting command recognition", e)
            resumeWakeWordDetection()
        }
    }
    
    /**
     * Stop command recognition
     */
    private fun stopCommandRecognition() {
        isListeningForCommand = false
        voskManager.stopRecognition()
        commandRecognizer = null
    }
    
    /**
     * Resume wake word detection
     */
    private fun resumeWakeWordDetection() {
        serviceScope.launch {
            delay(500) // Small delay before resuming
            if (!isPaused) {
                wakeWordDetector.startListening { wakeWord ->
                    onWakeWordDetected(wakeWord)
                }
                updateNotification("Слушаю...")
            }
        }
    }
    
    /**
     * Process recognized command
     */
    private fun processCommand(resultJson: String) {
        serviceScope.launch {
            try {
                val json = JSONObject(resultJson)
                val text = json.optString("text", "").trim()
                
                if (text.isEmpty()) {
                    ttsManager.speak("Команда не распознана")
                    return@launch
                }
                
                Log.i(TAG, "📝 Command recognized: $text")
                updateNotification("Выполняю: $text")
                
                // Execute command
                val result = commandExecutor.executeCommand(text, confirmed = false)
                
                when (result) {
                    is CommandExecutor.CommandResult.Success -> {
                        ttsManager.speak("Команда выполнена")
                        Log.i(TAG, "✓ Command executed successfully")
                    }
                    
                    is CommandExecutor.CommandResult.RequiresConfirmation -> {
                        // Ask for confirmation
                        ttsManager.speak("${result.details}. Скажите 'подтверждаю' для выполнения")
                        // TODO: Wait for confirmation
                    }
                    
                    is CommandExecutor.CommandResult.RequiresPermission -> {
                        ttsManager.speak("Требуется разрешение. Откройте настройки приложения")
                        Log.w(TAG, "Permission required: ${result.permission}")
                    }
                    
                    is CommandExecutor.CommandResult.Error -> {
                        ttsManager.speak("Ошибка: ${result.message}")
                        Log.e(TAG, "Command error: ${result.message}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing command", e)
                ttsManager.speak("Ошибка обработки команды")
            }
        }
    }
    
    /**
     * Acquire wake lock to keep CPU running
     */
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "FreeHands::AssistantWakeLock"
            ).apply {
                acquire(10 * 60 * 1000L) // 10 minutes
            }
            Log.d(TAG, "Wake lock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wake lock", e)
        }
    }
    
    /**
     * Release wake lock
     */
    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            wakeLock = null
            Log.d(TAG, "Wake lock released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock", e)
        }
    }
    
    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Голосовой ассистент",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомления голосового ассистента"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create foreground notification
     */
    private fun createNotification(status: String = "Запуск..."): Notification {
        // Create pending intents for notification actions
        val stopIntent = Intent(this, AssistantForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val pauseIntent = Intent(this, AssistantForegroundService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this, 1, pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FreeHands Assistant")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // TODO: Create proper icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                if (isPaused) "Возобновить" else "Приостановить",
                pausePendingIntent
            )
            .addAction(
                android.R.drawable.ic_delete,
                "Остановить",
                stopPendingIntent
            )
            .build()
    }
    
    /**
     * Update notification with new status
     */
    private fun updateNotification(status: String) {
        try {
            val notification = createNotification(status)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification", e)
        }
    }
}
