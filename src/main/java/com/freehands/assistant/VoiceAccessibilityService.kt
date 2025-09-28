package com.freehands.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import com.freehands.assistant.utils.VoiceCommandProcessor
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class VoiceAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var commandProcessor: VoiceCommandProcessor
    
    private var isServiceConnected = false
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.d("Accessibility service connected")
        isServiceConnected = true
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Handle accessibility events if needed
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // Handle window state changes
                val packageName = event.packageName?.toString()
                val className = event.className?.toString()
                Timber.d("Window changed to: $packageName/$className")
            }
            // Add more event types as needed
        }
    }
    
    override fun onInterrupt() {
        Timber.d("Accessibility service interrupted")
    }
    
    override fun onUnbind(intent: Intent?): Boolean {
        Timber.d("Accessibility service unbound")
        isServiceConnected = false
        return super.onUnbind(intent)
    }
    
    fun performGlobalAction(action: Int): Boolean {
        return when (action) {
            GLOBAL_ACTION_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            GLOBAL_ACTION_HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            GLOBAL_ACTION_RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            GLOBAL_ACTION_NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            GLOBAL_ACTION_QUICK_SETTINGS -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            else -> false
        }
    }
    
    fun performClick(node: AccessibilityNodeInfo?): Boolean {
        node?.let {
            if (it.isClickable) {
                it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            } else {
                // Try to find a clickable parent
                var parent = it.parent
                while (parent != null) {
                    if (parent.isClickable) {
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return true
                    }
                    parent = parent.parent
                }
            }
        }
        return false
    }
    
    fun performSwipe(startX: Int, startY: Int, endX: Int, endY: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val gestureBuilder = GestureDescription.Builder()
            val path = Path().apply {
                moveTo(startX.toFloat(), startY.toFloat())
                lineTo(endX.toFloat(), endY.toFloat())
            }
            
            val gesture = gestureBuilder
                .addStroke(GestureDescription.StrokeDescription(
                    path, 
                    0, 
                    500 // Duration in ms
                ))
                .build()
                
            dispatchGesture(gesture, null, null)
        }
    }
    
    fun performScroll(node: AccessibilityNodeInfo?, direction: Int): Boolean {
        return node?.let {
            when (direction) {
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> 
                    it.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> 
                    it.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                else -> false
            }
        } ?: false
    }
    
    fun findNodeByText(text: String): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        return nodes.firstOrNull()
    }
    
    fun findNodeById(id: String): AccessibilityNodeInfo? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            val rootNode = rootInActiveWindow ?: return null
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
            return nodes.firstOrNull()
        }
        return null
    }
    
    fun typeText(node: AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) return false
        
        // Focus the node first
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        
        // For Android 21+ we can use ACTION_SET_TEXT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val args = android.os.Bundle()
            args.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, 
                text
            )
            return node.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT, 
                args
            )
        } else {
            // For older versions, we need to simulate key events
            // This is a simplified version - in a real app, you'd need to handle different characters
            for (c in text) {
                val keyCode = KeyEvent.keyCodeFromString("KEYCODE_$c")
                val eventDown = KeyEvent(
                    KeyEvent.ACTION_DOWN,
                    keyCode
                )
                val eventUp = KeyEvent(
                    KeyEvent.ACTION_UP,
                    keyCode
                )
                dispatchKeyEvent(eventDown)
                dispatchKeyEvent(eventUp)
            }
            return true
        }
    }
    
    fun isServiceEnabled(): Boolean {
        return isServiceConnected
    }
    
    companion object {
        private const val TAG = "VoiceAccessibilityService"
    }
}
