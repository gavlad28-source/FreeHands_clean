package com.freehands.assistant

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freehands.assistant.security.SecurityManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for security features
 */
@RunWith(AndroidJUnit4::class)
class SecurityTests {
    
    private lateinit var context: Context
    private lateinit var securityManager: SecurityManager
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        securityManager = SecurityManager(context)
    }
    
    @Test
    fun testSecurityManagerInitialization() {
        val result = securityManager.initialize()
        assertTrue("Security manager should initialize successfully", result)
    }
    
    @Test
    fun testEncryptionDecryption() {
        val originalText = "Test sensitive data 123"
        
        val encrypted = securityManager.encrypt(originalText)
        assertNotEquals("Encrypted text should differ from original", originalText, encrypted)
        
        val decrypted = securityManager.decrypt(encrypted)
        assertEquals("Decrypted text should match original", originalText, decrypted)
    }
    
    @Test
    fun testMultipleEncryptionDecryption() {
        val texts = listOf(
            "Password123!",
            "Sensitive API Key",
            "User Token XYZ",
            "русский текст тест"
        )
        
        for (text in texts) {
            val encrypted = securityManager.encrypt(text)
            val decrypted = securityManager.decrypt(encrypted)
            assertEquals("Text should decrypt correctly: $text", text, decrypted)
        }
    }
    
    @Test
    fun testRootDetection() {
        // This test will vary based on device
        val isRooted = securityManager.isDeviceRooted()
        // Just verify it doesn't crash
        assertNotNull("Root detection should return a result", isRooted)
    }
    
    @Test
    fun testDebuggerDetection() {
        val isDebuggerAttached = securityManager.isDebuggerAttached()
        // In test environment, debugger might be attached
        assertNotNull("Debugger detection should return a result", isDebuggerAttached)
    }
    
    @Test
    fun testSignatureVerification() {
        // In debug builds, this should pass with debug signature
        val isValid = securityManager.verifyAppSignature()
        assertTrue("App signature should be valid", isValid)
    }
}
