package com.freehands.assistant;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SystemController {
    private static final String TAG = "SystemController";
    
    private final Context context;
    private final AudioManager audioManager;
    private final WifiManager wifiManager;
    private final CameraManager cameraManager;
    
    public interface NotificationCallback {
        void onNotificationsRead(String notifications);
        void onNoNotifications();
        void onError(String error);
    }
    
    public SystemController(Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }
    
    public void callContact(String contactName, CommandProcessor.CommandCallback callback) {
        String phoneNumber = getContactPhoneNumber(contactName);
        if (phoneNumber.isEmpty()) {
            callback.onCommandFailed("Contact not found: " + contactName);
            return;
        }
        
        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));
            callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(callIntent);
            
            callback.onCommandExecuted("Calling " + contactName);
        } catch (Exception e) {
            callback.onCommandFailed("Failed to call " + contactName + ": " + e.getMessage());
        }
    }
    
    public String getContactPhoneNumber(String contactName) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
                new String[]{"%" + contactName + "%"},
                null
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting contact phone number", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "";
    }
    
    public void takePhoto(boolean frontCamera, CommandProcessor.CommandCallback callback) {
        try {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (frontCamera) {
                cameraIntent.putExtra("android.intent.extras.CAMERA_FACING", 1);
            }
            cameraIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(cameraIntent);
            
            callback.onCommandExecuted("Camera opened");
        } catch (Exception e) {
            Log.e(TAG, "Error opening camera", e);
            callback.onCommandFailed("Failed to open camera: " + e.getMessage());
        }
    }
    
    public void openApp(String appName, CommandProcessor.CommandCallback callback) {
        try {
            // Try to find and launch the app
            PackageManager packageManager = context.getPackageManager();
            List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            
            for (ApplicationInfo app : apps) {
                String appLabel = packageManager.getApplicationLabel(app).toString().toLowerCase();
                if (appLabel.contains(appName.toLowerCase())) {
                    Intent launchIntent = packageManager.getLaunchIntentForPackage(app.packageName);
                    if (launchIntent != null) {
                        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(launchIntent);
                        callback.onCommandExecuted("Opened " + appLabel);
                        return;
                    }
                }
            }
            
            // If app not found, try common app mappings
            String packageName = getCommonAppPackage(appName);
            if (!packageName.isEmpty()) {
                Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
                if (launchIntent != null) {
                    launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(launchIntent);
                    callback.onCommandExecuted("Opened " + appName);
                    return;
                }
            }
            
            callback.onCommandFailed("App not found: " + appName);
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening app", e);
            callback.onCommandFailed("Failed to open app: " + e.getMessage());
        }
    }
    
    private String getCommonAppPackage(String appName) {
        String lowerAppName = appName.toLowerCase();
        
        switch (lowerAppName) {
            case "chrome":
            case "browser":
                return "com.android.chrome";
            case "gmail":
            case "email":
                return "com.google.android.gm";
            case "maps":
                return "com.google.android.apps.maps";
            case "youtube":
                return "com.google.android.youtube";
            case "photos":
                return "com.google.android.apps.photos";
            case "calculator":
                return "com.android.calculator2";
            case "calendar":
                return "com.android.calendar";
            case "settings":
                return "com.android.settings";
            case "contacts":
                return "com.android.contacts";
            case "messages":
                return "com.android.mms";
            case "phone":
                return "com.android.dialer";
            default:
                return "";
        }
    }
    
    public void closeCurrentApp(CommandProcessor.CommandCallback callback) {
        // This requires accessibility service to work properly
        VoiceAccessibilityService accessibilityService = VoiceAccessibilityService.getInstance();
        if (accessibilityService != null) {
            accessibilityService.performGlobalAction(VoiceAccessibilityService.GLOBAL_ACTION_BACK);
            callback.onCommandExecuted("App closed");
        } else {
            callback.onCommandFailed("Accessibility service not available");
        }
    }
    
    public void adjustVolume(boolean increase, CommandProcessor.CommandCallback callback) {
        try {
            int direction = increase ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
            audioManager.adjustVolume(AudioManager.STREAM_MUSIC, direction, 0);
            
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int volumePercent = (currentVolume * 100) / maxVolume;
            
            callback.onCommandExecuted("Volume " + (increase ? "increased" : "decreased") + " to " + volumePercent + "%");
        } catch (Exception e) {
            Log.e(TAG, "Error adjusting volume", e);
            callback.onCommandFailed("Failed to adjust volume: " + e.getMessage());
        }
    }
    
    public void muteVolume(CommandProcessor.CommandCallback callback) {
        try {
            audioManager.adjustVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
            callback.onCommandExecuted("Volume muted");
        } catch (Exception e) {
            Log.e(TAG, "Error muting volume", e);
            callback.onCommandFailed("Failed to mute volume: " + e.getMessage());
        }
    }
    
    public void adjustBrightness(boolean increase, CommandProcessor.CommandCallback callback) {
        try {
            int currentBrightness = Settings.System.getInt(
                context.getContentResolver(), 
                Settings.System.SCREEN_BRIGHTNESS
            );
            
            int newBrightness = increase ? 
                Math.min(255, currentBrightness + 25) : 
                Math.max(0, currentBrightness - 25);
            
            Settings.System.putInt(
                context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS,
                newBrightness
            );
            
            int brightnessPercent = (newBrightness * 100) / 255;
            callback.onCommandExecuted("Brightness " + (increase ? "increased" : "decreased") + " to " + brightnessPercent + "%");
            
        } catch (Settings.SettingNotFoundException e) {
            callback.onCommandFailed("Could not read current brightness");
        } catch (Exception e) {
            Log.e(TAG, "Error adjusting brightness", e);
            callback.onCommandFailed("Failed to adjust brightness. May require permission.");
        }
    }
    
    public void setWifi(boolean enabled, CommandProcessor.CommandCallback callback) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // On Android 10+, we can't programmatically control WiFi
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                callback.onCommandExecuted("Opening WiFi settings");
            } else {
                boolean success = wifiManager.setWifiEnabled(enabled);
                if (success) {
                    callback.onCommandExecuted("WiFi " + (enabled ? "enabled" : "disabled"));
                } else {
                    callback.onCommandFailed("Failed to " + (enabled ? "enable" : "disable") + " WiFi");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error controlling WiFi", e);
            callback.onCommandFailed("Failed to control WiFi: " + e.getMessage());
        }
    }
    
    public void toggleWifi(CommandProcessor.CommandCallback callback) {
        boolean currentState = wifiManager.isWifiEnabled();
        setWifi(!currentState, callback);
    }
    
    public void setBluetooth(boolean enabled, CommandProcessor.CommandCallback callback) {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                callback.onCommandFailed("Bluetooth not supported on this device");
                return;
            }
            
            if (enabled) {
                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    enableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(enableIntent);
                    callback.onCommandExecuted("Requesting Bluetooth enable");
                } else {
                    callback.onCommandExecuted("Bluetooth already enabled");
                }
            } else {
                if (bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.disable();
                    callback.onCommandExecuted("Bluetooth disabled");
                } else {
                    callback.onCommandExecuted("Bluetooth already disabled");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error controlling Bluetooth", e);
            callback.onCommandFailed("Failed to control Bluetooth: " + e.getMessage());
        }
    }
    
    public void toggleBluetooth(CommandProcessor.CommandCallback callback) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            setBluetooth(!bluetoothAdapter.isEnabled(), callback);
        } else {
            callback.onCommandFailed("Bluetooth not supported");
        }
    }
    
    public void toggleAirplaneMode(CommandProcessor.CommandCallback callback) {
        try {
            // Open airplane mode settings (can't toggle programmatically on modern Android)
            Intent intent = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callback.onCommandExecuted("Opening airplane mode settings");
        } catch (Exception e) {
            Log.e(TAG, "Error opening airplane mode settings", e);
            callback.onCommandFailed("Failed to open airplane mode settings: " + e.getMessage());
        }
    }
    
    public void navigateToDestination(String destination, CommandProcessor.CommandCallback callback) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String uri = "geo:0,0?q=" + Uri.encode(destination);
            intent.setData(Uri.parse(uri));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                callback.onCommandExecuted("Navigation started to " + destination);
            } else {
                callback.onCommandFailed("No navigation app available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting navigation", e);
            callback.onCommandFailed("Failed to start navigation: " + e.getMessage());
        }
    }
    
    public String getCurrentTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return timeFormat.format(new Date());
    }
    
    public String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
        return dateFormat.format(new Date());
    }
    
    public int getBatteryLevel() {
        try {
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, intentFilter);
            
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                
                return (level * 100) / scale;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting battery level", e);
        }
        return 0;
    }
    
    public void lockDevice(CommandProcessor.CommandCallback callback) {
        VoiceAccessibilityService accessibilityService = VoiceAccessibilityService.getInstance();
        if (accessibilityService != null) {
            accessibilityService.lockScreen();
            callback.onCommandExecuted("Device locked");
        } else {
            callback.onCommandFailed("Cannot lock device - accessibility service not available");
        }
    }
    
    public void goHome(CommandProcessor.CommandCallback callback) {
        VoiceAccessibilityService accessibilityService = VoiceAccessibilityService.getInstance();
        if (accessibilityService != null) {
            accessibilityService.performGlobalAction(VoiceAccessibilityService.GLOBAL_ACTION_HOME);
            callback.onCommandExecuted("Went to home screen");
        } else {
            // Fallback: launch home intent
            try {
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(homeIntent);
                callback.onCommandExecuted("Went to home screen");
            } catch (Exception e) {
                callback.onCommandFailed("Failed to go home: " + e.getMessage());
            }
        }
    }
    
    public void goBack(CommandProcessor.CommandCallback callback) {
        VoiceAccessibilityService accessibilityService = VoiceAccessibilityService.getInstance();
        if (accessibilityService != null) {
            accessibilityService.performGlobalAction(VoiceAccessibilityService.GLOBAL_ACTION_BACK);
            callback.onCommandExecuted("Went back");
        } else {
            callback.onCommandFailed("Cannot go back - accessibility service not available");
        }
    }
    
    public void showRecentApps(CommandProcessor.CommandCallback callback) {
        VoiceAccessibilityService accessibilityService = VoiceAccessibilityService.getInstance();
        if (accessibilityService != null) {
            accessibilityService.performGlobalAction(VoiceAccessibilityService.GLOBAL_ACTION_RECENTS);
            callback.onCommandExecuted("Showing recent apps");
        } else {
            callback.onCommandFailed("Cannot show recent apps - accessibility service not available");
        }
    }
    
    public void showNotifications(CommandProcessor.CommandCallback callback) {
        VoiceAccessibilityService accessibilityService = VoiceAccessibilityService.getInstance();
        if (accessibilityService != null) {
            accessibilityService.performGlobalAction(VoiceAccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
            callback.onCommandExecuted("Showing notifications");
        } else {
            callback.onCommandFailed("Cannot show notifications - accessibility service not available");
        }
    }
    
    public void readNotifications(NotificationCallback callback) {
        VoiceAccessibilityService accessibilityService = VoiceAccessibilityService.getInstance();
        if (accessibilityService != null) {
            String notifications = accessibilityService.getActiveNotifications();
            if (notifications.isEmpty()) {
                callback.onNoNotifications();
            } else {
                callback.onNotificationsRead(notifications);
            }
        } else {
            callback.onError("Cannot read notifications - accessibility service not available");
        }
    }
}
