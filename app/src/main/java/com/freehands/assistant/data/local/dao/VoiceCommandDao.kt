package com.freehands.assistant.data.local.dao

import androidx.room.*
import com.freehands.assistant.data.local.entity.VoiceCommandEntity
import com.freehands.assistant.data.model.CommandType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [VoiceCommandEntity] operations.
 * Provides methods to access the voice_commands table in the database.
 */
@Dao
interface VoiceCommandDao {
    
    // --- Insert Operations ---
    
    /**
     * Inserts a new voice command into the database.
     * @param command The voice command to insert
     * @return The row ID of the inserted command
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: VoiceCommandEntity): Long
    
    /**
     * Inserts multiple voice commands in a single transaction.
     * @param commands The list of voice commands to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(commands: List<VoiceCommandEntity>)
    
    // --- Update Operations ---
    
    /**
     * Updates an existing voice command in the database.
     * @param command The voice command to update
     * @return The number of rows updated (should be 1)
     */
    @Update
    suspend fun updateCommand(command: VoiceCommandEntity): Int
    
    /**
     * Updates the last used timestamp for a command.
     * @param id The ID of the command to update
     * @param timestamp The new timestamp
     */
    @Query("UPDATE voice_commands SET lastUsed = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: Long, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Increments the use count for a command.
     * @param id The ID of the command to update
     */
    @Query("UPDATE voice_commands SET useCount = useCount + 1, lastUsed = :timestamp WHERE id = :id")
    suspend fun incrementUseCount(id: Long, timestamp: Long = System.currentTimeMillis())
    
    // --- Delete Operations ---
    
    /**
     * Deletes a voice command from the database.
     * @param command The voice command to delete
     * @return The number of rows deleted (should be 1)
     */
    @Delete
    suspend fun deleteCommand(command: VoiceCommandEntity)
    
    /**
     * Deletes a voice command by its ID.
     * @param id The ID of the command to delete
     */
    @Query("DELETE FROM voice_commands WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    /**
     * Deletes all voice commands from the database.
     */
    @Query("DELETE FROM voice_commands")
    suspend fun deleteAll()
    
    // --- Query Operations ---
    
    /**
     * Retrieves a voice command by its ID.
     * @param id The ID of the command to retrieve
     * @return The command with the given ID, or null if not found
     */
    @Query("SELECT * FROM voice_commands WHERE id = :id")
    suspend fun getCommandById(id: Long): VoiceCommandEntity?
    
    /**
     * Retrieves a voice command by its exact phrase match.
     * @param phrase The exact phrase to search for
     * @return The command with the given phrase, or null if not found
     */
    @Query("SELECT * FROM voice_commands WHERE LOWER(phrase) = LOWER(:phrase) LIMIT 1")
    suspend fun getCommandByPhrase(phrase: String): VoiceCommandEntity?
    
    /**
     * Searches for commands that match the given query.
     * @param query The search query
     * @return A list of matching commands, ordered by relevance
     */
    @Query("""
        SELECT * FROM voice_commands 
        WHERE phrase LIKE '%' || :query || '%' 
        ORDER BY 
            CASE 
                WHEN phrase = :query THEN 1
                WHEN phrase LIKE :query || '%' THEN 2
                WHEN phrase LIKE '%' || :query THEN 4
                ELSE 3
            END,
            useCount DESC,
            lastUsed DESC
        LIMIT 10
    """)
    suspend fun searchCommands(query: String): List<VoiceCommandEntity>
    
    /**
     * Retrieves all voice commands, ordered by most recently used.
     * @return A [Flow] emitting a list of all commands
     */
    @Query("SELECT * FROM voice_commands ORDER BY lastUsed DESC")
    fun getAllCommands(): Flow<List<VoiceCommandEntity>>
    
    /**
     * Retrieves voice commands of a specific type, ordered by most recently used.
     * @param type The type of commands to retrieve
     * @return A [Flow] emitting a list of matching commands
     */
    @Query("SELECT * FROM voice_commands WHERE action = :type ORDER BY lastUsed DESC")
    fun getCommandsByType(type: CommandType): Flow<List<VoiceCommandEntity>>
    
    /**
     * Retrieves all custom voice commands defined by the user.
     * @return A [Flow] emitting a list of custom commands
     */
    @Query("SELECT * FROM voice_commands WHERE isCustom = 1 ORDER BY lastUsed DESC")
    fun getCustomCommands(): Flow<List<VoiceCommandEntity>>
    
    /**
     * Checks if any command with the given ID exists.
     * @param id The ID to check
     * @return true if a command with the ID exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM voice_commands WHERE id = :id LIMIT 1)")
    suspend fun commandExists(id: Long): Boolean
    
    /**
     * Retrieves the most frequently used commands.
     * @param limit Maximum number of commands to return
     * @return A list of the most used commands
     */
    @Query("SELECT * FROM voice_commands WHERE useCount > 0 ORDER BY useCount DESC, lastUsed DESC LIMIT :limit")
    suspend fun getMostUsedCommands(limit: Int = 5): List<VoiceCommandEntity>
    
    /**
     * Retrieves recently used commands.
     * @param limit Maximum number of commands to return
     * @return A list of recently used commands
     */
    @Query("SELECT * FROM voice_commands WHERE lastUsed > 0 ORDER BY lastUsed DESC LIMIT :limit")
    suspend fun getRecentCommands(limit: Int = 5): List<VoiceCommandEntity>
    
    /**
     * Searches for commands that require confirmation.
     * @return A list of commands that require user confirmation
     */
    @Query("SELECT * FROM voice_commands WHERE needsConfirmation = 1 ORDER BY lastUsed DESC")
    suspend fun getCommandsRequiringConfirmation(): List<VoiceCommandEntity>
    
    /**
     * Counts the total number of commands in the database.
     * @return The total count of commands
     */
    @Query("SELECT COUNT(*) FROM voice_commands")
    suspend fun getCommandCount(): Int
    
    /**
     * Counts the number of commands of a specific type.
     * @param type The command type to count
     * @return The count of matching commands
     */
    @Query("SELECT COUNT(*) FROM voice_commands WHERE action = :type")
    suspend fun getCommandCountByType(type: CommandType): Int
    
    /**
     * Checks if a command with the given phrase already exists.
     * @param phrase The phrase to check
     * @return true if a command with the phrase exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM voice_commands WHERE LOWER(phrase) = LOWER(:phrase) LIMIT 1)")
    suspend fun commandExists(phrase: String): Boolean
    
    /**
     * Retrieves unprocessed voice commands, ordered by timestamp (oldest first).
     * @return A [Flow] emitting a list of unprocessed commands
     */
    @Query("""
        SELECT * FROM voice_commands 
        WHERE lastUsed = 0 
        ORDER BY lastUsed ASC
    """)
    fun getUnprocessed(): Flow<List<VoiceCommandEntity>>
    
    /**
     * Retrieves voice commands within a date range, ordered by timestamp (newest first).
     * @param startTime The start of the time range (inclusive)
     * @param endTime The end of the time range (inclusive)
     * @return A [Flow] emitting a list of commands within the time range
     */
    @Query("""
        SELECT * FROM voice_commands 
        WHERE lastUsed BETWEEN :startTime AND :endTime 
        ORDER BY lastUsed DESC
    """)
    fun getByDateRange(startTime: Long, endTime: Long): Flow<List<VoiceCommandEntity>>
    
    /**
     * Marks a command as processed.
     * @param id The ID of the command to mark as processed
     * @param result The processing result to store
     * @return The number of rows updated (should be 1)
     */
    @Query("""
        UPDATE voice_commands 
        SET useCount = useCount + 1, lastUsed = :timestamp 
        WHERE id = :id
    """)
    suspend fun markAsProcessed(id: Long, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Deletes commands older than the specified timestamp.
     * @param olderThan The timestamp to compare against
     * @return The number of rows deleted
     */
    @Query("DELETE FROM voice_commands WHERE lastUsed < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long): Int
}
