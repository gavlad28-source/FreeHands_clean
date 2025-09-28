package com.freehands.assistant.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.freehands.assistant.data.service.VoiceAssistantService
import com.freehands.assistant.utils.AppConstants
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Start the voice assistant service on boot
            val serviceIntent = Intent(context, VoiceAssistantService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
