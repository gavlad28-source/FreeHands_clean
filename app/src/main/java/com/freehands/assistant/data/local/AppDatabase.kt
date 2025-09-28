package com.freehands.assistant.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.freehands.assistant.data.local.dao.VoiceCommandDao
import com.freehands.assistant.data.local.entity.VoiceCommandEntity
import com.freehands.assistant.data.model.CommandType
import com.freehands.assistant.utils.Converters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

/**
 * The Room database for the FreeHands Assistant app.
 * This database holds the voice commands and their metadata.
 */
@Database(
    entities = [
        VoiceCommandEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * Provides access to the VoiceCommandDao for database operations.
     */
    abstract fun voiceCommandDao(): VoiceCommandDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        private const val DATABASE_NAME = "freehands_voice_commands.db"
        
        /**
         * Gets the singleton instance of the database.
         * 
         * @param context The application context
         * @return The database instance
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate with some default commands
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                prePopulateDatabase(database.voiceCommandDao())
                            }
                        }
                    }
                })
                .addMigrations(
                    // Add migrations here when the database schema changes
                    // Example: MIGRATION_1_2, MIGRATION_2_3, etc.
                )
                .fallbackToDestructiveMigration() // For development only - remove in production
                .build()
                
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Pre-populates the database with some default voice commands.
         */
        private suspend fun prePopulateDatabase(dao: VoiceCommandDao) {
            val defaultCommands = listOf(
                VoiceCommandEntity(
                    phrase = "call {contact}",
                    action = CommandType.CALL,
                    target = "{contact}",
                    isCustom = false,
                    needsConfirmation = true,
                    useCount = 0
                ),
                VoiceCommandEntity(
                    phrase = "message {contact} {message}",
                    action = CommandType.MESSAGE,
                    target = "{contact}",
                    data = "{message}",
                    isCustom = false,
                    needsConfirmation = true,
                    useCount = 0
                ),
                VoiceCommandEntity(
                    phrase = "navigate to {location}",
                    action = CommandType.NAVIGATE,
                    target = "{location}",
                    isCustom = false,
                    needsConfirmation = true,
                    useCount = 0
                ),
                VoiceCommandEntity(
                    phrase = "play {song}",
                    action = CommandType.PLAY_MEDIA,
                    target = "{song}",
                    isCustom = false,
                    needsConfirmation = false,
                    useCount = 0
                ),
                VoiceCommandEntity(
                    phrase = "turn on wifi",
                    action = CommandType.ENABLE_FEATURE,
                    target = "wifi",
                    isCustom = false,
                    needsConfirmation = true,
                    useCount = 0
                ),
                VoiceCommandEntity(
                    phrase = "turn off wifi",
                    action = CommandType.DISABLE_FEATURE,
                    target = "wifi",
                    isCustom = false,
                    needsConfirmation = true,
                    useCount = 0
                ),
                VoiceCommandEntity(
                    phrase = "what can you do",
                    action = CommandType.HELP,
                    isCustom = false,
                    needsConfirmation = false,
                    useCount = 0
                )
            )
            
            // Only insert if the database is empty
            if (dao.getCommandCount() == 0) {
                dao.insertAll(defaultCommands)
            }
        }
                
                INSTANCE = instance
                instance
            }
        }
    }
}
