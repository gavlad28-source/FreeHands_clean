package com.freehands.assistant.utils

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class CrashReportingTree : Timber.Tree() {
    
    private val crashlytics = FirebaseCrashlytics.getInstance()
    
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.ERROR || priority == Log.WARN) {
            // Send crash reports for error and warning level logs
            crashlytics.log("$tag: $message")
            
            // Log exceptions to Crashlytics
            t?.let { crashlytics.recordException(it) }
        }
    }
}
