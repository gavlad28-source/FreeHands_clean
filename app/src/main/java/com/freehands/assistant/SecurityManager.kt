package com.freehands.assistant

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles encryption, decryption, and secure storage of sensitive data.
 */
@Singleton
class SecurityManager @Inject constructor(
    private val context: Context
) {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    private val secretKey: SecretKey by lazy {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        
        if (!keyStore.containsAlias(ENCRYPTION_KEY_ALIAS)) {
            generateSecretKey()
        }
        
        val entry = keyStore.getEntry(ENCRYPTION_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            ?: throw IllegalStateException("Failed to load secret key")
        
        entry.secretKey
    }
    
    private fun generateSecretKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                ENCRYPTION_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        )
        
        keyGenerator.generateKey()
    }
    
    fun encrypt(data: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        
        // Combine IV and encrypted data
        val result = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(encrypted, 0, result, iv.size, encrypted.size)
        
        return Base64.encodeToString(result, Base64.NO_WRAP)
    }
    
    fun decrypt(encryptedData: String): String {
        val decoded = Base64.decode(encryptedData, Base64.NO_WRAP)
        
        // Extract IV and encrypted data
        val iv = decoded.copyOfRange(0, IV_LENGTH)
        val encrypted = decoded.copyOfRange(IV_LENGTH, decoded.size)
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }
    
    fun saveSecureString(key: String, value: String) {
        try {
            val encrypted = encrypt(value)
            encryptedPrefs.edit().putString(key, encrypted).apply()
        } catch (e: Exception) {
            Timber.e(e, "Error saving secure string")
            throw SecurityException("Failed to save secure data", e)
        }
    }
    
    fun getSecureString(key: String, defaultValue: String? = null): String? {
        return try {
            val encrypted = encryptedPrefs.getString(key, null) ?: return defaultValue
            decrypt(encrypted)
        } catch (e: Exception) {
            Timber.e(e, "Error retrieving secure string")
            defaultValue
        }
    }
    
    fun removeSecureString(key: String) {
        encryptedPrefs.edit().remove(key).apply()
    }
    
    fun clearAllSecureData() {
        encryptedPrefs.edit().clear().apply()
    }
    
    fun isBiometricAuthAvailable(): Boolean {
        // Implementation depends on your biometric authentication requirements
        return true
    }
    
    companion object {
        private const val MASTER_KEY_ALIAS = "_freehands_master_key"
        private const val ENCRYPTED_PREFS_NAME = "secure_prefs"
        private const val ENCRYPTION_KEY_ALIAS = "freehands_encryption_key"
        private const val IV_LENGTH = 12 // 12 bytes for GCM
    }
}
