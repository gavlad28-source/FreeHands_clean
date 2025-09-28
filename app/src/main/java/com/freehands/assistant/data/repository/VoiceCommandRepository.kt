package com.freehands.assistant.data.repository

import com.freehands.assistant.data.local.dao.VoiceCommandDao
import com.freehands.assistant.data.local.entity.VoiceCommandEntity
import com.freehands.assistant.data.model.CommandType
import com.freehands.assistant.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing voice commands in the database.
 * Provides a clean API for data access to the rest of the application.
 */
@Singleton
class VoiceCommandRepository @Inject constructor(
    private val voiceCommandDao: VoiceCommandDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    // --- Command Management ---
    
    /**
     * Inserts a new voice command into the database.
     * @param command The command to insert
     * @return The ID of the inserted command
     */
    suspend fun insertCommand(command: VoiceCommandEntity): Long {
        return withContext(ioDispatcher) {
            voiceCommandDao.insertCommand(command)
        }
    }
    
    /**
     * Updates an existing voice command in the database.
     * @param command The command to update
     * @return The number of rows updated (should be 1)
     */
    suspend fun updateCommand(command: VoiceCommandEntity): Int {
        return withContext(ioDispatcher) {
            voiceCommandDao.updateCommand(command)
        }
    }
    
    /**
     * Deletes a voice command from the database.
     * @param command The command to delete
     * @return The number of rows deleted (should be 1)
     */
    suspend fun deleteCommand(command: VoiceCommandEntity): Int {
        return withContext(ioDispatcher) {
            voiceCommandDao.deleteCommand(command)
        }
    }
    
    /**
     * Deletes a voice command by its ID.
     * @param id The ID of the command to delete
     */
    suspend fun deleteCommandById(id: Long) {
        withContext(ioDispatcher) {
            voiceCommandDao.deleteById(id)
        }
    }
    
    // --- Query Methods ---
    
    /**
     * Retrieves a voice command by its ID.
     * @param id The ID of the command to retrieve
     * @return The command with the given ID, or null if not found
     */
    suspend fun getCommandById(id: Long): VoiceCommandEntity? {
        return withContext(ioDispatcher) {
            voiceCommandDao.getCommandById(id)
        }
    }
    
    /**
     * Retrieves all voice commands, ordered by most recently used.
     * @return A [Flow] emitting a list of all commands
     */
    fun getAllCommands(): Flow<List<VoiceCommandEntity>> {
        return voiceCommandDao.getAllCommands()
    }
    
    /**
     * Retrieves voice commands of a specific type, ordered by most recently used.
     * @param type The type of commands to retrieve
     * @return A [Flow] emitting a list of matching commands
     */
    fun getCommandsByType(type: CommandType): Flow<List<VoiceCommandEntity>> {
        return voiceCommandDao.getCommandsByType(type)
    }
    
    /**
     * Retrieves all custom voice commands defined by the user.
     * @return A [Flow] emitting a list of custom commands
     */
    fun getCustomCommands(): Flow<List<VoiceCommandEntity>> {
        return voiceCommandDao.getCustomCommands()
    }
    
    /**
     * Searches for commands that match the given query.
     * @param query The search query
     * @return A list of matching commands, ordered by relevance
     */
    suspend fun searchCommands(query: String): List<VoiceCommandEntity> {
        return withContext(ioDispatcher) {
            voiceCommandDao.searchCommands(query)
        }
    }
    
    /**
     * Retrieves the most frequently used commands.
     * @param limit Maximum number of commands to return
     * @return A list of the most used commands
     */
    suspend fun getMostUsedCommands(limit: Int = 5): List<VoiceCommandEntity> {
        return withContext(ioDispatcher) {
            voiceCommandDao.getMostUsedCommands(limit)
        }
    }
    
    /**
     * Retrieves recently used commands.
     * @param limit Maximum number of commands to return
     * @return A list of recently used commands
     */
    suspend fun getRecentCommands(limit: Int = 5): List<VoiceCommandEntity> {
        return withContext(ioDispatcher) {
            voiceCommandDao.getRecentCommands(limit)
        }
    }
    
    /**
     * Retrieves commands that require confirmation.
     * @return A list of commands that require user confirmation
     */
    suspend fun getCommandsRequiringConfirmation(): List<VoiceCommandEntity> {
        return withContext(ioDispatcher) {
            voiceCommandDao.getCommandsRequiringConfirmation()
        }
    }
    
    // --- Utility Methods ---
    
    /**
     * Increments the use count for a command.
     * @param id The ID of the command to update
     */
    suspend fun incrementUseCount(id: Long) {
        withContext(ioDispatcher) {
            voiceCommandDao.incrementUseCount(id)
        }
    }
    
    /**
     * Checks if a command with the given phrase already exists.
     * @param phrase The phrase to check
     * @return true if a command with the phrase exists, false otherwise
     */
    suspend fun commandExists(phrase: String): Boolean {
        return withContext(ioDispatcher) {
            voiceCommandDao.commandExists(phrase)
        }
    }
    
    /**
     * Counts the total number of commands in the database.
     * @return The total count of commands
     */
    suspend fun getCommandCount(): Int {
        return withContext(ioDispatcher) {
            voiceCommandDao.getCommandCount()
        }
    }
    
    /**
     * Counts the number of commands of a specific type.
     * @param type The command type to count
     * @return The count of matching commands
     */
    suspend fun getCommandCountByType(type: CommandType): Int {
        return withContext(ioDispatcher) {
            voiceCommandDao.getCommandCountByType(type)
        }
    }
}
