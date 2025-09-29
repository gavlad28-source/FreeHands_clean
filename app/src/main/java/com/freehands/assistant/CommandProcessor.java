package com.freehands.assistant;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.telecom.TelecomManager;
import android.telephony.SmsManager;
import android.util.Log;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandProcessor {
    private static final String TAG = "CommandProcessor";
    
    private final Context context;
    private final SystemController systemController;
    private final VoiceEngine voiceEngine;
    private final Map<String, CommandHandler> commandHandlers;
    
    public interface CommandCallback {
        void onCommandExecuted(String result);
        void onCommandFailed(String error);
    }
    
    private interface CommandHandler {
        void execute(String command, String[] params, CommandCallback callback);
    }
    
    public CommandProcessor(Context context) {
        this.context = context;
        this.systemController = new SystemController(context);
        this.voiceEngine = VoiceEngine.getInstance(context);
        this.commandHandlers = new HashMap<>();
        
        initializeCommandHandlers();
    }
    
    private void initializeCommandHandlers() {
        // Phone call commands
        commandHandlers.put("call", this::handleCallCommand);
        commandHandlers.put("phone", this::handleCallCommand);
        commandHandlers.put("dial", this::handleCallCommand);
        
        // Text message commands
        commandHandlers.put("text", this::handleTextCommand);
        commandHandlers.put("message", this::handleTextCommand);
        commandHandlers.put("sms", this::handleTextCommand);
        commandHandlers.put("send", this::handleTextCommand);
        
        // Camera commands
        commandHandlers.put("camera", this::handleCameraCommand);
        commandHandlers.put("photo", this::handleCameraCommand);
        commandHandlers.put("picture", this::handleCameraCommand);
        commandHandlers.put("selfie", this::handleCameraCommand);
        
        // App control commands
        commandHandlers.put("open", this::handleOpenAppCommand);
        commandHandlers.put("launch", this::handleOpenAppCommand);
        commandHandlers.put("start", this::handleOpenAppCommand);
        commandHandlers.put("close", this::handleCloseAppCommand);
        
        // System settings commands
        commandHandlers.put("volume", this::handleVolumeCommand);
        commandHandlers.put("brightness", this::handleBrightnessCommand);
        commandHandlers.put("wifi", this::handleWifiCommand);
        commandHandlers.put("bluetooth", this::handleBluetoothCommand);
        commandHandlers.put("airplane", this::handleAirplaneModeCommand);
        
        // Navigation commands
        commandHandlers.put("navigate", this::handleNavigateCommand);
        commandHandlers.put("directions", this::handleNavigateCommand);
        commandHandlers.put("map", this::handleNavigateCommand);
        
        // Information commands
        commandHandlers.put("time", this::handleTimeCommand);
        commandHandlers.put("date", this::handleDateCommand);
        commandHandlers.put("battery", this::handleBatteryCommand);
        commandHandlers.put("weather", this::handleWeatherCommand);
        
        // Device control commands
        commandHandlers.put("lock", this::handleLockCommand);
        commandHandlers.put("unlock", this::handleUnlockCommand);
        commandHandlers.put("home", this::handleHomeCommand);
        commandHandlers.put("back", this::handleBackCommand);
        commandHandlers.put("recent", this::handleRecentAppsCommand);
        
        // Notification commands
        commandHandlers.put("notifications", this::handleNotificationsCommand);
        commandHandlers.put("read", this::handleReadNotificationsCommand);
        
        // Emergency commands
        commandHandlers.put("emergency", this::handleEmergencyCommand);
        commandHandlers.put("help", this::handleHelpCommand);
    }
    
    public void processCommand(String command, CommandCallback callback) {
        if (command == null || command.trim().isEmpty()) {
            callback.onCommandFailed("Empty command received");
            return;
        }
        
        String normalizedCommand = command.toLowerCase().trim();
        if (BuildConfig.DEBUG) { Log.d(TAG, "Processing command: " + normalizedCommand); }

        
        // Parse command and extract parameters
        String[] words = normalizedCommand.split("\\s+");
        String primaryCommand = words[0];
        
        CommandHandler handler = commandHandlers.get(primaryCommand);
        if (handler != null) {
            handler.execute(normalizedCommand, words, callback);
        } else {
            // Try to find partial matches or context-based commands
            handleContextualCommand(normalizedCommand, callback);
        }
    }
    
    private void handleContextualCommand(String command, CommandCallback callback) {
        // Handle more complex natural language commands
        
        if (command.contains("call") || command.contains("phone")) {
            handleCallCommand(command, command.split("\\s+"), callback);
        } else if (command.contains("text") || command.contains("message")) {
            handleTextCommand(command, command.split("\\s+"), callback);
        } else if (command.contains("take") && (command.contains("photo") || command.contains("picture"))) {
            handleCameraCommand(command, command.split("\\s+"), callback);
        } else if (command.contains("open") || command.contains("launch")) {
            handleOpenAppCommand(command, command.split("\\s+"), callback);
        } else if (command.contains("what") && command.contains("time")) {
            handleTimeCommand(command, command.split("\\s+"), callback);
        } else if (command.contains("turn") && command.contains("volume")) {
            handleVolumeCommand(command, command.split("\\s+"), callback);
        } else {
            callback.onCommandFailed("Command not recognized: " + command);
            voiceEngine.voiceEngine.speak("Sorry, I didn't understand that command.");
        }
    }
    
    private void handleCallCommand(String command, String[] params, CommandCallback callback) {
        try {
            // Extract phone number or contact name
            String target = extractCallTarget(command);
            
            if (target.isEmpty()) {
                callback.onCommandFailed("No phone number or contact specified");
                voiceEngine.voiceEngine.speak("Please specify who you want to call");
                return;
            }
            
            // Check if it's a phone number or contact name
            if (isPhoneNumber(target)) {
                makeCall(target, callback);
            } else {
                // Look up contact and call
                systemController.callContact(target, callback);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling call command", e);
            callback.onCommandFailed("Failed to make call: " + e.getMessage());
            voiceEngine.speak("Unable to make the call");
        }
    }
    
    private String extractCallTarget(String command) {
        // Extract phone number or name from command
        Pattern phonePattern = Pattern.compile("\\b\\d{3}[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b");
        Matcher matcher = phonePattern.matcher(command);
        
        if (matcher.find()) {
            return matcher.group().replaceAll("[-.\\s]", "");
        }
        
        // Extract contact name (words after "call" or "phone")
        String[] words = command.split("\\s+");
        StringBuilder name = new StringBuilder();
        boolean foundCallKeyword = false;
        
        for (String word : words) {
            if (foundCallKeyword && !word.equals("number")) {
                name.append(word).append(" ");
            }
            if (word.equals("call") || word.equals("phone") || word.equals("dial")) {
                foundCallKeyword = true;
            }
        }
        
        return name.toString().trim();
    }
    
    private boolean isPhoneNumber(String text) {
        return text.matches("\\d{10,15}");
    }
    
    private void makeCall(String phoneNumber, CommandCallback callback) {
        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));
            callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            context.startActivity(callIntent);
            
            callback.onCommandExecuted("Calling " + phoneNumber);
            voiceEngine.voiceEngine.speak("Calling " + phoneNumber);
            
        } catch (SecurityException e) {
            callback.onCommandFailed("Permission denied to make phone calls");
            voiceEngine.voiceEngine.speak("I don't have permission to make phone calls");
        } catch (Exception e) {
            callback.onCommandFailed("Failed to make call: " + e.getMessage());
            voiceEngine.voiceEngine.speak("Unable to make the call");
        }
    }
    
    private void handleTextCommand(String command, String[] params, CommandCallback callback) {
        try {
            // Extract recipient and message
            String recipient = extractTextRecipient(command);
            String message = extractTextMessage(command);
            
            if (recipient.isEmpty()) {
                callback.onCommandFailed("No recipient specified for text message");
                voiceEngine.voiceEngine.speak("Please specify who you want to text");
                return;
            }
            
            if (message.isEmpty()) {
                callback.onCommandFailed("No message content specified");
                voiceEngine.voiceEngine.speak("Please specify what message to send");
                return;
            }
            
            sendTextMessage(recipient, message, callback);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling text command", e);
            callback.onCommandFailed("Failed to send text: " + e.getMessage());
            voiceEngine.voiceEngine.speak("Unable to send the text message");
        }
    }
    
    private String extractTextRecipient(String command) {
        // Similar logic to extractCallTarget but for text recipients
        Pattern phonePattern = Pattern.compile("\\b\\d{3}[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b");
        Matcher matcher = phonePattern.matcher(command);
        
        if (matcher.find()) {
            return matcher.group().replaceAll("[-.\\s]", "");
        }
        
        // Extract recipient name
        String[] words = command.split("\\s+");
        StringBuilder name = new StringBuilder();
        boolean foundTextKeyword = false;
        
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (foundTextKeyword && !word.equals("saying") && !word.equals("that")) {
                if (i < words.length - 1 && (words[i + 1].equals("saying") || words[i + 1].equals("that"))) {
                    name.append(word);
                    break;
                }
                name.append(word).append(" ");
            }
            if (word.equals("text") || word.equals("message") || word.equals("send")) {
                foundTextKeyword = true;
            }
        }
        
        return name.toString().trim();
    }
    
    private String extractTextMessage(String command) {
        // Extract message content after "saying" or "that"
        String[] keywords = {"saying", "that", "message"};
        
        for (String keyword : keywords) {
            int index = command.indexOf(keyword);
            if (index != -1) {
                return command.substring(index + keyword.length()).trim();
            }
        }
        
        return "";
    }
    
    private void sendTextMessage(String recipient, String message, CommandCallback callback) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            
            // If recipient is a name, resolve to phone number
            String phoneNumber = isPhoneNumber(recipient) ? recipient : systemController.getContactPhoneNumber(recipient);
            
            if (phoneNumber.isEmpty()) {
                callback.onCommandFailed("Could not find phone number for " + recipient);
                voiceEngine.voiceEngine.speak("Could not find phone number for " + recipient);
                return;
            }
            
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            
            callback.onCommandExecuted("Text sent to " + recipient);
            voiceEngine.voiceEngine.speak("Text message sent to " + recipient);
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending text message", e);
            callback.onCommandFailed("Failed to send text: " + e.getMessage());
            voiceEngine.voiceEngine.speak("Unable to send text message");
        }
    }
    
    private void handleCameraCommand(String command, String[] params, CommandCallback callback) {
        try {
            boolean frontCamera = command.contains("selfie") || command.contains("front");
            systemController.takePhoto(frontCamera, callback);
            voiceEngine.speak(frontCamera ? "Taking a selfie" : "Taking a photo");
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling camera command", e);
            callback.onCommandFailed("Failed to open camera: " + e.getMessage());
            voiceEngine.voiceEngine.speak("Unable to open the camera");
        }
    }
    
    private void handleOpenAppCommand(String command, String[] params, CommandCallback callback) {
        String appName = extractAppName(command);
        if (appName.isEmpty()) {
            callback.onCommandFailed("No app name specified");
            voiceEngine.voiceEngine.speak("Please specify which app to open");
            return;
        }
        
        systemController.openApp(appName, callback);
        voiceEngine.voiceEngine.speak("Opening " + appName);
    }
    
    private String extractAppName(String command) {
        String[] words = command.split("\\s+");
        StringBuilder appName = new StringBuilder();
        boolean foundOpenKeyword = false;
        
        for (String word : words) {
            if (foundOpenKeyword) {
                appName.append(word).append(" ");
            }
            if (word.equals("open") || word.equals("launch") || word.equals("start")) {
                foundOpenKeyword = true;
            }
        }
        
        return appName.toString().trim();
    }
    
    private void handleCloseAppCommand(String command, String[] params, CommandCallback callback) {
        // Close current app or specified app
        systemController.closeCurrentApp(callback);
        voiceEngine.voiceEngine.speak("Closing app");
    }
    
    private void handleVolumeCommand(String command, String[] params, CommandCallback callback) {
        try {
            if (command.contains("up") || command.contains("increase") || command.contains("higher")) {
                systemController.adjustVolume(true, callback);
                voiceEngine.voiceEngine.speak("Volume up");
            } else if (command.contains("down") || command.contains("decrease") || command.contains("lower")) {
                systemController.adjustVolume(false, callback);
                voiceEngine.voiceEngine.speak("Volume down");
            } else if (command.contains("mute")) {
                systemController.muteVolume(callback);
                voiceEngine.voiceEngine.speak("Volume muted");
            } else {
                callback.onCommandFailed("Volume command not recognized");
                voiceEngine.voiceEngine.speak("Please specify volume up, down, or mute");
            }
        } catch (Exception e) {
            callback.onCommandFailed("Failed to adjust volume: " + e.getMessage());
            voiceEngine.voiceEngine.speak("Unable to adjust volume");
        }
    }
    
    private void handleBrightnessCommand(String command, String[] params, CommandCallback callback) {
        try {
            if (command.contains("up") || command.contains("increase") || command.contains("brighter")) {
                systemController.adjustBrightness(true, callback);
                voiceEngine.voiceEngine.speak("Brightness increased");
            } else if (command.contains("down") || command.contains("decrease") || command.contains("dimmer")) {
                systemController.adjustBrightness(false, callback);
                voiceEngine.speak("Brightness decreased");
            } else {
                callback.onCommandFailed("Brightness command not recognized");
                voiceEngine.speak("Please specify brightness up or down");
            }
        } catch (Exception e) {
            callback.onCommandFailed("Failed to adjust brightness: " + e.getMessage());
            voiceEngine.speak("Unable to adjust brightness");
        }
    }
    
    private void handleWifiCommand(String command, String[] params, CommandCallback callback) {
        try {
            if (command.contains("on") || command.contains("enable")) {
                systemController.setWifi(true, callback);
                voiceEngine.speak("WiFi turned on");
            } else if (command.contains("off") || command.contains("disable")) {
                systemController.setWifi(false, callback);
                voiceEngine.speak("WiFi turned off");
            } else {
                systemController.toggleWifi(callback);
                voiceEngine.speak("WiFi toggled");
            }
        } catch (Exception e) {
            callback.onCommandFailed("Failed to control WiFi: " + e.getMessage());
            voiceEngine.speak("Unable to control WiFi");
        }
    }
    
    private void handleBluetoothCommand(String command, String[] params, CommandCallback callback) {
        try {
            if (command.contains("on") || command.contains("enable")) {
                systemController.setBluetooth(true, callback);
                voiceEngine.speak("Bluetooth turned on");
            } else if (command.contains("off") || command.contains("disable")) {
                systemController.setBluetooth(false, callback);
                voiceEngine.speak("Bluetooth turned off");
            } else {
                systemController.toggleBluetooth(callback);
                voiceEngine.speak("Bluetooth toggled");
            }
        } catch (Exception e) {
            callback.onCommandFailed("Failed to control Bluetooth: " + e.getMessage());
            voiceEngine.speak("Unable to control Bluetooth");
        }
    }
    
    private void handleAirplaneModeCommand(String command, String[] params, CommandCallback callback) {
        try {
            systemController.toggleAirplaneMode(callback);
            voiceEngine.speak("Airplane mode toggled");
        } catch (Exception e) {
            callback.onCommandFailed("Failed to toggle airplane mode: " + e.getMessage());
            voiceEngine.speak("Unable to toggle airplane mode");
        }
    }
    
    private void handleNavigateCommand(String command, String[] params, CommandCallback callback) {
        String destination = extractDestination(command);
        if (destination.isEmpty()) {
            callback.onCommandFailed("No destination specified");
            voiceEngine.speak("Please specify where you want to navigate to");
            return;
        }
        
        systemController.navigateToDestination(destination, callback);
        voiceEngine.speak("Opening navigation to " + destination);
    }
    
    private String extractDestination(String command) {
        String[] keywords = {"to", "navigate", "directions"};
        
        for (String keyword : keywords) {
            int index = command.indexOf(keyword);
            if (index != -1) {
                String remaining = command.substring(index + keyword.length()).trim();
                if (!remaining.isEmpty()) {
                    return remaining;
                }
            }
        }
        
        return "";
    }
    
    private void handleTimeCommand(String command, String[] params, CommandCallback callback) {
        String currentTime = systemController.getCurrentTime();
        callback.onCommandExecuted("Current time: " + currentTime);
        voiceEngine.speak("The current time is " + currentTime);
    }
    
    private void handleDateCommand(String command, String[] params, CommandCallback callback) {
        String currentDate = systemController.getCurrentDate();
        callback.onCommandExecuted("Current date: " + currentDate);
        voiceEngine.speak("Today's date is " + currentDate);
    }
    
    private void handleBatteryCommand(String command, String[] params, CommandCallback callback) {
        int batteryLevel = systemController.getBatteryLevel();
        String batteryStatus = "Battery level is " + batteryLevel + " percent";
        callback.onCommandExecuted(batteryStatus);
        speak(batteryStatus);
    }
    
    private void handleWeatherCommand(String command, String[] params, CommandCallback callback) {
        // This would typically require a weather API integration
        callback.onCommandExecuted("Weather information requires internet connection");
        voiceEngine.speak("Weather information is not available offline");
    }
    
    private void handleLockCommand(String command, String[] params, CommandCallback callback) {
        systemController.lockDevice(callback);
        voiceEngine.speak("Locking device");
    }
    
    private void handleUnlockCommand(String command, String[] params, CommandCallback callback) {
        // Unlock would typically require biometric authentication
        callback.onCommandFailed("Device unlock requires physical authentication");
        voiceEngine.speak("Please use your fingerprint or PIN to unlock");
    }
    
    private void handleHomeCommand(String command, String[] params, CommandCallback callback) {
        systemController.goHome(callback);
        voiceEngine.speak("Going to home screen");
    }
    
    private void handleBackCommand(String command, String[] params, CommandCallback callback) {
        systemController.goBack(callback);
        voiceEngine.speak("Going back");
    }
    
    private void handleRecentAppsCommand(String command, String[] params, CommandCallback callback) {
        systemController.showRecentApps(callback);
        voiceEngine.speak("Showing recent apps");
    }
    
    private void handleNotificationsCommand(String command, String[] params, CommandCallback callback) {
        systemController.showNotifications(callback);
        voiceEngine.speak("Showing notifications");
    }
    
    private void handleReadNotificationsCommand(String command, String[] params, CommandCallback callback) {
        systemController.readNotifications(new SystemController.NotificationCallback() {
            @Override
            public void onNotificationsRead(String notifications) {
                callback.onCommandExecuted("Notifications read");
                voiceEngine.speak("You have the following notifications: " + notifications);
            }
            
            @Override
            public void onNoNotifications() {
                callback.onCommandExecuted("No notifications");
                voiceEngine.speak("You have no new notifications");
            }
            
            @Override
            public void onError(String error) {
                callback.onCommandFailed("Failed to read notifications: " + error);
                voiceEngine.speak("Unable to read notifications");
            }
        });
    }
    
    private void handleEmergencyCommand(String command, String[] params, CommandCallback callback) {
        // Emergency call to 911 or local emergency number
        try {
            Intent emergencyIntent = new Intent(Intent.ACTION_CALL);
            emergencyIntent.setData(Uri.parse("tel:911"));
            emergencyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(emergencyIntent);
            
            callback.onCommandExecuted("Emergency call initiated");
            voiceEngine.speak("Calling emergency services");
            
        } catch (Exception e) {
            callback.onCommandFailed("Failed to make emergency call: " + e.getMessage());
            voiceEngine.speak("Unable to make emergency call");
        }
    }
    
    private void handleHelpCommand(String command, String[] params, CommandCallback callback) {
        String helpText = "Available commands include: call, text, camera, open app, volume, brightness, " +
                         "WiFi, Bluetooth, navigate, time, date, battery, lock, home, back, notifications, and emergency";
        callback.onCommandExecuted(helpText);
        voiceEngine.speak(helpText);
    }
    
    public void shutdown() {
        if (voiceEngine != null) {
            voiceEngine.shutdown();
        }
    }
}
