package com.freehands.assistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.RecognizerIntent;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class VoiceListeningService extends Service {
    private static final String TAG = "VoiceListeningService";
    private static final String CHANNEL_ID = "VOICE_LISTENING_CHANNEL";
    private static final int NOTIFICATION_ID = 1001;
    private static final String WAKE_WORD = "hey freehands";
    
    private static boolean isServiceRunning = false;
    
    // Audio recording components
    private AudioRecord audioRecord;
    private WakeWordDetector wakeWordDetector;
    private SpeechRecognizer speechRecognizer;
    private VoiceBiometricAuthenticator authenticator;
    private CommandProcessor commandProcessor;
    private SecurityManager securityManager;
    
    // Threading and lifecycle management
    private ExecutorService audioProcessingExecutor;
    private AtomicBoolean isListening = new AtomicBoolean(false);
    private AtomicBoolean isProcessingCommand = new AtomicBoolean(false);
    private PowerManager.WakeLock wakeLock;
    
    // Audio configuration
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferSize;

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) { Log.d(TAG, "VoiceListeningService created"); }

        
        initializeComponents();
        createNotificationChannel();
        acquireWakeLock();
        
        isServiceRunning = true;
    }
    
    private void initializeComponents() {
        // Initialize audio processing
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        audioProcessingExecutor = Executors.newSingleThreadExecutor();
        
        // Initialize voice components
        wakeWordDetector = new WakeWordDetector(this, WAKE_WORD);
        authenticator = new VoiceBiometricAuthenticator(this);
        commandProcessor = new CommandProcessor(this);
        securityManager = new SecurityManager(this);
        
        // Initialize speech recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new VoiceRecognitionListener());
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Voice Listening Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Always-listening voice assistant service");
            channel.setShowBadge(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "FreeHands::VoiceListeningWakeLock"
        );
        wakeLock.acquire(10*60*1000L /*10 minutes*/);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (BuildConfig.DEBUG) { Log.d(TAG, "Starting voice listening service"); }

        
        Notification notification = createForegroundNotification();
        startForeground(NOTIFICATION_ID, notification);
        
        startContinuousListening();
        
        return START_STICKY; // Restart if killed by system
    }
    
    private Notification createForegroundNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FreeHands Active")
            .setContentText("Listening for 'Hey FreeHands'...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build();
    }
    
    private void startContinuousListening() {
        if (isListening.get()) {
            Log.w(TAG, "Already listening");
            return;
        }
        
        audioProcessingExecutor.execute(() -> {
            try {
                // Initialize audio recording
                if (audioRecord != null) {
                    audioRecord.release();
                }
                
                audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize * 2
                );
                
                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioRecord initialization failed");
                    return;
                }
                
                audioRecord.startRecording();
                isListening.set(true);
                
                if (BuildConfig.DEBUG) { Log.d(TAG, "Started continuous audio recording"); }

                
                // Continuous audio processing loop
                byte[] audioBuffer = new byte[bufferSize];
                
                while (isListening.get() && !Thread.currentThread().isInterrupted()) {
                    int bytesRead = audioRecord.read(audioBuffer, 0, bufferSize);
                    
                    if (bytesRead > 0) {
                        // Process audio for wake word detection
                        if (!isProcessingCommand.get()) {
                            processAudioForWakeWord(audioBuffer, bytesRead);
                        }
                    }
                    
                    // Small delay to prevent excessive CPU usage
                    // REMOVED Thread.sleep for responsiveness
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in continuous listening", e);
            }
        });
    }
    
    private void processAudioForWakeWord(byte[] audioData, int length) {
        wakeWordDetector.processAudio(audioData, length, new WakeWordDetector.WakeWordCallback() {
            @Override
            public void onWakeWordDetected(float confidence) {
                if (BuildConfig.DEBUG) { Log.d(TAG, "Wake word detected with confidence: " + confidence); }

                onWakeWordTriggered();
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Wake word detection error: " + error);
            }
        });
    }
    
    private void onWakeWordTriggered() {
        if (isProcessingCommand.get()) {
            if (BuildConfig.DEBUG) { Log.d(TAG, "Already processing a command, ignoring wake word"); }

            return;
        }
        
        if (BuildConfig.DEBUG) { Log.d(TAG, "Wake word triggered - starting command processing"); }

        isProcessingCommand.set(true);
        
        // Update notification to show active state
        updateNotification("Listening for command...");
        
        // Start voice authentication and command recognition
        startVoiceAuthentication();
    }
    
    private void startVoiceAuthentication() {
        authenticator.authenticateVoice(new VoiceBiometricAuthenticator.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded() {
                if (BuildConfig.DEBUG) { Log.d(TAG, "Voice authentication successful"); }

                startCommandRecognition();
            }
            
            @Override
            public void onAuthenticationFailed(String reason) {
                Log.w(TAG, "Voice authentication failed: " + reason);
                updateNotification("Authentication failed - unauthorized voice detected");
                
                // Log security event
                securityManager.logSecurityEvent("Voice authentication failed: " + reason);
                
                // Return to listening state after delay
                audioProcessingExecutor.execute(() -> {
                    try {
                        // REMOVED Thread.sleep for responsiveness
                        resetToListeningState();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Voice authentication error: " + error);
                resetToListeningState();
            }
        });
    }
    
    private void startCommandRecognition() {
        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        
        speechRecognizer.startListening(recognizerIntent);
    }
    
    private void updateNotification(String message) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FreeHands Active")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
            
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, notification);
    }
    
    private void resetToListeningState() {
        isProcessingCommand.set(false);
        updateNotification("Listening for 'Hey FreeHands'...");
    }
    
    private class VoiceRecognitionListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params) {
            if (BuildConfig.DEBUG) { Log.d(TAG, "Ready for speech"); }

            updateNotification("Speak your command now");
        }
        
        @Override
        public void onBeginningOfSpeech() {
            if (BuildConfig.DEBUG) { Log.d(TAG, "Speech started"); }

        }
        
        @Override
        public void onRmsChanged(float rmsdB) {
            // Voice level feedback could be added here
        }
        
        @Override
        public void onBufferReceived(byte[] buffer) {
            // Real-time audio data processing if needed
        }
        
        @Override
        public void onEndOfSpeech() {
            if (BuildConfig.DEBUG) { Log.d(TAG, "Speech ended"); }

            updateNotification("Processing command...");
        }
        
        @Override
        public void onError(int error) {
            String errorMessage = getErrorMessage(error);
            Log.e(TAG, "Speech recognition error: " + errorMessage);
            updateNotification("Command not recognized");
            
            // Return to listening state after delay
            audioProcessingExecutor.execute(() -> {
                try {
                    // REMOVED Thread.sleep for responsiveness
                    resetToListeningState();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String command = matches.get(0);
                if (BuildConfig.DEBUG) { Log.d(TAG, "Command recognized: " + command); }

                processVoiceCommand(command);
            } else {
                Log.w(TAG, "No speech results");
                resetToListeningState();
            }
        }
        
        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                if (BuildConfig.DEBUG) { Log.d(TAG, "Partial result: " + matches.get(0)); }

            }
        }
        
        @Override
        public void onEvent(int eventType, Bundle params) {
            // Handle other speech events if needed
        }
        
        private String getErrorMessage(int error) {
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO: return "Audio recording error";
                case SpeechRecognizer.ERROR_CLIENT: return "Client side error";
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Insufficient permissions";
                case SpeechRecognizer.ERROR_NETWORK: return "Network error";
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Network timeout";
                case SpeechRecognizer.ERROR_NO_MATCH: return "No speech match";
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "Recognition service busy";
                case SpeechRecognizer.ERROR_SERVER: return "Server error";
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "No speech input";
                default: return "Unknown error";
            }
        }
    }
    
    private void processVoiceCommand(String command) {
        updateNotification("Executing: " + command);
        
        commandProcessor.processCommand(command, new CommandProcessor.CommandCallback() {
            @Override
            public void onCommandExecuted(String result) {
                if (BuildConfig.DEBUG) { Log.d(TAG, "Command executed successfully: " + result); }

                updateNotification("Command completed: " + result);
                
                // Return to listening state after delay
                audioProcessingExecutor.execute(() -> {
                    try {
                        // REMOVED Thread.sleep for responsiveness
                        resetToListeningState();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            
            @Override
            public void onCommandFailed(String error) {
                Log.e(TAG, "Command failed: " + error);
                updateNotification("Command failed: " + error);
                
                // Return to listening state after delay
                audioProcessingExecutor.execute(() -> {
                    try {
                        // REMOVED Thread.sleep for responsiveness
                        resetToListeningState();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        });
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; // This is a started service, not bound
    }
    
    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG) { Log.d(TAG, "VoiceListeningService destroyed"); }

        
        isListening.set(false);
        isServiceRunning = false;
        
        // Clean up resources
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping audio recording", e);
            }
        }
        
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        
        if (audioProcessingExecutor != null && !audioProcessingExecutor.isShutdown()) {
            audioProcessingExecutor.shutdownNow();
        }
        
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        
        super.onDestroy();
    }
    
    public static boolean isRunning() {
        return isServiceRunning;
    }
}
