package com.freehands.assistant

import android.content.Context
import android.os.Build
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.freehands.assistant.utils.EncryptionUtils
import timber.log.Timber
import java.security.KeyStore
import java.security.Signature
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles biometric authentication including voice, fingerprint, and face recognition.
 */
@Singleton
class VoiceBiometricAuthenticator @Inject constructor(
    private val context: Context,
    private val voiceProfileManager: VoiceProfileManager,
    private val securityManager: SecurityManager
) {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val keyAlias = "biometric_encryption_key"
    
    private val biometricManager = BiometricManager.from(context)
    private var biometricPrompt: BiometricPrompt? = null
    
    /**
     * Checks if biometric authentication is available on the device.
     */
    fun isBiometricAvailable(): Boolean {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> false
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> false
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> false
            else -> false
        }
    }
    
    /**
     * Authenticates the user using biometrics.
     * @param activity The host activity
     * @param title Dialog title
     * @param subtitle Dialog subtitle
     * @param callback Callback for authentication result
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Authenticate",
        subtitle: String = "Use your biometric credential",
        callback: (Result<AuthResult>) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        
        biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    callback(Result.failure(Exception("Authentication error: $errString")))
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val cryptoObject = result.cryptoObject
                    val authResult = if (cryptoObject != null) {
                        // Handle encryption/decryption with the crypto object
                        AuthResult.Success(cryptoObject)
                    } else {
                        // Basic authentication success
                        AuthResult.Success()
                    }
                    callback(Result.success(authResult))
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    callback(Result.failure(Exception("Authentication failed")))
                }
            })
        
        // Create a crypto object for the authentication
        val cryptoObject = createCryptoObject()
        
        // Start authentication
        cryptoObject?.let {
            biometricPrompt?.authenticate(promptInfo, it)
        } ?: run {
            biometricPrompt?.authenticate(promptInfo)
        }
    }
    
    /**
     * Authenticates the user using voice recognition.
     * @param userId The user ID to authenticate
     * @param callback Callback with authentication result and confidence score
     */
    fun authenticateWithVoice(userId: String, callback: (Result<Pair<Boolean, Float>>) -> Unit) {
        voiceProfileManager.verifyVoiceProfile(userId) { result ->
            callback(result)
        }
    }
    
    /**
     * Creates a cryptographic object for secure authentication.
     */
    private fun createCryptoObject(): BiometricPrompt.CryptoObject? {
        return try {
            // Try to create a signature object
            val signature = Signature.getInstance("SHA256withECDSA")
            val key = getOrCreateKey()
            signature.initSign(key)
            BiometricPrompt.CryptoObject(signature)
        } catch (e: Exception) {
            Timber.e(e, "Error creating crypto object")
            null
        }
    }
    
    /**
     * Gets an existing key or creates a new one if it doesn't exist.
     */
    private fun getOrCreateKey(): SecretKey {
        return if (keyStore.containsAlias(keyAlias)) {
            // Key already exists, retrieve it
            val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
            entry?.secretKey ?: generateNewKey()
        } else {
            // Generate a new key
            generateNewKey()
        }
    }
    
    /**
     * Generates a new cryptographic key for biometric authentication.
     */
    private fun generateNewKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        
        val builder = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).apply {
            setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            setKeySize(256)
            setUserAuthenticationRequired(true)
            
            // Invalidate the key if the user registers a new biometric credential
            setInvalidatedByBiometricEnrollment(true)
            
            // Set the user authentication validity period (in seconds)
            // 0 means the key is only valid immediately after authentication
            setUserAuthenticationValidityDurationSeconds(0)
            
            // For devices with Android 8.0 (API level 26) and higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                setUnlockedDeviceRequired(true)
            }
        }
        
        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }
    
    /**
     * Encrypts data using biometric authentication.
     */
    suspend fun encryptWithBiometric(data: String): String? {
        return try {
            val cipher = Cipher.getInstance(
                "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/" +
                        "${KeyProperties.ENCRYPTION_PADDING_NONE}"
            )
            
            val key = getOrCreateKey()
            cipher.init(Cipher.ENCRYPT_MODE, key)
            
            val iv = cipher.iv
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            
            // Combine IV and encrypted data
            val result = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, result, 0, iv.size)
            System.arraycopy(encrypted, 0, result, iv.size, encrypted.size)
            
            Base64.encodeToString(result, Base64.NO_WRAP)
        } catch (e: Exception) {
            Timber.e(e, "Error encrypting with biometric")
            null
        }
    }
    
    /**
     * Decrypts data using biometric authentication.
     */
    suspend fun decryptWithBiometric(encryptedData: String): String? {
        return try {
            val decoded = Base64.decode(encryptedData, Base64.NO_WRAP)
            
            // Extract IV and encrypted data
            val iv = decoded.copyOfRange(0, 12) // 12 bytes for GCM
            val encrypted = decoded.copyOfRange(12, decoded.size)
            
            val cipher = Cipher.getInstance(
                "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/" +
                        "${KeyProperties.ENCRYPTION_PADDING_NONE}"
            )
            
            val key = getOrCreateKey()
            val spec = GCMParameterSpec(128, iv)
            
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            Timber.e(e, "Error decrypting with biometric")
            null
        }
    }
    
    /**
     * Sealed class representing authentication results.
     */
    sealed class AuthResult {
        data class Success(val cryptoObject: BiometricPrompt.CryptoObject? = null) : AuthResult()
        data class Error(val message: String) : AuthResult()
    }
    
    companion object {
        private const val TAG = "BiometricAuth"
    }
}
