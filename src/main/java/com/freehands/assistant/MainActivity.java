package com.freehands.assistant;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int ACCESSIBILITY_REQUEST_CODE = 1002;
    private static final int OVERLAY_REQUEST_CODE = 1003;
    
    private TextView statusText;
    private Button setupButton;
    private Button settingsButton;
    private VoiceBiometricAuthenticator authenticator;
    private VoiceEngine voiceEngine;
    
    // Required permissions for the app to function
    private final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.MODIFY_AUDIO_SETTINGS,
        Manifest.permission.SYSTEM_ALERT_WINDOW
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeViews();
        initializeAuthenticator();
        initializeVoiceEngine();
        checkPermissionsAndSetup();
    }
    
    private void initializeViews() {
        statusText = findViewById(R.id.status_text);
        setupButton = findViewById(R.id.setup_button);
        settingsButton = findViewById(R.id.settings_button);
        
        setupButton.setOnClickListener(v -> checkPermissionsAndSetup());
        settingsButton.setOnClickListener(v -> openSettings());
    }
    
    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
    
    private void initializeAuthenticator() {
        authenticator = new VoiceBiometricAuthenticator(this);
    }
    
    private void initializeVoiceEngine() {
        voiceEngine = VoiceEngine.getInstance(this);
    }
    
    private void checkPermissionsAndSetup() {
        List<String> missingPermissions = new ArrayList<>();
        
        // Check regular permissions
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        
        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                missingPermissions.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
            return;
        }
        
        // Check accessibility service
        if (!isAccessibilityServiceEnabled()) {
            showAccessibilityDialog();
            return;
        }
        
        // Check system alert window permission
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivityForResult(intent, OVERLAY_REQUEST_CODE);
            return;
        }
        
        // All permissions granted, start the service
        startVoiceService();
    }
    
    private boolean isAccessibilityServiceEnabled() {
        String enabledServices = Settings.Secure.getString(
            getContentResolver(), 
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        
        if (enabledServices == null) return false;
        
        return enabledServices.contains(getPackageName() + "/" + VoiceAccessibilityService.class.getName());
    }
    
    private void showAccessibilityDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Accessibility Service Required")
            .setMessage("FreeHands requires accessibility service to control your phone with voice commands. Please enable it in the next screen.")
            .setPositiveButton("Enable", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivityForResult(intent, ACCESSIBILITY_REQUEST_CODE);
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                statusText.setText("Accessibility service is required for voice control");
            })
            .show();
    }
    
    private void startVoiceService() {
        // Initialize voice biometric authentication first
        authenticator.initializeVoiceProfile(new VoiceBiometricAuthenticator.InitializationCallback() {
            @Override
            public void onInitialized() {
                // Start the voice listening service
                Intent serviceIntent = new Intent(MainActivity.this, VoiceListeningService.class);
                startForegroundService(serviceIntent);
                
                statusText.setText("✓ FreeHands is active and listening");
                setupButton.setText("Service Running");
                setupButton.setEnabled(false);
                
                Toast.makeText(MainActivity.this, "Say 'Hey FreeHands' to activate", Toast.LENGTH_LONG).show();
                
                // Minimize the app to background
                moveTaskToBack(true);
            }
            
            @Override
            public void onError(String error) {
                statusText.setText("Error initializing voice authentication: " + error);
                Toast.makeText(MainActivity.this, "Voice setup failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                checkPermissionsAndSetup();
            } else {
                statusText.setText("All permissions are required for FreeHands to work properly");
                Toast.makeText(this, "Please grant all permissions to use voice control", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == ACCESSIBILITY_REQUEST_CODE || requestCode == OVERLAY_REQUEST_CODE) {
            // Recheck permissions after returning from settings
            checkPermissionsAndSetup();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }
    
    private void updateStatus() {
        if (VoiceListeningService.isRunning()) {
            statusText.setText("✓ FreeHands is active and listening");
            setupButton.setText("Service Running");
            setupButton.setEnabled(false);
        } else {
            statusText.setText("FreeHands is not active. Tap setup to begin.");
            setupButton.setText("Setup & Start");
            setupButton.setEnabled(true);
        }
    }
}
