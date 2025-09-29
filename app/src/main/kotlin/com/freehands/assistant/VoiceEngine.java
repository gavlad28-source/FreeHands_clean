package com.freehands.assistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import java.util.Locale;
import java.util.Set;

public class VoiceEngine {
    private static final String TAG = "VoiceEngine";
    private static VoiceEngine instance;
    private TextToSpeech textToSpeech;
    private Context context;
    private boolean isInitialized = false;
    
    // Voice characteristics
    private String currentVoiceType = "Female";
    private int currentQuality = 75;
    
    private VoiceEngine(Context context) {
        this.context = context.getApplicationContext();
        initializeTextToSpeech();
        loadVoiceSettings();
    }
    
    public static synchronized VoiceEngine getInstance(Context context) {
        if (instance == null) {
            instance = new VoiceEngine(context);
        }
        return instance;
    }
    
    public static synchronized VoiceEngine getInstance() {
        if (instance == null) {
            throw new IllegalStateException("VoiceEngine must be initialized with context first");
        }
        return instance;
    }
    
    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(Locale.getDefault());
                    if (result == TextToSpeech.LANG_MISSING_DATA || 
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "Language not supported, using default");
                        textToSpeech.setLanguage(Locale.US);
                    }
                    isInitialized = true;
                    applyVoiceSettings();
                    if (BuildConfig.DEBUG) { Log.i(TAG, "TextToSpeech initialized successfully"); }

                } else {
                    Log.e(TAG, "TextToSpeech initialization failed");
                }
            }
        });
    }
    
    private void loadVoiceSettings() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        currentVoiceType = preferences.getString("voice_type", "Female");
        currentQuality = preferences.getInt("voice_quality", 75);
    }
    
    public void setVoiceType(String voiceType) {
        this.currentVoiceType = voiceType;
        applyVoiceSettings();
    }
    
    public void setVoiceQuality(int quality) {
        this.currentQuality = quality;
        applyVoiceSettings();
    }
    
    private void applyVoiceSettings() {
        if (!isInitialized || textToSpeech == null) {
            return;
        }
        
        try {
            // Set voice based on type preference
            Voice selectedVoice = findBestVoiceForType(currentVoiceType);
            if (selectedVoice != null) {
                textToSpeech.setVoice(selectedVoice);
                if (BuildConfig.DEBUG) { Log.i(TAG, "Voice set to: " + selectedVoice.getName()); }

            }
            
            // Set speech rate based on quality (higher quality = more natural pace)
            float speechRate = calculateSpeechRate(currentQuality);
            textToSpeech.setSpeechRate(speechRate);
            
            // Set pitch based on voice type
            float pitch = calculatePitch(currentVoiceType);
            textToSpeech.setPitch(pitch);
            
            if (BuildConfig.DEBUG) { Log.i(TAG, "Voice settings applied - Type: " + currentVoiceType + 
                  ", Quality: " + currentQuality + "%, Rate: " + speechRate + ", Pitch: " + pitch); }

                  
        } catch (Exception e) {
            Log.e(TAG, "Error applying voice settings", e);
        }
    }
    
    private Voice findBestVoiceForType(String voiceType) {
        if (textToSpeech == null) return null;
        
        Set<Voice> voices = textToSpeech.getVoices();
        if (voices == null) return null;
        
        Voice bestVoice = null;
        int bestScore = 0;
        
        for (Voice voice : voices) {
            if (!voice.getLocale().getLanguage().equals(Locale.getDefault().getLanguage())) {
                continue;
            }
            
            int score = 0;
            String voiceName = voice.getName().toLowerCase();
            
            // Score based on voice type preference
            switch (voiceType.toLowerCase()) {
                case "male":
                    if (voiceName.contains("male") && !voiceName.contains("female")) {
                        score += 100;
                    }
                    break;
                case "female":
                    if (voiceName.contains("female")) {
                        score += 100;
                    }
                    break;
                case "child":
                    if (voiceName.contains("child") || voiceName.contains("young")) {
                        score += 100;
                    }
                    break;
            }
            
            // Prefer higher quality voices
            if (voice.getQuality() == Voice.QUALITY_VERY_HIGH) {
                score += 30;
            } else if (voice.getQuality() == Voice.QUALITY_HIGH) {
                score += 20;
            } else if (voice.getQuality() == Voice.QUALITY_NORMAL) {
                score += 10;
            }
            
            // Prefer network voices for higher quality
            if (!voice.isNetworkConnectionRequired()) {
                score += 5; // Slight preference for local voices for privacy
            }
            
            if (score > bestScore) {
                bestScore = score;
                bestVoice = voice;
            }
        }
        
        return bestVoice;
    }
    
    private float calculateSpeechRate(int quality) {
        // Higher quality = more natural, slower speech rate
        // Range: 0.6 (slow, high quality) to 1.2 (fast, low quality)
        float baseRate = 1.0f;
        float adjustment = (75 - quality) * 0.008f; // 0.008 per percentage point from 75%
        return Math.max(0.6f, Math.min(1.2f, baseRate + adjustment));
    }
    
    private float calculatePitch(String voiceType) {
        switch (voiceType.toLowerCase()) {
            case "male":
                return 0.8f; // Lower pitch
            case "female":
                return 1.1f; // Higher pitch
            case "child":
                return 1.3f; // Highest pitch
            default:
                return 1.0f; // Normal pitch
        }
    }
    
    public void speak(String text) {
        if (!isInitialized || textToSpeech == null) {
            Log.w(TAG, "TextToSpeech not initialized, cannot speak");
            return;
        }
        
        if (BuildConfig.DEBUG) { Log.d(TAG, "Speaking: " + text); }

        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }
    
    public void speakWithQueue(String text) {
        if (!isInitialized || textToSpeech == null) {
            Log.w(TAG, "TextToSpeech not initialized, cannot speak");
            return;
        }
        
        if (BuildConfig.DEBUG) { Log.d(TAG, "Speaking (queued): " + text); }

        textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, null);
    }
    
    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }
    
    public boolean isSpeaking() {
        return textToSpeech != null && textToSpeech.isSpeaking();
    }
    
    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        isInitialized = false;
    }
    
    public String getCurrentVoiceType() {
        return currentVoiceType;
    }
    
    public int getCurrentQuality() {
        return currentQuality;
    }
}