package com.freehands.assistant.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages storage and retrieval of voice feature profiles.
 */
@Singleton
class VoiceFeatureStorage @Inject constructor(
    private val context: Context,
    private val securityManager: SecurityManager
) {
    private val gson = Gson()
    private val profilesDir by lazy {
        File(context.filesDir, "voice_profiles").apply {
            if (!exists()) {
                mkdirs()
                // Set directory to private
                setReadable(false, false)
                setReadable(true, true)
                setWritable(false, false)
                setWritable(true, true)
                setExecutable(false, false)
            }
        }
    }
    
    /**
     * Saves a voice profile to internal storage.
     * @param userId The user ID associated with the profile
     * @param features The feature vector to save
     * @return true if successful, false otherwise
     */
    fun saveProfile(userId: String, features: FloatArray): Boolean {
        return try {
            // Encrypt the features before saving
            val featuresJson = gson.toJson(features.toList())
            val encryptedData = securityManager.encrypt(featuresJson)
            
            // Save to a file with restricted permissions
            val profileFile = File(profilesDir, "${userId}.json")
            profileFile.writeText(encryptedData)
            
            // Set file permissions to private
            profileFile.setReadable(false, false)
            profileFile.setReadable(true, true)
            profileFile.setWritable(false, false)
            profileFile.setWritable(true, true)
            
            true
        } catch (e: Exception) {
            Log.e("VoiceFeatureStorage", "Error saving voice profile", e)
            false
        }
    }
    
    /**
     * Loads a voice profile from internal storage.
     * @param userId The user ID associated with the profile
     * @return The feature vector or null if not found or error
     */
    fun loadProfile(userId: String): FloatArray? {
        return try {
            val profileFile = File(profilesDir, "${userId}.json")
            if (!profileFile.exists()) return null
            
            // Read and decrypt the features
            val encryptedData = profileFile.readText()
            val featuresJson = securityManager.decrypt(encryptedData) ?: return null
            
            // Parse the JSON array back to FloatArray
            val listType = object : TypeToken<List<Float>>() {}.type
            val floatList: List<Float> = gson.fromJson(featuresJson, listType)
            floatList.toFloatArray()
            
        } catch (e: Exception) {
            Log.e("VoiceFeatureStorage", "Error loading voice profile", e)
            null
        }
    }
    
    /**
     * Deletes a voice profile.
     * @param userId The user ID of the profile to delete
     * @return true if successful, false otherwise
     */
    fun deleteProfile(userId: String): Boolean {
        return try {
            val profileFile = File(profilesDir, "${userId}.json")
            if (profileFile.exists()) {
                profileFile.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e("VoiceFeatureStorage", "Error deleting voice profile", e)
            false
        }
    }
    
    /**
     * Checks if a profile exists for the given user ID.
     */
    fun profileExists(userId: String): Boolean {
        return File(profilesDir, "${userId}.json").exists()
    }
    
    /**
     * Gets a list of all saved profile user IDs.
     */
    fun listProfiles(): List<String> {
        return profilesDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.map { it.nameWithoutExtension } ?: emptyList()
    }
    
    /**
     * Backs up all voice profiles to the specified output stream.
     * @param outputStream The output stream to write the backup to
     * @return true if successful, false otherwise
     */
    fun backupProfiles(outputStream: OutputStream): Boolean {
        return try {
            val profiles = profilesDir.listFiles()
                ?.filter { it.extension == "json" }
                ?.associate { file ->
                    file.nameWithoutExtension to file.readText()
                } ?: emptyMap()
            
            val backupData = gson.toJson(profiles)
            outputStream.bufferedWriter().use { writer ->
                writer.write(backupData)
            }
            true
        } catch (e: Exception) {
            Log.e("VoiceFeatureStorage", "Error backing up profiles", e)
            false
        }
    }
    
    /**
     * Restores voice profiles from a backup input stream.
     * @param inputStream The input stream to read the backup from
     * @return true if successful, false otherwise
     */
    fun restoreProfiles(inputStream: InputStream): Boolean {
        return try {
            val backupData = inputStream.bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<String, String>>() {}.type
            val profiles = gson.fromJson<Map<String, String>>(backupData, type)
            
            // Clear existing profiles
            profilesDir.listFiles()?.forEach { it.delete() }
            
            // Restore profiles
            profiles.forEach { (userId, encryptedData) ->
                val profileFile = File(profilesDir, "${userId}.json")
                profileFile.writeText(encryptedData)
                
                // Set file permissions to private
                profileFile.setReadable(false, false)
                profileFile.setReadable(true, true)
                profileFile.setWritable(false, false)
                profileFile.setWritable(true, true)
            }
            
            true
        } catch (e: Exception) {
            Log.e("VoiceFeatureStorage", "Error restoring profiles", e)
            false
        }
    }
    
    /**
     * Exports a voice profile to a File object.
     * @param userId The user ID of the profile to export
     * @return File containing the exported profile, or null if failed
     */
    fun exportProfile(userId: String): File? {
        return try {
            val profileFile = File(profilesDir, "${userId}.json")
            if (!profileFile.exists()) return null
            
            // Create a temporary file for export
            val exportFile = File(context.cacheDir, "${userId}_voice_profile.fhp")
            profileFile.copyTo(exportFile, overwrite = true)
            exportFile
        } catch (e: Exception) {
            Log.e("VoiceFeatureStorage", "Error exporting profile", e)
            null
        }
    }
    
    /**
     * Imports a voice profile from a File object.
     * @param userId The user ID to associate with the imported profile
     * @param importFile The file containing the profile data
     * @return true if successful, false otherwise
     */
    fun importProfile(userId: String, importFile: File): Boolean {
        return try {
            if (!importFile.exists()) return false
            
            // Read the imported data
            val importedData = importFile.readText()
            
            // Verify the data is valid by trying to decrypt it
            val decrypted = securityManager.decrypt(importedData) ?: return false
            
            // Save the profile
            val profileFile = File(profilesDir, "${userId}.json")
            profileFile.writeText(importedData)
            
            // Set file permissions to private
            profileFile.setReadable(false, false)
            profileFile.setReadable(true, true)
            profileFile.setWritable(false, false)
            profileFile.setWritable(true, true)
            
            true
        } catch (e: Exception) {
            Log.e("VoiceFeatureStorage", "Error importing profile", e)
            false
        }
    }
}
