package com.freehands.assistant;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class VoiceAccessibilityService extends AccessibilityService {
    private static final String TAG = "VoiceAccessibilityService";
    private static VoiceAccessibilityService instance;
    
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        
        if (BuildConfig.DEBUG) { Log.d(TAG, "Voice Accessibility Service connected"); }

        
        // Configure accessibility service
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS |
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                    AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
        info.notificationTimeout = 100;
        
        setServiceInfo(info);
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Log accessibility events for debugging
        if (event != null) {
            if (BuildConfig.DEBUG) { Log.v(TAG, "Accessibility event: " + event.getEventType() + 
                  " from " + event.getPackageName()); }

        }
    }
    
    @Override
    public void onInterrupt() {
        if (BuildConfig.DEBUG) { Log.d(TAG, "Voice Accessibility Service interrupted"); }

    }
    
    public static VoiceAccessibilityService getInstance() {
        return instance;
    }
    
    /**
     * Perform global system actions
     */
    public boolean performGlobalAction(int action) {
        try {
            boolean result = super.performGlobalAction(action);
            if (BuildConfig.DEBUG) { Log.d(TAG, "Global action " + action + " performed: " + result); }

            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error performing global action " + action, e);
            return false;
        }
    }
    
    /**
     * Navigate to home screen
     */
    public void goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME);
    }
    
    /**
     * Navigate back
     */
    public void goBack() {
        performGlobalAction(GLOBAL_ACTION_BACK);
    }
    
    /**
     * Show recent apps
     */
    public void showRecentApps() {
        performGlobalAction(GLOBAL_ACTION_RECENTS);
    }
    
    /**
     * Show notifications
     */
    public void showNotifications() {
        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
    }
    
    /**
     * Lock the screen using device admin
     */
    public void lockScreen() {
        try {
            DevicePolicyManager devicePolicyManager = 
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            
            if (devicePolicyManager != null) {
                devicePolicyManager.lockNow();
                if (BuildConfig.DEBUG) { Log.d(TAG, "Screen locked successfully"); }

            } else {
                Log.w(TAG, "DevicePolicyManager not available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error locking screen", e);
        }
    }
    
    /**
     * Click on a specific UI element by text
     */
    public boolean clickByText(String text) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.w(TAG, "No root node available for clicking");
            return false;
        }
        
        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(text);
        for (AccessibilityNodeInfo node : nodes) {
            if (node.isClickable()) {
                boolean result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                if (BuildConfig.DEBUG) { Log.d(TAG, "Clicked on '" + text + "': " + result); }

                return result;
            }
        }
        
        Log.w(TAG, "No clickable node found with text: " + text);
        return false;
    }
    
    /**
     * Type text into the currently focused input field
     */
    public boolean typeText(String text) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            return false;
        }
        
        AccessibilityNodeInfo focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focusedNode != null && focusedNode.isEditable()) {
            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            boolean result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
            if (BuildConfig.DEBUG) { Log.d(TAG, "Typed text '" + text + "': " + result); }

            return result;
        }
        
        Log.w(TAG, "No focused editable field found for typing");
        return false;
    }
    
    /**
     * Scroll in a specific direction
     */
    public boolean scroll(boolean forward) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            return false;
        }
        
        int action = forward ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : 
                              AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;
        
        return performActionOnScrollableNode(rootNode, action);
    }
    
    private boolean performActionOnScrollableNode(AccessibilityNodeInfo node, int action) {
        if (node == null) {
            return false;
        }
        
        if (node.isScrollable()) {
            return node.performAction(action);
        }
        
        // Try child nodes
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (performActionOnScrollableNode(child, action)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get active notifications text
     */
    public String getActiveNotifications() {
        StringBuilder notificationText = new StringBuilder();
        
        try {
            // This would require NotificationListenerService
            // For now, return a placeholder
            notificationText.append("Notifications feature requires notification access permission");
            
        } catch (Exception e) {
            Log.e(TAG, "Error reading notifications", e);
        }
        
        return notificationText.toString();
    }
    
    /**
     * Find and interact with specific apps
     */
    public boolean openApp(String appName) {
        // Launch app by package name or search for it
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            return false;
        }
        
        // Look for app icons or labels containing the app name
        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(appName);
        for (AccessibilityNodeInfo node : nodes) {
            if (node.isClickable()) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
        
        return false;
    }
    
    /**
     * Handle phone calls through accessibility
     */
    public boolean answerCall() {
        return clickByText("Answer") || clickByText("Accept") || 
               performGlobalAction(GLOBAL_ACTION_HOME); // Fallback
    }
    
    public boolean endCall() {
        return clickByText("End call") || clickByText("Hang up") || 
               performGlobalAction(GLOBAL_ACTION_BACK);
    }
    
    /**
     * Navigate through system settings
     */
    public boolean openSettings(String settingName) {
        try {
            Intent settingsIntent = new Intent(android.provider.Settings.ACTION_SETTINGS);
            settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(settingsIntent);
            
            // Wait a moment for settings to load, then navigate
            new android.os.Handler().postDelayed(() -> {
                clickByText(settingName);
            }, 1000);
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error opening settings", e);
            return false;
        }
    }
    
    /**
     * Handle media controls
     */
    public boolean playPauseMedia() {
        return performGlobalAction(GLOBAL_ACTION_RECENTS); // This would need proper media control implementation
    }
    
    /**
     * Emergency functions
     */
    public boolean emergencyCall() {
        try {
            Intent emergencyIntent = new Intent(Intent.ACTION_CALL);
            emergencyIntent.setData(android.net.Uri.parse("tel:911"));
            emergencyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(emergencyIntent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error making emergency call", e);
            return false;
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        if (BuildConfig.DEBUG) { Log.d(TAG, "Voice Accessibility Service destroyed"); }

    }
}
