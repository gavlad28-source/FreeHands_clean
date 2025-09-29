package com.freehands.assistant.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.*
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for handling encryption and decryption operations.
 * Uses AES-256 in GCM mode for authenticated encryption.
 */
@Singleton
class EncryptionUtils @Inject constructor() {
    
    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_ALGORITHM = "AES"
        private const val KEY_SIZE = 256 // bits
        private const val GCM_IV_LENGTH = 12 // 12 bytes for GCM
        private const val GCM_TAG_LENGTH = 128 // 128-bit auth tag length
        private const val KEYSTORE_ALIAS = "FreeHands_Encryption_Key"
    }
    
    /**
     * Encrypts the given data using AES-256-GCM.
     * @param data The data to encrypt
     * @param key The encryption key (must be 32 bytes for AES-256)
     * @return Base64 encoded string containing IV + encrypted data + auth tag
     */
    fun encrypt(data: ByteArray, key: ByteArray): String {
        if (key.size != 32) {
            throw IllegalArgumentException("Key must be 32 bytes (256 bits) for AES-256")
        }
        
        val iv = ByteArray(GCM_IV_LENGTH).also { 
            SecureRandom().nextBytes(it)
        }
        
        val keySpec = SecretKeySpec(key, KEY_ALGORITHM)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec)
        
        val encrypted = cipher.doFinal(data)
        
        // Combine IV + encrypted data + auth tag
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }
    
    /**
     * Decrypts the given data using AES-256-GCM.
     * @param encryptedData Base64 encoded string containing IV + encrypted data + auth tag
     * @param key The encryption key (must be 32 bytes for AES-256)
     * @return The decrypted data
     */
    fun decrypt(encryptedData: String, key: ByteArray): ByteArray {
        if (key.size != 32) {
            throw IllegalArgumentException("Key must be 32 bytes (256 bits) for AES-256")
        }
        
        val combined = Base64.decode(encryptedData, Base64.NO_WRAP)
        
        // Extract IV and encrypted data + auth tag
        if (combined.size <= GCM_IV_LENGTH) {
            throw IllegalArgumentException("Invalid encrypted data format")
        }
        
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        
        val keySpec = SecretKeySpec(key, KEY_ALGORITHM)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec)
        
        return cipher.doFinal(encrypted)
    }
    
    /**
     * Generates a secure random encryption key.
     * @return A new 256-bit (32-byte) encryption key
     */
    fun generateKey(): ByteArray {
        val keyGen = KeyGenerator.getInstance(KEY_ALGORITHM)
        keyGen.init(KEY_SIZE, SecureRandom())
        return keyGen.generateKey().encoded
    }
    
    /**
     * Hashes the given data using SHA-256.
     * @param data The data to hash
     * @return The SHA-256 hash as a hex string
     */
    fun sha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.fold("") { str, it -> str + "%02x".format(it) }
    }
    
    /**
     * Generates a secure random salt.
     * @param size The size of the salt in bytes (default: 16 bytes)
     * @return A random salt
     */
    fun generateSalt(size: Int = 16): ByteArray {
        val salt = ByteArray(size)
        SecureRandom().nextBytes(salt)
        return salt
    }
    
    /**
     * Derives a key from a password using PBKDF2.
     * @param password The password
     * @param salt The salt
     * @param iterations Number of iterations (default: 100,000)
     * @param keyLength Desired key length in bits (default: 256)
     * @return The derived key
     */
    fun deriveKey(
        password: String,
        salt: ByteArray,
        iterations: Int = 100_000,
        keyLength: Int = 256
    ): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = javax.crypto.spec.PBEKeySpec(
            password.toCharArray(),
            salt,
            iterations,
            keyLength
        )
        return factory.generateSecret(spec).encoded
    }
    
    /**
     * Generates an RSA key pair for asymmetric encryption.
     * @param keySize Key size in bits (default: 2048)
     * @return A KeyPair containing public and private keys
     */
    fun generateRSAKeyPair(keySize: Int = 2048): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore"
        )
        
        val builder = KeyGenParameterSpec.Builder(
            "${KEYSTORE_ALIAS}_RSA_$keySize",
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).apply {
            setBlockModes(KeyProperties.BLOCK_MODE_ECB, KeyProperties.BLOCK_MODE_CBC)
            setEncryptionPaddings(
                KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1,
                KeyProperties.ENCRYPTION_PADDING_RSA_OAEP
            )
            setKeySize(keySize)
            setRandomizedEncryptionRequired(true)
        }
        
        keyPairGenerator.initialize(builder.build())
        return keyPairGenerator.generateKeyPair()
    }
    
    /**
     * Encrypts data using RSA public key.
     * @param data The data to encrypt
     * @param publicKey The RSA public key
     * @return The encrypted data as a Base64 string
     */
    fun rsaEncrypt(data: ByteArray, publicKey: PublicKey): String {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        
        val encrypted = cipher.doFinal(data)
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }
    
    /**
     * Decrypts data using RSA private key.
     * @param encryptedData The encrypted data as a Base64 string
     * @param privateKey The RSA private key
     * @return The decrypted data
     */
    fun rsaDecrypt(encryptedData: String, privateKey: PrivateKey): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        
        val decoded = Base64.decode(encryptedData, Base64.NO_WRAP)
        return cipher.doFinal(decoded)
    }
    
    /**
     * Creates a digital signature for the given data.
     * @param data The data to sign
     * @param privateKey The private key to sign with
     * @return The signature as a Base64 string
     */
    fun sign(data: ByteArray, privateKey: PrivateKey): String {
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(data)
        
        val signatureBytes = signature.sign()
        return Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
    }
    
    /**
     * Verifies a digital signature.
     * @param data The original data
     * @param signature The signature to verify (Base64 encoded)
     * @param publicKey The public key to verify with
     * @return true if the signature is valid, false otherwise
     */
    fun verify(data: ByteArray, signature: String, publicKey: PublicKey): Boolean {
        return try {
            val sig = Signature.getInstance("SHA256withRSA")
            sig.initVerify(publicKey)
            sig.update(data)
            
            val signatureBytes = Base64.decode(signature, Base64.NO_WRAP)
            sig.verify(signatureBytes)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Securely compares two byte arrays in constant time to prevent timing attacks.
     */
    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) {
            return false
        }
        
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        
        return result == 0
    }
}
