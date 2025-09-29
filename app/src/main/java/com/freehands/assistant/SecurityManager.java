package com.freehands.assistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SecurityManager {
    private static final String TAG = "SecurityManager";
    private static final String SECURITY_PREFS = "security_manager_prefs";
    private static final String KEY_SECURITY_EVENTS = "security_events";
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts";
    private static final String KEY_LAST_AUTH_TIME = "last_auth_time";
    private static final String KEY_DEVICE_FINGERPRINT = "device_fingerprint";
    
    // Security thresholds
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 30 * 60 * 1000; // 30 minutes
    private static final long SESSION_TIMEOUT_MS = 60 * 60 * 1000; // 1 hour
    private static final int MAX_SECURITY_EVENTS = 100;
    
    private final Context context;
    private final SharedPreferences encryptedPrefs;
    private final ConcurrentLinkedQueue<SecurityEvent> recentEvents;
    private final Gson gson;
    
    public SecurityManager(Context context) {
        this.context = context;
        this.gson = new Gson();
        this.recentEvents = new ConcurrentLinkedQueue<>();
        this.encryptedPrefs = initializeEncryptedPreferences();
        
        // Initialize device fingerprint
        initializeDeviceFingerprint();
        
        // Load recent security events
        loadSecurityEvents();
        
        if (BuildConfig.DEBUG) { Log.d(TAG, "SecurityManager initialized"); }

    }
    
    private SharedPreferences initializeEncryptedPreferences() {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            
            return EncryptedSharedPreferences.create(
                SECURITY_PREFS,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize encrypted preferences, using regular", e);
            return context.getSharedPreferences(SECURITY_PREFS, Context.MODE_PRIVATE);
        }
    }
    
    private void initializeDeviceFingerprint() {
        String existingFingerprint = encryptedPrefs.getString(KEY_DEVICE_FINGERPRINT, null);
        if (existingFingerprint == null) {
            String deviceFingerprint = generateDeviceFingerprint();
            encryptedPrefs.edit()
                .putString(KEY_DEVICE_FINGERPRINT, deviceFingerprint)
                .apply();
            
            if (BuildConfig.DEBUG) { Log.d(TAG, "Device fingerprint created"); }

        }
    }
    
    private String generateDeviceFingerprint() {
        try {
            StringBuilder fingerprintData = new StringBuilder();
            
            // Device identifiers
            fingerprintData.append(Build.MANUFACTURER);
            fingerprintData.append(Build.MODEL);
            fingerprintData.append(Build.DEVICE);
            fingerprintData.append(Build.SERIAL);
            fingerprintData.append(Build.HARDWARE);
            
            // Android version info
            fingerprintData.append(Build.VERSION.RELEASE);
            fingerprintData.append(Build.VERSION.SDK_INT);
            
            // Settings-based identifiers
            String androidId = Settings.Secure.getString(
                context.getContentResolver(), 
                Settings.Secure.ANDROID_ID
            );
            fingerprintData.append(androidId);
            
            // Create hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(fingerprintData.toString().getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating device fingerprint", e);
            return "unknown_device_" + System.currentTimeMillis();
        }
    }
    
    public void logSecurityEvent(String eventType, String description) {
        logSecurityEvent(eventType, description, SecurityLevel.INFO);
    }
    
    public void logSecurityEvent(String eventType, String description, SecurityLevel level) {
        SecurityEvent event = new SecurityEvent(
            System.currentTimeMillis(),
            eventType,
            description,
            level,
            getDeviceFingerprint()
        );
        
        // Add to in-memory queue
        recentEvents.offer(event);
        
        // Limit queue size
        while (recentEvents.size() > MAX_SECURITY_EVENTS) {
            recentEvents.poll();
        }
        
        // Persist to storage
        persistSecurityEvents();
        
        // Log based on severity
        switch (level) {
            case CRITICAL:
                Log.e(TAG, "CRITICAL SECURITY EVENT: " + eventType + " - " + description);
                handleCriticalSecurityEvent(event);
                break;
            case WARNING:
                Log.w(TAG, "SECURITY WARNING: " + eventType + " - " + description);
                break;
            case INFO:
                if (BuildConfig.DEBUG) { Log.i(TAG, "Security Info: " + eventType + " - " + description); }

                break;
        }
    }
    
    private void handleCriticalSecurityEvent(SecurityEvent event) {
        // Increment failed attempts for authentication failures
        if (event.getEventType().contains("authentication_failed")) {
            incrementFailedAttempts();
        }
        
        // Additional critical event handling could be added here
        // Such as notifying the user, disabling features temporarily, etc.
    }
    
    public boolean isAuthenticationAllowed() {
        int failedAttempts = encryptedPrefs.getInt(KEY_FAILED_ATTEMPTS, 0);
        
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            long lastAttemptTime = encryptedPrefs.getLong(KEY_LAST_AUTH_TIME, 0);
            long currentTime = System.currentTimeMillis();
            
            if (currentTime - lastAttemptTime < LOCKOUT_DURATION_MS) {
                Log.w(TAG, "Authentication blocked due to too many failed attempts");
                return false;
            } else {
                // Reset failed attempts after lockout period
                resetFailedAttempts();
            }
        }
        
        return true;
    }
    
    public void recordSuccessfulAuthentication() {
        resetFailedAttempts();
        encryptedPrefs.edit()
            .putLong(KEY_LAST_AUTH_TIME, System.currentTimeMillis())
            .apply();
        
        logSecurityEvent("authentication_success", "Voice authentication successful");
    }
    
    public void recordFailedAuthentication(String reason) {
        incrementFailedAttempts();
        encryptedPrefs.edit()
            .putLong(KEY_LAST_AUTH_TIME, System.currentTimeMillis())
            .apply();
        
        logSecurityEvent("authentication_failed", "Voice authentication failed: " + reason, 
                        SecurityLevel.WARNING);
    }
    
    private void incrementFailedAttempts() {
        int currentAttempts = encryptedPrefs.getInt(KEY_FAILED_ATTEMPTS, 0);
        encryptedPrefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, currentAttempts + 1)
            .apply();
    }
    
    private void resetFailedAttempts() {
        encryptedPrefs.edit()
            .remove(KEY_FAILED_ATTEMPTS)
            .apply();
    }
    
    public boolean isSessionValid() {
        long lastAuthTime = encryptedPrefs.getLong(KEY_LAST_AUTH_TIME, 0);
        return (System.currentTimeMillis() - lastAuthTime) < SESSION_TIMEOUT_MS;
    }
    
    public void invalidateSession() {
        encryptedPrefs.edit()
            .remove(KEY_LAST_AUTH_TIME)
            .apply();
        
        logSecurityEvent("session_invalidated", "User session invalidated");
    }
    
    public String getDeviceFingerprint() {
        return encryptedPrefs.getString(KEY_DEVICE_FINGERPRINT, "unknown");
    }
    
    public boolean validateDeviceIntegrity() {
        try {
            // Check if device is rooted (basic check)
            boolean isRooted = isDeviceRooted();
            if (isRooted) {
                logSecurityEvent("device_rooted", "Device appears to be rooted", SecurityLevel.CRITICAL);
                return false;
            }
            
            // Check if debugging is enabled
            boolean debugEnabled = Settings.Global.getInt(
                context.getContentResolver(),
                Settings.Global.ADB_ENABLED, 0
            ) == 1;
            
            if (debugEnabled) {
                logSecurityEvent("debug_enabled", "ADB debugging is enabled", SecurityLevel.WARNING);
            }
            
            // Device integrity is acceptable
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error validating device integrity", e);
            logSecurityEvent("integrity_check_failed", "Device integrity validation failed", 
                            SecurityLevel.WARNING);
            return true; // Allow operation but log the issue
        }
    }
    
    private boolean isDeviceRooted() {
        // Basic root detection - check for common root indicators
        try {
            // Check for su binary
            Process process = Runtime.getRuntime().exec("which su");
            process.waitFor();
            if (process.exitValue() == 0) {
                return true;
            }
            
            // Check for common root paths
            String[] rootPaths = {
                "/system/app/Superuser.apk",
                "/system/xbin/su",
                "/system/bin/su",
                "/sbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su"
            };
            
            for (String path : rootPaths) {
                if (new java.io.File(path).exists()) {
                    return true;
                }
            }
            
        } catch (Exception e) {
            // If we can't check, assume not rooted
            if (BuildConfig.DEBUG) { Log.v(TAG, "Could not check for root", e); }

        }
        
        return false;
    }
    
    public List<SecurityEvent> getRecentSecurityEvents() {
        return new ArrayList<>(recentEvents);
    }
    
    public List<SecurityEvent> getSecurityEventsByType(String eventType) {
        List<SecurityEvent> filteredEvents = new ArrayList<>();
        for (SecurityEvent event : recentEvents) {
            if (event.getEventType().equals(eventType)) {
                filteredEvents.add(event);
            }
        }
        return filteredEvents;
    }
    
    private void loadSecurityEvents() {
        try {
            String eventsJson = encryptedPrefs.getString(KEY_SECURITY_EVENTS, null);
            if (eventsJson != null) {
                List<SecurityEvent> events = gson.fromJson(eventsJson, 
                    new TypeToken<List<SecurityEvent>>(){}.getType());
                
                if (events != null) {
                    recentEvents.addAll(events);
                    
                    // Limit loaded events
                    while (recentEvents.size() > MAX_SECURITY_EVENTS) {
                        recentEvents.poll();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading security events", e);
        }
    }
    
    private void persistSecurityEvents() {
        try {
            List<SecurityEvent> eventsList = new ArrayList<>(recentEvents);
            String eventsJson = gson.toJson(eventsList);
            
            encryptedPrefs.edit()
                .putString(KEY_SECURITY_EVENTS, eventsJson)
                .apply();
                
        } catch (Exception e) {
            Log.e(TAG, "Error persisting security events", e);
        }
    }
    
    public SecurityReport generateSecurityReport() {
        int totalEvents = recentEvents.size();
        int criticalEvents = 0;
        int warningEvents = 0;
        int infoEvents = 0;
        
        for (SecurityEvent event : recentEvents) {
            switch (event.getLevel()) {
                case CRITICAL:
                    criticalEvents++;
                    break;
                case WARNING:
                    warningEvents++;
                    break;
                case INFO:
                    infoEvents++;
                    break;
            }
        }
        
        int failedAttempts = encryptedPrefs.getInt(KEY_FAILED_ATTEMPTS, 0);
        boolean sessionValid = isSessionValid();
        boolean deviceIntegrityOk = validateDeviceIntegrity();
        
        return new SecurityReport(
            totalEvents,
            criticalEvents,
            warningEvents,
            infoEvents,
            failedAttempts,
            sessionValid,
            deviceIntegrityOk,
            getDeviceFingerprint()
        );
    }
    
    // Data classes
    public enum SecurityLevel {
        INFO, WARNING, CRITICAL
    }
    
    public static class SecurityEvent {
        private final long timestamp;
        private final String eventType;
        private final String description;
        private final SecurityLevel level;
        private final String deviceFingerprint;
        
        public SecurityEvent(long timestamp, String eventType, String description, 
                           SecurityLevel level, String deviceFingerprint) {
            this.timestamp = timestamp;
            this.eventType = eventType;
            this.description = description;
            this.level = level;
            this.deviceFingerprint = deviceFingerprint;
        }
        
        public long getTimestamp() { return timestamp; }
        public String getEventType() { return eventType; }
        public String getDescription() { return description; }
        public SecurityLevel getLevel() { return level; }
        public String getDeviceFingerprint() { return deviceFingerprint; }
        
        public String getFormattedTimestamp() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }
    
    public static class SecurityReport {
        private final int totalEvents;
        private final int criticalEvents;
        private final int warningEvents;
        private final int infoEvents;
        private final int failedAttempts;
        private final boolean sessionValid;
        private final boolean deviceIntegrityOk;
        private final String deviceFingerprint;
        
        public SecurityReport(int totalEvents, int criticalEvents, int warningEvents, 
                            int infoEvents, int failedAttempts, boolean sessionValid, 
                            boolean deviceIntegrityOk, String deviceFingerprint) {
            this.totalEvents = totalEvents;
            this.criticalEvents = criticalEvents;
            this.warningEvents = warningEvents;
            this.infoEvents = infoEvents;
            this.failedAttempts = failedAttempts;
            this.sessionValid = sessionValid;
            this.deviceIntegrityOk = deviceIntegrityOk;
            this.deviceFingerprint = deviceFingerprint;
        }
        
        // Getters
        public int getTotalEvents() { return totalEvents; }
        public int getCriticalEvents() { return criticalEvents; }
        public int getWarningEvents() { return warningEvents; }
        public int getInfoEvents() { return infoEvents; }
        public int getFailedAttempts() { return failedAttempts; }
        public boolean isSessionValid() { return sessionValid; }
        public boolean isDeviceIntegrityOk() { return deviceIntegrityOk; }
        public String getDeviceFingerprint() { return deviceFingerprint; }
    }
}
