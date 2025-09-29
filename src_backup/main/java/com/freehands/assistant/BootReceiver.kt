package com.freehands.assistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import timber.log.Timber

/**
 * BroadcastReceiver that starts the VoiceListeningService when the device boots up.
 * This ensures that the voice assistant is always running in the background.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || 
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            
            Timber.d("Boot completed or app updated, starting voice service")
            
            // Start the voice listening service
            val serviceIntent = Intent(context, VoiceListeningService::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
