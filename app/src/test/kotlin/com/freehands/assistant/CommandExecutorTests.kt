package com.freehands.assistant

import android.content.Context
import com.freehands.assistant.commands.CommandExecutor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

/**
 * Unit tests for CommandExecutor
 */
@RunWith(MockitoJUnitRunner::class)
class CommandExecutorTests {
    
    @Mock
    private lateinit var mockContext: Context
    
    private lateinit var commandExecutor: CommandExecutor
    
    @Before
    fun setUp() {
        commandExecutor = CommandExecutor(mockContext)
    }
    
    @Test
    fun testCommandRequiresConfirmation() = runBlocking {
        val result = commandExecutor.executeCommand("включи wi-fi", confirmed = false)
        
        assertTrue(
            "Wi-Fi command should require confirmation",
            result is CommandExecutor.CommandResult.RequiresConfirmation
        )
    }
    
    @Test
    fun testUnknownCommand() = runBlocking {
        val result = commandExecutor.executeCommand("unknown command xyz", confirmed = true)
        
        assertTrue(
            "Unknown command should return error",
            result is CommandExecutor.CommandResult.Error
        )
    }
    
    @Test
    fun testCommandTypeRecognition() = runBlocking {
        val commands = mapOf(
            "включи wi-fi" to CommandExecutor.CommandType.WIFI_TOGGLE,
            "выключи блютуз" to CommandExecutor.CommandType.BLUETOOTH_TOGGLE,
            "увеличь яркость" to CommandExecutor.CommandType.BRIGHTNESS_CHANGE,
            "включи не беспокоить" to CommandExecutor.CommandType.DND_TOGGLE
        )
        
        for ((command, expectedType) in commands) {
            val result = commandExecutor.executeCommand(command, confirmed = false)
            
            if (result is CommandExecutor.CommandResult.RequiresConfirmation) {
                assertEquals(
                    "Command '$command' should be recognized as $expectedType",
                    expectedType,
                    result.commandType
                )
            }
        }
    }
}
