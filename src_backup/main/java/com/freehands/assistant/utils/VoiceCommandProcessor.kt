package com.freehands.assistant.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import com.freehands.assistant.VoiceViewModel.CommandResult
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceCommandProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val commandHandlers = mapOf<String, (List<String>) -> CommandResult>(
        "call" to ::handleCallCommand,
        "text" to ::handleTextCommand,
        "message" to ::handleTextCommand,
        "email" to ::handleEmailCommand,
        "set alarm" to ::handleAlarmCommand,
        "set timer" to ::handleTimerCommand,
        "create event" to ::handleCalendarEventCommand,
        "take a note" to ::handleNoteCommand,
        "open" to ::handleOpenAppCommand,
        "search" to ::handleSearchCommand,
        "navigate" to ::handleNavigationCommand,
        "play" to ::handlePlayMediaCommand
    )
    
    suspend fun processCommand(command: String): CommandResult {
        return try {
            val (action, params) = parseCommand(command)
            val handler = commandHandlers[action.lowercase()] 
                ?: return CommandResult.Error("I don't know how to handle '$action' command")
            
            handler(params)
        } catch (e: Exception) {
            Timber.e(e, "Error processing voice command")
            CommandResult.Error("Sorry, I couldn't process that command")
        }
    }
    
    private fun parseCommand(command: String): Pair<String, List<String>> {
        val parts = command.trim().split("\s+".toRegex())
        return when {
            parts.size >= 2 && parts[0].equals("set", ignoreCase = true) && 
                    (parts[1].equals("alarm", ignoreCase = true) || 
                     parts[1].equals("timer", ignoreCase = true)) -> {
                parts[0] + " " + parts[1] to parts.drop(2)
            }
            parts.size >= 3 && parts[0].equals("create", ignoreCase = true) && 
                    parts[1].equals("event", ignoreCase = true) -> {
                "create event" to parts.drop(2)
            }
            parts.size >= 4 && parts[0].equals("take", ignoreCase = true) && 
                    parts[1].equals("a", ignoreCase = true) && 
                    parts[2].equals("note", ignoreCase = true) -> {
                "take a note" to parts.drop(3)
            }
            else -> {
                parts.firstOrNull()?.lowercase() ?: "" to parts.drop(1)
            }
        }
    }
    
    private fun handleCallCommand(params: List<String>): CommandResult {
        if (params.isEmpty()) {
            return CommandResult.Error("Who would you like to call?")
        }
        
        val contactName = params.joinToString(" ")
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$contactName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            CommandResult.Success("Calling $contactName")
        } else {
            CommandResult.Error("No phone app found to make a call")
        }
    }
    
    private fun handleTextCommand(params: List<String>): CommandResult {
        if (params.size < 2) {
            return CommandResult.Error("Please specify both the contact and message")
        }
        
        // This is a simplified version - in a real app, you'd parse the contact name and message
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms:")
            putExtra("sms_body", params.drop(1).joinToString(" "))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            CommandResult.Success("Opening messages")
        } else {
            CommandResult.Error("No messaging app found")
        }
    }
    
    private fun handleEmailCommand(params: List<String>): CommandResult {
        if (params.isEmpty()) {
            return CommandResult.Error("Please specify the email recipient and subject")
        }
        
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, "")
            putExtra(Intent.EXTRA_TEXT, "")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            CommandResult.Success("Opening email")
        } else {
            CommandResult.Error("No email app found")
        }
    }
    
    private fun handleAlarmCommand(params: List<String>): CommandResult {
        if (params.isEmpty()) {
            return CommandResult.Error("Please specify the alarm time")
        }
        
        val time = params[0]
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_MESSAGE, "Voice Alarm")
            putExtra(AlarmClock.EXTRA_HOUR, parseHour(time))
            putExtra(AlarmClock.EXTRA_MINUTES, parseMinute(time))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            CommandResult.Success("Setting alarm for $time")
        } else {
            CommandResult.Error("No alarm app found")
        }
    }
    
    private fun handleTimerCommand(params: List<String>): CommandResult {
        if (params.isEmpty()) {
            return CommandResult.Error("Please specify the timer duration")
        }
        
        val duration = parseDuration(params[0])
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, duration)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Voice Timer")
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            CommandResult.Success("Setting timer for ${duration / 60} minutes")
        } else {
            CommandResult.Error("No timer app found")
        }
    }
    
    private fun handleCalendarEventCommand(params: List<String>): CommandResult {
        if (params.isEmpty()) {
            return CommandResult.Error("Please specify the event details")
        }
        
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, params.joinToString(" "))
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, System.currentTimeMillis() + 3600000) // 1 hour from now
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            CommandResult.Success("Creating calendar event")
        } else {
            CommandResult.Error("No calendar app found")
        }
    }
    
    private fun handleNoteCommand(params: List<String>): CommandResult {
        if (params.isEmpty()) {
            return CommandResult.Error("What would you like to note down?")
        }
        
        val note = params.joinToString(" ")
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "Note_${System.currentTimeMillis()}.txt")
            putExtra(Intent.EXTRA_TEXT, note)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            CommandResult.Success("Saving your note")
        } else {
            CommandResult.Error("No app found to save notes")
        }
    }
    
    private fun handleOpenAppCommand(params: List<String>): CommandResult {
        if (params.isEmpty()) {
            return CommandResult.Error("Which app would you like to open?")
        }
        
        val appName = params.joinToString(" ").lowercase()
        val intent = context.packageManager.getLaunchIntentForPackage("com.example.$appName")
            ?: return CommandResult.Error("I couldn't find the $appName app")
        
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
        return CommandResult.Success("Opening $appName")
    }
    
    private fun handleSearchCommand(params: List<String>): CommandResult {
        if (params.isEmpty()) {
            return CommandResult.Error("What would you like to search for?")
        }
        
        val query = params.joinToString(" ")
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra("query", query)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            CommandResult.Success("Searching for $query")
        } else {
            CommandResult.Error("No web browser found")
        }
    }
    
    private fun handleNavigationCommand(params: List<String>): CommandResult {
        if (params.isEmpty()) {
            return CommandResult.Error("Where would you like to navigate to?")
        }
        
        val location = params.joinToString(" ")
        val uri = Uri.parse("geo:0,0?q=" + Uri.encode(location))
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            CommandResult.Success("Navigating to $location")
        } else {
            val webIntent = Intent(
                Intent.ACTION_VIEW, 
                Uri.parse("http://maps.google.com/maps?q=$location")
            )
            context.startActivity(webIntent)
            CommandResult.Success("Opening maps in browser")
        }
    }
    
    private fun handlePlayMediaCommand(params: List<String>): CommandResult {
        if (params.isEmpty()) {
            return CommandResult.Error("What would you like to play?")
        }
        
        val query = params.joinToString(" ")
        val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE)
            putExtra(MediaStore.EXTRA_MEDIA_ARTIST, query)
            putExtra(SearchManager.QUERY, query)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            CommandResult.Success("Playing $query")
        } else {
            CommandResult.Error("No media player found")
        }
    }
    
    private fun parseHour(time: String): Int {
        // Simple parsing for demo - in a real app, use a proper time parser
        return try {
            time.split(":")[0].toInt()
        } catch (e: Exception) {
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        }
    }
    
    private fun parseMinute(time: String): Int {
        // Simple parsing for demo - in a real app, use a proper time parser
        return try {
            time.split(":").getOrNull(1)?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    private fun parseDuration(duration: String): Int {
        // Simple parsing for demo - in a real app, parse more formats
        return try {
            val minutes = duration.toInt()
            minutes * 60 // Convert to seconds
        } catch (e: Exception) {
            300 // Default to 5 minutes
        }
    }
}
