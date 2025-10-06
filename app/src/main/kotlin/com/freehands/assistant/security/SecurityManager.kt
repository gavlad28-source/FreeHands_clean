package com.freehands.assistant.security

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * SecurityManager handles app security features:
 * - APK signature verification
 * - Root detection
 * - Debugger detection
 * - Encryption key management
 */
class SecurityManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SecurityManager"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "FreeHandsSecretKey"
        
        // TODO: Update this with your actual release signature after building first release APK
        // Get it using: keytool -list -v -keystore your-release.keystore -alias your-key-alias
        private const val EXPECTED_SIGNATURE_SHA256 = "DEBUG_SIGNATURE_PLACEHOLDER"
        
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }
    
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }
    
    /**
     * Initialize security checks
     * @return true if all security checks pass
     */
    fun initialize(): Boolean {
        try {
            // Create encryption key if doesn't exist
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                generateEncryptionKey()
            }
            
            // Verify app signature
            val signatureValid = verifyAppSignature()
            if (!signatureValid) {
                Log.e(TAG, "⚠️ App signature verification failed! App may be tampered.")
                return false
            }
            
            // Check for root
            if (isDeviceRooted()) {
                Log.w(TAG, "⚠️ Device appears to be rooted. Some features may be disabled.")
            }
            
            // Check for debugger
            if (isDebuggerAttached()) {
                Log.w(TAG, "⚠️ Debugger detected!")
                if (!isDebugBuild()) {
                    // In release builds, exit if debugger attached
                    return false
                }
            }
            
            Log.i(TAG, "✓ Security initialization complete")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Security initialization failed", e)
            return false
        }
    }
    
    /**
     * Verify app signature matches expected value
     */
    fun verifyAppSignature(): Boolean {
        try {
            val signatures = getAppSignatures()
            if (signatures.isEmpty()) {
                Log.e(TAG, "No signatures found!")
                return false
            }
            
            for (signature in signatures) {
                val sha256 = calculateSHA256(signature.toByteArray())
                
                // In debug builds, just log and return true
                if (isDebugBuild()) {
                    Log.d(TAG, "Current signature SHA-256: $sha256")
                    Log.d(TAG, "Debug build - signature check bypassed")
                    return true
                }
                
                // In release builds, compare with expected signature
                if (sha256 == EXPECTED_SIGNATURE_SHA256) {
                    Log.i(TAG, "✓ Signature verified")
                    return true
                }
            }
            
            Log.e(TAG, "⚠️ Signature mismatch! App may be tampered.")
            return false
            
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying signature", e)
            return false
        }
    }
    
    /**
     * Get app signatures
     */
    private fun getAppSignatures(): List<Signature> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                packageInfo.signingInfo?.let { signingInfo ->
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners.toList()
                    } else {
                        signingInfo.signingCertificateHistory.toList()
                    }
                } ?: emptyList()
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                packageInfo.signatures.toList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting signatures", e)
            emptyList()
        }
    }
    
    /**
     * Calculate SHA-256 hash
     */
    private fun calculateSHA256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Check if device is rooted
     */
    fun isDeviceRooted(): Boolean {
        // Check for su binary
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        
        for (path in paths) {
            if (java.io.File(path).exists()) {
                return true
            }
        }
        
        // Check for common root packages
        val rootPackages = arrayOf(
            "com.noshufou.android.su",
            "com.thirdparty.superuser",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.zachspong.temprootremovejb",
            "com.ramdroid.appquarantine",
            "com.topjohnwu.magisk"
        )
        
        for (packageName in rootPackages) {
            try {
                context.packageManager.getPackageInfo(packageName, 0)
                return true
            } catch (e: PackageManager.NameNotFoundException) {
                // Package not found, continue
            }
        }
        
        return false
    }
    
    /**
     * Check if debugger is attached
     */
    fun isDebuggerAttached(): Boolean {
        return android.os.Debug.isDebuggerConnected() || 
               android.os.Debug.waitingForDebugger()
    }
    
    /**
     * Check if this is a debug build
     */
    private fun isDebugBuild(): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                0
            )
            (packageInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Generate encryption key in Android Keystore
     */
    private fun generateEncryptionKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
        
        Log.i(TAG, "✓ Encryption key generated")
    }
    
    /**
     * Get encryption key from keystore
     */
    private fun getEncryptionKey(): SecretKey {
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }
    
    /**
     * Encrypt data using AES-GCM
     * @param plaintext Data to encrypt
     * @return Base64 encoded "IV:Ciphertext"
     */
    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, getEncryptionKey())
        
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        
        // Combine IV and ciphertext
        val combined = iv + ciphertext
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }
    
    /**
     * Decrypt data using AES-GCM
     * @param encrypted Base64 encoded "IV:Ciphertext"
     * @return Decrypted plaintext
     */
    fun decrypt(encrypted: String): String {
        val combined = Base64.decode(encrypted, Base64.NO_WRAP)
        
        // Extract IV and ciphertext
        val iv = combined.copyOfRange(0, 12) // GCM standard IV size
        val ciphertext = combined.copyOfRange(12, combined.size)
        
        val cipher = Cipher.getInstance(AES_MODE)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getEncryptionKey(), spec)
        
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }
    
    /**
     * Securely wipe sensitive data from memory
     */
    fun wipeSensitiveData(data: ByteArray) {
        data.fill(0)
    }
    
    /**
     * Securely wipe sensitive data from memory
     */
    fun wipeSensitiveData(data: CharArray) {
        data.fill('0')
    }
}
